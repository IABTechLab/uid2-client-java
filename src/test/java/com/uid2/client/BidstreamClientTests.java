package com.uid2.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static com.uid2.client.EncryptionV4Tests.validateAdvertisingToken;
import static com.uid2.client.SharingClientTests.keySetToJsonForSharing;
import static com.uid2.client.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class BidstreamClientTests {

    final private BidstreamClient bidstreamClient = new BidstreamClient("ep", "ak", CLIENT_SECRET);

    @ParameterizedTest
    @CsvSource({
            "UID2, V2",
            "EUID, V2",
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void smokeTestForBidstream(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant now = Instant.now();
        String advertisingToken = AdvertisingTokenBuilder.builder().withScope(identityScope).withVersion(tokenVersion).withEstablished(now.minus(120, ChronoUnit.DAYS)).withGenerated(now.minus(1, ChronoUnit.DAYS)).withExpiry(now.plus(2, ChronoUnit.DAYS)).build();
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

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
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertTrue(decryptionResponse.isSuccess());
        assertEquals(rawUidPhone, decryptionResponse.getUid());
        assertEquals(tokenVersion.ordinal() + 2, decryptionResponse.getAdvertisingTokenVersion());
        assertEquals(IdentityType.Phone, decryptionResponse.getIdentityType());
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
    public void tokenLifetimeTooLongForBidstreamButRemainingLifetimeAllowed(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant generated = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant tokenExpiry = generated.plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withExpiry(tokenExpiry).withScope(identityScope).withVersion(tokenVersion).withGenerated(generated).build();
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);

        if (tokenVersion == TokenVersionForTesting.V2) {
            assertSuccess(decryptionResponse, tokenVersion);
        } else {
            assertFails(decryptionResponse, tokenVersion);
        }
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
    public void tokenRemainingLifetimeTooLongForBidstream(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant tokenExpiry = Instant.now().plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        Instant generated = Instant.now();
        String advertisingToken = AdvertisingTokenBuilder.builder().withExpiry(tokenExpiry).withScope(identityScope).withVersion(tokenVersion).withGenerated(generated).build();
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertFails(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    //Note V2 does not have a "token generated" field, therefore v2 tokens can't have a future "token generated" date and are excluded from this test.
    @CsvSource({
            "UID2, V3",
            "EUID, V3",
            "UID2, V4",
            "EUID, V4"
    })
    public void tokenGeneratedInTheFutureToSimulateClockSkew(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        Instant tokenGenerated = Instant.now().plus(31, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withGenerated(tokenGenerated).withScope(identityScope).withVersion(tokenVersion).build();
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        DecryptionResponse decryptionResponse = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
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
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        decryptAndAssertSuccess(advertisingToken, tokenVersion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"V2", "V3", "V4"})
    public void legacyResponseFromOldOperator(TokenVersionForTesting tokenVersion) throws Exception {
        refresh(keySetToJsonForSharing(MASTER_KEY, SITE_KEY));
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
    public void tokenGeneratedInTheFutureLegacyClient(IdentityScope identityScope, TokenVersionForTesting tokenVersion) throws Exception {
        UID2Client client = new UID2Client("ep", "ak", CLIENT_SECRET, identityScope);
        client.refreshJson(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        Instant tokenGenerated = Instant.now().plus(31, ChronoUnit.MINUTES);
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
        client.refreshJson(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        Instant tokenExpiry = Instant.now().plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);
        String advertisingToken = AdvertisingTokenBuilder.builder().withExpiry(tokenExpiry).withScope(identityScope).withVersion(tokenVersion).build();

        DecryptionResponse decryptionResponse = client.decrypt(advertisingToken);
        assertSuccess(decryptionResponse, tokenVersion);
    }

    @ParameterizedTest
    @MethodSource("data_IdentityScopeAndType_TestCases")
    public void identityScopeAndType_TestCases(String uid, IdentityScope identityScope, IdentityType identityType) throws Exception {
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        String advertisingToken = AdvertisingTokenBuilder.builder().withRawUid(uid).withScope(identityScope).build();
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

    // These are the domain or app names associated with site SITE_ID, as defined by keyBidstreamResponse()
    @ParameterizedTest
    @CsvSource({
            "example.com, V2",
            "example.org, V2",
            "com.123.Game.App.android, V2",
            "123456789, V2",
            "example.com, V3",
            "example.org, V3",
            "com.123.Game.App.android, V3",
            "123456789, V3",
            "example.com, V4",
            "example.org, V4",
            "com.123.Game.App.android, V4",
            "123456789, V4"
    })
    public void tokenIsCstgDerivedTest(String domainName, TokenVersionForTesting tokenVersion) throws Exception {
        refresh(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        int privacyBits = PrivacyBitsBuilder.Builder().WithClientSideGenerated(true).Build();

        String advertisingToken = AdvertisingTokenBuilder.builder().withVersion(tokenVersion).withPrivacyBits(privacyBits).build();

        validateAdvertisingToken(advertisingToken, IdentityScope.UID2, IdentityType.Email, tokenVersion);
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, domainName);
        assertTrue(res.getIsClientSideGenerated());
        assertTrue(res.isSuccess());
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    // These are the domain or app names associated with site SITE_ID but vary in capitalization, as defined by keyBidstreamResponse()
    @ParameterizedTest
    @CsvSource({
            "Example.com, V2",
            "Example.Org, V2",
            "com.123.Game.App.android, V2",
            "Example.com, V3",
            "Example.Org, V3",
            "com.123.Game.App.android, V3",
            "Example.com, V4",
            "Example.Org, V4",
            "com.123.Game.App.android, V4",
    })
    public void domainOrAppNameCaseInSensitiveTest(String domainName, TokenVersionForTesting tokenVersion) throws Exception {
        refresh(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        int privacyBits = PrivacyBitsBuilder.Builder().WithClientSideGenerated(true).Build();

        String advertisingToken = AdvertisingTokenBuilder.builder().withVersion(tokenVersion).withPrivacyBits(privacyBits).build();

        validateAdvertisingToken(advertisingToken, IdentityScope.UID2, IdentityType.Email, tokenVersion);
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, domainName);
        assertTrue(res.getIsClientSideGenerated());
        assertTrue(res.isSuccess());
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    @ParameterizedTest
    @CsvSource({
            ", V2",
            "example.net, V2", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "example.edu, V2", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "com.123.Game.App.ios, V2", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "123456780, V2", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "foo.com, V2",     // Domain not associated with any site.
            ", V3",
            "example.net, V3", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "example.edu, V3", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "com.123.Game.App.ios, V3", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "123456780, V3", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "foo.com, V3",     // Domain not associated with any site.
            ", V4",
            "example.net, V4", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "example.edu, V4", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "com.123.Game.App.ios, V4", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "123456780, V4", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "foo.com, V4",     // Domain not associated with any site.
    })
    public void tokenIsCstgDerivedDomainOrAppNameFailTest(String domainName, TokenVersionForTesting tokenVersion) throws Exception {
        refresh(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        int privacyBits = PrivacyBitsBuilder.Builder().WithClientSideGenerated(true).Build();

        String advertisingToken = AdvertisingTokenBuilder.builder().withVersion(tokenVersion).withPrivacyBits(privacyBits).build();

        validateAdvertisingToken(advertisingToken, IdentityScope.UID2, IdentityType.Email, tokenVersion);
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, domainName);
        assertTrue(res.getIsClientSideGenerated());
        assertFalse(res.isSuccess());
        assertEquals(DecryptionStatus.DOMAIN_OR_APP_NAME_CHECK_FAILED, res.getStatus());
        assertNull(res.getUid());
    }

    // Any domain or app name is OK, because the token is not client-side generated.
    @ParameterizedTest
    @CsvSource({
            ", V2",
            "example.net, V2", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "example.edu, V2", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "com.123.Game.App.ios, V2", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "123456780, V2", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "foo.com, V2",     // Domain not associated with any site.
            ", V3",
            "example.net, V3", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "example.edu, V3", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "com.123.Game.App.ios, V3", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "123456780, V3", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "foo.com, V3",     // Domain not associated with any site.
            ", V4",
            "example.net, V4", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "example.edu, V4", // Domain associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "com.123.Game.App.ios, V4", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "123456780, V4", // App associated with site SITE_ID2, as defined by keyBidstreamResponse().
            "foo.com, V4",     // Domain not associated with any site.
    })
    public void tokenIsNotCstgDerivedDomainNameSuccessTest(String domainName, TokenVersionForTesting tokenVersion) throws Exception {
        refresh(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        int privacyBits = PrivacyBitsBuilder.Builder().WithClientSideGenerated(false).Build();

        String advertisingToken = AdvertisingTokenBuilder.builder().withVersion(tokenVersion).withPrivacyBits(privacyBits).build();

        validateAdvertisingToken(advertisingToken, IdentityScope.UID2, IdentityType.Email, tokenVersion);
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, domainName);
        assertFalse(res.getIsClientSideGenerated());
        assertTrue(res.isSuccess());
        assertEquals(DecryptionStatus.SUCCESS, res.getStatus());
        assertEquals(EXAMPLE_UID, res.getUid());
    }

    // tests below taken from EncryptionTestsV4.cs above "//  Sharing tests" comment (but excluding deprecated EncryptData/DecryptData methods) and modified to use BidstreamClient and the new JSON /key/bidstream response
    @Test
    public void emptyKeyContainer() throws Exception {
        String advertisingToken = AdvertisingTokenBuilder.builder().build();
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.NOT_INITIALIZED, res.getStatus());
    }

    @Test
    public void expiredKeyContainer() throws Exception {
        String advertisingToken = AdvertisingTokenBuilder.builder().build();

        Key masterKeyExpired = new Key(MASTER_KEY_ID, -1, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getMasterSecret());
        Key siteKeyExpired = new Key(SITE_KEY_ID, SITE_ID, NOW, NOW.minus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.HOURS), getSiteSecret());
        refresh(keyBidstreamResponse(IdentityScope.UID2, masterKeyExpired, siteKeyExpired));

        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.KEYS_NOT_SYNCED, res.getStatus());
    }

    @Test
    public void notAuthorizedForMasterKey() throws Exception {
        String advertisingToken = AdvertisingTokenBuilder.builder().build();

        Key anotherMasterKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 1, -1, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getMasterSecret());
        Key anotherSiteKey = new Key(MASTER_KEY_ID + SITE_KEY_ID + 2, SITE_ID, NOW, NOW, NOW.plus(1, ChronoUnit.HOURS), getSiteSecret());
        refresh(keyBidstreamResponse(IdentityScope.UID2, anotherMasterKey, anotherSiteKey));

        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY, res.getStatus());
    }

    @Test
    public void invalidPayload() throws Exception {
        String payload = AdvertisingTokenBuilder.builder().build();
        byte[] payloadInBytes = Uid2Base64UrlCoder.decode(payload);
        String advertisingToken = Uid2Base64UrlCoder.encode(Arrays.copyOfRange(payloadInBytes, 0, payloadInBytes.length - 1));
        refresh(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null);
        assertEquals(DecryptionStatus.INVALID_PAYLOAD, res.getStatus());
    }

    @Test
    public void tokenExpiryAndCustomNow() throws Exception {
        final Instant expiry = Instant.parse("2021-03-22T09:01:02Z");
        final Instant generated = expiry.minus(60, ChronoUnit.SECONDS);

        refresh(keyBidstreamResponse(IdentityScope.UID2, MASTER_KEY, SITE_KEY));
        String advertisingToken = AdvertisingTokenBuilder.builder().withExpiry(expiry).withGenerated(generated).build();

        DecryptionResponse res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null, expiry.plus(1, ChronoUnit.SECONDS));
        assertEquals(DecryptionStatus.EXPIRED_TOKEN, res.getStatus());

        res = bidstreamClient.decryptTokenIntoRawUid(advertisingToken, null, expiry.minus(1, ChronoUnit.SECONDS));
        assertEquals(EXAMPLE_UID, res.getUid());

        // case when domain / app name is present
        int privacyBits = PrivacyBitsBuilder.Builder().WithClientSideGenerated(true).Build();
        String cstgAdvertisingToken = AdvertisingTokenBuilder.builder().withExpiry(expiry).withGenerated(generated)
                .withPrivacyBits(privacyBits).build();
        res = bidstreamClient.decryptTokenIntoRawUid(cstgAdvertisingToken, "example.com", expiry.minus(1, ChronoUnit.SECONDS));
        assertTrue(res.isSuccess());
    }

    @ParameterizedTest
    @EnumSource(IdentityScope.class)
    void decryptV4TokenEncodedAsBase64(IdentityScope identityScope) throws Exception {
        refresh(keyBidstreamResponse(identityScope, MASTER_KEY, SITE_KEY));

        String advertisingToken;
        do {
            advertisingToken = AdvertisingTokenBuilder.builder()
                    .withVersion(TokenVersionForTesting.V4)
                    .withScope(identityScope)
                    .build();

            byte[] tokenAsBytes = Uid2Base64UrlCoder.decode(advertisingToken);
            advertisingToken = Base64.getEncoder().encodeToString(tokenAsBytes);
        }
        while (!advertisingToken.contains("=") || !advertisingToken.contains("/") || !advertisingToken.contains("+"));

        decryptAndAssertSuccess(advertisingToken, TokenVersionForTesting.V4);
    }

    private void refresh(String json) {
        RefreshResponse refreshResponse = bidstreamClient.refreshJson(json);
        assertTrue(refreshResponse.isSuccess());
    }

    private void decryptAndAssertSuccess(String token, TokenVersionForTesting tokenVersion) {
        DecryptionResponse response = bidstreamClient.decryptTokenIntoRawUid(token, null);
        assertSuccess(response, tokenVersion);
    }

    static void assertSuccess(DecryptionResponse response, TokenVersionForTesting tokenVersion) {
        assertTrue(response.isSuccess());
        assertEquals(EXAMPLE_UID, response.getUid());
        assertEquals(tokenVersion.ordinal() + 2, response.getAdvertisingTokenVersion());
        if (tokenVersion != TokenVersionForTesting.V2) {
            assertEquals(IdentityType.Email, response.getIdentityType());
        }
    }

    static void assertFails(DecryptionResponse response, TokenVersionForTesting tokenVersion) {
        assertFalse(response.isSuccess());
        assertEquals(DecryptionStatus.INVALID_TOKEN_LIFETIME, response.getStatus());
        assertEquals(tokenVersion.ordinal() + 2, response.getAdvertisingTokenVersion());
        if (tokenVersion != TokenVersionForTesting.V2) {
            assertEquals(IdentityType.Email, response.getIdentityType());
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
        domainNames1.add("com.123.Game.App.android");
        domainNames1.add("123456789");
        site1.add("domain_names", domainNames1);
        site1.addProperty("unexpected_domain_field", "123");


        JsonObject site2 = new JsonObject();
        site2.addProperty("id", SITE_ID2);
        JsonArray domainNames2 = new JsonArray();
        domainNames2.add("example.net");
        domainNames2.add("example.edu");
        site2.add("domain_names", domainNames2);
        site2.addProperty("unexpected_domain_field", "123");

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
