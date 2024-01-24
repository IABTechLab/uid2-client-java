package com.uid2.client;

public class AdvertiserAndDataProviderUid2Client {
    AdvertiserAndDataProviderUid2Client(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        advertiserAndDataProviderUid2Helper = new AdvertiserAndDataProviderUid2Helper(base64SecretKey);
        uid2ClientHelper = new Uid2ClientHelper(uid2BaseUrl, clientApiKey);

    }

    public IdentityMapResponse generateIdentityMap(IdentityMapInput identityMapInput) {
        EnvelopeV2 envelope = advertiserAndDataProviderUid2Helper.createEnvelopeForIdentityMapRequest(identityMapInput);

        String responseString = uid2ClientHelper.makeRequest(envelope, "/v2/identity/map");
        return advertiserAndDataProviderUid2Helper.createIdentityMapResponse(responseString, envelope);
    }


    AdvertiserAndDataProviderUid2Helper advertiserAndDataProviderUid2Helper;
    Uid2ClientHelper uid2ClientHelper;
}
