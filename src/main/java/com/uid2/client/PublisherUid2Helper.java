package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;


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
     * @throws Uid2Exception if the response did not contain a "success" status
     */
    public IdentityTokens createIdentityfromTokenGenerateResponse(String response, EnvelopeV2 envelope) {
        String identityJsonString = decrypt(response, secretKey, false, envelope.getNonce());
        JsonObject responseJson = new Gson().fromJson(identityJsonString, JsonObject.class);

        if (!"success".equals(responseJson.get("status").getAsString())) {
            throw new Uid2Exception("Got unexpected token generate status in decrypted response:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(responseJson));
        }

        return IdentityTokens.fromJson(TokenRefreshResponse.getBodyAsJson(responseJson));
    }

    /**
     * @param encryptedResponse the response body returned by a call to <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-refresh.md">/token/refresh</a>
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
        return "java-diy-" + getArtifactAndVersion();
    }

    static String getArtifactAndVersion() {
        return artifactAndVersion;
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

    static TokenRefreshResponse createTokenRefreshResponseImpl(String encryptedResponse, IdentityTokens currentIdentity, Instant timestamp) {
        String response;
        String refreshResponseKey = currentIdentity.getRefreshResponseKey();
        if (refreshResponseKey != null) {
            response = decrypt(encryptedResponse, InputUtil.base64ToByteArray(refreshResponseKey), true, null);
        } else { //if refresh_response_key doesn't exist, assume refresh_token came from a v1/token/generate query. In that scenario, /v2/token/refresh will return an unencrypted response.
            response = encryptedResponse;
        }

        return new TokenRefreshResponse(response, timestamp);
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

    private static String setArtifactAndVersion() {
        String artifactAndVersion;

        try { // https://stackoverflow.com/a/3697482/297451
            Class<?> cls = Class.forName("com.uid2.client.PublisherUid2Client");

            InputStream istream = cls.getClassLoader().getResourceAsStream("project.properties");
            if (istream == null) {
                throw new IOException("project.properties not found");
            }

            final Properties properties = new Properties();
            properties.load(istream);
            artifactAndVersion = properties.getProperty("artifactId") + "-" + properties.getProperty("version");
        } catch (ClassNotFoundException | IOException e) {
            artifactAndVersion = "<err:" + e.getMessage() + ">"; //keep this short (getMessage() instead of just "e") as it can appear in dashboards with limited screen real-estate
        }
        return artifactAndVersion;
    }


    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] secretKey;
    private static final int TIMESTAMP_LENGTH = 8;
    private static final String artifactAndVersion = setArtifactAndVersion();
}
