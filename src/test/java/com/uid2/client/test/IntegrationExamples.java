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

 package com.uid2.client.test;

import com.uid2.client.*;

import java.util.Timer;
import java.util.TimerTask;
import org.junit.Test;

public class IntegrationExamples {
    private static final String TEST_ENDPOINT = "https://operator-integ.uidapi.com";
    private static final String TEST_API_KEY = "your-api-key";
    private static final String TEST_SECRET_KEY = "your-secret-key";
    private static final String TEST_TOKEN = "AgAAAOl7YylgZJJ/hUsxVU7YCuXMuCAq3Muz7KaJN/miMBL/q6hgN8QY/ocy5c8d/zBvkfDMrLz+6jJvFWWHnl1u2O1mtX20/Ctft8CRwZl32b0d58fWoEhijxROd1q5DBww6+7N7ay26IttdQ+B4Rf4MTL2T/3PK9yyETae1l4v0ODd7w==";

    @Test // - insert your API key & test uid before enabling this test
    public void runE2E() throws Exception {
        IUID2Client client = UID2ClientFactory.create(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);
        client.refresh();
        DecryptionResponse result = client.decrypt(TEST_TOKEN);
        System.out.println(result.getStatus());
        System.out.println(result.getEstablished());
        System.out.println(result.getSiteId());
        System.out.println(result.getUid());
    }

    // this test works as an example for how you can perform auto refresh on this client
    @Test // - insert your API key & test uid before enabling this test
    public void runAutoRefreshWithTimer() throws Exception {
        IUID2Client client = UID2ClientFactory.create(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("refresh...");
                    client.refresh();
                    DecryptionResponse result = client.decrypt(TEST_TOKEN);
                    System.out.println(result.getStatus());
                    System.out.println(result.getEstablished());
                    System.out.println(result.getUid());
                } catch (UID2ClientException e) {
                    System.out.println(e);
                }
            }
        }, 0, 3000);

        Thread.sleep(10000);
        timer.cancel();
    }

    @Test
    public void runEncryptDecryptData() throws Exception {
        IUID2Client client = UID2ClientFactory.create(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);
        client.refresh();

        final byte[] data = "Hello World!".getBytes();
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(TEST_TOKEN));
        if (!encrypted.isSuccess()) {
            System.out.println("Failed to encrypt data: " + encrypted.getStatus());
        } else {
            DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
            if (!decrypted.isSuccess()) {
                System.out.println("Failed to decrypt data: " + decrypted.getStatus());
            } else {
                System.out.println("Encrypted: " + encrypted.getEncryptedData());
                System.out.println("Decrypted: " + new String(decrypted.getDecryptedData()));
                System.out.println("Encrypted at: " + decrypted.getEncryptedAt());
            }
        }
    }
}
