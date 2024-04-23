package com.uid2.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.stream.Stream;

import static com.uid2.client.EncryptionTestsV4.validateAdvertisingToken;
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
        String advertisingToken = AdvertisingTokenBuilder.builder().withScope(identityScope).withVersion(tokenVersion).withEstablished(now.minus(120, ChronoUnit.DAYS)).withGenerated(now.plus(-1, ChronoUnit.DAYS)).withExpiry(now.plus(2, ChronoUnit.DAYS)).build();
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

    @ParameterizedTest
    @CsvSource({
            "example.com, V2",
            "example.org, V2",
            "example.com, V4",
            "example.org, V4",
            "example.com, V4",
            "example.org, V4"
    })
    public void TokenIsCstgDerivedTest(String domainName, TokenVersionForTesting tokenVersion) throws Exception {
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
