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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public class UID2Client implements IUID2Client {

    private final AtomicReference<KeyContainer> container;
    private final String endpoint;
    private final String authKey;

    public UID2Client(String endpoint, String authKey) {
        this.endpoint = endpoint;
        this.authKey = authKey;
        this.container = new AtomicReference<>(null);
    }

    @Override
    public void refresh() throws UID2ClientException {
        try {
            URL serviceUrl = new URL(endpoint + "/v1/key/latest");
            URLConnection conn = serviceUrl.openConnection();
            HttpURLConnection httpsConnection = (HttpURLConnection) conn;
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setDoInput(true);
            httpsConnection.setDoOutput(true);
            httpsConnection.setRequestProperty("Authorization", "Bearer " + this.authKey);
            int statusCode = httpsConnection.getResponseCode();

            if (statusCode == 401) {
                throw new UID2ClientException("remote service returns 401 Unauthorized, check your api-key");
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new UID2ClientException("unexpected status code: " + statusCode);
            }

            try {
                this.container.set(KeyParser.parse(httpsConnection.getInputStream()));
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
            return Decryption.decrypt(Base64.getDecoder().decode(token), container, now);
        } catch (Exception e) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    @Override
    public EncryptionDataResponse encryptData(EncryptionDataRequest request) {
        return Decryption.encryptData(request, this.container.get());
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
            return Decryption.decryptData(Base64.getDecoder().decode(encryptedData), container);
        } catch (Exception e) {
            return DecryptionDataResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }
}
