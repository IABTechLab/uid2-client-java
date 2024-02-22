package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashMap;

public class IdentityMapResponse {
    IdentityMapResponse(String response, IdentityMapInput identityMapInput) {
        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
        status = responseJson.get("status").getAsString();

        if (!isSuccess()) {
            throw new Uid2Exception("Got unexpected identity map status: " + status);
        }

        Gson gson = new Gson();
        JsonObject body = getBodyAsJson(responseJson);

        Iterable<JsonElement> mapped = getJsonArray(body, "mapped");
        for (JsonElement identity : mapped) {
            String rawDii = getRawDii(identity, identityMapInput);
            mappedIdentities.put(rawDii, gson.fromJson(identity, MappedIdentity.class));
        }

        Iterable<JsonElement> unmapped = getJsonArray(body, "unmapped");
        for (JsonElement identity : unmapped) {
            String rawDii = getRawDii(identity, identityMapInput);
            unmappedIdentities.put(rawDii, gson.fromJson(identity, UnmappedIdentity.class));
        }
    }

    private static Iterable<JsonElement> getJsonArray(JsonObject body, String header) {
        JsonElement jsonElement = body.get(header);
        if (jsonElement == null) {
            return Collections.emptyList();
        }
        return jsonElement.getAsJsonArray();
    }

    private String getRawDii(JsonElement identity, IdentityMapInput identityMapInput) {
        String identifier = identity.getAsJsonObject().get("identifier").getAsString();
        return identityMapInput.getRawDii(identifier);
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

        @SerializedName("advertising_id")
        private final String rawUid;
        @SerializedName("bucket_id")
        private final String bucketId;
    }

    static public class UnmappedIdentity {
        public UnmappedIdentity(String reason) {
            this.reason = reason;
        }

        public String getReason() {return reason;}

        private final String reason;
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
