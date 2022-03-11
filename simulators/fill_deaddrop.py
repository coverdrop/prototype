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
    headers = headers = {**auth_headers, **JSON_HEADERS}
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

    msgs = [sgx_sign_priv.sign(bytes(os.urandom(296))) for _ in range(args.target)]
    post(args, '/send_to_users', {'messages': [crypto.to_hex(m) for m in msgs]})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', type=str)
    parser.add_argument('--target', type=int, default=240)
    args = parser.parse_args()

    run(args)
