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
    final String TEST_DOMAIN = System.getenv("UID2_TEST_DOMAIN");

    @Test // - insert your API key & test uid & test domain before enabling this test
    public void ExampleBidStreamClient() {
        BidstreamClient client = new BidstreamClient(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);
        RefreshResponse refreshResult = client.refresh();
        if (!refreshResult.isSuccess()) {
            System.out.println("Failed to refresh keys: " + refreshResult.getReason());
        }
        DecryptionResponse result = client.decryptTokenIntoRawUid(TEST_TOKEN, TEST_DOMAIN);
        System.out.println("DecryptedSuccess: " + result.isSuccess() + " Status: " + result.getStatus());
        System.out.println("IdentityType: " + result.getIdentityType());
        System.out.println("SiteId: " + result.getSiteId());
        System.out.println("UID: " + result.getUid());
        System.out.println("Established: " + result.getEstablished());
        System.out.println("SiteId: " + result.getSiteId());
        System.out.println("IsClientSideGenerated: " + result.getIsClientSideGenerated());
    }

    @Test // - insert your API key & test uid & test domain before enabling this test
    public void ExampleAutoRefreshBidStreamClient() throws InterruptedException {
        BidstreamClient client = new BidstreamClient(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);

        Thread refreshThread = new Thread(() -> {
            for (int i = 0; i < 8; ++i) {
                try {
                    RefreshResponse refreshResult = client.refresh();
                    System.out.println("Refresh keys, success=" + refreshResult.isSuccess());
                    System.out.flush();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        refreshThread.start();

        for (int i = 0; i < 5; ++i) {
            // Since this block will run initially, it will fail at the first time. Subsequently, upon refreshing, the call should succeed.
            DecryptionResponse result = client.decryptTokenIntoRawUid(TEST_TOKEN, TEST_DOMAIN);
            System.out.println("DecryptSuccess=" + result.isSuccess() + " Status=" + result.getStatus() + " UID=" + result.getUid());
            System.out.flush();
            Thread.sleep(5000);
        }

        refreshThread.join();
    }

    @Test // - insert your API key & test uid before enabling this test
    public void ExampleSharingClient() {
        SharingClient client = new SharingClient(TEST_ENDPOINT, TEST_API_KEY, TEST_SECRET_KEY);
        RefreshResponse refreshResponse = client.refresh();
        if (!refreshResponse.isSuccess()) {
            System.out.println("Failed to refresh keys: " + refreshResponse.getReason());
        }

        final String rawUid = "P2xdbu2ldlpXV1z6n3bET7T1g0xfqmldZPDdPTvydRQ=";
        EncryptionDataResponse encrypted = client.encryptRawUidIntoToken(rawUid);

        if (!encrypted.isSuccess()) {
            System.out.println("Failed to encrypt data: " + encrypted.getStatus());
            return;
        }

        DecryptionResponse decrypted = client.decryptTokenIntoRawUid(encrypted.getEncryptedData());
        if (!decrypted.isSuccess()) {
            System.out.println("Failed to decrypt data: " + decrypted.getStatus());
        }

        System.out.println("RawUid: " + rawUid);
        System.out.println("Encrypted: " + encrypted.getEncryptedData());
        System.out.println("Decrypted: " + decrypted.getUid());
        System.out.println("Encrypted at: " + decrypted.getEstablished());
    }

    @Test // - insert your API key & test uid before enabling this test
    public void runE2EDeprecated() throws Exception {
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
    public void runAutoRefreshWithTimerDeprecated() throws Exception {
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
    public void runSharingExampleDeprecated() throws Exception {
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
