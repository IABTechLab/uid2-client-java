# This script was used for the PublisherTests.decryptGenerateResponse() test, to ensure python and java had identical results

import base64
import json
import sys
from datetime import datetime

from Crypto.Cipher import AES

secret = base64.b64decode(sys.argv[1].strip())
print(f'Secret: {secret.hex()}')


is_refresh_response = 1 if len(sys.argv) > 2 and sys.argv[2] == '--is-refresh' else 0
response = "".join(sys.stdin.readlines())

print()
try:
    err_resp = json.loads(response)
    print("Error response:")
    print(json.dumps(err_resp, indent=4))
except:
    print(f'Response: {response}')
    resp_bytes = base64.b64decode(response)
    iv = resp_bytes[:12]
    data = resp_bytes[12:len(resp_bytes) - 16]
    tag = resp_bytes[len(resp_bytes) - 16:]

    cipher = AES.new(secret, AES.MODE_GCM, nonce=iv)
    decrypted = cipher.decrypt_and_verify(data, tag)

    if is_refresh_response != 1:
        tm = datetime.fromtimestamp(int.from_bytes(decrypted[:8], 'big') / 1000)
        print(f'Response timestamp: {tm}')
        nonce = int.from_bytes(decrypted[8:16], 'big')
        print(f'Response nonce: {nonce}')
        json_resp = json.loads(decrypted[16:].decode("utf-8"))
    else:
        json_resp = json.loads(decrypted.decode("utf-8"))
    print("Response JSON:")
    print(json.dumps(json_resp, indent=4))
    print()
