import argparse

from nacl.exceptions import BadSignatureError, CryptoError
import crypto
import datetime
import json
import os
import time
import requests

from nacl.encoding import HexEncoder
from nacl.public import PrivateKey, SealedBox

AUTH_HEADERS_USER = {'Authorization': 'Token reporter_app_token'}
JSON_HEADERS = {'Content-Type': 'application/json'}


def get_json(args, path, auth_headers=AUTH_HEADERS_USER):
    print("[ ] GET", path)
    resp = requests.get(args.url + path, headers=auth_headers)
    resp.raise_for_status()
    return resp.json()


def post(args, path, data, auth_headers=AUTH_HEADERS_USER):
    print("[ ] POST", path)
    headers = headers = {**auth_headers, **JSON_HEADERS}
    resp = requests.post(
        args.url + path,
        data=json.dumps(data),
        headers=headers)
    resp.raise_for_status()


def run(args):
    reporter_priv = crypto.load_priv_key('reporter_%d_key_private.hex' % args.reporter_id)
    with open(os.path.join(crypto.get_public_keys_path(), 'reporter_%d_key.hex' % args.reporter_id), 'r') as f:
        reporter_pub = crypto.read_pub_key(f.readline())

    pubkeys = get_json(args, '/pubkeys', auth_headers={'Authorization': 'Token news_app_token'})
    sgx_pub = crypto.read_pub_key(pubkeys['sgx_key'])
    sgx_verify = crypto.read_verify_key(pubkeys['sgx_sign_key'])
    reporter_pub_1 = crypto.read_pub_key(pubkeys['reporter_keys'][str(args.reporter_id)])
    print("[+] Downloaded and parsed all required public keys")

    assert reporter_pub == reporter_pub_1
    print("[+] Published reporter public key matches local one")

    seen_messages = set()

    try:
        while True:
            # download reporter deaddrop
            deaddrop = get_json(args, '/reporter_inbox')

            # try to decode any message
            decoded_messages = []
            for packet in deaddrop:
                try:
                    m = crypto.reporter_decrypt_packet_from_sgx(
                        reporter_priv, sgx_verify,
                        crypto.from_hex(packet)
                    )

                except CryptoError:
                    # ignore messages that we cannot decrypt
                    continue
                decoded_messages.append(m)

            new_messages = []
            for m in decoded_messages:
                if m not in seen_messages:
                    new_messages.append(m)
                    seen_messages.add(m)

            print("[ ] Deaddrop has %d messages. I can decode %d messages and %d are new" % (
                len(deaddrop),
                len(decoded_messages),
                len(new_messages)
            ))

            for remote_pub, remote_text in new_messages:
                print("[+] NEW MESSAGE:", crypto.to_hex(remote_pub.__bytes__()), remote_text)

                # send a reply
                text = "Reply at %s for: %s" % (datetime.datetime.now().strftime("%d-%b %H:%M:%S"), remote_text.decode())
                print(f"[ ] I am sending a real message: '{text}'")
                packet = crypto.reporter_encrypt_message_to_user(reporter_priv, sgx_pub, remote_pub, text, real=True)
                post(args, '/reporter_message', {'message': crypto.to_hex(packet)})

            # send a dummy message
            print(f"[ ] I am sending a dummy message")
            temp_key = PrivateKey.generate().public_key
            packet = crypto.reporter_encrypt_message_to_user(reporter_priv, sgx_pub, temp_key, "dummy", real=False)
            post(args, '/reporter_message', {'message': crypto.to_hex(packet)})

            print("[+] Finished iteration")

            time.sleep(args.delay)

    except KeyboardInterrupt:
        print("Received CTRL+C")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', type=str, default='https://lambdapioneer.de')
    parser.add_argument('--delay', type=int, default=5)
    parser.add_argument('--reporter-id', type=int, default=1)
    args = parser.parse_args()

    run(args)
