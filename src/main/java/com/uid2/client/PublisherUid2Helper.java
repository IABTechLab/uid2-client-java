package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;


public class PublisherUid2Helper {
    /**
     * @param base64SecretKey your UID2 client secret
     */
    public PublisherUid2Helper(String base64SecretKey) {
        secretKey = InputUtil.base64ToByteArray(base64SecretKey);
    }

    /**
     * @param tokenGenerateInput represents the input required for <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md#unencrypted-json-body-parameters">/token/generate</a>
     * @return an EnvelopeV2 instance to use in the POST body of <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md">/token/generate</a>
     */
    public EnvelopeV2 createEnvelopeForTokenGenerateRequest(TokenGenerateInput tokenGenerateInput) {
        final int nonceLength = 8;
        byte[] nonce = new byte[nonceLength];
        secureRandom.nextBytes(nonce);
        return createEnvelopeImpl(tokenGenerateInput, nonce, Instant.now(), null);
    }

    /**
     * @param response the response body returned by a call to <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md">/token/generate</a>
     * @param envelope the EnvelopeV2 instance returned by {@link #createEnvelopeForTokenGenerateRequest}
     * @return an IdentityTokens instance
     */
    public IdentityTokens createIdentityfromTokenGenerateResponse(String response, EnvelopeV2 envelope) {
        String identityJsonString = decrypt(response, secretKey, false, envelope.getNonce());
        JsonObject jsonIdentity = new Gson().fromJson(identityJsonString, JsonObject.class);

        if (!"success".equals(jsonIdentity.get("status").getAsString())) {
            throw new Uid2Exception("Got unexpected token generate status in decrypted response:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIdentity));
        }

        return IdentityTokens.fromJson(getBodyAsJson(jsonIdentity));
    }

    /**
     * @param encryptedResponse the response body returned by a call to <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-refresh.md">/token/refresh</a>
     * @param currentIdentity the current IdentityTokens instance, typically retrieved from a user's session
     * @return the refreshed IdentityTokens instance (with a new advertising token and updated expiry times). Typically this will be used to replace the current identity in the user's session
     */
    public static IdentityTokens createIdentityFromTokenRefreshResponse(String encryptedResponse, IdentityTokens currentIdentity) {
        return createIdentityFromTokenRefreshResponseImpl(encryptedResponse, currentIdentity, Instant.now());
    }

    EnvelopeV2 createEnvelopeImpl(TokenGenerateInput tokenGenerateInput, byte[] nonce, Instant timestamp, byte[] iv) {
        byte[] jsonEmailBytes = tokenGenerateInput.getAsJsonString().getBytes(StandardCharsets.UTF_8);

        //Note this is very similar to UID2Client.makeV2Request (could share implementation)
        ByteBuffer writer = ByteBuffer.allocate(TIMESTAMP_LENGTH + nonce.length + jsonEmailBytes.length);
        writer.putLong(timestamp.toEpochMilli());
        writer.put(nonce);
        writer.put(jsonEmailBytes);

        byte[] encrypted = Decryption.encryptGCM(writer.array(), iv, secretKey);
        ByteBuffer envelopeBuffer = ByteBuffer.allocate(encrypted.length + 1);
        final byte envelopeVersion = 1;
        envelopeBuffer.put(envelopeVersion);
        envelopeBuffer.put(encrypted);
        return new EnvelopeV2(InputUtil.byteArrayToBase64(envelopeBuffer.array()), nonce);
    }

    static IdentityTokens createIdentityFromTokenRefreshResponseImpl(String encryptedResponse, IdentityTokens currentIdentity, Instant timestamp) {
        String response;
        String refreshResponseKey = currentIdentity.getRefreshResponseKey();
        if (refreshResponseKey != null) {
            response = decrypt(encryptedResponse, InputUtil.base64ToByteArray(refreshResponseKey), true, null);
        } else { //if refresh_response_key doesn't exist, assume refresh_token came from a v1/token/generate query. In that scenario, /v2/token/refresh will return an unencrypted response.
            response = encryptedResponse;
        }

        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
        String status = responseJson.get("status").getAsString();

        if ("optout".equals(status)) {
            return null;
        } else if (!"success".equals(status)) {
            throw new Uid2Exception("Got unexpected token refresh status: " + status);
        }

        IdentityTokens refreshedIdentity = IdentityTokens.fromJson(getBodyAsJson(responseJson));
        if (!refreshedIdentity.isRefreshableImpl(timestamp) || refreshedIdentity.hasIdentityExpired(timestamp)) {
            throw new Uid2Exception("Invalid identity in token refresh response: " + response);
        }

        return refreshedIdentity;
    }


    private static String decrypt(String response, byte[] secretKey, boolean isRefreshResponse, byte[] nonceInRequest)
    {
        //from parseV2Response
        byte[] responseBytes = InputUtil.base64ToByteArray(response);
        byte[] payload = Decryption.decryptGCM(responseBytes, 0, secretKey);

        byte[] resultBytes;
        if (!isRefreshResponse) {
            byte[] nonceInResponse = Arrays.copyOfRange(payload, TIMESTAMP_LENGTH, TIMESTAMP_LENGTH + nonceInRequest.length);
            if (!Arrays.equals(nonceInResponse, nonceInRequest)) {
                throw new Uid2Exception("Nonce in request does not match nonce in response");
            }
            resultBytes = Arrays.copyOfRange(payload, TIMESTAMP_LENGTH + nonceInRequest.length, payload.length);
        }
        else {
            resultBytes = payload;
        }
        return new String(resultBytes, StandardCharsets.UTF_8);
    }

    private static JsonObject getBodyAsJson(JsonObject jsonResponse) {
        return jsonResponse.get("body").getAsJsonObject();
    }

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] secretKey;
    private static final int TIMESTAMP_LENGTH = 8;
}
