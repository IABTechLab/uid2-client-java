package com.uid2.client;

public class IdentityMapClient {
    /**
     * @param uid2BaseUrl       The <a href="https://unifiedid.com/docs/getting-started/gs-environments">UID2 Base URL</a>
     * @param clientApiKey      Your client API key
     * @param base64SecretKey   Your client secret key
     */
    public IdentityMapClient(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        identityMapHelper = new IdentityMapHelper(base64SecretKey);
        uid2ClientHelper = new Uid2ClientHelper(uid2BaseUrl, clientApiKey);
    }

    /**
     * @param identityMapInput  represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-identity-map">/identity/map</a>
     * @return                  an IdentityMapResponse instance
     * @throws Uid2Exception    if the response did not contain a "success" status, or the response code was not 200, or there was an error communicating with the provided UID2 Base URL
     */
    public IdentityMapResponse generateIdentityMap(IdentityMapInput identityMapInput) {
        EnvelopeV2 envelope = identityMapHelper.createEnvelopeForIdentityMapRequest(identityMapInput);

        String responseString = uid2ClientHelper.makeRequest(envelope, "/v2/identity/map");
        return identityMapHelper.createIdentityMapResponse(responseString, envelope, identityMapInput);
    }

    private final IdentityMapHelper identityMapHelper;
    private final Uid2ClientHelper uid2ClientHelper;
}
