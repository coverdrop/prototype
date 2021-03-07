import argparse

import crypto
import os
import sys

from nacl.encoding import HexEncoder


def write_sgx_keys(args):
    sgx_priv, sgx_pub = crypto.create_keypair()
    _write_key(
        os.path.join(crypto.get_public_keys_path(), 'sgx_key.hex'),
        sgx_pub)
    _write_key(
        os.path.join(crypto.get_private_keys_path(), 'sgx_key_private.hex'),
        sgx_priv)

    sgx_sign_priv, sgx_sign_pub = crypto.create_sign_keypair()
    _write_key(
        os.path.join(crypto.get_public_keys_path(), 'sgx_sign_key.hex'),
        sgx_sign_pub)
    _write_key(
        os.path.join(crypto.get_private_keys_path(), 'sgx_sign_key_private.hex'),
        sgx_sign_priv)


def write_reporter_keys(args):
    for rid in (1, 2, 3):
        reporter_priv, reporter_pub = crypto.create_keypair()
        _write_key(
            os.path.join(crypto.get_public_keys_path(),
                         'reporter_%d_key.hex' % rid),
            reporter_pub)
        _write_key(
            os.path.join(crypto.get_private_keys_path(),
                         'reporter_%d_key_private.hex' % rid),
            reporter_priv)


def write_user_keys(args):
    user_priv, user_pub = crypto.create_keypair()
    _write_key(
        os.path.join('.', 'user_key.hex'),
        user_pub)
    _write_key(
        os.path.join('.', 'user_key_private.hex'),
        user_priv)


def _write_key(path, key):
    key_hex_bytes = HexEncoder().encode(key.__bytes__())
    with open(path, 'w') as f:
        f.write(key_hex_bytes.decode())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    write_sgx_key_cmd = subparsers.add_parser("write_sgx_keys")
    write_sgx_key_cmd.set_defaults(func=write_sgx_keys)

    write_reporter_keys_cmd = subparsers.add_parser("write_reporter_keys")
    write_reporter_keys_cmd.set_defaults(func=write_reporter_keys)

    write_user_keys_cmd = subparsers.add_parser("write_user_keys")
    write_user_keys_cmd.set_defaults(func=write_user_keys)

    args = parser.parse_args()
    if 'func' not in args:
        parser.print_help()
        sys.exit(1)

    args.func(args)
