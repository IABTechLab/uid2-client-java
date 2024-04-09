package com.uid2.client;

import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.uid2.client.TestData.*;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class EncryptionTestsV4 {
    // unit tests to ensure the base64url encoding and decoding are identical in all supported
    // uid2 client sdks in different programming languages
    @Test
    public void crossPlatformConsistencyCheck_Base64UrlTestCases()
    {
        int[] case1 = { 0xff, 0xE0, 0x88, 0xFF, 0xEE, 0x99, 0x99 };
        //the Base64 equivalent is "/+CI/+6ZmQ=="
        //and we want the Base64URL encoded to remove 2 '=' paddings at the back
        //String case1Base64Encoded = Base64.getEncoder().encodeToString(intArrayToByteArray(case1));
        crossPlatformConsistencyCheck_Base64UrlTest(case1, "_-CI_-6ZmQ");

        //the Base64 equivalent is "/+CI/+6ZmZk=" to remove 1 padding
        int[] case2 = { 0xff, 0xE0, 0x88, 0xFF, 0xEE, 0x99, 0x99, 0x99};
        //String case2Base64Encoded = Base64.getEncoder().encodeToString(intArrayToByteArray(case2));
        crossPlatformConsistencyCheck_Base64UrlTest(case2, "_-CI_-6ZmZk");

        //the Base64 equivalent is "/+CI/+6Z" which requires no padding removal
        int[] case3 = { 0xff, 0xE0, 0x88, 0xFF, 0xEE, 0x99};
        //String case3Base64Encoded = Base64.getEncoder().encodeToString(intArrayToByteArray(case3));
        crossPlatformConsistencyCheck_Base64UrlTest(case3, "_-CI_-6Z");
    }

    public void crossPlatformConsistencyCheck_Base64UrlTest(int[] rawInput, String expectedBase64URLStr)
    {
        byte[] rawInputBytes = intArrayToByteArray(rawInput);
        final ByteBuffer writer = ByteBuffer.allocate(rawInput.length);
        for (int i = 0; i < rawInput.length; i++)
        {
            writer.put(rawInputBytes[i]);
        }

        String base64UrlEncodedStr = Uid2Base64UrlCoder.encode(writer.array());
        assertEquals(expectedBase64URLStr, base64UrlEncodedStr);

        byte[] decoded = Uid2Base64UrlCoder.decode(base64UrlEncodedStr);
        assertEquals(rawInput.length, decoded.length);
        for (int i = 0; i < rawInput.length; i++)
        {
            assertEquals(decoded[i] & 0xff, rawInput[i]);
        }
    }

    // verify that the Base64URL decoder can decode Base64URL String with NO '=' paddings added
    @Test
    public void crossPlatformConsistencyCheck_Decrypt() throws Exception {
        final String crossPlatformAdvertisingToken = "AIAAAACkOqJj9VoxXJNnuX3v-ymceRf8_Av0vA5asOj9YBZJc1kV1vHdmb0AIjlzWnFF-gxIlgXqhRFhPo3iXpugPBl3gv4GKnGkw-Zgm2QqMsDPPLpMCYiWrIUqHPm8hQiq9PuTU-Ba9xecRsSIAN0WCwKLwA_EDVdzmnLJu64dQoeYmuu3u1G2EuTkuMrevmP98tJqSUePKwnfK73-0Zdshw";
        //Sunday, 1 January 2023 1:01:01 AM UTC
        final long referenceTimestampMs = 1672534861000L;
        // 1 hour before ref timestamp
        Instant masterKeyCreated = Instant.ofEpochMilli(referenceTimestampMs).minus(1, ChronoUnit.DAYS);
        Instant siteKeyCreated = Instant.ofEpochMilli(referenceTimestampMs).minus(10, ChronoUnit.DAYS);
        Instant masterKeyActivates = Instant.ofEpochMilli(referenceTimestampMs);
        Instant siteKeyActivates = Instant.ofEpochMilli(referenceTimestampMs).minus(1, ChronoUnit.DAYS);
        //for the next ~20 years ...
        Instant masterKeyExpires = Instant.ofEpochMilli(referenceTimestampMs).plus(365*20, ChronoUnit.DAYS);
        Instant siteKeyExpires = Instant.ofEpochMilli(referenceTimestampMs).plus(365*20, ChronoUnit.DAYS);
        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams().withTokenExpiry(Instant.ofEpochMilli(referenceTimestampMs).plus(365*20, ChronoUnit.DAYS));

        final Key masterKey = new Key(MASTER_KEY_ID, -1, masterKeyCreated, masterKeyActivates, masterKeyExpires, getMasterSecret());
        final Key siteKey = new Key(SITE_KEY_ID, SITE_ID, siteKeyCreated, siteKeyActivates, siteKeyExpires, getSiteSecret());

        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(masterKey, siteKey));
        //verify that the dynamically created ad token can be decrypted
        String runtimeAdvertisingToken = generateUid2TokenV4(EXAMPLE_UID, masterKey, SITE_ID, siteKey, params);
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

    public static void validateAdvertisingToken(String advertisingTokenString, IdentityScope identityScope, IdentityType identityType, TokenVersionForTesting tokenVersion) {
        if (tokenVersion == TokenVersionForTesting.V2) {
            assertEquals("Ag", advertisingTokenString.substring(0, 2));
            return;
        }

        String firstChar = advertisingTokenString.substring(0, 1);
        if (identityScope == IdentityScope.UID2) {
            assertEquals(identityType == IdentityType.Email ? "A" : "B", firstChar);
        } else {
            assertEquals(identityType == IdentityType.Email ? "E" : "F", firstChar);
        }

        String secondChar = advertisingTokenString.substring(1, 2);
        if (tokenVersion == TokenVersionForTesting.V3)
        {
            assertEquals("3", secondChar);

        }
        else
        {
            assertEquals("4", secondChar);
            //No URL-unfriendly characters allowed:
            assertEquals(-1, advertisingTokenString.indexOf('='));
            assertEquals(-1, advertisingTokenString.indexOf('+'));
            assertEquals(-1, advertisingTokenString.indexOf('/'));
        }
    }

    private static String generateUid2TokenV4(String uid, Key masterKey, long siteId, Key siteKey, Uid2TokenGenerator.Params params) {
        String advertisingToken = Uid2TokenGenerator.generateUid2TokenV4(uid, masterKey, siteId, siteKey, params);
        validateAdvertisingToken(advertisingToken, IdentityScope.UID2, IdentityType.Email, TokenVersionForTesting.V4);
        return advertisingToken;
    }


    @Test
    public void smokeTest() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        client.refreshJson(keySetToJson(MASTER_KEY, SITE_KEY));
        String advertisingToken = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void emptyKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_INITIALIZED, res.getStatus());
    }

    @Test
    public void expiredKeyContainer() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());

        Key masterKeyExpired = new Key(MASTER_KEY_ID, -1, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key siteKeyExpired = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(masterKeyExpired, siteKeyExpired));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.KEYS_NOT_SYNCED, res.getStatus());
    }

    @Test
    public void notAuthorizedForMasterKey() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String advertisingToken = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());

        Key anotherMasterKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 1, -1, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getMasterSecret());
        Key anotherSiteKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 2, SITE_ID, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getSiteSecret());
        client.refreshJson(keySetToJson(anotherMasterKey, anotherSiteKey));

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY, res.getStatus());
    }

    @Test
    public void invalidPayload() throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        String payload = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, Uid2TokenGenerator.defaultParams());
        byte[] payloadInBytes = Uid2Base64UrlCoder.decode(payload);
        String advertisingToken = Uid2Base64UrlCoder.encode(Arrays.copyOfRange(payloadInBytes, 0, payloadInBytes.length - 1));
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
        String advertisingToken = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, SITE_KEY, params);

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
        final String advertisingToken = generateUid2TokenV4(EXAMPLE_UID, MASTER_KEY, SITE_ID, key, Uid2TokenGenerator.defaultParams());
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

    private static byte[] intArrayToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i] = (byte) (intArray[i] & 0xFF);
        }
        return byteArray;
    }

    //////////////////////  Sharing tests //////////////////////////////////////////////////////////////////

    private static class ClientAndToken {
        UID2Client client;
        String advertisingToken;
    }

    private ClientAndToken SharingSetupAndEncrypt()
    {
        ClientAndToken clientAndToken = new ClientAndToken();
        clientAndToken.client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharing(MASTER_KEY, SITE_KEY);
        clientAndToken.client.refreshJson(json);

        clientAndToken.advertisingToken = SharingEncrypt(clientAndToken.client);
        return clientAndToken;
    }

    private String SharingEncrypt(UID2Client client)
    {
        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        return encrypted.getEncryptedData();
    }

    @Test
    public void CanEncryptAndDecryptForSharing() throws Exception
    {
        ClientAndToken clientAndToken = SharingSetupAndEncrypt();

        DecryptionResponse res = clientAndToken.client.decrypt(clientAndToken.advertisingToken);
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void CanDecryptAnotherClientsEncryptedToken() throws Exception
    {
        ClientAndToken sendingClientAndToken = SharingSetupAndEncrypt();

        UID2Client receivingClient = new UID2Client("endpoint2", "authkey2", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharingWithHeader("\"default_keyset_id\": 12345,", 4874, MASTER_KEY, SITE_KEY);

        receivingClient.refreshJson(json);

        DecryptionResponse res = receivingClient.decrypt(sendingClientAndToken.advertisingToken);
        assertSame(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }


    @Test
    public void SharingTokenIsV4()
    {
        ClientAndToken clientAndToken = SharingSetupAndEncrypt();
        String advertisingToken = clientAndToken.advertisingToken;

        boolean containsBase64SpecialChars = advertisingToken.contains("+") || advertisingToken.contains("/") || advertisingToken.contains(("="));
        assertFalse(containsBase64SpecialChars);
    }

    @Test
    public void Uid2ClientProducesUid2Token()
    {
        ClientAndToken clientAndToken = SharingSetupAndEncrypt();

        assertEquals("A", clientAndToken.advertisingToken.substring(0, 1));
    }


    @Test
    public void EuidClientProducesEuidToken()
    {
        UID2Client client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.EUID);
        String json = KeySetToJsonForSharing(MASTER_KEY, SITE_KEY);
        client.refreshJson(json);


        String advertisingToken = SharingEncrypt(client);

        assertEquals("E", advertisingToken.substring(0, 1));
    }

    @Test
    public void RawUidProducesCorrectIdentityTypeInToken() throws Exception
    {
        UID2Client client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharing(MASTER_KEY, SITE_KEY);
        client.refreshJson(json);

        //see UID2-79+Token+and+ID+format+v3 . Also note EUID does not support v2 or phone
        assertEquals(IdentityType.Email, GetTokenIdentityType("Q4bGug8t1xjsutKLCNjnb5fTlXSvIQukmahYDJeLBtk=", client)); //v2 +12345678901. Although this was generated from a phone number, it's a v2 raw UID which doesn't encode this information, so token assumes email by default.
        assertEquals(IdentityType.Phone, GetTokenIdentityType("BEOGxroPLdcY7LrSiwjY52+X05V0ryELpJmoWAyXiwbZ", client)); //v3 +12345678901
        assertEquals(IdentityType.Email, GetTokenIdentityType("oKg0ZY9ieD/CGMEjAA0kcq+8aUbLMBG0MgCT3kWUnJs=", client)); //v2 test@example.com
        assertEquals(IdentityType.Email, GetTokenIdentityType("AKCoNGWPYng/whjBIwANJHKvvGlGyzARtDIAk95FlJyb", client)); //v3 test@example.com
        assertEquals(IdentityType.Email, GetTokenIdentityType("EKCoNGWPYng/whjBIwANJHKvvGlGyzARtDIAk95FlJyb", client)); //v3 EUID test@example.com
    }

    private IdentityType GetTokenIdentityType(String rawUid, UID2Client client) throws Exception
    {
        EncryptionDataResponse encrypted = client.encrypt(rawUid);
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        assertEquals(rawUid, client.decrypt(encrypted.getEncryptedData()).getUid());

        char firstChar = encrypted.getEncryptedData().charAt(0);
        if ('A' == firstChar || 'E' == firstChar) //from UID2-79+Token+and+ID+format+v3
            return IdentityType.Email;
        else if ('F' == firstChar || 'B' == firstChar)
            return IdentityType.Phone;

        throw new Exception("unknown IdentityType");
    }

    @Test
    public void MultipleKeysPerKeyset() throws Exception
    {
        Key MASTER_KEY2 = new Key(264, -1, NOW.minus(2, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key SITE_KEY2 = new Key(265, SITE_ID, NOW.minus(10, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());

        UID2Client client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharing(MASTER_KEY, MASTER_KEY2, SITE_KEY, SITE_KEY2);
        client.refreshJson(json);

        String advertisingToken = SharingEncrypt(client);

        DecryptionResponse res = client.decrypt(advertisingToken);
        assertSame(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void CannotEncryptIfNoKeyFromTheDefaultKeyset()
    {
        UID2Client client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharing(MASTER_KEY);
        client.refreshJson(json);

        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void CannotEncryptIfTheresNoDefaultKeysetHeader()
    {
        UID2Client client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharingWithHeader("", SITE_ID, MASTER_KEY, SITE_KEY);
        client.refreshJson(json);

        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void ExpiryInTokenMatchesExpiryInResponse()
    {
        UID2Client client = new UID2Client("endpoint", "authkey", CLIENT_SECRET, IdentityScope.UID2);
        String json = KeySetToJsonForSharingWithHeader("\"default_keyset_id\": 99999, \"token_expiry_seconds\": 2,", SITE_ID, MASTER_KEY, SITE_KEY);
        client.refreshJson(json);

        Instant encryptedAt = Instant.now();
        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID, encryptedAt);
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());

        DecryptionResponse res = client.decrypt(encrypted.getEncryptedData(), encryptedAt.plusSeconds(1));
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());

        DecryptionResponse futureDecryption = client.decrypt(encrypted.getEncryptedData(), Instant.now().plusSeconds(3));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, futureDecryption.getStatus());
    }

    @Test
    public void EncryptKeyExpired()
    {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(KeySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus()); //note: KeyInactive was the result for EncryptData, because EncryptData allowed you to pass an expired key. In the Sharing scenario, expired and inactive keys are ignored when encrypting.
    }

    @Test
    public void EncryptKeyInactive()
    {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(KeySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }


    @Test
    public void EncryptSiteKeyExpired()
    {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(KeySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void EncryptSiteKeyInactive()
    {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, IdentityScope.UID2);
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), getTestSecret(9));
        client.refreshJson(KeySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = client.encrypt(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }


    private static String KeySetToJsonForSharing(Key... keys)
    {
        return KeySetToJsonForSharingWithHeader("\"default_keyset_id\": 99999,", SITE_ID, keys);
    }

    private static String CalculateKeySetId(int siteId, String defaultKeyset)
    {
        int keysetId;
        //{k.SiteId switch { -1 => 1, SITE_ID => 99999, _ => k.SiteId }},
        switch (siteId)
        {
            case -1:
                keysetId = 1;
                break;
            case SITE_ID:
                if (!defaultKeyset.contains("99999"))
                    return "";
                else
                    keysetId = 99999;
                break;
            default:
                keysetId = siteId;
                break;
        }

        return String.format("                \"keyset_id\": %d,\n", keysetId);
    }

    private static String KeySetToJsonForSharingWithHeader(String defaultKeyset, int callerSiteId, Key... keys)
    {

        return String.format("{\n" +
                "    \"body\": {\n" +
                "        \"caller_site_id\": %d,\n" +
                "        \"master_keyset_id\": 1,\n" +
                "        %s\n" +
                "        \"keys\": [\n%s\n        ]\n" +
                "    }\n" +
                "}", callerSiteId, defaultKeyset,
                Arrays.stream(keys)
                        .map(k -> String.format("{\n" +
                                "                \"id\": %d,\n%s" +
                                "                \"created\": %d,\n" +
                                "                \"activates\": %d,\n" +
                                "                \"expires\": %d,\n" +
                                "                \"secret\": \"%s\"\n" +
                                "            }",
                                k.getId(),
                                CalculateKeySetId(k.getSiteId(), defaultKeyset),
                                k.getCreated().getEpochSecond(),
                                k.getActivates().getEpochSecond(),
                                k.getExpires().getEpochSecond(),
                                Base64.getEncoder().encodeToString(k.getSecret())
                        ))
                        .collect(Collectors.joining(",\n"))
        );
    }
}
