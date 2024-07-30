package com.uid2.client;
import org.junit.jupiter.api.Test;

import static com.uid2.client.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionV3Tests {
    @Test
    public void smokeTest() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void emptyKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_INITIALIZED, res.getStatus());
    }

    @Test
    public void expiredKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());

        Key masterKeyExpired = new Key(MASTER_KEY_ID, -1, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key siteKeyExpired = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(masterKeyExpired, siteKeyExpired));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.KEYS_NOT_SYNCED, res.getStatus());
    }

    @Test
    public void notAuthorizedForKey() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());

        Key anotherMasterKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 1, -1, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getMasterSecret());
        Key anotherSiteKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 2, SITE_ID, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(anotherMasterKey, anotherSiteKey));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY, res.getStatus());
    }

    @Test
    public void invalidPayload() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String payload = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        byte[] payloadInBytes = Base64.getDecoder().decode(payload);
        String advertisingToken = Base64.getEncoder().encodeToString(Arrays.copyOfRange(payloadInBytes, 0, payloadInBytes.length - 1));
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
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, params);

        DecryptionResponse res = client.decrypt(advertisingToken, expiry.plus(1, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, res.getStatus());

        res = client.decrypt(advertisingToken, expiry.minus(1, ChronoUnit.SECONDS));
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void encryptDataSpecificKeyAndIv() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        final byte[] iv = new byte[12];
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY).withInitializationVector(iv));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        client.refreshJson(keySetToJson(SITE_KEY));
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
    }

    @Test
    public void encryptDataSpecificKeyAndGeneratedIv() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        client.refreshJson(keySetToJson(SITE_KEY));
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
    }

    @Test
    public void encryptDataSpecificSiteId() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withSiteId(SITE_KEY.getSiteId()));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
    }

    @Test
    public void encryptDataSiteIdFromToken() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
    }

    @Test
    public void encryptDataSiteIdFromTokenCustomSiteKeySiteId() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID2, SITE_KEY, Uid2TokenGenerator.defaultParams());
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
    }

    @Test
    public void encryptDataSiteIdAndTokenSet() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        assertThrows(IllegalArgumentException.class, () -> {
            client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken).withSiteId(SITE_KEY.getSiteId()));
        });
    }

    @Test
    public void encryptDataTokenDecryptFailed() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken("bogus-token"));
        assertEquals(EncryptionStatus.TOKEN_DECRYPT_FAILURE, encrypted.getStatus());
    }

    @Test
    public void encryptDataKeyExpired() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        final Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(keySetToJson(key));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(key));
        assertEquals(EncryptionStatus.KEY_INACTIVE, encrypted.getStatus());
    }

    @Test
    public void encryptDataTokenDecryptKeyExpired() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        final Key key = new Key(SITE_KEY_ID, SITE_ID2, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(keySetToJson(MASTER_KEY, key));
        final String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, key, Uid2TokenGenerator.defaultParams());
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken));
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void encryptDataKeyInactive() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        final Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(keySetToJson(key));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(key));
        assertEquals(EncryptionStatus.KEY_INACTIVE, encrypted.getStatus());
    }

    @Test
    public void encryptDataKeyExpiredCustomNow() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY).withNow(SITE_KEY.getExpires()));
        assertEquals(EncryptionStatus.KEY_INACTIVE, encrypted.getStatus());
    }

    @Test
    public void encryptDataKeyInactiveCustomNow() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY).withNow(SITE_KEY.getActivates().minusSeconds(1)));
        assertEquals(EncryptionStatus.KEY_INACTIVE, encrypted.getStatus());
    }

    @Test
    public void encryptDataNoSiteKey() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withSiteId(SITE_ID2));
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void encryptDataSiteKeyExpired() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        final Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(keySetToJson(MASTER_KEY, key));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withSiteId(key.getSiteId()));
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void encryptDataSiteKeyInactive() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        final Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(keySetToJson(MASTER_KEY, key));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withSiteId(key.getSiteId()));
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void encryptDataSiteKeyInactiveCustomNow() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(
                EncryptionDataRequest.forData(data).withSiteId(SITE_KEY.getSiteId()).withNow(SITE_KEY.getActivates().minusSeconds(1)));
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void encryptDataTokenExpired() throws Exception {
        final Instant expiry = NOW.minusSeconds(60);
        final Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams().withTokenExpiry(expiry);

        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV3(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, params);
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken));
        assertEquals(EncryptionStatus.TOKEN_DECRYPT_FAILURE, encrypted.getStatus());

        Instant now = expiry.minusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken).withNow(now));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
        assertEquals(DecryptionStatus.SUCCESS, decrypted.getStatus());
        assertArrayEquals(data, decrypted.getDecryptedData());
        assertEquals(now, decrypted.getEncryptedAt());
    }

    @Test
    public void decryptDataBadPayloadType() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted.getEncryptedData());
        encryptedBytes[0] = (byte)0;
        DecryptionDataResponse decrypted = client.decryptData(Base64.getEncoder().encodeToString(encryptedBytes));
        assertEquals(DecryptionStatus.INVALID_PAYLOAD_TYPE, decrypted.getStatus());
    }

    @Test
    public void decryptDataBadVersion() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted.getEncryptedData());
        encryptedBytes[1] = (byte)0;
        DecryptionDataResponse decrypted = client.decryptData(Base64.getEncoder().encodeToString(encryptedBytes));
        assertEquals(DecryptionStatus.VERSION_NOT_SUPPORTED, decrypted.getStatus());
    }

    @Test
    public void decryptDataBadPayload() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted.getEncryptedData());

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
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(SITE_KEY));
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withKey(SITE_KEY));
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        client.refreshJson(keySetToJson(MASTER_KEY));
        DecryptionDataResponse decrypted = client.decryptData(encrypted.getEncryptedData());
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
