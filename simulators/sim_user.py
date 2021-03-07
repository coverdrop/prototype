import argparse

from nacl.exceptions import BadSignatureError, CryptoError
from nacl.public import PrivateKey
import crypto
import datetime
import json
import time
import requests

AUTH_HEADERS_USER = {'Authorization': 'Token news_app_token'}
JSON_HEADERS = {'Content-Type': 'application/json'}


def get_json(args, path, auth_headers=AUTH_HEADERS_USER):
    print("[ ] GET", path)
    resp = requests.get(args.url + path, headers=auth_headers)
    resp.raise_for_status()
    return resp.json()


def post(args, path, data, auth_headers=AUTH_HEADERS_USER):
    print("[ ] POST", path)
    headers =  headers={**auth_headers, **JSON_HEADERS}
    resp = requests.post(
        args.url + path, 
        data=json.dumps(data),
       headers=headers)
    resp.raise_for_status()


def run(args):
    user_priv = crypto.load_priv_key('user_key_private.hex')
    with open('user_key.hex', 'r') as f:
        user_pub = crypto.read_pub_key(f.readline())

    pubkeys = get_json(args, '/pubkeys')
    sgx_pub = crypto.read_pub_key(pubkeys['sgx_key'])
    sgx_verify = crypto.read_verify_key(pubkeys['sgx_sign_key'])
    reporter_pub = crypto.read_pub_key(pubkeys['reporter_keys'][str(args.reporter_contact)])
    print("[+] Downloaded and parsed all required public keys")

    seen_messages = set()

    try:
        while True:
            # download user deaddrop
            deaddrop = get_json(args, '/deaddrop')

            # try to decode any message
            decoded_messages = []
            for packet in deaddrop:
                try:
                    m = crypto.user_decrypt_packet_from_sgx(
                        user_priv, reporter_pub, sgx_verify,
                        crypto.from_hex(packet)
                    )
                except CryptoError:
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

            for m in new_messages:
                print("[+] NEW MESSAGE:", m)

            # send a new message
            text = "Hello at %s local time" % datetime.datetime.now().strftime("%d-%b %H:%M:%S")
            print(f"[ ] I am sending a real message: '{text}'")
            packet = crypto.user_encrypt_message(user_pub, sgx_pub, reporter_pub, text, real=True)
            post(args, '/user_message', {'message': crypto.to_hex(packet)})

            # send a dummy message
            print(f"[ ] I am sending a dummy message")
            temp_key = PrivateKey.generate().public_key
            packet = crypto.user_encrypt_message(user_pub, sgx_pub, temp_key, "dummy", real=False)
            post(args, '/user_message', {'message': crypto.to_hex(packet)})

            print("[+] Finished iteration")

            time.sleep(args.delay)

    except KeyboardInterrupt:
        print("Received CTRL+C")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', type=str, default='https://lambdapioneer.de')
    parser.add_argument('--delay', type=int, default=5)
    parser.add_argument('--reporter-contact', type=int, default=1)
    args = parser.parse_args()

    run(args)
