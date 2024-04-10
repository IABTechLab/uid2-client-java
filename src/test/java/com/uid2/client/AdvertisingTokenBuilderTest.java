package com.uid2.client;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.uid2.client.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdvertisingTokenBuilderTest {
    private final int V2TokenLength = 180;
    private final int V3TokenLength = 220;
    private final int V4TokenLength = 218;
    private final Key someKey = new Key(75, 10, Instant.now().plus(42, ChronoUnit.SECONDS), Instant.now().plus(42, ChronoUnit.HOURS), Instant.now().plus(42, ChronoUnit.HOURS), getMasterSecret());


    @Test
    public void createsTokenOfDesiredVersion_V2() throws Exception {
        String token = AdvertisingTokenBuilder.builder().withVersion(TokenVersionForTesting.V2).build();
        assertEquals(V2TokenLength, token.length());
    }

    @Test
    public void createsTokenOfDesiredVersion_V3() throws Exception {
        String token = AdvertisingTokenBuilder.builder().withVersion(TokenVersionForTesting.V3).build();
        assertEquals(V3TokenLength, token.length());
    }

    @Test
    public void createsTokenOfDesiredVersion_V4() throws Exception {
        String token = AdvertisingTokenBuilder.builder().withVersion(TokenVersionForTesting.V4).build();
        assertEquals(V4TokenLength, token.length());
    }

    @Test
    public void createsTokenOfDesiredVersion_ByDefaultCreatesV4Token() throws Exception {
        String token = AdvertisingTokenBuilder.builder().build();
        assertEquals(V4TokenLength, token.length());
    }

    @Test
    public void builderSetterTests_Version() {
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder().withVersion(TokenVersionForTesting.V3);
        assertEquals(TokenVersionForTesting.V3, builder.version);
    }

    @Test
    public void builderSetterTests_RawUid() {
        String rawUid = "raw uid";
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder().withRawUid(rawUid);
        assertEquals(rawUid, builder.rawUid);
    }

    @Test
    public void builderSetterTests_MasterKey() {
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder().withMasterKey(someKey);
        assertEquals(someKey, builder.masterKey);
    }

    @Test
    public void builderSetterTests_SiteKey() {
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder().withSiteKey(someKey);
        assertEquals(someKey, builder.siteKey);
    }

    @Test
    public void builderSetterTests_SiteId() {
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder();
        assertEquals(SITE_ID, builder.siteId);
    }

    @Test
    public void builderSetterTests_PrivacyBits() {
        int privacyBits = 42;
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder().withPrivacyBits(privacyBits);
        assertEquals(privacyBits, builder.privacyBits);
    }

    @Test
    public void builderSetterTests_Expiry() {
        Instant expiry = Instant.now().plus(42, ChronoUnit.HOURS);
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder().withExpiry(expiry);
        assertEquals(expiry, builder.expiry);
    }

    @Test
    public void builderSetterTests_Scope() {
        AdvertisingTokenBuilder builder = AdvertisingTokenBuilder.builder();
        assertEquals(IdentityScope.UID2, builder.identityScope);
    }
}
