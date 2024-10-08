package com.uid2.client;

import java.time.Instant;

public class BidstreamClient {
    private final TokenHelper tokenHelper;

    public BidstreamClient(String baseUrl, String clientApiKey, String base64SecretKey) {
        tokenHelper = new TokenHelper(baseUrl, clientApiKey, base64SecretKey);
    }

    public DecryptionResponse decryptTokenIntoRawUid(String token, String domainOrAppNameFromBidRequest) {
        return tokenHelper.decrypt(token, Instant.now(), domainOrAppNameFromBidRequest, ClientType.BIDSTREAM);
    }

    public DecryptionResponse decryptTokenIntoRawUid(String token, String domainOrAppNameFromBidRequest, Instant now) {
        return tokenHelper.decrypt(token, now, domainOrAppNameFromBidRequest, ClientType.BIDSTREAM);
    }

    public RefreshResponse refresh() {
        return tokenHelper.refresh("/v2/key/bidstream");
    }

    RefreshResponse refreshJson(String json) {
        return tokenHelper.refreshJson(json);
    }
}
