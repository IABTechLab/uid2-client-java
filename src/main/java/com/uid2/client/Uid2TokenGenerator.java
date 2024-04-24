package com.uid2.client;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

class Uid2TokenGenerator {
    public static class Params
    {
        Instant tokenExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
        public int identityScope = IdentityScope.UID2.value;
        public Instant tokenGenerated = Instant.now();
        public Instant identityEstablished = Instant.now();
        public int tokenPrivacyBits = 0;

        public Params() {}
        public Params withTokenExpiry(Instant expiry) { tokenExpiry = expiry; return this; }
        public Params WithTokenGenerated(Instant generated) { tokenGenerated = generated; return this; } //when was the most recent refresh done (or if not refreshed, when was the /token/generate or CSTG call)
        public Params WithIdentityEstablished(Instant established) { identityEstablished = established; return this; } //when was the first call to /token/generate or CSTG
        public Params WithPrivacyBits(int privacyBits) {  tokenPrivacyBits = privacyBits; return this; }
    }

    public static Params defaultParams() { return new Params(); }

    public static byte[] generateUid2TokenV2(String uid, Key masterKey, long siteId, Key siteKey) throws Exception {
        return generateUid2TokenV2(uid, masterKey, siteId, siteKey, defaultParams());
    }

    public static byte[] generateUid2TokenV2(String uid, Key masterKey, long siteId, Key siteKey, Params params) throws Exception {
        Random rd = new Random();
        byte[] uidBytes = uid.getBytes(StandardCharsets.UTF_8);
        ByteBuffer identityWriter = ByteBuffer.allocate(4 + 4 + uidBytes.length + 4 + 8);

        identityWriter.putInt((int) siteId);
        identityWriter.putInt(uidBytes.length);
        identityWriter.put(uidBytes);
        identityWriter.putInt(params.tokenPrivacyBits);
        identityWriter.putLong(params.identityEstablished.toEpochMilli());
        byte[] identityIv = new byte[16];
        rd.nextBytes(identityIv);
        byte[] encryptedIdentity = encrypt(identityWriter.array(), identityIv, siteKey.getSecret());

        ByteBuffer masterWriter = ByteBuffer.allocate(8 + 4 + encryptedIdentity.length);

        masterWriter.putLong(params.tokenExpiry.toEpochMilli());
        masterWriter.putInt((int) siteKey.getId());
        masterWriter.put(encryptedIdentity);

        byte[] masterIv = new byte[16];
        rd.nextBytes(masterIv);
        byte[] encryptedMasterPayload = encrypt(masterWriter.array(), masterIv, masterKey.getSecret());

        ByteBuffer rootWriter = ByteBuffer.allocate(1 + 4 + encryptedMasterPayload.length);
        rootWriter.put((byte) 2);
        rootWriter.putInt((int) masterKey.getId());
        rootWriter.put(encryptedMasterPayload);

        return rootWriter.array();
    }

    public static String generateUid2TokenV3(String uid, Key masterKey, long siteId, Key siteKey, Params params) {
        return generateUID2TokenV3OrV4(uid, masterKey, siteId, siteKey, params, AdvertisingTokenVersion.V3);
    }

    public static String generateUid2TokenV4(String uid, Key masterKey, long siteId, Key siteKey, Params params)  {
        return generateUID2TokenV3OrV4(uid, masterKey, siteId, siteKey, params, AdvertisingTokenVersion.V4);
    }

    public static String generateEuidTokenV4(String uid, Key masterKey, long siteId, Key siteKey, Params params) {
        params.identityScope = IdentityScope.EUID.value;
        return generateUID2TokenV3OrV4(uid, masterKey, siteId, siteKey, params, AdvertisingTokenVersion.V4);
    }


    private static String generateUID2TokenV3OrV4(String uid, Key masterKey, long siteId, Key siteKey, Params params, AdvertisingTokenVersion adTokenVersion) {
        final ByteBuffer sitePayloadWriter = ByteBuffer.allocate(128);

        // publisher data
        sitePayloadWriter.putInt((int)siteId);
        sitePayloadWriter.putLong(0L); // publisher id
        sitePayloadWriter.putInt(0); // client key id

        // user identity data
        sitePayloadWriter.putInt(params.tokenPrivacyBits); // privacy bits
        sitePayloadWriter.putLong(params.identityEstablished.toEpochMilli()); // established
        sitePayloadWriter.putLong(params.tokenGenerated.toEpochMilli()); // last refreshed
        sitePayloadWriter.put(Base64.getDecoder().decode(uid));

        final ByteBuffer masterPayloadWriter = ByteBuffer.allocate(256);
        masterPayloadWriter.putLong(params.tokenExpiry.toEpochMilli());
        masterPayloadWriter.putLong(params.tokenGenerated.toEpochMilli()); //identity refreshed, seems to be identical to TokenGenerated in Operator

        // operator identity data
        masterPayloadWriter.putInt(0); // site id
        masterPayloadWriter.put((byte)1); // operator type
        masterPayloadWriter.putInt(0); // operator version
        masterPayloadWriter.putInt(0); // operator key id
        masterPayloadWriter.putInt((int)siteKey.getId());
        masterPayloadWriter.put(encryptGCM(Arrays.copyOfRange(sitePayloadWriter.array(), 0, sitePayloadWriter.position()), siteKey.getSecret()));

        final byte[] encryptedMasterPayload = encryptGCM(Arrays.copyOfRange(masterPayloadWriter.array(), 0, masterPayloadWriter.position()), masterKey.getSecret());
        final ByteBuffer rootWriter = ByteBuffer.allocate(encryptedMasterPayload.length + 6);

        char firstChar = uid.charAt(0);
        IdentityType identityType = (firstChar == 'F' || firstChar == 'B') ? IdentityType.Phone : IdentityType.Email; //see UID2-79+Token+and+ID+format+v3

        rootWriter.put((byte)((params.identityScope << 4) | (identityType.value << 2) | 3));
        rootWriter.put((byte)adTokenVersion.value());
        rootWriter.putInt((int)masterKey.getId());
        rootWriter.put(encryptedMasterPayload);

        if (adTokenVersion == AdvertisingTokenVersion.V4) {
            return Uid2Base64UrlCoder.encode(rootWriter.array());

        }
        else {
            return Base64.getEncoder().encodeToString(rootWriter.array());
        }
    }

    public static String encryptDataV2(byte[] data, Key key, int siteId, Instant now) throws Exception {
        final byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        final byte[] encryptedData = encrypt(data, iv, key.getSecret());

        final ByteBuffer writer = ByteBuffer.allocate(encryptedData.length + 18);
        writer.put((byte)128);
        writer.put((byte)1); // version
        writer.putLong(now.toEpochMilli());
        writer.putInt(siteId);
        writer.putInt((int)key.getId());
        writer.put(encryptedData);

        return Base64.getEncoder().encodeToString(writer.array());
    }

    private static byte[] encrypt(byte[] data, byte[] iv, byte[] secret) throws Exception {
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        byte[] finalized = new byte[16 + encrypted.length];
        for (int i = 0; i < 16; i++) finalized[i] = iv[i];
        for (int i = 0; i < encrypted.length; i++) finalized[16 + i] = encrypted[i];
        return finalized;
    }

    public static byte[] encryptGCM(byte[] b, byte[] secretBytes) {
        try {
            final SecretKey k = new SecretKeySpec(secretBytes, "AES");
            final Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            final byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, iv);
            c.init(Cipher.ENCRYPT_MODE, k, gcmParameterSpec);
            ByteBuffer buffer = ByteBuffer.allocate(b.length + 12 + 16);
            buffer.put(iv);
            buffer.put(c.doFinal(b));
            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Unable to Encrypt", e);
        }
    }
}
