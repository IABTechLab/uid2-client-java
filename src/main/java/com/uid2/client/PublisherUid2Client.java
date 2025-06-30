package com.uid2.client;

public class PublisherUid2Client {
    /**
     * @param uid2BaseUrl     The <a href="https://unifiedid.com/docs/getting-started/gs-environments">UID2 Base URL</a>
     * @param clientApiKey    Your client API key
     * @param base64SecretKey Your client secret key
     */
    public PublisherUid2Client(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        publisherUid2Helper = new PublisherUid2Helper(base64SecretKey);
        uid2ClientHelper = new Uid2ClientHelper(uid2BaseUrl, clientApiKey);
    }

    /**
     * @param tokenGenerateInput represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-token-generate#unencrypted-json-body-parameters">/token/generate</a>
     * @return an IdentityTokens instance
     * @throws Uid2Exception if the response did not contain a "success" status, or the response code was not 200, or there was an error communicating with the provided UID2 Base URL
     * @deprecated Use {@link PublisherUid2Client#generateTokenResponse}
     */
    @Deprecated
    public IdentityTokens generateToken(TokenGenerateInput tokenGenerateInput) {
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(tokenGenerateInput);

        String responseString = uid2ClientHelper.makeRequest("/v2/token/generate", envelope).getAsString();
        return publisherUid2Helper.createIdentityfromTokenGenerateResponse(responseString, envelope);
    }

    /**
     * @param tokenGenerateInput represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-token-generate#unencrypted-json-body-parameters">/token/generate</a>
     * @return an TokenGenerateResponse instance, which will contain an IdentityTokens instance, if successful.
     * @throws Uid2Exception if the response did not contain a "success" or "optout" status, or the response code was not 200, or there was an error communicating with the provided UID2 Base URL
     */
    public TokenGenerateResponse generateTokenResponse(TokenGenerateInput tokenGenerateInput) {
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(tokenGenerateInput);

        String responseString = uid2ClientHelper.makeRequest("/v2/token/generate", envelope).getAsString();
        return publisherUid2Helper.createTokenGenerateResponse(responseString, envelope);
    }

    /**
     * @param currentIdentity the current IdentityTokens instance, typically retrieved from a user's session
     * @return the refreshed IdentityTokens instance (with a new advertising token and updated expiry times). Typically, this will be used to replace the current identity in the user's session
     */
    public TokenRefreshResponse refreshToken(IdentityTokens currentIdentity) {
        String responseString = uid2ClientHelper.makeRequest("/v2/token/refresh", currentIdentity.getRefreshToken()).getAsString();
        return PublisherUid2Helper.createTokenRefreshResponse(responseString, currentIdentity);
    }

    private final PublisherUid2Helper publisherUid2Helper;
    private final Uid2ClientHelper uid2ClientHelper;
}


