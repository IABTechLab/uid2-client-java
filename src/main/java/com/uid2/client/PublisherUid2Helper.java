package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class PublisherUid2Helper {
    /**
     * @param base64SecretKey your UID2 client secret
     */
    public PublisherUid2Helper(String base64SecretKey) {
        uid2Helper = new Uid2Helper(base64SecretKey);
    }

    /**
     * @param tokenGenerateInput represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-token-generate#unencrypted-json-body-parameters">/token/generate</a>
     * @return an EnvelopeV2 instance to use in the POST body of <a href="https://unifiedid.com/docs/endpoints/post-token-generate">/token/generate</a>
     */
    public EnvelopeV2 createEnvelopeForTokenGenerateRequest(TokenGenerateInput tokenGenerateInput) {
        return createEnvelopeImpl(tokenGenerateInput, uid2Helper.createNonce(), Instant.now(), null);
    }

    /**
     * @deprecated Use {@link PublisherUid2Helper#createTokenGenerateResponse}
     * @param response the response body returned by a call to <a href="https://unifiedid.com/docs/endpoints/post-token-generate">/token/generate</a>
     * @param envelope the EnvelopeV2 instance returned by {@link #createEnvelopeForTokenGenerateRequest}
     * @return an IdentityTokens instance
     * @throws Uid2Exception if the response did not contain a "success" status
     */
    @Deprecated
    public IdentityTokens createIdentityfromTokenGenerateResponse(String response, EnvelopeV2 envelope) {
        String identityJsonString = uid2Helper.decrypt(response, envelope.getNonce());
        JsonObject responseJson = new Gson().fromJson(identityJsonString, JsonObject.class);

        if (!"success".equals(responseJson.get("status").getAsString())) {
            throw new Uid2Exception("Got unexpected token generate status in decrypted response:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(responseJson));
        }

        return IdentityTokens.fromJson(TokenRefreshResponse.getBodyAsJson(responseJson));
    }

    /**
     * @param response the response body returned by a call to <a href="https://unifiedid.com/docs/endpoints/post-token-generate">/token/generate</a>
     * @param envelope the EnvelopeV2 instance returned by {@link #createEnvelopeForTokenGenerateRequest}
     * @return an TokenGenerateResponse instance
     * @throws Uid2Exception if the response did not contain a "success" or "optout" status
     */
    public TokenGenerateResponse createTokenGenerateResponse(String response, EnvelopeV2 envelope) {
        String identityJsonString = uid2Helper.decrypt(response, envelope.getNonce());

        return new TokenGenerateResponse(identityJsonString);
    }

    /**
     * @param encryptedResponse the response body returned by a call to <a href="https://unifiedid.com/docs/endpoints/post-token-refresh">/token/refresh</a>
     * @param currentIdentity the current IdentityTokens instance, typically retrieved from a user's session
     * @return the refreshed IdentityTokens instance (with a new advertising token and updated expiry times). Typically, this will be used to replace the current identity in the user's session
     */
    public static TokenRefreshResponse createTokenRefreshResponse(String encryptedResponse, IdentityTokens currentIdentity) {
        return createTokenRefreshResponseImpl(encryptedResponse, currentIdentity, Instant.now());
    }

    /**
     * @return the SDK version string, to be used with an "X-UID2-Client-Version" HTTP header. This header is required for all requests sent to /token/generate and /token/refresh.
     */
    public static String getVersionHttpHeader() {
        return "java-diy-" + Uid2Helper.getArtifactAndVersion();
    }

    EnvelopeV2 createEnvelopeImpl(TokenGenerateInput tokenGenerateInput, byte[] nonce, Instant timestamp, byte[] iv) {
        byte[] jsonEmailBytes = tokenGenerateInput.getAsJsonString().getBytes(StandardCharsets.UTF_8);

        return uid2Helper.createEnvelopeV2(nonce, timestamp, iv, jsonEmailBytes);
    }


    static TokenRefreshResponse createTokenRefreshResponseImpl(String encryptedResponse, IdentityTokens currentIdentity, Instant timestamp) {
        String response;
        String refreshResponseKey = currentIdentity.getRefreshResponseKey();
        if (refreshResponseKey != null) {
            response = Uid2Helper.decryptTokenRefreshResponse(encryptedResponse, InputUtil.base64ToByteArray(refreshResponseKey));
        } else { //if refresh_response_key doesn't exist, assume refresh_token came from a v1/token/generate query. In that scenario, /v2/token/refresh will return an unencrypted response.
            response = encryptedResponse;
        }

        return new TokenRefreshResponse(response, timestamp);
    }


    Uid2Helper uid2Helper;
}
