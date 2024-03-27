package com.uid2.client;

import java.time.Instant;

public class SharingClient {
    private final TokenHelper tokenHelper;

    public SharingClient(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        tokenHelper = new TokenHelper(uid2BaseUrl, clientApiKey, base64SecretKey);
    }

    public DecryptionResponse decryptTokenIntoRawUid(String token, String domainNameFromBidRequest) {
        return tokenHelper.decrypt(token, Instant.now(), domainNameFromBidRequest, ClientType.BIDSTREAM);
    }

    public EncryptionDataResponse encryptRawUidIntoToken(String rawUid) {
        return tokenHelper.encryptRawUidIntoToken(rawUid, Instant.now());
    }

    public void refresh() throws UID2ClientException {
        tokenHelper.refresh("/v2/key/sharing");
    }

    public void refreshJson(String json) throws UID2ClientException {
        tokenHelper.refreshJson(json);
    }
}
