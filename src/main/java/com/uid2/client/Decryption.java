package com.uid2.client;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

class Decryption {

    public static final int GCM_AUTHTAG_LENGTH = 16;
    public static final int GCM_IV_LENGTH = 12;

    static DecryptionResponse decrypt(byte[] encryptedId, IKeyContainer keys, Instant now, IdentityScope identityScope) throws Exception {
        if (encryptedId[0] == 2)
        {
            return decryptV2(encryptedId, keys, now);
        }
        else if (encryptedId[1] == 112)
        {
            return decryptV3(encryptedId, keys, now, identityScope);
        }

        return DecryptionResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
    }

    static DecryptionResponse decryptV2(byte[] encryptedId, IKeyContainer keys, Instant now) throws Exception {
        try {
            ByteBuffer rootReader = ByteBuffer.wrap(encryptedId);
            int version = (int) rootReader.get();
            if (version != 2) {
                return DecryptionResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
            }

            long masterKeyId = rootReader.getInt();
            Key masterKey = keys.getKey(masterKeyId);
            if (masterKey == null) {
                return DecryptionResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY);
            }

            byte[] masterIv = new byte[16];
            rootReader.get(masterIv);
            byte[] masterDecrypted = decrypt(
                    Arrays.copyOfRange(encryptedId, 21, encryptedId.length),
                    masterIv,
                    masterKey.getSecret());

            ByteBuffer masterPayloadReader = ByteBuffer.wrap(masterDecrypted);
            long expiryMilliseconds = masterPayloadReader.getLong();

            long siteKeyId = masterPayloadReader.getInt();
            Key siteKey = keys.getKey(siteKeyId);
            if (siteKey == null) {
                return DecryptionResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY);
            }

            byte[] identityIv = new byte[16];
            masterPayloadReader.get(identityIv);
            byte[] identityDecrypted = decrypt(
                    Arrays.copyOfRange(masterDecrypted, 28, masterDecrypted.length),
                    identityIv,
                    siteKey.getSecret());

            ByteBuffer identityPayloadReader = ByteBuffer.wrap(identityDecrypted);
            int siteId = identityPayloadReader.getInt();
            int idLength = identityPayloadReader.getInt();
            byte[] idBytes = new byte[idLength];
            identityPayloadReader.get(idBytes);
            String idString = new String(idBytes, StandardCharsets.UTF_8);
            int privacyBits = identityPayloadReader.getInt();
            long establishedMilliseconds = identityPayloadReader.getLong();
            Instant established = Instant.ofEpochMilli(establishedMilliseconds);

            Instant expiry = Instant.ofEpochMilli(expiryMilliseconds);
            if (now.isAfter(expiry)) {
                return DecryptionResponse.makeError(DecryptionStatus.EXPIRED_TOKEN, established, siteId, siteKey.getSiteId());
            }

            return new DecryptionResponse(DecryptionStatus.SUCCESS, idString, established, siteId, siteKey.getSiteId());
        } catch (ArrayIndexOutOfBoundsException payloadEx) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    static DecryptionResponse decryptV3(byte[] encryptedId, IKeyContainer keys, Instant now, IdentityScope identityScope) throws Exception {
        try {
            final ByteBuffer rootReader = ByteBuffer.wrap(encryptedId);
            final byte prefix = rootReader.get();
            if (decodeIdentityScopeV3(prefix) != identityScope)
            {
                return DecryptionResponse.makeError(DecryptionStatus.INVALID_IDENTITY_SCOPE);
            }

            final int version = (int) rootReader.get();
            if (version != 112) {
                return DecryptionResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
            }

            final long masterKeyId = rootReader.getInt();
            final Key masterKey = keys.getKey(masterKeyId);
            if (masterKey == null) {
                return DecryptionResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY);
            }

            final byte[] masterPayload = decryptGCM(encryptedId, rootReader.position(), masterKey.getSecret());
            final ByteBuffer masterReader = ByteBuffer.wrap(masterPayload);

            final long expiresMilliseconds = masterReader.getLong();
            final long createdMilliseconds = masterReader.getLong();

            final int operatorSideId = masterReader.getInt();
            final byte operatorType = masterReader.get();
            final int operatorVersion = masterReader.getInt();
            final int operatorKeyId = masterReader.getInt();

            final long siteKeyId = masterReader.getInt();
            final Key siteKey = keys.getKey(siteKeyId);
            if (siteKey == null) {
                return DecryptionResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY);
            }

            final byte[] sitePayload = decryptGCM(masterPayload, masterReader.position(), siteKey.getSecret());
            final ByteBuffer siteReader = ByteBuffer.wrap(sitePayload);

            final int siteId = siteReader.getInt();
            final long publisherId = siteReader.getLong();
            final int clientKeyId = siteReader.getInt();

            final int privacyBits = siteReader.getInt();
            final long establishedMilliseconds = siteReader.getLong();
            final long refreshedMilliseconds = siteReader.getLong();
            final byte[] id = Arrays.copyOfRange(sitePayload, siteReader.position(), sitePayload.length);
            final String idString = Base64.getEncoder().encodeToString(id);
            final Instant established = Instant.ofEpochMilli(establishedMilliseconds);

            final Instant expiry = Instant.ofEpochMilli(expiresMilliseconds);
            if (now.isAfter(expiry)) {
                return DecryptionResponse.makeError(DecryptionStatus.EXPIRED_TOKEN, established, siteId, siteKey.getSiteId());
            }

            return new DecryptionResponse(DecryptionStatus.SUCCESS, idString, established, siteId, siteKey.getSiteId());
        } catch (ArrayIndexOutOfBoundsException payloadEx) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    static EncryptionDataResponse encryptData(EncryptionDataRequest request, IKeyContainer keys, IdentityScope identityScope) {
        if (request.getData() == null) {
            throw new IllegalArgumentException("data to encrypt must not be null");
        }

        final Instant now = request.getNow();
        Key key = request.getKey();
        int siteId = -1;
        if (key == null) {
            int siteKeySiteId;
            if (keys == null) {
                return EncryptionDataResponse.makeError(EncryptionStatus.NOT_INITIALIZED);
            } else if (!keys.isValid(now)) {
                return EncryptionDataResponse.makeError(EncryptionStatus.KEYS_NOT_SYNCED);
            } else if (request.getSiteId() != null && request.getAdvertisingToken() != null) {
                throw new IllegalArgumentException("only one of siteId or advertisingToken can be specified");
            } else if (request.getSiteId() != null) {
                siteId = request.getSiteId();
                siteKeySiteId = siteId;
            } else {
                try {
                    DecryptionResponse decryptedToken = decrypt(Base64.getDecoder().decode(request.getAdvertisingToken()), keys, now, identityScope);
                    if (!decryptedToken.isSuccess()) {
                        return EncryptionDataResponse.makeError(EncryptionStatus.TOKEN_DECRYPT_FAILURE);
                    }

                    siteId = decryptedToken.getSiteId();
                    siteKeySiteId = decryptedToken.getSiteKeySiteId();
                } catch (Exception ex) {
                    return EncryptionDataResponse.makeError(EncryptionStatus.TOKEN_DECRYPT_FAILURE);
                }
            }

            key = keys.getActiveSiteKey(siteKeySiteId, now);
            if (key == null) {
                return EncryptionDataResponse.makeError(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY);
            }
        } else if (!key.isActive(now)) {
            return EncryptionDataResponse.makeError(EncryptionStatus.KEY_INACTIVE);
        } else {
            siteId = key.getSiteId();
        }

        byte[] iv = request.getInitializationVector();

        try {
            final ByteBuffer payloadWriter = ByteBuffer.allocate(request.getData().length + 12);
            payloadWriter.putLong(now.toEpochMilli());
            payloadWriter.putInt(siteId);
            payloadWriter.put(request.getData());
            final byte[] encryptedPayload = encryptGCM(payloadWriter.array(), iv, key.getSecret());

            final ByteBuffer writer = ByteBuffer.allocate(encryptedPayload.length + 6);
            writer.put((byte)(PayloadType.ENCRYPTED_DATA_V3.value | (identityScope.value << 4) | 0xB));
            writer.put((byte)112); // version
            writer.putInt((int)key.getId());
            writer.put(encryptedPayload);

            return new EncryptionDataResponse(EncryptionStatus.SUCCESS, Base64.getEncoder().encodeToString(writer.array()));
        } catch (Exception ex) {
            return EncryptionDataResponse.makeError(EncryptionStatus.ENCRYPTION_FAILURE);
        }
    }

    static DecryptionDataResponse decryptData(byte[] encryptedBytes, IKeyContainer keys, IdentityScope identityScope) throws Exception {
        if ((encryptedBytes[0] & 224) == (int)PayloadType.ENCRYPTED_DATA_V3.value)
        {
            return decryptDataV3(encryptedBytes, keys, identityScope);
        }
        else
        {
            return decryptDataV2(encryptedBytes, keys);
        }
    }

    static DecryptionDataResponse decryptDataV2(byte[] encryptedBytes, IKeyContainer keys) throws Exception {
        ByteBuffer reader = ByteBuffer.wrap(encryptedBytes);
        if(Byte.toUnsignedInt(reader.get()) != PayloadType.ENCRYPTED_DATA.value) {
            return DecryptionDataResponse.makeError(DecryptionStatus.INVALID_PAYLOAD_TYPE);
        } else if (reader.get() != 1) {
            return DecryptionDataResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
        }

        final Instant encryptedAt = Instant.ofEpochMilli(reader.getLong());
        final int siteId = reader.getInt();
        final long keyId = reader.getInt();

        final Key key = keys.getKey(keyId);
        if (key == null) {
            return DecryptionDataResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY);
        }

        byte[] iv = new byte[16];
        reader.get(iv);
        byte[] decryptedData = decrypt(
                Arrays.copyOfRange(encryptedBytes, 34, encryptedBytes.length),
                iv,
                key.getSecret());

        return new DecryptionDataResponse(DecryptionStatus.SUCCESS, decryptedData, encryptedAt);
    }

    static DecryptionDataResponse decryptDataV3(byte[] encryptedBytes, IKeyContainer keys, IdentityScope identityScope) throws Exception {
        final ByteBuffer reader = ByteBuffer.wrap(encryptedBytes);
        final IdentityScope payloadScope = decodeIdentityScopeV3(reader.get());
        if (payloadScope != identityScope)
        {
            return DecryptionDataResponse.makeError(DecryptionStatus.INVALID_IDENTITY_SCOPE);
        }
        if (reader.get() != 112)
        {
            return DecryptionDataResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
        }

        final long keyId = reader.getInt();
        final Key key = keys.getKey(keyId);
        if (key == null) {
            return DecryptionDataResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_KEY);
        }

        final byte[] payload = decryptGCM(encryptedBytes, reader.position(), key.getSecret());
        final ByteBuffer payloadReader = ByteBuffer.wrap(payload, 0, payload.length);

        final Instant encryptedAt = Instant.ofEpochMilli(payloadReader.getLong());
        final int siteId = payloadReader.getInt();
        final byte[] decryptedData = Arrays.copyOfRange(payload, payloadReader.position(), payload.length);

        return new DecryptionDataResponse(DecryptionStatus.SUCCESS, decryptedData, encryptedAt);
    }

    private static byte[] decrypt(byte[] data, byte[] iv, byte[] secret)
            throws CryptoException,
            NoSuchPaddingException,
            NoSuchAlgorithmException {
        try {
            SecretKey key = new SecretKeySpec(secret, 0, secret.length, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (InvalidAlgorithmParameterException|InvalidKeyException|BadPaddingException|IllegalBlockSizeException e) {
            throw new CryptoException(e);
        }
        // if NoSuchPaddingException or NoSuchAlgorithmException
        // your system/jvm has no AES algorithm providers
    }

    public static byte[] encryptGCM(byte[] b, byte[] iv, byte[] secretBytes) {
        try {
            final SecretKey k = new SecretKeySpec(secretBytes, "AES");
            final Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            if (iv == null) {
                iv = new byte[GCM_IV_LENGTH];
                new SecureRandom().nextBytes(iv);
            }
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_AUTHTAG_LENGTH * 8, iv);
            c.init(Cipher.ENCRYPT_MODE, k, gcmParameterSpec);
            ByteBuffer buffer = ByteBuffer.allocate(b.length + GCM_IV_LENGTH + GCM_AUTHTAG_LENGTH);
            buffer.put(iv);
            buffer.put(c.doFinal(b));
            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Unable to Encrypt", e);
        }
    }

    public static byte[] decryptGCM(byte[] encryptedBytes, int offset, byte[] secretBytes) {
        try {
            final SecretKey key = new SecretKeySpec(secretBytes, "AES");
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_AUTHTAG_LENGTH * 8, encryptedBytes, offset, GCM_IV_LENGTH);
            final Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            return c.doFinal(encryptedBytes, offset + GCM_IV_LENGTH, encryptedBytes.length - offset - GCM_IV_LENGTH);
        } catch (Exception e) {
            throw new RuntimeException("Unable to Decrypt", e);
        }
    }

    private static IdentityScope decodeIdentityScopeV3(byte value)
    {
        return IdentityScope.fromValue((value >> 4) & 1);
    }

    public static class CryptoException extends Exception {
        public CryptoException(Throwable inner) {
            super(inner);
        }
    }
}
