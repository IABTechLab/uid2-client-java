package com.uid2.client;

import org.junit.jupiter.api.Test;

import static com.uid2.client.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionTestsV2 {
    @Test
    public void smokeTest() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY));
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void emptyKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY));
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_INITIALIZED, res.getStatus());
    }

    @Test
    public void expiredKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY));

        Key masterKeyExpired = new Key(MASTER_KEY_ID, -1, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key siteKeyExpired = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(masterKeyExpired, siteKeyExpired));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.KEYS_NOT_SYNCED, res.getStatus());
    }

    @Test
    public void notAuthorizedForKey() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY));

        Key anotherMasterKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 1, -1, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getMasterSecret());
        Key anotherSiteKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 2, SITE_ID, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(anotherMasterKey, anotherSiteKey));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY, res.getStatus());
    }

    @Test
    public void invalidPayload() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        byte[] payload = Uid2TokenGenerator.generateUid2TokenV2(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY);
        String advertisingToken = Base64.getEncoder().encodeToString(Arrays.copyOfRange(payload, 0, payload.length - 1));

        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.INVALID_PAYLOAD, res.getStatus());
    }

    @Test
    public void tokenExpiryAndCustomNow() throws Exception {
        final Instant expiry = Instant.parse("2021-03-22T09:01:02Z");
        final Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams().withTokenExpiry(expiry);

        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, params));

        DecryptionResponse res = client.decrypt(advertisingToken, expiry.plus(1, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, res.getStatus());

        res = client.decrypt(advertisingToken, expiry.minus(1, ChronoUnit.SECONDS));
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void decryptData() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        final String encrypted = Uid2TokenGenerator.encryptDataV2(data, SITE_KEY, 12345, now);
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        DecryptionDataResponse decrypted = client.decryptData(encrypted);
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
        assertEquals(now, decrypted.getEncryptedAt());
    }

    @Test
    public void decryptDataBadPayloadType() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        final String encrypted = Uid2TokenGenerator.encryptDataV2(data, SITE_KEY, 12345, Instant.now());
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[0] = (byte)0;
        DecryptionDataResponse decrypted = client.decryptData(Base64.getEncoder().encodeToString(encryptedBytes));
        assertEquals(DecryptionStatus.INVALID_PAYLOAD_TYPE, decrypted.getStatus());
    }

    @Test
    public void decryptDataBadVersion() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        final String encrypted = Uid2TokenGenerator.encryptDataV2(data, SITE_KEY, 12345, Instant.now());
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[1] = (byte)0;
        DecryptionDataResponse decrypted = client.decryptData(Base64.getEncoder().encodeToString(encryptedBytes));
        assertEquals(DecryptionStatus.VERSION_NOT_SUPPORTED, decrypted.getStatus());
    }

    @Test
    public void decryptDataBadPayload() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        final String encrypted = Uid2TokenGenerator.encryptDataV2(data, SITE_KEY, 12345, Instant.now());
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);

        byte[] encryptedBytesMod = new byte[encryptedBytes.length+1];
        System.arraycopy(encryptedBytes, 0, encryptedBytesMod, 0, encryptedBytes.length);
        DecryptionDataResponse decrypted = client.decryptData(Base64.getEncoder().encodeToString(encryptedBytesMod));
        assertEquals(DecryptionStatus.INVALID_PAYLOAD, decrypted.getStatus());

        encryptedBytesMod = new byte[encryptedBytes.length-2];
        System.arraycopy(encryptedBytes, 0, encryptedBytesMod, 0, encryptedBytes.length-2);
        decrypted = client.decryptData(Base64.getEncoder().encodeToString(encryptedBytesMod));
        assertEquals(DecryptionStatus.INVALID_PAYLOAD, decrypted.getStatus());
    }

    @Test
    public void decryptDataNoDecryptionKey() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        final String encrypted = Uid2TokenGenerator.encryptDataV2(data, SITE_KEY, 12345, Instant.now());
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        client.refreshJson(keySetToJson(MASTER_KEY));
        DecryptionDataResponse decrypted = client.decryptData(encrypted);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY, decrypted.getStatus());
    }

    private static byte[] getMasterSecret() {
        return intArrayToByteArray(INT_MASTER_SECRET);
    }

    private static byte[] getSiteSecret() {
        return intArrayToByteArray(INT_SITE_SECRET);
    }

    private static byte[] intArrayToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i] = (byte) (intArray[i] & 0xFF);
        }
        return byteArray;
    }
}
