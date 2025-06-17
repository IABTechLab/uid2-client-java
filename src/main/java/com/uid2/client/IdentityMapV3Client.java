package com.uid2.client;

public class IdentityMapV3Client {
    /**
     * @param uid2BaseUrl       The <a href="https://unifiedid.com/docs/getting-started/gs-environments">UID2 Base URL</a>
     * @param clientApiKey      Your client API key
     * @param base64SecretKey   Your client secret key
     */
    public IdentityMapV3Client(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        identityMapHelper = new IdentityMapV3Helper(base64SecretKey);
        uid2ClientHelper = new Uid2ClientHelper(uid2BaseUrl, clientApiKey);
    }

    /**
     * @param identityMapInput  represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-identity-map">/identity/map</a>
     * @return                  an IdentityMapV3Response instance
     * @throws Uid2Exception    if the response did not contain a "success" status, or the response code was not 200, or there was an error communicating with the provided UID2 Base URL
     */
    public IdentityMapV3Response generateIdentityMap(IdentityMapV3Input identityMapInput) {
        EnvelopeV2 envelope = identityMapHelper.createEnvelopeForIdentityMapRequest(identityMapInput);

        Uid2Response response = uid2ClientHelper.makeBinaryRequest("/v3/identity/map", envelope);
        if (response.isBinary()) {
            return identityMapHelper.createIdentityMapResponse(response.getAsBytes(), envelope, identityMapInput);
        } else {
            return identityMapHelper.createIdentityMapResponse(response.getAsString(), envelope, identityMapInput);
        }
    }

    private final IdentityMapV3Helper identityMapHelper;
    private final Uid2ClientHelper uid2ClientHelper;
}
