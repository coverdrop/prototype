import os

from nacl.encoding import HexEncoder
from nacl.public import Box, PublicKey, PrivateKey, SealedBox
from nacl.signing import SigningKey, SignedMessage, VerifyKey

#
# Helper methods
#


def to_hex(bytes):
    return HexEncoder().encode(bytes).decode()


def from_hex(string):
    return HexEncoder().decode(string)

#
# Read/write/create keys
#


def get_private_keys_path():
    return '.'


def get_public_keys_path():
    return os.path.join('..', os.path.join('WebApi', 'keys'))


def get_reporter_key_path(rid):
    folder = os.path.join('..', os.path.join('WebApi', 'keys'))
    return [os.path.join(folder, x) for x in ('reporter_%d_private.hex', 'sgx_key.hex')]


def load_priv_key(path):
    with open(path, 'r') as f:
        return PrivateKey(f.readline(), encoder=HexEncoder)


def read_pub_key(hex_string):
    return PublicKey(hex_string.encode(), encoder=HexEncoder)


def load_sign_key(path):
    with open(path, 'r') as f:
        return SigningKey(f.readline(), encoder=HexEncoder)


def read_verify_key(hex_string):
    return VerifyKey(hex_string.encode(), encoder=HexEncoder)


def create_keypair():
    sk = PrivateKey.generate()
    return sk, sk.public_key


def create_sign_keypair():
    sk = SigningKey.generate()
    return sk, sk.verify_key


#
# Per entity operations
#


def user_encrypt_message(user_pub, sgx_pub, reporter_pub, text, real=True):
    MAX_TEXT_LEN = 255
    text_bytes = text.encode()
    assert len(text_bytes) <= MAX_TEXT_LEN

    # Inner payload contains both the user payload (so the reporter can
    # encrypt messages to them), the size of the message, the message
    # and padding.
    inner_payload = bytearray()
    inner_payload += user_pub.__bytes__()
    inner_payload.append(len(text))
    inner_payload += text_bytes
    for _ in range(MAX_TEXT_LEN - len(text_bytes)):
        inner_payload.append(0x00)

    # Encrypt for the reporter
    inner_box = SealedBox(reporter_pub)
    inner_ciphertext = inner_box.encrypt(bytes(inner_payload))

    # Outer payload (for the SGX) contains the real/dummy flag
    # and the inner ciphertext
    outer_payload = bytearray()
    outer_payload.append(0x01 if real else 0x00)
    outer_payload += inner_ciphertext

    # Encrypt for the SGX
    outer_box = SealedBox(sgx_pub)
    outer_ciphertext = outer_box.encrypt(bytes(outer_payload))

    assert len(outer_ciphertext) == 385, f"got: {len(outer_ciphertext)}"
    return outer_ciphertext


def sgx_decrypt_packet_from_user(sgx_priv, packet):
    unseal_box = SealedBox(sgx_priv)
    payload = unseal_box.decrypt(packet)
    assert len(payload) == 337, f"got: {len(payload)}"

    is_real_message = payload[0] == 0x01
    return is_real_message, payload[1:]


def sgx_sign_packet_to_reporter(sgx_sign_priv, packet):
    signed_packet = sgx_sign_priv.sign(packet)
    assert len(signed_packet) == 400, f"got: {len(signed_packet)}"
    return signed_packet


def reporter_decrypt_packet_from_sgx(reporter_priv, sgx_sign_pub, packet):
    packet = sgx_sign_pub.verify(packet)

    # Extract the remote public key and message
    unseal_box = SealedBox(reporter_priv)
    payload = unseal_box.decrypt(packet)
    pub_key = payload[0:32]
    size = payload[32]
    message = payload[33:33+size]

    return PublicKey(pub_key), message


def reporter_encrypt_message_to_user(reporter_priv, sgx_pub, remote_pub, text, real=True):
    MAX_TEXT_LEN = 255
    text_bytes = text.encode()
    assert len(text_bytes) <= MAX_TEXT_LEN

    # The inner payload contains the size of the message, the message, and
    # the padding. Note that we do not need to include our public key, because
    # the reporter public key is published
    inner_payload = bytearray()
    inner_payload.append(len(text))
    inner_payload += text_bytes
    for _ in range(MAX_TEXT_LEN - len(text_bytes)):
        inner_payload.append(0x00)

    # We can use the proper "Box" instead of the "SealedBox" since the reporter (and
    # the recipient) now both sides public keys now.
    inner_box = Box(reporter_priv, remote_pub)
    inner_ciphertext = inner_box.encrypt(bytes(inner_payload))

    outer_payload = bytearray()
    outer_payload.append(0x01 if real else 0x00)
    outer_payload += inner_ciphertext

    outer_box = SealedBox(sgx_pub)
    outer_ciphertext = outer_box.encrypt(bytes(outer_payload))

    assert len(outer_ciphertext) == 345, f"got: {len(outer_ciphertext)}"
    return outer_ciphertext


def sgx_decrypt_packet_from_reporter(sgx_priv, packet):
    unseal_box = SealedBox(sgx_priv)
    payload = unseal_box.decrypt(packet)
    assert len(payload) == 297, f"got: {len(payload)}"

    is_real_message = payload[0] == 0x01
    return is_real_message, payload[1:]


def sgx_sign_packet_to_user(sgx_sign_priv, packet):
    signed_packet = sgx_sign_priv.sign(packet)
    assert len(signed_packet) == 360, f"got: {len(signed_packet)}"
    return signed_packet


def user_decrypt_packet_from_sgx(user_priv, reporter_pub, sgx_sign_pub, packet):
    packet = sgx_sign_pub.verify(packet)

    unseal_box = Box(user_priv, reporter_pub)
    payload = unseal_box.decrypt(packet)
    size = payload[0]
    message = payload[1:1+size]

    return message


def output(identifier, bytes):
    hexstring = HexEncoder().encode(bytes).decode()
    print(identifier + ":", hexstring, "(%dB)" % len(bytes))


if __name__ == "__main__":
    print("-- keys --")

    sgx_priv, sgx_pub = create_keypair()
    sgx_sign_priv, sgx_sign_pub = create_sign_keypair()
    output("sgx_priv", sgx_priv.__bytes__())
    output("sgx_pub", sgx_pub.__bytes__())
    output("sgx_sign_priv", sgx_sign_priv.__bytes__())
    output("sgx_sign_pub", sgx_sign_pub.__bytes__())

    reporter_priv, reporter_pub = create_keypair()
    output("reporter_priv", reporter_priv.__bytes__())
    output("reporter_pub", reporter_pub.__bytes__())

    user_priv, user_pub = create_keypair()
    output("user_priv", user_priv.__bytes__())
    output("user_pub", user_pub.__bytes__())

    print()
    print("-- from user to reporter --")

    user_message = "Hello World!"
    packet_user_to_sgx = user_encrypt_message(
        user_pub,
        sgx_pub,
        reporter_pub,
        user_message)
    output("packet_user_to_sgx", packet_user_to_sgx)

    sgx_internal_is_real, sgx_internal_payload = sgx_decrypt_packet_from_user(
        sgx_priv,
        packet_user_to_sgx)
    output("sgx_internal_payload", sgx_internal_payload)
    print("is real message:", sgx_internal_is_real)

    packet_sgx_to_reporter = sgx_sign_packet_to_reporter(
        sgx_sign_priv,
        sgx_internal_payload)
    output("packet_sgx_to_reporter", packet_sgx_to_reporter)

    remote_pub, remote_message = reporter_decrypt_packet_from_sgx(
        reporter_priv,
        sgx_sign_pub,
        packet_sgx_to_reporter)
    output("remote_pub", remote_pub.__bytes__())
    print("remote_remote_message:", remote_message)
    assert user_message == remote_message.decode('ascii')

    print()
    print("-- from reporter to user --")

    reporter_message = "Wassup?"
    packet_reporter_to_sgx = reporter_encrypt_message_to_user(
        reporter_priv,
        sgx_pub,
        remote_pub,
        reporter_message)
    output("packet_reporter_to_sgx:", packet_reporter_to_sgx)

    sgx_internal_is_real_2, sgx_internal_payload_2 = sgx_decrypt_packet_from_reporter(
        sgx_priv,
        packet_reporter_to_sgx
    )
    output("sgx_internal_payload_2", sgx_internal_payload_2)
    print("is real message:", sgx_internal_is_real_2)

    packet_sgx_to_user = sgx_sign_packet_to_user(
        sgx_sign_priv,
        sgx_internal_payload_2)
    output("packet_sgx_to_user", packet_sgx_to_user)

    user_remote_message = user_decrypt_packet_from_sgx(
        user_priv,
        reporter_pub,
        sgx_sign_pub,
        packet_sgx_to_user
    )
    print("user_remote_message:", user_remote_message)
    assert reporter_message == user_remote_message.decode('ascii')
