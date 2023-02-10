package com.uid2.client.test;

import com.google.gson.stream.JsonWriter;
import com.uid2.client.*;
import org.junit.Test;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.Assert.*;

public class EncryptionTestsV4 {

    public static final long MASTER_KEY_ID = 164;
    public static final long SITE_KEY_ID = 165;
    public static final int SITE_ID = 9000;
    public static final int SITE_ID2 = 2;
    public static final int[] INT_MASTER_SECRET = new int[] { 139, 37, 241, 173, 18, 92, 36, 232, 165, 168, 23, 18, 38, 195, 123, 92, 160, 136, 185, 40, 91, 173, 165, 221, 168, 16, 169, 164, 38, 139, 8, 155 };
    public static final int[] INT_SITE_SECRET = new int[] { 32, 251, 7, 194, 132, 154, 250, 86, 202, 116, 104, 29, 131, 192, 139, 215, 48, 164, 11, 65, 226, 110, 167, 14, 108, 51, 254, 125, 65, 24, 23, 133 };
    public static final Instant NOW = Instant.now();
    public static final Key MASTER_KEY = new Key(MASTER_KEY_ID, -1, NOW.minus(1, ChronoUnit.DAYS), NOW, NOW.plus(1, ChronoUnit.DAYS), getMasterSecret());
    public static final Key SITE_KEY = new Key(SITE_KEY_ID, SITE_ID, NOW.minus(10, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), getSiteSecret());
    public static final String EXAMPLE_UID = "ywsvDNINiZOVSsfkHpLpSJzXzhr6Jx9Z/4Q0+lsEUvM=";
    private static final String CLIENT_SECRET = "ioG3wKxAokmp+rERx6A4kM/13qhyolUXIu14WN16Spo=";

    // unit tests to ensure the base64url encoding and decoding are identical in all supported
    // uid2 client sdks in different programming languages
    @Test
    public void crossPlatformConsistencyCheck_Base64UrlTest()
    {
        int[] rawInput = { 0xff, 0xE0, 0x88, 0xFF, 0xEE, 0x99, 0x99 };
        byte[] rawInputBytes = intArrayToByteArray(rawInput);

        //the Base64 equivalent is "/+CI/+6ZmQ=="
        //and we want the Base64URL encoded to remove the '=' padding
        String expectedBase64URLStr =  "_-CI_-6ZmQ";
        final ByteBuffer writer = ByteBuffer.allocate(rawInput.length);
        for (int i = 0; i < rawInput.length; i++)
        {
            writer.put(rawInputBytes[i]);
        }

        String base64UrlEncodedStr = UID2Base64UrlCoder.encode(writer.array());
        assertEquals(expectedBase64URLStr, base64UrlEncodedStr);

        byte[] decoded = UID2Base64UrlCoder.decode(base64UrlEncodedStr);
        assertEquals(rawInput.length, decoded.length);
        for (int i = 0; i < rawInput.length; i++)
        {
            assertEquals((int) (decoded[i] & 0xff), rawInput[i]);
        }
    }

    // verify that the Base64URL decoder can decode Base64URL string with NO '=' paddings added
    @Test
    public void crossPlatformConsistencyCheck_Decrypt() throws Exception {
        final String crossPlatformAdvertisingToken = "AIAAAACkOqJj9VoxXJNnuX3v-ymceRf8_Av0vA5asOj9YBZJc1kV1vHdmb0AIjlzWnFF-gxIlgXqhRFhPo3iXpugPBl3gv4GKnGkw-Zgm2QqMsDPPLpMCYiWrIUqHPm8hQiq9PuTU-Ba9xecRsSIAN0WCwKLwA_EDVdzmnLJu64dQoeYmuu3u1G2EuTkuMrevmP98tJqSUePKwnfK73-0Zdshw";
        //Sunday, 1 January 2023 1:01:01 AM UTC
        final long referenceTimestampMs = 1672534861000L;
        // 1 hour before ref timestamp
        final long establishedMs = referenceTimestampMs - (3600 * 1000);
        final long lastRefreshedMs = referenceTimestampMs;
        final long tokenCreatedMs = referenceTimestampMs;
        Instant masterKeyCreated = Instant.ofEpochMilli(referenceTimestampMs).minus(1, ChronoUnit.DAYS);
        Instant siteKeyCreated = Instant.ofEpochMilli(referenceTimestampMs).minus(10, ChronoUnit.DAYS);
        Instant masterKeyActivates = Instant.ofEpochMilli(referenceTimestampMs);
        Instant siteKeyActivates = Instant.ofEpochMilli(referenceTimestampMs).minus(1, ChronoUnit.DAYS);
        //for the next ~20 years ...
        Instant masterKeyExpires = Instant.ofEpochMilli(referenceTimestampMs).plus(1*365*20, ChronoUnit.DAYS);
        Instant siteKeyExpires = Instant.ofEpochMilli(referenceTimestampMs).plus(1*365*20, ChronoUnit.DAYS);
        UID2TokenGenerator.Params params = UID2TokenGenerator.defaultParams().withTokenExpiry(Instant.ofEpochMilli(referenceTimestampMs).plus(1*365*20, ChronoUnit.DAYS));

        final Key masterKey = new Key(MASTER_KEY_ID, -1, masterKeyCreated, masterKeyActivates, masterKeyExpires, getMasterSecret());
        final Key siteKey = new Key(SITE_KEY_ID, SITE_ID, siteKeyCreated, siteKeyActivates, siteKeyExpires, getSiteSecret());

        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(masterKey, siteKey));
        //verify that the dynamically created ad token can be decrypted
        String runtimeAdvertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, masterKey, SITE_ID, siteKey, params);
        //best effort check as the token might simply just not require padding
        assertEquals(-1, runtimeAdvertisingToken.indexOf('='));

        assertEquals(-1, runtimeAdvertisingToken.indexOf('+'));
        assertEquals(-1, runtimeAdvertisingToken.indexOf('/'));

        DecryptionResponse res = client.decrypt(runtimeAdvertisingToken);
        assertEquals(EXAMPLE_UID, res.getUid());
        //can also decrypt a known token generated from other SDK
        res = client.decrypt(crossPlatformAdvertisingToken);
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void smokeTest() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, UID2TokenGenerator.defaultParams());
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void emptyKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, UID2TokenGenerator.defaultParams());
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_INITIALIZED, res.getStatus());
    }

    @Test
    public void expiredKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, UID2TokenGenerator.defaultParams());

        Key masterKeyExpired = new Key(MASTER_KEY_ID, -1, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key siteKeyExpired = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(masterKeyExpired, siteKeyExpired));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.KEYS_NOT_SYNCED, res.getStatus());
    }

    @Test
    public void notAuthorizedForKey() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, UID2TokenGenerator.defaultParams());

        Key anotherMasterKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 1, -1, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getMasterSecret());
        Key anotherSiteKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 2, SITE_ID, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(anotherMasterKey, anotherSiteKey));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY, res.getStatus());
    }

    @Test
    public void invalidPayload() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String payload = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, UID2TokenGenerator.defaultParams());
        byte[] payloadInBytes = UID2Base64UrlCoder.decode(payload);
        String advertisingToken = UID2Base64UrlCoder.encode(Arrays.copyOfRange(payloadInBytes, 0, payloadInBytes.length - 1));
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.INVALID_PAYLOAD, res.getStatus());
    }

    @Test
    public void tokenExpiryAndCustomNow() throws Exception {
        final Instant expiry = Instant.parse("2021-03-22T09:01:02Z");
        final UID2TokenGenerator.Params params = UID2TokenGenerator.defaultParams().withTokenExpiry(expiry);

        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, params);

        DecryptionResponse res = client.decrypt(advertisingToken, expiry.plus(1, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, res.getStatus());

        res = client.decrypt(advertisingToken, expiry.minus(1, ChronoUnit.SECONDS));
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void encryptDataTokenDecryptKeyExpired() throws Exception {
        final byte[] data = {1, 2, 3, 4, 5, 6};
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        final Key key = new Key(SITE_KEY_ID, SITE_ID2, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(keySetToJson(MASTER_KEY, key));
        final String advertisingToken = UID2TokenGenerator.generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, key, UID2TokenGenerator.defaultParams());
        EncryptionDataResponse encrypted = client.encryptData(EncryptionDataRequest.forData(data).withAdvertisingToken(advertisingToken));
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    private static String keySetToJson(Key ... keys) throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject();
        writer.name("body");
        writer.beginArray();
        for(Key k : keys) {
            writer.beginObject();
            writer.name("id"); writer.value(k.getId());
            writer.name("site_id"); writer.value(k.getSiteId());
            writer.name("created"); writer.value(k.getCreated().getEpochSecond());
            writer.name("activates"); writer.value(k.getActivates().getEpochSecond());
            writer.name("expires"); writer.value(k.getExpires().getEpochSecond());
            writer.name("secret"); writer.value(Base64.getEncoder().encodeToString(k.getSecret()));
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
        return sw.toString();
    }

    private static byte[] getMasterSecret() {
        return intArrayToByteArray(INT_MASTER_SECRET);
    }

    private static byte[] getSiteSecret() {
        return intArrayToByteArray(INT_SITE_SECRET);
    }

    private static byte[] getTestSecret(int value) {
        byte[] secret = new byte[32];
        Arrays.fill(secret, (byte)value);
        return secret;
    }

    private static byte[] intArrayToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i] = (byte) (intArray[i] & 0xFF);
        }
        return byteArray;
    }
}
