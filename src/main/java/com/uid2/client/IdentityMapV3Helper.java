package com.uid2.client;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class IdentityMapV3Helper {
    /**
     * @param base64SecretKey your UID2 client secret
     */
    public IdentityMapV3Helper(String base64SecretKey) {uid2Helper = new Uid2Helper(base64SecretKey);}

    /**
     * @param identityMapInput represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-identity-map">/identity/map</a>
     * @return an EnvelopeV2 instance to use in the POST body of <a href="https://unifiedid.com/docs/endpoints/post-identity-map">/identity/map</a>
     */
    public EnvelopeV2 createEnvelopeForIdentityMapRequest(IdentityMapV3Input identityMapInput) {
        byte[] jsonBytes = new Gson().toJson(identityMapInput).getBytes(StandardCharsets.UTF_8);
        return uid2Helper.createEnvelopeV2(jsonBytes);
    }


    /**
     * @param responseString the response body returned by a call to <a href="https://unifiedid.com/docs/endpoints/post-identity-map">/identity/map</a>
     * @param envelope the EnvelopeV2 instance returned by {@link #createEnvelopeForIdentityMapRequest}
     * @param identityMapInput the same instance that was passed to {@link #createEnvelopeForIdentityMapRequest}.
     * @return an IdentityMapV3Response instance
     */
    public IdentityMapV3Response createIdentityMapResponse(String responseString, EnvelopeV2 envelope, IdentityMapV3Input identityMapInput) {
        String decryptedResponseString = uid2Helper.decrypt(responseString, envelope.getNonce());
        return new IdentityMapV3Response(decryptedResponseString, identityMapInput);
    }

    Uid2Helper uid2Helper;
}
