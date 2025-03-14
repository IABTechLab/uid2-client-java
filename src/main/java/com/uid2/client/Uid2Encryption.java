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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

class Uid2Encryption {

    public static final int GCM_AUTHTAG_LENGTH = 16;
    public static final int GCM_IV_LENGTH = 12;

    static DecryptionResponse decrypt(String token, KeyContainer keys, Instant now, IdentityScope identityScope, String domainOrAppName, ClientType clientType) throws Exception {

        if (token.length() < 4)
        {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }

        String headerStr = token.substring(0, 4);
        boolean isBase64UrlEncoding = (headerStr.indexOf('-') != -1 || headerStr.indexOf('_') != -1);
        byte[] data = isBase64UrlEncoding ? Uid2Base64UrlCoder.decode(headerStr) : Base64.getDecoder().decode(headerStr);

        if (data[0] == 2)
        {
            return decryptV2(Base64.getDecoder().decode(token), keys, now, domainOrAppName, clientType);
        }
        //java byte is signed so we wanna convert to unsigned before checking the enum
        int unsignedByte = ((int) data[1]) & 0xff;
        if (unsignedByte == AdvertisingTokenVersion.V3.value())
        {
            return decryptV3(Base64.getDecoder().decode(token), keys, now, identityScope, domainOrAppName, clientType, 3);
        }
        else if (unsignedByte  == AdvertisingTokenVersion.V4.value())
        {
            // Accept either base64 or base64url encoding.
            return decryptV3(Base64.getDecoder().decode(base64UrlToBase64(token)), keys, now, identityScope, domainOrAppName, clientType, 4);
        }

        return DecryptionResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
    }

    static String base64UrlToBase64(String value) {
        // Base64 decoder doesn't require padding.
        return value.replace('-', '+')
                .replace('_', '/');
    }

    static DecryptionResponse decryptV2(byte[] encryptedId, KeyContainer keys, Instant now, String domainOrAppName, ClientType clientType) throws Exception {
        try {
            ByteBuffer rootReader = ByteBuffer.wrap(encryptedId);
            int version = (int) rootReader.get();
            if (version != 2) {
                return DecryptionResponse.makeError(DecryptionStatus.VERSION_NOT_SUPPORTED);
            }

            long masterKeyId = rootReader.getInt();
            Key masterKey = keys.getKey(masterKeyId);
            if (masterKey == null) {
                return DecryptionResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY);
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
            PrivacyBits privacyBits = new PrivacyBits(identityPayloadReader.getInt());
            long establishedMilliseconds = identityPayloadReader.getLong();
            Instant established = Instant.ofEpochMilli(establishedMilliseconds);

            int advertisingTokenVersion = 2;
            Instant expiry = Instant.ofEpochMilli(expiryMilliseconds);
            if (now.isAfter(expiry)) {
                return DecryptionResponse.makeError(DecryptionStatus.EXPIRED_TOKEN, established, siteId, siteKey.getSiteId(), null, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
            }
            if (!isDomainOrAppNameAllowedForSite(clientType, privacyBits.isClientSideGenerated(), siteId, domainOrAppName, keys)) {
                return DecryptionResponse.makeError(DecryptionStatus.DOMAIN_OR_APP_NAME_CHECK_FAILED, established, siteId, siteKey.getSiteId(), null, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
            }

            if (!doesTokenHaveValidLifetime(clientType, keys, now, expiry, now)) {
                return DecryptionResponse.makeError(DecryptionStatus.INVALID_TOKEN_LIFETIME, established, siteId, siteKey.getSiteId(), null, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
            }

            return new DecryptionResponse(DecryptionStatus.SUCCESS, idString, established, siteId, siteKey.getSiteId(), null, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
        } catch (ArrayIndexOutOfBoundsException payloadEx) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    static DecryptionResponse decryptV3(byte[] encryptedId, KeyContainer keys, Instant now, IdentityScope identityScope, String domainOrAppName, ClientType clientType, int advertisingTokenVersion) {
        try {
            final IdentityType identityType = getIdentityType(encryptedId);
            final ByteBuffer rootReader = ByteBuffer.wrap(encryptedId);
            final byte prefix = rootReader.get();
            if (decodeIdentityScopeV3(prefix) != identityScope)
            {
                return DecryptionResponse.makeError(DecryptionStatus.INVALID_IDENTITY_SCOPE);
            }

            //version
            rootReader.get();

            final long masterKeyId = rootReader.getInt();
            final Key masterKey = keys.getKey(masterKeyId);
            if (masterKey == null) {
                return DecryptionResponse.makeError(DecryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY);
            }

            final byte[] masterPayload = decryptGCM(encryptedId, rootReader.position(), masterKey.getSecret());
            final ByteBuffer masterReader = ByteBuffer.wrap(masterPayload);

            final long expiresMilliseconds = masterReader.getLong();
            final long generatedMilliseconds = masterReader.getLong();
            Instant generated = Instant.ofEpochMilli(generatedMilliseconds);

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

            final PrivacyBits privacyBits = new PrivacyBits(siteReader.getInt());
            final long establishedMilliseconds = siteReader.getLong();
            final long refreshedMilliseconds = siteReader.getLong();
            final byte[] id = Arrays.copyOfRange(sitePayload, siteReader.position(), sitePayload.length);
            final String idString = Base64.getEncoder().encodeToString(id);
            final Instant established = Instant.ofEpochMilli(establishedMilliseconds);

            final Instant expiry = Instant.ofEpochMilli(expiresMilliseconds);
            if (now.isAfter(expiry)) {
                return DecryptionResponse.makeError(DecryptionStatus.EXPIRED_TOKEN, established, siteId, siteKey.getSiteId(), identityType, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
            }
            if (!isDomainOrAppNameAllowedForSite(clientType, privacyBits.isClientSideGenerated(), siteId, domainOrAppName, keys)) {
                return DecryptionResponse.makeError(DecryptionStatus.DOMAIN_OR_APP_NAME_CHECK_FAILED, established, siteId, siteKey.getSiteId(), identityType, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
            }

            if (!doesTokenHaveValidLifetime(clientType, keys, generated, expiry, now)) {
                return DecryptionResponse.makeError(DecryptionStatus.INVALID_TOKEN_LIFETIME, generated, siteId, siteKey.getSiteId(), identityType, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
            }

            return new DecryptionResponse(DecryptionStatus.SUCCESS, idString, established, siteId, siteKey.getSiteId(), identityType, advertisingTokenVersion, privacyBits.isClientSideGenerated(), expiry);
        } catch (ArrayIndexOutOfBoundsException payloadEx) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    static EncryptionDataResponse encrypt(String rawUid, KeyContainer keys, IdentityScope identityScope, Instant now)
    {
        if (keys == null)
            return EncryptionDataResponse.makeError(EncryptionStatus.NOT_INITIALIZED);

        else if (!keys.isValid(now))
            return EncryptionDataResponse.makeError(EncryptionStatus.KEYS_NOT_SYNCED);

        Key masterKey = keys.getMasterKey(now);
        if (masterKey == null)
            return EncryptionDataResponse.makeError(EncryptionStatus.NOT_AUTHORIZED_FOR_MASTER_KEY);

        Key defaultKey = keys.getDefaultKey(now);
        if (defaultKey == null)
        {
            return EncryptionDataResponse.makeError(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY);
        }

        Instant expiry = now.plusSeconds(keys.getTokenExpirySeconds());
        Uid2TokenGenerator.Params encryptParams = Uid2TokenGenerator.defaultParams().WithTokenGenerated(Instant.now()).withTokenExpiry(expiry);

        try
        {
            String advertisingToken = (identityScope == IdentityScope.UID2) ? Uid2TokenGenerator.generateUid2TokenV4(rawUid, masterKey, keys.getCallerSiteId(), defaultKey, encryptParams) :
                    Uid2TokenGenerator.generateEuidTokenV4(rawUid, masterKey, keys.getCallerSiteId(), defaultKey, encryptParams);

            return new EncryptionDataResponse(EncryptionStatus.SUCCESS, advertisingToken);
        }
        catch (Exception e)
        {
            return EncryptionDataResponse.makeError(EncryptionStatus.ENCRYPTION_FAILURE);
        }
    }


    static EncryptionDataResponse encryptData(EncryptionDataRequest request, KeyContainer keys, IdentityScope identityScope, String domainOrAppName, ClientType clientType) {
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
                    DecryptionResponse decryptedToken = decrypt(request.getAdvertisingToken(), keys, now, identityScope, domainOrAppName, clientType);
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

    static DecryptionDataResponse decryptData(byte[] encryptedBytes, KeyContainer keys, IdentityScope identityScope) throws Exception {
        if ((encryptedBytes[0] & 224) == (int)PayloadType.ENCRYPTED_DATA_V3.value)
        {
            return decryptDataV3(encryptedBytes, keys, identityScope);
        }
        else
        {
            return decryptDataV2(encryptedBytes, keys);
        }
    }

    static DecryptionDataResponse decryptDataV2(byte[] encryptedBytes, KeyContainer keys) throws Exception {
        ByteBuffer reader = ByteBuffer.wrap(encryptedBytes);
        if (Byte.toUnsignedInt(reader.get()) != PayloadType.ENCRYPTED_DATA.value) {
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

    static DecryptionDataResponse decryptDataV3(byte[] encryptedBytes, KeyContainer keys, IdentityScope identityScope) {
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

    private static boolean isDomainOrAppNameAllowedForSite(ClientType clientType, boolean isClientSideGenerated, Integer siteId, String domainOrAppName, KeyContainer keys) {
        if (!isClientSideGenerated) {
            return true;
        } else if (!clientType.equals(ClientType.BIDSTREAM) && !clientType.equals(ClientType.LEGACY)) {
            return true;
        } else {
            return keys.isDomainOrAppNameAllowedForSite(siteId, domainOrAppName);
        }
    }

    private static boolean doesTokenHaveValidLifetime(ClientType clientType, KeyContainer keys, Instant generatedOrNow, Instant expiry, Instant now) {
        long maxLifetimeSeconds;
        switch (clientType) {
            case BIDSTREAM:
                maxLifetimeSeconds = keys.getMaxBidstreamLifetimeSeconds();
                break;
            case SHARING:
                maxLifetimeSeconds = keys.getMaxSharingLifetimeSeconds();
                break;
            default: //Legacy
                return true;
        }
        //generatedOrNow allows "now" for token v2, since v2 does not contain a "token generated" field. v2 therefore checks against remaining lifetime rather than total lifetime.
        return doesTokenHaveValidLifetimeImpl(generatedOrNow, expiry, now, maxLifetimeSeconds, keys.getAllowClockSkewSeconds());
    }

    private static boolean doesTokenHaveValidLifetimeImpl(Instant generatedOrNow, Instant expiry, Instant now, long maxLifetimeSeconds, long allowClockSkewSeconds)
    {
        Duration lifetime = Duration.between(generatedOrNow, expiry);
        if (lifetime.getSeconds() > maxLifetimeSeconds) {
            return false;
        }

        Duration skewDuration = Duration.between(now, generatedOrNow);
        return skewDuration.getSeconds() <= allowClockSkewSeconds;
    }

    private static IdentityType getIdentityType(byte[] encryptedId)
    {
        // For specifics about the bitwise logic, check:
        // Confluence - UID2-79 UID2 Token v3/v4 and Raw UID2 format v3
        // In the base64-encoded version of encryptedId, the first character is always either A/B/E/F.
        // After converting to binary and performing the AND operation against 1100,the result is always 0X00.
        // So just bitshift right twice to get 000X, which results in either 0 or 1.
        byte idType = encryptedId[0];
        byte piiType = (byte) ((idType & 0b1100) >> 2);
        return piiType == 0 ? IdentityType.Email : IdentityType.Phone;
    }
}
