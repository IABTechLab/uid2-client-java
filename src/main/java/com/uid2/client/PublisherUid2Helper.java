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
    public PublisherUid2Helper(String base64SecretKey) {
        secretKey = InputUtil.base64ToByteArray(base64SecretKey);
    }

    public Envelope createEnvelope(IdentityInput identityInput) {
        final int nonceLength = 8;
        byte[] nonce = new byte[nonceLength];
        secureRandom.nextBytes(nonce);
        return createEnvelopeImpl(identityInput, nonce, Instant.now(), null);
    }

    public IdentityTokens createIdentityfromTokenGenerateResponse(String response, byte[] nonce) {
        String identityJsonString = decrypt(response, secretKey, false, nonce);
        JsonObject jsonIdentity = new Gson().fromJson(identityJsonString, JsonObject.class);

        if (!"success".equals(jsonIdentity.get("status").getAsString())) {
            throw new Uid2Exception("Got unexpected token generate status in decrypted response:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIdentity));
        }

        return IdentityTokens.fromJson(getBodyAsJson(jsonIdentity));
    }

    public static IdentityTokens createIdentityFromTokenRefreshResponse(String encryptedResponse, IdentityTokens currentIdentity) {
        return createIdentityFromTokenRefreshResponseImpl(encryptedResponse, currentIdentity, Instant.now());
    }

    Envelope createEnvelopeImpl(IdentityInput identityInput, byte[] nonce, Instant timestamp, byte[] iv) {
        byte[] jsonEmailBytes = identityInput.getAsJsonString().getBytes(StandardCharsets.UTF_8);

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
        return new Envelope(InputUtil.byteArrayToBase64(envelopeBuffer.array()), nonce);
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
