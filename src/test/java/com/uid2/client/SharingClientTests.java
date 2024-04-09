package com.uid2.client;

import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static com.uid2.client.BidstreamClientTests.*;
import static com.uid2.client.EncryptionTestsV4.validateAdvertisingToken;
import static com.uid2.client.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class SharingClientTests {
    private final SharingClient sharingClient = new SharingClient("ap", "ak", CLIENT_SECRET);

    private static String keySharingResponse(IdentityScope identityScope, Integer callerSiteId, Integer defaultKeysetId, Integer tokenExpirySeconds, Key... keys) throws IOException {
        if (callerSiteId == null) {
            callerSiteId = SITE_ID;
        }

        if (tokenExpirySeconds == null) {
            tokenExpirySeconds = 2592000; // 30 days
        }

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);

        jsonWriter.beginObject(); // Start the main object

        jsonWriter.name("body").beginObject(); // Start "body" object
        jsonWriter.name("caller_site_id").value(callerSiteId);
        jsonWriter.name("master_keyset_id").value(1);
        jsonWriter.name("token_expiry_seconds").value(tokenExpirySeconds);
        jsonWriter.name("identity_scope").value(identityScope.toString());
        jsonWriter.name("allow_clock_skew_seconds").value(1800); // 30 min
        jsonWriter.name("max_sharing_lifetime_seconds").value(2592000); // 30 days
        jsonWriter.name("unexpected_header_field").value("123"); //ensure new fields can be handled by old SDK versions

        if (defaultKeysetId != null)
        {
            jsonWriter.name("default_keyset_id").value(defaultKeysetId);
        }

        // Start "keys" array
        jsonWriter.name("keys").beginArray();

        for(Key key : keys) {
            jsonWriter.beginObject();
            jsonWriter.name("id").value(key.getId());
            jsonWriter.name("keyset_id").value(getKeySetId(key.getSiteId()));
            jsonWriter.name("created").value(key.getCreated().getEpochSecond());
            jsonWriter.name("activates").value(key.getActivates().getEpochSecond());
            jsonWriter.name("expires").value(key.getExpires().getEpochSecond());
            jsonWriter.name("secret").value(Base64.getEncoder().encodeToString(key.getSecret()));
            jsonWriter.name("unexpected_key_field").value("123"); //ensure new fields can be handled by old SDK versions
            jsonWriter.endObject();
        }
        jsonWriter.endArray(); // End "keys" array

        jsonWriter.endObject(); // End "body" object

        jsonWriter.endObject(); // End the main object

        return stringWriter.toString();
    }

    private static String keySharingResponse(IdentityScope identityScope, Key... keys) throws IOException {
        return keySharingResponse(identityScope, null, null, null, keys);
    }

    static String keySetToJsonForSharing(Key... keys) throws IOException {
        return keySharingResponse(IdentityScope.UID2, SITE_ID, 99999, null, keys);
    }

    static String keySetToJsonForSharing(IdentityScope identityScope, Key... keys) throws IOException {
        return keySharingResponse(identityScope, SITE_ID, 99999, null, keys);
    }

    private static int getKeySetId(int siteId) {
        switch (siteId) {
            case -1: return 1;
            case SITE_ID: return 99999;
            default: return siteId;
        }
    }

    private void decryptAndAssertSuccess(String advertisingToken, TokenVersionForTesting tokenVersion) {
        DecryptionResponse decryptionResponse = sharingClient.decryptTokenIntoRawUid(advertisingToken);
        assertSuccess(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void smokeTest(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        String advertisingToken = AdvertisingTokenBuilder.builder().withScope(identityScope).withVersion(tokenVersion).build();

        RefreshResponse refreshResponse = sharingClient.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void tokenLifetimeTooLongForSharing(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant tokenExpiry = Instant.now().plus(30, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withExpiry(tokenExpiry).withScope(identityScope).withVersion(tokenVersion).build();
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        DecryptionResponse decryptionResponse = sharingClient.decryptTokenIntoRawUid(advertisingToken);
        assertFails(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void tokenGeneratedInTheFutureToSimulateClockSkew(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant tokenGenerated = Instant.now().plus(31, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withGenerated(tokenGenerated).withScope(identityScope).withVersion(tokenVersion).build();
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        DecryptionResponse decryptionResponse = sharingClient.decryptTokenIntoRawUid(advertisingToken);
        assertFails(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void tokenGeneratedInTheFutureWithinAllowedClockSkew(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant tokenGenerated = Instant.now().plus(30, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withGenerated(tokenGenerated).withScope(identityScope).withVersion(tokenVersion).build();
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void phoneTest(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        String rawUidPhone = "BEOGxroPLdcY7LrSiwjY52+X05V0ryELpJmoWAyXiwbZ";
        String advertisingToken = AdvertisingTokenBuilder.builder().withRawUid(rawUidPhone).withScope(identityScope).withVersion(tokenVersion).build();
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        DecryptionResponse decryptionResponse = sharingClient.decryptTokenIntoRawUid(advertisingToken);
        assertTrue(decryptionResponse.isSuccess());
        assertEquals(rawUidPhone, decryptionResponse.getUid());
        assertEquals(tokenVersion.ordinal() + 2, decryptionResponse.getAdvertisingTokenVersion());
        assertEquals(IdentityType.Phone, decryptionResponse.getIdentityType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"V2", "V3", "V4"})
    public void legacyResponseFromOldOperator(TokenVersionForTesting tokenVersion) throws Exception {
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());
        String advertisingToken = AdvertisingTokenBuilder.builder().withVersion(tokenVersion).build();

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    //similar to BidstreamClientTests.TokenGeneratedInTheFutureLegacyClient, but uses KeySharingResponse and Decrypt without domain parameter
    public void tokenGeneratedInTheFutureLegacyClient(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, identityScope);
        client.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));

        Instant tokenGenerated = Instant.now().plus(99, ChronoUnit.DAYS);
        String advertisingToken = AdvertisingTokenBuilder.builder().withGenerated(tokenGenerated).withScope(identityScope).withVersion(tokenVersion).build();

        DecryptionResponse decryptionResponse = client.decrypt(advertisingToken);
        assertSuccess(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void tokenLifetimeTooLongLegacyClient(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, identityScope);
        client.refreshJson(keySharingResponse(identityScope, MASTER_KEY, SITE_KEY));

        Instant tokenExpiry = Instant.now().plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withExpiry(tokenExpiry).withScope(identityScope).withVersion(tokenVersion).build();

        DecryptionResponse decryptionResponse = client.decrypt(advertisingToken);
        assertSuccess(decryptionResponse, tokenVersion);
    }

    // tests below taken from EncryptionTestsV4.cs under "//  Sharing tests" comment and modified to use SharingClient and the new JSON /key/sharing response
    private SharingClient SharingSetupAndEncrypt() throws IOException {
        RefreshResponse refreshResult = sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, SITE_KEY));
        assertTrue(refreshResult.isSuccess());

        return sharingClient;
    }

    private String sharingEncrypt(SharingClient client) {
        return sharingEncrypt(client, IdentityScope.UID2);
    }

    private String sharingEncrypt(SharingClient client, IdentityScope identityScope) {
        EncryptionDataResponse encrypted = client.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        validateAdvertisingToken(encrypted.getEncryptedData(), identityScope, IdentityType.Email, TokenVersionForTesting  .V4);
        return encrypted.getEncryptedData();
    }

    @ParameterizedTest
    @ValueSource(strings = {"UID2", "EUID"})
    public void ClientProducesTokenWithCorrectPrefix(IdentityScope identityScope) throws Exception {
        SharingClient sharingClient = new SharingClient("ep", "ak", CLIENT_SECRET);
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySetToJsonForSharing(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        sharingEncrypt(sharingClient, identityScope); //this validates token and asserts
    }

    @Test
    public void CanEncryptAndDecryptForSharing() throws Exception
    {
        SharingClient sharingClient = SharingSetupAndEncrypt();
        String advertisingToken = sharingEncrypt(sharingClient);

        DecryptionResponse res = sharingClient.decryptTokenIntoRawUid(advertisingToken);
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void CanDecryptAnotherClientsEncryptedToken() throws Exception
    {
        SharingClient sendingClient = SharingSetupAndEncrypt();
        String advertisingToken = sharingEncrypt(sendingClient);

        SharingClient receivingClient = new SharingClient("ep", "ak", CLIENT_SECRET);
        String json = keySharingResponse(IdentityScope.UID2, 4874, 12345, null, MASTER_KEY, SITE_KEY);

        RefreshResponse refreshResponse = receivingClient.refreshJson(json);
        assertTrue(refreshResponse.isSuccess());

        DecryptionResponse res = receivingClient.decryptTokenIntoRawUid(advertisingToken);
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void SharingTokenIsV4() throws IOException {
        SharingClient sharingClient = SharingSetupAndEncrypt();
        String advertisingToken = sharingEncrypt(sharingClient);

        boolean containsBase64SpecialChars = advertisingToken.contains("+") || advertisingToken.contains("/") || advertisingToken.contains(("="));
        assertFalse(containsBase64SpecialChars);
    }

    @Test
    public void Uid2ClientProducesUid2Token() throws IOException {
        SharingClient sharingClient = SharingSetupAndEncrypt();
        String advertisingToken = sharingEncrypt(sharingClient);

        assertEquals("A", advertisingToken.substring(0, 1));
    }

    @Test
    public void EuidClientProducesEuidToken() throws IOException {
        SharingClient sharingClient = new SharingClient("ep", "ak", CLIENT_SECRET);
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySetToJsonForSharing(IdentityScope.EUID, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        String advertisingToken = sharingEncrypt(sharingClient, IdentityScope.EUID); //this validates token and asserts

        assertEquals("E", advertisingToken.substring(0, 1));
    }

    @Test
    public void RawUidProducesCorrectIdentityTypeInToken() throws Exception
    {
        RefreshResponse refreshResponse = sharingClient.refreshJson(keySetToJsonForSharing(IdentityScope.EUID, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());

        //see UID2-79+Token+and+ID+format+v3 . Also note EUID does not support v2 or phone
        assertEquals(IdentityType.Email, GetTokenIdentityType("Q4bGug8t1xjsutKLCNjnb5fTlXSvIQukmahYDJeLBtk=")); //v2 +12345678901. Although this was generated from a phone number, it's a v2 raw UID which doesn't encode this information, so token assumes email by default.
        assertEquals(IdentityType.Phone, GetTokenIdentityType("BEOGxroPLdcY7LrSiwjY52+X05V0ryELpJmoWAyXiwbZ")); //v3 +12345678901
        assertEquals(IdentityType.Email, GetTokenIdentityType("oKg0ZY9ieD/CGMEjAA0kcq+8aUbLMBG0MgCT3kWUnJs=")); //v2 test@example.com
        assertEquals(IdentityType.Email, GetTokenIdentityType("AKCoNGWPYng/whjBIwANJHKvvGlGyzARtDIAk95FlJyb")); //v3 test@example.com
        assertEquals(IdentityType.Email, GetTokenIdentityType("EKCoNGWPYng/whjBIwANJHKvvGlGyzARtDIAk95FlJyb")); //v3 EUID test@example.com
    }

    private IdentityType GetTokenIdentityType(String rawUid) throws Exception
    {
        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(rawUid);
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());
        assertEquals(rawUid, sharingClient.decryptTokenIntoRawUid(encrypted.getEncryptedData()).getUid());

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

        RefreshResponse refreshResponse = sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, MASTER_KEY2, SITE_KEY, SITE_KEY2));
        assertTrue(refreshResponse.isSuccess());

        String advertisingToken = sharingEncrypt(sharingClient);

        DecryptionResponse res = sharingClient.decryptTokenIntoRawUid(advertisingToken);
        assertSame(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @Test
    public void CannotEncryptIfNoKeyFromTheDefaultKeyset() throws IOException {
        String json = keySetToJsonForSharing(MASTER_KEY);
        RefreshResponse refreshResponse = sharingClient.refreshJson(json);
        assertTrue(refreshResponse.isSuccess());

        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void CannotEncryptIfTheresNoDefaultKeysetHeader() throws IOException {
        String json = keySetToJsonForSharing(MASTER_KEY);
        RefreshResponse refreshResponse = sharingClient.refreshJson(json);
        assertTrue(refreshResponse.isSuccess());

        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void ExpiryInTokenMatchesExpiryInResponse() throws IOException {
        String json = keySharingResponse(IdentityScope.UID2, SITE_ID,99999, 2, MASTER_KEY, SITE_KEY);
        RefreshResponse refreshResponse = sharingClient.refreshJson(json);
        assertTrue(refreshResponse.isSuccess());

        Instant encryptedAt = Instant.now();
        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID, encryptedAt);
        assertEquals(EncryptionStatus.SUCCESS, encrypted.getStatus());

        DecryptionResponse res = sharingClient.decryptTokenIntoRawUid(encrypted.getEncryptedData(), encryptedAt.plus(1, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());

        DecryptionResponse futureDecryption = sharingClient.decryptTokenIntoRawUid(encrypted.getEncryptedData(), Instant.now().plus(3, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, futureDecryption.getStatus());
    }

    @Test
    public void EncryptKeyExpired() throws IOException {
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus()); //note: KeyInactive was the result for EncryptData, because EncryptData allowed you to pass an expired key. In the Sharing scenario, expired and inactive keys are ignored when encrypting.
    }

    @Test
    public void EncryptKeyInactive() throws IOException {
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), getTestSecret(9));
        sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }


    @Test
    public void EncryptSiteKeyExpired() throws IOException {
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW, NOW.minus(1, ChronoUnit.DAYS), getTestSecret(9));
        sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }

    @Test
    public void EncryptSiteKeyInactive() throws IOException {
        Key key = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.plus(1, ChronoUnit.DAYS), NOW.plus(2, ChronoUnit.DAYS), getTestSecret(9));
        sharingClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, key));
        EncryptionDataResponse encrypted = sharingClient.encryptRawUidIntoToken(EXAMPLE_UID);
        assertEquals(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY, encrypted.getStatus());
    }
}
