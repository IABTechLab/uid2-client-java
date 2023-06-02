package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.Instant;

public class TokenGenerateResponse {
    /**
     * @return {@link IdentityTokens#getJsonString()} if this was a successful response ({@link #isSuccess}), otherwise null
     */
    public String getIdentityJsonString() {
        return isSuccess() ? getIdentity().getJsonString() : null;
    }

    /**
     * @return whether this was a successful response
     */
    public boolean isSuccess() {
        return "success".equals(status);
    }

    /**
     * @return whether the user has opted out. See <a href="https://unifiedid.com/docs/endpoints/post-token-generate#optout">Optout</a>
     */
    public boolean isOptout() {
        return "optout".equals(status);
    }

    /**
     * @return the refreshed IdentityTokens instance if {@link #isSuccess} is true, otherwise null
     */
    public IdentityTokens getIdentity() {
        return tokens;
    }

    static JsonObject getBodyAsJson(JsonObject jsonResponse) {
        return jsonResponse.get("body").getAsJsonObject();
    }

    TokenGenerateResponse(String response) {
        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
        status = responseJson.get("status").getAsString();

        if (isOptout()) {
            return;
        } else if (!isSuccess()) {
            throw new Uid2Exception("Got unexpected token generate status: " + status);
        }

        tokens = IdentityTokens.fromJson(getBodyAsJson(responseJson));
    }


    private final String status;
    private IdentityTokens tokens;
}
