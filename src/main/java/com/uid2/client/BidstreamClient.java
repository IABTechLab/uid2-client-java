package com.uid2.client;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class BidstreamClient {
    private final TokenHelper tokenHelper;

    public BidstreamClient(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        tokenHelper = new TokenHelper(uid2BaseUrl, clientApiKey, base64SecretKey);
    }

    public DecryptionResponse decryptTokenIntoRawUid(String token, String domainNameFromBidRequest) {
        return tokenHelper.decrypt(token, Instant.now(), domainNameFromBidRequest, ClientType.BIDSTREAM);
    }

    public RefreshResponse refresh() {
        return tokenHelper.refresh("/v2/key/bidstream");
    }

    public RefreshResponse refreshJson(String json) {
        return tokenHelper.refreshJson(json);
    }
}
