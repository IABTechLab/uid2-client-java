// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.client;

import javax.crypto.*;
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
import java.util.Map;

class Decryption {

    static DecryptionResponse decrypt(byte[] encryptedId, IKeyContainer keys, Instant now) throws Exception {
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
            Instant expiry = Instant.ofEpochMilli(expiryMilliseconds);
            if (now.isAfter(expiry)) {
                return DecryptionResponse.makeError(DecryptionStatus.EXPIRED_TOKEN);
            }

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

            return new DecryptionResponse(DecryptionStatus.SUCCESS, idString, established, siteId);
        } catch (ArrayIndexOutOfBoundsException payloadEx) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    static EncryptionDataResponse encryptData(EncryptionDataRequest request, IKeyContainer keys) {
        if (request.getData() == null) {
            throw new IllegalArgumentException("data to encrypt must not be null");
        }

        final Instant now = request.getNow();
        Key key = request.getKey();
        int siteId = -1;
        if (key == null) {
            if (keys == null) {
                return EncryptionDataResponse.makeError(EncryptionStatus.NOT_INITIALIZED);
            } else if (!keys.isValid(now)) {
                return EncryptionDataResponse.makeError(EncryptionStatus.KEYS_NOT_SYNCED);
            } else if (request.getSiteId() != null && request.getAdvertisingToken() != null) {
                throw new IllegalArgumentException("only one of siteId or advertisingToken can be specified");
            } else if (request.getSiteId() != null) {
                siteId = request.getSiteId();
            } else {
                try {
                    DecryptionResponse decryptedToken = decrypt(Base64.getDecoder().decode(request.getAdvertisingToken()), keys, now);
                    if (!decryptedToken.isSuccess()) {
                        return EncryptionDataResponse.makeError(EncryptionStatus.TOKEN_DECRYPT_FAILURE);
                    }

                    siteId = decryptedToken.getSiteId();
                } catch (Exception ex) {
                    return EncryptionDataResponse.makeError(EncryptionStatus.TOKEN_DECRYPT_FAILURE);
                }
            }

            key = keys.getActiveSiteKey(siteId, now);
            if (key == null) {
                return EncryptionDataResponse.makeError(EncryptionStatus.NOT_AUTHORIZED_FOR_KEY);
            }
        } else if (!key.isActive(now)) {
            return EncryptionDataResponse.makeError(EncryptionStatus.KEY_INACTIVE);
        } else {
            siteId = key.getSiteId();
        }

        byte[] iv = request.getInitializationVector();
        if (iv == null) {
            iv = generateIv();
        }

        try {
            byte[] encryptedData = encrypt(request.getData(), iv, key.getSecret());
            ByteBuffer writer = ByteBuffer.allocate(encryptedData.length + 18);
            writer.put((byte)PayloadType.ENCRYPTED_DATA.value);
            writer.put((byte)1); // version
            writer.putLong(now.toEpochMilli());
            writer.putInt(siteId);
            writer.putInt((int)key.getId());
            writer.put(encryptedData);
            return new EncryptionDataResponse(EncryptionStatus.SUCCESS, Base64.getEncoder().encodeToString(writer.array()));
        } catch (Exception ex) {
            return EncryptionDataResponse.makeError(EncryptionStatus.ENCRYPTION_FAILURE);
        }
    }

    static DecryptionDataResponse decryptData(byte[] encryptedBytes, IKeyContainer keys) throws Exception {
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

    private static byte[] encrypt(byte[] data, byte[] iv, byte[] secret)
            throws CryptoException,
            NoSuchPaddingException,
            NoSuchAlgorithmException {
        try {
            SecretKey key = new SecretKeySpec(secret, 0, secret.length, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(data);
            byte[] finalized = new byte[16 + encrypted.length];
            for (int i = 0; i < 16; i++) finalized[i] = iv[i];
            for (int i = 0; i < encrypted.length; i++) finalized[16 + i] = encrypted[i];
            return finalized;
        } catch (InvalidAlgorithmParameterException|InvalidKeyException|BadPaddingException|IllegalBlockSizeException e) {
            throw new CryptoException(e);
        }
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static class CryptoException extends Exception {
        public CryptoException(Throwable inner) {
            super(inner);
        }
    }
}
