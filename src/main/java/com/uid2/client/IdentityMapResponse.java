package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;

public class IdentityMapResponse {
    IdentityMapResponse(String response, IdentityMapInput identityMapInput) {
        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
        status = responseJson.get("status").getAsString();

        if (!isSuccess()) {
            throw new Uid2Exception("Got unexpected identity map status: " + status);
        }

        JsonObject body = getBodyAsJson(responseJson);

        JsonArray mapped = body.get("mapped").getAsJsonArray();
        for (JsonElement identity  : mapped) {
            JsonObject identityObject = identity.getAsJsonObject();
            String identifier = getJsonString(identityObject, "identifier");
            String rawDii = identityMapInput.getRawDii(identifier);
            mappedIdentities.put(rawDii, new MappedIdentity(getJsonString(identityObject, "advertising_id"), getJsonString(identityObject, "bucket_id")));
        }

        JsonArray unmapped = body.get("unmapped").getAsJsonArray();
        for (JsonElement identity  : unmapped) {
            JsonObject identityObject = identity.getAsJsonObject();
            String identifier = getJsonString(identityObject, "identifier");
            String rawDii = identityMapInput.getRawDii(identifier);
            unmappedIdentities.put(rawDii, new UnmappedIdentity(getJsonString(identityObject, "reason")));
        }

    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
    static JsonObject getBodyAsJson(JsonObject jsonResponse) {
        return jsonResponse.get("body").getAsJsonObject();
    }

    static public class MappedIdentity {
        public MappedIdentity(String rawUid, String bucketId) {
            this.rawUid = rawUid;
            this.bucketId = bucketId;
        }

        public String getRawUid() {return rawUid;}
        public String getBucketId() {return bucketId;}

        private final String rawUid;
        private final String bucketId;
    }

    static public class UnmappedIdentity {
        public UnmappedIdentity(String reason) {
            this.reason = reason;
        }

        public String getReason() {return reason;}

        private final String reason;
    }

    static private String getJsonString(JsonObject json, String key) {
        JsonElement keyElem = json.get(key);
        return keyElem.getAsString();
    }

    public HashMap<String, MappedIdentity> getMappedIdentities() {
        return mappedIdentities;
    }

    public HashMap<String, UnmappedIdentity> getUnmappedIdentities() {
        return unmappedIdentities;
    }

    private final String status;
    private final HashMap<String, MappedIdentity> mappedIdentities = new HashMap<>();
    private final HashMap<String, UnmappedIdentity> unmappedIdentities = new HashMap<>();
}
