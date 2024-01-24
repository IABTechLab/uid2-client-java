package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class IdentityMapResponse {
    IdentityMapResponse(String response) {
        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
        status = responseJson.get("status").getAsString();

        if (!isSuccess()) {
            throw new Uid2Exception("Got unexpected identity map status: " + status);
        }

        JsonObject body = getBodyAsJson(responseJson);

        JsonArray mapped = body.get("mapped").getAsJsonArray();
        for (JsonElement identity  : mapped) {
            JsonObject identityObject = identity.getAsJsonObject();
            mappedIdentities.add(new MappedIdentity(getJsonString(identityObject, "identifier"), getJsonString(identityObject, "advertising_id"), getJsonString(identityObject, "bucket_id")));
        }

        JsonArray unmapped = body.get("unmapped").getAsJsonArray();
        for (JsonElement identity  : unmapped) {
            JsonObject identityObject = identity.getAsJsonObject();
            unmappedIdentities.add(new UnmappedIdentity(getJsonString(identityObject, "identifier"), getJsonString(identityObject, "reason")));
        }

    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
    static JsonObject getBodyAsJson(JsonObject jsonResponse) {
        return jsonResponse.get("body").getAsJsonObject();
    }

    static public class MappedIdentity {
        public MappedIdentity(String identifier, String rawUid, String bucketId) {
            this.identifier = identifier;
            this.rawUid = rawUid;
            this.bucketId = bucketId;
        }

        public String getIdentifier() {return identifier;}
        public String getRawUid() {return rawUid;}
        public String getBucketId() {return bucketId;}

        private final String identifier;
        private final String rawUid;
        private final String bucketId;
    }

    static public class UnmappedIdentity {
        public UnmappedIdentity(String identifier, String reason) {
            this.identifier = identifier;
            this.reason = reason;
        }

        public String getIdentifier() {return identifier;}
        public String getReason() {return reason;}

        private final String identifier;
        private final String reason;
    }

    static private String getJsonString(JsonObject json, String key) {
        JsonElement keyElem = json.get(key);
        return keyElem.getAsString();
    }

    public List<MappedIdentity> getMappedIdentities() {
        return mappedIdentities;
    }

    public List<UnmappedIdentity> getUnmappedIdentities() {
        return unmappedIdentities;
    }

    private final String status;
    private final List<MappedIdentity> mappedIdentities = new ArrayList<>();
    private final List<UnmappedIdentity> unmappedIdentities = new ArrayList<>();
}
