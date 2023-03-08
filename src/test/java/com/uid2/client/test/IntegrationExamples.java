 package com.uid2.client.test;

import com.uid2.client.*;

import java.util.Timer;
import java.util.TimerTask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class IntegrationExamples {
    final String TEST_ENDPOINT = System.getenv("UID2_BASE_URL");
    final String TEST_API_KEY =  System.getenv("UID2_API_KEY");
    final String TEST_SECRET_KEY = System.getenv("UID2_SECRET_KEY");
    final String TEST_TOKEN = System.getenv("UID2_TEST_TOKEN");


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
    public void runSharingExample() throws Exception {
        IUID2Client client = UID2ClientFactory.create(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);
        client.refresh();

        final String rawUid = "P2xdbu2ldlpXV1z6n3bET7T1g0xfqmldZPDdPTvydRQ=";
        EncryptionDataResponse encrypted = client.encrypt(rawUid);
        if (!encrypted.isSuccess()) {
            System.out.println("Failed to encrypt data: " + encrypted.getStatus());
        } else {
            DecryptionResponse decrypted = client.decrypt(encrypted.getEncryptedData());
            if (!decrypted.isSuccess()) {
                System.out.println("Failed to decrypt data: " + decrypted.getStatus());
            } else {
                System.out.println("Encrypted: " + encrypted.getEncryptedData());
                System.out.println("Decrypted: " + decrypted.getUid());
                System.out.println("Encrypted at: " + decrypted.getEstablished());
                assertEquals(rawUid, decrypted.getUid());
            }
        }
    }
}
