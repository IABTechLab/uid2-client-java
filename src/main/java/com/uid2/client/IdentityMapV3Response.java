package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentityMapV3Response {
    IdentityMapV3Response(String response, IdentityMapV3Input identityMapInput) {
        ApiResponse apiResponse = new Gson().fromJson(response, ApiResponse.class);
        status = apiResponse.status;

        if (!isSuccess()) {
            throw new Uid2Exception("Got unexpected identity map status: " + status);
        }

        populateIdentities(apiResponse.body, identityMapInput);
    }

    private void populateIdentities(Map<String, List<ApiIdentity>> apiResponse, IdentityMapV3Input identityMapInput) {
        for (Map.Entry<String, List<ApiIdentity>> identitiesForType : apiResponse.entrySet()) {
            populateIdentitiesForType(identityMapInput, identitiesForType.getKey(), identitiesForType.getValue());
        }
    }

    private void populateIdentitiesForType(IdentityMapV3Input identityMapInput, String identityType, List<ApiIdentity> identities) {
        for (int i = 0; i < identities.size(); i++) {
            ApiIdentity apiIdentity = identities.get(i);
            List<String> inputDiis = identityMapInput.getInputDiis(identityType, i);
            for (String inputDii : inputDiis) {
                if (apiIdentity.error != null) {
                    unmappedIdentities.put(inputDii, new UnmappedIdentity(apiIdentity));
                } else {
                    mappedIdentities.put(inputDii, new MappedIdentity(apiIdentity));
                }
            }
        }
    }

    private static IdentityMapV3Input getIdentityMapInput(IdentityMapV3Input identityMapInput) {
        return identityMapInput;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }

    static public class ApiResponse {
        @SerializedName("status")
        public String status;

        @SerializedName("body")
        public Map<String, List<ApiIdentity>> body;
    }

    static public class ApiIdentity {
        @SerializedName("u")
        public String currentUid;

        @SerializedName("p")
        public String previousUid;

        @SerializedName("r")
        public Long refreshFromSeconds;

        @SerializedName("e")
        public String error;
    }

    static public class MappedIdentity {
        public MappedIdentity(String currentUid, String previousUid, Instant refreshFrom) {
            this.currentUid = currentUid;
            this.previousUid = previousUid;
            this.refreshFrom = refreshFrom;
        }

        public MappedIdentity(ApiIdentity apiIdentity) {
            this(apiIdentity.currentUid, apiIdentity.previousUid, Instant.ofEpochSecond(apiIdentity.refreshFromSeconds));
        }

        private final String currentUid;
        private final String previousUid;
        private final Instant refreshFrom;

        public String getCurrentRawUid() {
            return currentUid;
        }

        public String getPreviousRawUid() {
            return previousUid;
        }

        public Instant getRefreshFrom() {
            return refreshFrom;
        }
    }

    static public class UnmappedIdentity {
        public UnmappedIdentity(String reason) {
            this.reason = reason;
        }

        public UnmappedIdentity(ApiIdentity apiIdentity) {
            this(apiIdentity.error);
        }

        private final String reason;

        public String getReason() {
            return reason;
        }
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
