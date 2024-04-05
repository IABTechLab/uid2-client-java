package com.uid2.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.uid2.client.builder.PrivacyBitsBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static com.uid2.client.EncryptionTestsV4.validateAdvertisingToken;
import static com.uid2.client.SharingClientTests.keySetToJsonForSharing;
import static com.uid2.client.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class BidstreamClientTests {

    final private BidstreamClient bidstreamClient = new BidstreamClient("ep", "ak", CLIENT_SECRET);

    @ParameterizedTest
    @CsvSource({
            "UID2, 2",
            "EUID, 2",
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void smokeTest(IdentityScope identityScope, int tokenVersion) throws Exception {
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, EXAMPLE_UID, Uid2TokenGenerator.defaultParams());
        callAndVerifyRefreshJson(identityScope);

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void phoneTest(IdentityScope identityScope, int tokenVersion) throws Exception {
        String rawUidPhone = "BEOGxroPLdcY7LrSiwjY52+X05V0ryELpJmoWAyXiwbZ";
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, rawUidPhone, Uid2TokenGenerator.defaultParams());
        callAndVerifyRefreshJson(identityScope);

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertTrue(decryptionResponse.isSuccess());
        assertEquals(rawUidPhone, decryptionResponse.getUid());
        assertEquals(tokenVersion, decryptionResponse.getAdvertisingTokenVersion());
        assertEquals(IdentityType.Phone, decryptionResponse.getIdentityType());
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, 2",
            "EUID, 2",
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void tokenLifetimeTooLongForBidstream(IdentityScope identityScope, int tokenVersion) throws Exception {
        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams();
        params.tokenExpiry = Instant.now().plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, EXAMPLE_UID, params);
        callAndVerifyRefreshJson(identityScope);

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertFails(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, 2",
            "EUID, 2",
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void tokenGeneratedInTheFutureToSimulateClockSkew(IdentityScope identityScope, int tokenVersion) throws Exception {
        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams();
        params.tokenGenerated = Instant.now().plus(31, ChronoUnit.MINUTES);
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, EXAMPLE_UID, params);
        callAndVerifyRefreshJson(identityScope);

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertFails(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, 2",
            "EUID, 2",
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void tokenGeneratedInTheFutureWithinAllowedClockSkew(IdentityScope identityScope, int tokenVersion) throws Exception {
        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams();
        params.tokenGenerated = Instant.now().plus(30, ChronoUnit.MINUTES);
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, EXAMPLE_UID, params);
        callAndVerifyRefreshJson(identityScope);

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    public void legacyResponseFromOldOperator(int tokenVersion) throws Exception {
        RefreshResponse refreshResponse = bidstreamClient.refreshJson(keySetToJsonForSharing(MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());
        String advertisingToken = getAdvertisingToken(IdentityScope.UID2, tokenVersion, EXAMPLE_UID, Uid2TokenGenerator.defaultParams());

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, 2",
            "EUID, 2",
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void tokenGeneratedInTheFutureLegacyClient(IdentityScope identityScope, int tokenVersion) throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, identityScope);
        client.refreshJson(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams();
        params.tokenGenerated = Instant.now().plus(31, ChronoUnit.MINUTES);
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, EXAMPLE_UID, params);

        DecryptionResponse decryptionResponse = client.decrypt(advertisingToken);
        assertSuccess(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "UID2, 2",
            "EUID, 2",
            "UID2, 3",
            "EUID, 3",
            "UID2, 4",
            "EUID, 4"
    })
    public void tokenLifetimeTooLongLegacyClient(IdentityScope identityScope, int tokenVersion) throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, identityScope);
        client.refreshJson(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams();
        params.tokenExpiry = Instant.now().plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        String advertisingToken = getAdvertisingToken(identityScope, tokenVersion, EXAMPLE_UID, params);

        DecryptionResponse decryptionResponse = client.decrypt(advertisingToken);
        assertSuccess(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @MethodSource("data_IdentityScopeAndType_TestCases")
    public void identityScopeAndType_TestCases(String uid, IdentityScope identityScope, IdentityType identityType) throws Exception {
        callAndVerifyRefreshJson(identityScope);

        String advertisingToken = getAdvertisingToken(identityScope, 4, uid, Uid2TokenGenerator.defaultParams());
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertTrue(res.isSuccess());
        assertEquals(uid, res.getUid());
        assertEquals(identityType, res.getIdentityType());
        assertEquals(4, res.getAdvertisingTokenVersion());
    }

    private static Stream<Arguments> data_IdentityScopeAndType_TestCases() {
        return Stream.of(
                Arguments.of(EXAMPLE_UID, IdentityScope.UID2, IdentityType.Email),
                Arguments.of(EXAMPLE_PHONE_RAW_UID2_V3, IdentityScope.UID2, IdentityType.Phone),
                Arguments.of(EXAMPLE_UID, IdentityScope.EUID, IdentityType.Email),
                Arguments.of(EXAMPLE_PHONE_RAW_UID2_V3, IdentityScope.EUID, IdentityType.Phone)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "example.com, 2",
            "example.org, 2",
            "example.com, 4",
            "example.org, 4",
            "example.com, 4",
            "example.org, 4"
    })
    public void TokenIsCstgDerivedTest(String domainName, int tokenVersion) throws Exception {
        callAndVerifyRefreshJson(IdentityScope.UID2);
        int privacyBits = PrivacyBitsBuilder.Builder().WithClientSideGenerated(true).Build();

        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams();
        params.tokenPrivacyBits = privacyBits;
        String advertisingToken = getAdvertisingToken(IdentityScope.UID2, tokenVersion, EXAMPLE_UID, params);

        validateAdvertisingToken(advertisingToken, IdentityScope.UID2, IdentityType.Email, tokenVersion);
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, domainName);
        assertTrue(res.getIsClientSideGenerated());
        assertTrue(res.isSuccess());
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    // tests below taken from EncryptionTestsV4.cs above "//  Sharing tests" comment (but excluding deprecated EncryptData/DecryptData methods) and modified to use BidstreamClient and the new JSON /key/bidstream response
    @Test
    public void emptyKeyContainer() throws Exception {
        String advertisingToken = getAdvertisingToken(IdentityScope.UID2, 4, EXAMPLE_UID, Uid2TokenGenerator.defaultParams());
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.NOT_INITIALIZED, res.getStatus());
    }

    @Test
    public void expiredKeyContainer() throws Exception {
        String advertisingToken = getAdvertisingToken(IdentityScope.UID2, 4, EXAMPLE_UID, Uid2TokenGenerator.defaultParams());

        Key masterKeyExpired = new Key(MASTER_KEY_ID, -1, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key siteKeyExpired = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());
        RefreshResponse refreshResponse = bidstreamClient.refreshJson(keyBidstreamResponse(IdentityScope.UID2, masterKeyExpired, siteKeyExpired));
        assertTrue(refreshResponse.isSuccess());

        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.KEYS_NOT_SYNCED, res.getStatus());
    }

    @Test
    public void notAuthorizedForMasterKey() throws Exception {
        String advertisingToken = getAdvertisingToken(IdentityScope.UID2, 4, EXAMPLE_UID, Uid2TokenGenerator.defaultParams());

        Key anotherMasterKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 1, -1, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getMasterSecret());
        Key anotherSiteKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 2, SITE_ID, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getSiteSecret());
        RefreshResponse refreshResponse = bidstreamClient.refreshJson(keyBidstreamResponse(IdentityScope.UID2, anotherMasterKey, anotherSiteKey));
        assertTrue(refreshResponse.isSuccess());

        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY, res.getStatus());
    }

    @Test
    public void invalidPayload() throws Exception {
        String payload = getAdvertisingToken(IdentityScope.UID2, 4, EXAMPLE_UID, Uid2TokenGenerator.defaultParams());
        byte[] payloadInBytes = Uid2Base64UrlCoder.decode(payload);
        String advertisingToken = Uid2Base64UrlCoder.encode(Arrays.copyOfRange(payloadInBytes, 0, payloadInBytes.length - 1));
        bidstreamClient.refreshJson(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.INVALID_PAYLOAD, res.getStatus());
    }

    @Test
    public void tokenExpiryAndCustomNow() throws Exception {
        final Instant expiry = Instant.parse("2021-03-22T09:01:02Z");
        final Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams().WithTokenGenerated(expiry.minus(60, ChronoUnit.SECONDS)).withTokenExpiry(expiry);

        bidstreamClient.refreshJson(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        String advertisingToken = getAdvertisingToken(IdentityScope.UID2, 4, EXAMPLE_UID, params);

        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null, expiry.plus(1, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, res.getStatus());

        res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null, expiry.minus(1, ChronoUnit.SECONDS));
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    private void callAndVerifyRefreshJson(IdentityScope identityScope) {
        RefreshResponse refreshResponse = bidstreamClient.refreshJson(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));
        assertTrue(refreshResponse.isSuccess());
    }

    private void decryptAndAssertSuccess(String token, int tokenVersion) {
        DecryptionResponse response = bidstreamClient.decryptTokenIntoRawUid(token, null);
        assertSuccess(response, tokenVersion);
    }

    public static void assertSuccess(DecryptionResponse response, int tokenVersion) {
        assertTrue(response.isSuccess());
        assertEquals(EXAMPLE_UID, response.getUid());
        assertEquals(tokenVersion, response.getAdvertisingTokenVersion());
        assertEquals(tokenVersion, response.getAdvertisingTokenVersion());
        if (tokenVersion != 2) {
            assertEquals(IdentityType.Email, response.getIdentityType());
        }
    }

    public static void assertFails(DecryptionResponse response, int tokenVersion) {
        assertFalse(response.isSuccess());
        assertEquals(DecryptionStatus.INVALID_TOKEN_LIFETIME, response.getStatus());
        assertEquals(tokenVersion, response.getAdvertisingTokenVersion());
        if (tokenVersion != 2) {
            assertEquals(IdentityType.Email, response.getIdentityType());
        }
    }

    public static String getAdvertisingToken(IdentityScope identityScope, int tokenVersion, String uid, Uid2TokenGenerator.Params params) throws Exception {
        params.identityScope = identityScope.value;
        switch (tokenVersion) {
            case 2:
                return Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(uid, MASTER_KEY, SITE_ID, SITE_KEY, params));
            case 3:
                return Uid2TokenGenerator.generateUid2TokenV3(uid, MASTER_KEY, SITE_ID, SITE_KEY, params);
            case 4:
                return Uid2TokenGenerator.generateUid2TokenV4(uid, MASTER_KEY, SITE_ID, SITE_KEY, params);
            default:
                throw new Uid2Exception("Invalid token UID2 version: " + tokenVersion);
        }
    }

    private static String keyBidstreamResponse(IdentityScope identityScope, Key... keys) {
        JsonArray keyToJson = new JsonArray();
        for(Key key : keys) {
            JsonObject keyTojsonElement = new JsonObject();
            keyTojsonElement.addProperty("id", key.getId());
            keyTojsonElement.addProperty("created", key.getCreated().getEpochSecond());
            keyTojsonElement.addProperty("activates", key.getActivates().getEpochSecond());
            keyTojsonElement.addProperty("expires", key.getExpires().getEpochSecond());
            keyTojsonElement.addProperty("secret", java.util.Base64.getEncoder().encodeToString(key.getSecret()));
            keyTojsonElement.addProperty("unexpected_key_field", "123"); //ensure new fields can be handled by old SDK versions
            keyToJson.add(keyTojsonElement);
        };

        JsonObject site1 = new JsonObject();
        site1.addProperty("id", SITE_ID);
        JsonArray domainNames1 = new JsonArray();
        domainNames1.add("example.com");
        domainNames1.add("example.org");
        site1.add("domain_names", domainNames1);
        site1.addProperty("unexpected_domain_field", "123");

        JsonObject site2 = new JsonObject();
        site1.addProperty("id", SITE_ID2);
        JsonArray domainNames2 = new JsonArray();
        domainNames2.add("example.net");
        domainNames2.add("example.edu");
        site1.add("domain_names", domainNames2);
        site1.addProperty("unexpected_domain_field", "123");

        JsonArray siteData = new JsonArray();
        siteData.add(site1);
        siteData.add(site2);

        JsonObject body = new JsonObject();
        body.addProperty("max_bidstream_lifetime_seconds", Duration.ofDays(3).getSeconds());
        body.addProperty("identity_scope", identityScope.toString());
        body.addProperty("allow_clock_skew_seconds", 1800);
        body.add("keys", keyToJson);
        body.addProperty("unexpected_header_field", "12345");
        body.add("site_data", siteData);

        JsonObject json = new JsonObject();
        json.add("body", body);
        return json.toString();
    }
}
