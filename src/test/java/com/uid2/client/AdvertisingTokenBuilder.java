package com.uid2.client;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static com.uid2.client.TestData.*;

class AdvertisingTokenBuilder {
    TokenVersionForTesting version = TokenVersionForTesting.V4;
    String rawUid = EXAMPLE_UID;
    Key masterKey = MASTER_KEY;
    Key siteKey = SITE_KEY;
    int siteId = SITE_ID;
    int privacyBits = PrivacyBitsBuilder.Builder().WithAllFlagsDisabled().Build();
    Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);
    IdentityScope identityScope = IdentityScope.UID2;
    Instant generated = Instant.now();
    Instant established = Instant.now();

    static AdvertisingTokenBuilder builder() {
        return new AdvertisingTokenBuilder();
    }

    AdvertisingTokenBuilder withVersion(TokenVersionForTesting version)
    {
        this.version = version;
        return this;
    }

    AdvertisingTokenBuilder withRawUid(String rawUid)
    {
        this.rawUid = rawUid;
        return this;
    }

    AdvertisingTokenBuilder withMasterKey(Key masterKey)
    {
        this.masterKey = masterKey;
        return this;
    }

    AdvertisingTokenBuilder withSiteKey(Key siteKey)
    {
        this.siteKey = siteKey;
        return this;
    }

    AdvertisingTokenBuilder withPrivacyBits(int privacyBits)
    {
        this.privacyBits = privacyBits;
        return this;
    }

    AdvertisingTokenBuilder withExpiry(Instant expiry)
    {
        this.expiry = expiry;
        return this;
    }

    AdvertisingTokenBuilder withScope(IdentityScope identityScope)
    {
        this.identityScope = identityScope;
        return this;
    }

    AdvertisingTokenBuilder withGenerated(Instant generated)
    {
        this.generated = generated;
        return this;
    }

    AdvertisingTokenBuilder withEstablished(Instant established)
    {
        this.established = established;
        return this;
    }

    String build() throws Exception {
        Uid2TokenGenerator.Params params = Uid2TokenGenerator.defaultParams().WithPrivacyBits(privacyBits).withTokenExpiry(expiry).WithTokenGenerated(generated).WithIdentityEstablished(established);

        params.identityScope = identityScope.value;
        String token;
        switch (version) {
            case V2:
                token = Base64.getEncoder().encodeToString(Uid2TokenGenerator.generateUid2TokenV2(rawUid, MASTER_KEY, SITE_ID, SITE_KEY, params));
                break;
            case V3:
                token = Uid2TokenGenerator.generateUid2TokenV3(rawUid, MASTER_KEY, SITE_ID, SITE_KEY, params);
                break;
            case V4:
                token = Uid2TokenGenerator.generateUid2TokenV4(rawUid, MASTER_KEY, SITE_ID, SITE_KEY, params);
                break;
            default:
                throw new Uid2Exception("Invalid token UID2 version: " + version);
        }

        IdentityType identityType = IdentityType.Email;
        if (version != TokenVersionForTesting.V2)
        {
            char firstChar = rawUid.charAt(0);
            if (firstChar == 'F' || firstChar == 'B')
                identityType = IdentityType.Phone;
        }


        EncryptionV4Tests.validateAdvertisingToken(token, identityScope, identityType, version);
        return token;
    }
}

