package com.uid2.client;

import java.nio.charset.StandardCharsets;

public class AdvertiserAndDataProviderUid2Helper {
    /**
     * @param base64SecretKey your UID2 client secret
     */
    public AdvertiserAndDataProviderUid2Helper(String base64SecretKey) {uid2Helper = new Uid2Helper(base64SecretKey);}

    public EnvelopeV2 createEnvelopeForIdentityMapRequest(IdentityMapInput identityMapInput) {
        byte[] jsonBytes = identityMapInput.getAsJsonString().getBytes(StandardCharsets.UTF_8);
        return uid2Helper.createEnvelopeV2(jsonBytes);
    }

    public IdentityMapResponse createIdentityMapResponse(String responseString, EnvelopeV2 envelope) {
        String decryptedResponseString = uid2Helper.decrypt(responseString, envelope.getNonce());
        return new IdentityMapResponse(decryptedResponseString);
    }

    Uid2Helper uid2Helper;
}
