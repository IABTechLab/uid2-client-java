package com.uid2.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;

public class Uid2Helper {
    Uid2Helper(String base64SecretKey) {
        secretKey = InputUtil.base64ToByteArray(base64SecretKey);
    }

    public EnvelopeV2 createEnvelopeV2(byte[] jsonBytes) {
        return createEnvelopeV2(createNonce(), Instant.now(), null, jsonBytes);
    }

    public EnvelopeV2 createEnvelopeV2(byte[] nonce, Instant timestamp, byte[] iv, byte[] jsonBytes) {
        //Note this is very similar to UID2Client.makeV2Request (could share implementation)
        ByteBuffer writer = ByteBuffer.allocate(TIMESTAMP_LENGTH + nonce.length + jsonBytes.length);
        writer.putLong(timestamp.toEpochMilli());
        writer.put(nonce);
        writer.put(jsonBytes);

        byte[] encrypted = Uid2Encryption.encryptGCM(writer.array(), iv, secretKey);
        ByteBuffer envelopeBuffer = ByteBuffer.allocate(encrypted.length + 1);
        final byte envelopeVersion = 1;
        envelopeBuffer.put(envelopeVersion);
        envelopeBuffer.put(encrypted);
        return new EnvelopeV2(InputUtil.byteArrayToBase64(envelopeBuffer.array()), nonce);
    }

    public String decrypt(String response, byte[] nonceInRequest) {
        return decrypt(response, secretKey, false, nonceInRequest);
    }

    static String decryptTokenRefreshResponse(String response, byte[] secretKey) {
        return decrypt(response, secretKey, true, null);
    }

    private static String decrypt(String response, byte[] secretKey, boolean isRefreshResponse, byte[] nonceInRequest) {
        //from parseV2Response
        byte[] responseBytes = InputUtil.base64ToByteArray(response);
        byte[] payload = Uid2Encryption.decryptGCM(responseBytes, 0, secretKey);

        byte[] resultBytes;
        if (!isRefreshResponse) {
            byte[] nonceInResponse = Arrays.copyOfRange(payload, TIMESTAMP_LENGTH, TIMESTAMP_LENGTH + nonceInRequest.length);
            if (!Arrays.equals(nonceInResponse, nonceInRequest)) {
                throw new Uid2Exception("Nonce in request does not match nonce in response");
            }
            resultBytes = Arrays.copyOfRange(payload, TIMESTAMP_LENGTH + nonceInRequest.length, payload.length);
        } else {
            resultBytes = payload;
        }
        return new String(resultBytes, StandardCharsets.UTF_8);
    }

    byte[] createNonce() {
        final int nonceLength = 8;
        byte[] nonce = new byte[nonceLength];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    public static String getArtifactAndVersion() {
        return artifactAndVersion;
    }

    private static String setArtifactAndVersion() {
        String artifactAndVersion;

        try { // https://stackoverflow.com/a/3697482/297451
            Class<?> cls = Class.forName("com.uid2.client.Uid2Helper");

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


    private static final int TIMESTAMP_LENGTH = 8;
    private final byte[] secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String artifactAndVersion = setArtifactAndVersion();
}
