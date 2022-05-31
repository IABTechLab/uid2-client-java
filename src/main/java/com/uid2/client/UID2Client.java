// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public class UID2Client implements IUID2Client {

    private final AtomicReference<KeyContainer> container;
    private final String endpoint;
    private final String authKey;
    private final byte[] secretKey;
    private final IdentityScope identityScope;

    public UID2Client(String endpoint, String authKey, String secretKey, IdentityScope identityScope) {
        this.endpoint = endpoint;
        this.authKey = authKey;
        this.secretKey = Base64.getDecoder().decode(secretKey);
        this.identityScope = identityScope;
        this.container = new AtomicReference<>(null);
    }

    @Override
    public void refresh() throws UID2ClientException {
        try {
            V2Request request = makeV2Request(Instant.now());
            URL serviceUrl = new URL(endpoint + "/v2/key/latest");
            URLConnection conn = serviceUrl.openConnection();
            HttpURLConnection httpsConnection = (HttpURLConnection) conn;
            httpsConnection.setRequestMethod("POST");
            httpsConnection.setDoInput(true);
            httpsConnection.setDoOutput(true);
            httpsConnection.setRequestProperty("Authorization", "Bearer " + this.authKey);
            try(OutputStream os = httpsConnection.getOutputStream()) {
                os.write(request.envelope);
            }
            int statusCode = httpsConnection.getResponseCode();

            if (statusCode == 401) {
                throw new UID2ClientException("remote service returns 401 Unauthorized, check your api-key");
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new UID2ClientException("unexpected status code: " + statusCode);
            }

            try {
                byte[] response = parseV2Response(readAllBytes(httpsConnection.getInputStream()), request.nonce);
                this.container.set(KeyParser.parse(new ByteArrayInputStream(response)));
            } catch (Exception e) {
                throw new UID2ClientException("error while parsing json response", e);
            }
        } catch (IOException e) {
            throw new UID2ClientException("error communicating with api endpoint", e);
        }
    }

    public void refreshJson(String json) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        this.container.set(KeyParser.parse(inputStream));
    }

    @Override
    public DecryptionResponse decrypt(String token, Instant now) {
        KeyContainer container = this.container.get();
        if(container == null) {
            return DecryptionResponse.makeError(DecryptionStatus.NOT_INITIALIZED);
        }

        if(!container.isValid(now)) {
            return DecryptionResponse.makeError(DecryptionStatus.KEYS_NOT_SYNCED);
        }

        try {
            return Decryption.decrypt(Base64.getDecoder().decode(token), container, now, this.identityScope);
        } catch (Exception e) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    @Override
    public EncryptionDataResponse encryptData(EncryptionDataRequest request) {
        return Decryption.encryptData(request, this.container.get(), this.identityScope);
    }

    @Override
    public DecryptionDataResponse decryptData(String encryptedData) {
        KeyContainer container = this.container.get();
        if(container == null) {
            return DecryptionDataResponse.makeError(DecryptionStatus.NOT_INITIALIZED);
        }

        if(!container.isValid(Instant.now())) {
            return DecryptionDataResponse.makeError(DecryptionStatus.KEYS_NOT_SYNCED);
        }

        try {
            return Decryption.decryptData(Base64.getDecoder().decode(encryptedData), container, this.identityScope);
        } catch (Exception e) {
            return DecryptionDataResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    private V2Request makeV2Request(Instant now) {
        byte[] nonce = new byte[8];
        new SecureRandom().nextBytes(nonce);
        ByteBuffer writer = ByteBuffer.allocate(16);
        writer.putLong(now.toEpochMilli());
        writer.put(nonce);
        byte[] encrypted = Decryption.encryptGCM(writer.array(), null, this.secretKey);
        ByteBuffer request = ByteBuffer.allocate(encrypted.length + 1);
        request.put((byte)1); // version
        request.put(encrypted);
        return new V2Request(Base64.getEncoder().encode(request.array()), nonce);
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[4096];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    private byte[] parseV2Response(byte[] envelope, byte[] nonce) throws IllegalStateException {
        byte[] envelopeBytes = Base64.getDecoder().decode(envelope);
        byte[] payload = Decryption.decryptGCM(envelopeBytes, 0, this.secretKey);
        byte[] receivedNonce = Arrays.copyOfRange(payload, 8, 8 + nonce.length);
        if (!Arrays.equals(receivedNonce, nonce)) {
            throw new IllegalStateException("nonce mismatch");
        }
        return Arrays.copyOfRange(payload, 16, payload.length);
    }

    private static class V2Request
    {
        public final byte[] envelope;
        public final byte[] nonce;

        public V2Request(byte[] envelope, byte[] nonce) {
            this.envelope = envelope;
            this.nonce = nonce;
        }
    }
}
