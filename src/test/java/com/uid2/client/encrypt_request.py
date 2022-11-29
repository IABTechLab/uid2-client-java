# This script was used to generate envelopes used in the PublisherTests java class

import base64
import os
import sys
import time
from datetime import datetime

from Crypto.Cipher import AES

secret = base64.b64decode(sys.argv[1])
payload = "".join(sys.stdin.readlines())

# iv = os.urandom(12)
iv = bytes.fromhex("cc3ccaca9889eab3800e787e")
cipher = AES.new(secret, AES.MODE_GCM, nonce=iv)

# millisec = int(time.time() * 1000)
millisec = 1667885597644
# nonce = os.urandom(8)
nonce = bytes.fromhex("312fe5aa08b2a049")

print(f'Request iv: {iv.hex()}', file=sys.stderr)
print(f'Request timestamp: {millisec}  -  {datetime.fromtimestamp(millisec/1000)}', file=sys.stderr)
print(f'Request nonce: {int.from_bytes(nonce, "big")} or hex: {nonce.hex()}', file=sys.stderr)
print(f'Request payload: --{payload}--', file=sys.stderr)
print(file=sys.stderr)

millisecBinary = bytearray(millisec.to_bytes(8, 'big'))
print(f'millisecHex: {millisecBinary.hex()}', file=sys.stderr)
print(f'payloadHex: {bytes(payload, "utf-8").hex()}', file=sys.stderr)

body = bytearray(millisec.to_bytes(8, 'big'))
body += bytearray(nonce)
body += bytearray(bytes(payload, 'utf-8'))

print(f'Body: {body.hex()}', file=sys.stderr)

ciphertext, tag = cipher.encrypt_and_digest(body)

print(f'Ciphertext: {ciphertext.hex()}', file=sys.stderr)
print(f'Tag: {tag.hex()}', file=sys.stderr)

envelope = bytearray(b'\x01')
envelope += bytearray(iv)
envelope += bytearray(ciphertext)
envelope += bytearray(tag)

print(f'Envelope: {base64.b64encode(bytes(envelope)).decode()}', file=sys.stderr)
print(file=sys.stderr)

print(base64.b64encode(bytes(envelope)).decode() + "\n")
