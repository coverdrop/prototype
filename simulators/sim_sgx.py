import argparse
import crypto
import json
import os
import time
import requests

from nacl.encoding import HexEncoder
from nacl.public import SealedBox

AUTH_HEADERS_SGX = {'Authorization': 'Token sgx_token'}
JSON_HEADERS = {'Content-Type': 'application/json'}


def get_json(args, path, auth_headers=AUTH_HEADERS_SGX):
    print("[ ] GET", path)
    resp = requests.get(args.url + path, headers=auth_headers)
    resp.raise_for_status()
    return resp.json()


def post(args, path, data, auth_headers=AUTH_HEADERS_SGX):
    print("[ ] POST", path)
    headers =  headers={**auth_headers, **JSON_HEADERS}
    resp = requests.post(
        args.url + path, 
        data=json.dumps(data),
       headers=headers)
    resp.raise_for_status()


def delete(args, path, headers=AUTH_HEADERS_SGX):
    print("[ ] DELETE", path)
    requests.delete(args.url + path, headers=headers)


def verify_setup(args, sgx_priv, sgx_sign_priv):
    """Optional method, but checks that we did not forget
    to upload the current set of local keys.
    """
    j = get_json(args, '/pubkeys', {'Authorization': 'Token news_app_token'})
    sgx_pub = crypto.read_pub_key(j['sgx_key'])
    sgx_sign_pub = crypto.read_verify_key(j['sgx_sign_key'])
    message = "test message".encode()

    # test encryption
    cipher = SealedBox(sgx_pub).encrypt(message)
    plaintext = SealedBox(sgx_priv).decrypt(cipher)
    assert plaintext == message
    print("[+] verified that sgx_priv and sgx_pub match")

    # test signature
    signed = sgx_sign_priv.sign(message)
    sgx_sign_pub.verify(signed)
    print("[+] verified that sgx_sign_priv and sgx_sign_pub match")


def run(args):
    sgx_priv = crypto.load_priv_key(os.path.join(
        crypto.get_private_keys_path(),
        'sgx_key_private.hex'))
    sgx_sign_priv = crypto.load_sign_key(os.path.join(
        crypto.get_private_keys_path(),
        'sgx_sign_key_private.hex'))

    verify_setup(args, sgx_priv, sgx_sign_priv)

    INPUT_THRESHOLD = 2
    OUTPUT_THRESHOLD = 4
    buffer_user_to_reporter = []
    buffer_reporter_to_user = []

    try:
        while True:
            #
            # USER -> REPORTER
            # 

            # get user messages
            j = get_json(args, '/user_messages?count=2')

            for m in j:
                buffer_user_to_reporter.append(m['message'])
                delete(args, '/user_message/%d' % int(m['id']))

            if len(buffer_user_to_reporter) >= INPUT_THRESHOLD:
                in_buffer = buffer_user_to_reporter[:INPUT_THRESHOLD]
                buffer_user_to_reporter = buffer_user_to_reporter[INPUT_THRESHOLD:]
                print(f"[+] U2R: Processing {len(in_buffer)} messages")

                # filter out real messages
                out_buffer = []
                for m in in_buffer:
                    is_real, inner = crypto.sgx_decrypt_packet_from_user(
                        sgx_priv,
                        HexEncoder.decode(m))
                    if is_real:
                        out_buffer.append(inner)

                # add dummy traffic
                num_dummy = max(0, OUTPUT_THRESHOLD - len(out_buffer))
                print(f"[+] U2R: Got {len(out_buffer)} real messages. Will fill up with {num_dummy} messages")
                for _ in range(num_dummy):
                    # FIXME: for debug only, real code fills in a dummy message with ephemeral keys
                    out_buffer.append(bytes(bytearray(336)))

                # sign outbuffer
                out_buffer = [
                    sgx_sign_priv.sign(x)
                    for x in out_buffer
                ]

                # post messages to reporters
                post(args, '/send_to_reporter', {'messages': [crypto.to_hex(x) for x in out_buffer]})


            #
            # REPORTER -> USER
            # 
            # get reporter messages
            j = get_json(args, '/reporter_messages?count=2')

            for m in j:
                buffer_reporter_to_user.append(m['message'])
                delete(args, '/reporter_message/%d' % int(m['id']))

            if len(buffer_reporter_to_user) >= INPUT_THRESHOLD:
                in_buffer = buffer_reporter_to_user[:INPUT_THRESHOLD]
                buffer_reporter_to_user = buffer_reporter_to_user[INPUT_THRESHOLD:]
                print(f"[+] R2U: Processing {len(in_buffer)} messages")

                # filter out real messages
                out_buffer = []
                for m in in_buffer:
                    is_real, inner = crypto.sgx_decrypt_packet_from_reporter(
                        sgx_priv,
                        HexEncoder.decode(m))
                    if is_real:
                        out_buffer.append(inner)

                # add dummy traffic
                num_dummy = max(0, OUTPUT_THRESHOLD - len(out_buffer))
                print(f"[+] R2U: Got {len(out_buffer)} real messages. Will fill up with {num_dummy} messages")
                for _ in range(num_dummy):
                    # FIXME: for debug only, real code fills in a dummy message with ephemeral keys
                    out_buffer.append(bytes(bytearray(296)))

                # sign outbuffer
                out_buffer = [
                    sgx_sign_priv.sign(x)
                    for x in out_buffer
                ]

                # post messages to reporters
                post(args, '/send_to_users', {'messages': [crypto.to_hex(x) for x in out_buffer]})

            time.sleep(args.delay)
    except KeyboardInterrupt:
        print("Received CTRL+C")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', type=str)
    parser.add_argument('--delay', type=int, default=5)
    args = parser.parse_args()

    run(args)
