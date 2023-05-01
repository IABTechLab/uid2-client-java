package com.uid2.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.AudioFormat.Encoding;

import com.google.gson.Gson;

public final class PayloadHelper {
    /* Constants */
    private static final int NONCE_SIZE = 8;
    private static final int TIMESTAMP_SIZE = 8;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int GCM_TAG_LENGTH_BITS = GCM_TAG_LENGTH * 8;
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] clientSecret;

    public PayloadHelper(final byte[] clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Creates an unencrypted request envelope from the input data.
     * See: https://unifiedid.com/docs/getting-started/gs-encryption-decryption#unencrypted-request-data-envelope
     * 
     * @param data A byte array of JSON encoded in UTF-8
     * @return A tuple whose first parameter is byte array with the unencrypted
     *         request envelope and whose second parameter is the nonce used to
     *         generate it.
     */
    public Tuple<byte[], byte[]> createUnencryptedRequestEnvelope(
        byte[] data
    ) {
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(nonce);

        ByteBuffer buffer = ByteBuffer.allocate(
            TIMESTAMP_SIZE + NONCE_SIZE + data.length
        );

        /* UID2 requests are ALWAYS big endian  */
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(System.currentTimeMillis());
        buffer.put(nonce);
        buffer.put(data);
        return new Tuple<>(buffer.array(), nonce);
    }

    /**
     * An encrypted request envelope. The final message format before being sent
     * to the UID operator API.
     * See: https://unifiedid.com/docs/getting-started/gs-encryption-decryption#encrypted-request-envelope
     * 
     * @param data A byte array representing an unencrypted request envelope.
     *        See PayloadBuilder.createUnencryptedRequestEnvelope
     * @return A byte array representing the data of the encrypted request
     *         envelope
     * @throws NoSuchAlgorithmException If "AES/GCM" is not found
     *         within the java cryptography library as a valid algorithm
     * @throws NoSuchPaddingException If NoPadding is not available as padding
     *         scheme for the AES/GCM implementation
     * @throws InvalidKeyException If the given user key was incorrect.
     * @throws InvalidAlgorithmParameterException If the GCM Parameters are
     *         incorrect. These are generated internal to the method and should
     *         never be wrong.
     * @throws IllegalBlockSizeException The total input length of the data
     *         processed is not a multiple of 128 bits
     */
    public byte[] createEncryptedRequestEnvelope(final byte[] data) throws
        NoSuchAlgorithmException,
        NoSuchPaddingException,
        InvalidKeyException,
        InvalidAlgorithmParameterException,
        IllegalBlockSizeException,
        BadPaddingException
    {
        final SecretKey k = new SecretKeySpec(
            this.clientSecret,
            "AES"
        );
        final Cipher c = Cipher.getInstance(AES_GCM_NOPADDING);
        byte[] iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(
            GCM_TAG_LENGTH_BITS, iv
        );
        c.init(Cipher.ENCRYPT_MODE, k, gcmParameterSpec);

        /* 1st initial byte reserved for version */
        ByteBuffer buffer = ByteBuffer.allocate(
            1 + GCM_IV_LENGTH + data.length + GCM_TAG_LENGTH
        );

        buffer.put((byte) 1);             /* version */
        buffer.put(iv);                   /* nonce */
        try {
            buffer.put(c.doFinal(data));  /* payload includes tag at the end */
        } catch(BadPaddingException bpe) {
            return null;                  /* Only occurs in decrypt mode; will
                                             not happen */
        }
        return buffer.array();
    }

    /**
     * Decrypts an encrypted response envelope into an unencrypted response
     * envelope.
     * 
     * See https://unifiedid.com/docs/getting-started/gs-encryption-decryption#encrypted-response-envelope
     * and https://unifiedid.com/docs/getting-started/gs-encryption-decryption#unencrypted-response-data-envelope
     * @param data The encrypted response envelope, as a byte array
     * @return The unencrypted response envelope, as a byte array
     * @throws NoSuchAlgorithmException If "AES/GCM" is not found
     *         within the java cryptography library as a valid algorithm
     * @throws NoSuchPaddingException If NoPadding is not available as padding
     *         scheme for the AES/GCM implementation
     * @throws InvalidKeyException If the given user key was incorrect.
     * @throws InvalidAlgorithmParameterException If the GCM Parameters are
     *         incorrect. These are generated internal to the method and should
     *         never be wrong.
     * @throws IllegalBlockSizeException The total input length of the data
     *         processed is not a multiple of 128 bits
     */
    public byte[] decryptEncryptedResponseEnvelope(byte[] data) throws
        NoSuchAlgorithmException,
        NoSuchPaddingException,
        InvalidKeyException,
        InvalidAlgorithmParameterException,
        IllegalBlockSizeException
    {
        byte[] decodedEnvelope = Base64.getDecoder().decode(data);
        SecretKey k = new SecretKeySpec(this.clientSecret, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(
            GCM_TAG_LENGTH_BITS,
            decodedEnvelope, /* encrypted response envelope */
            0,
            GCM_IV_LENGTH
        );

        Cipher c = Cipher.getInstance(AES_GCM_NOPADDING);
        c.init(Cipher.DECRYPT_MODE, k, gcmParameterSpec);

        try {
            return c.doFinal(
                decodedEnvelope,
                GCM_IV_LENGTH,
                decodedEnvelope.length - GCM_IV_LENGTH
            );
        }catch(BadPaddingException bpe) {
            return null; /* no padding expected, so this will never be thrown */
        }
    }

    public <T> T parseResponse(
        byte[] encryptedResponseEnvelope,
        byte[] originalNonce,
        Class<T> classOfT
    ) throws UID2ClientException {
        byte[] unencryptedResponseEnvelope;
        try {
            unencryptedResponseEnvelope = decryptEncryptedResponseEnvelope(
                encryptedResponseEnvelope
            );
        } catch (Exception e) {
            throw new UID2ClientException(
                "Failed to decrypt encrypted response envelope. Got " +
                e.getMessage()
            );
        }

        if(unencryptedResponseEnvelope.length < 16) {
            throw new UID2ClientException(
                "Invalid response length. Message contains no nonce."
            );
        }

        byte[] receivedNonce = Arrays.copyOfRange(unencryptedResponseEnvelope, 8, 16);
        if(!Arrays.equals(originalNonce, receivedNonce)){
            throw new UID2ClientException("Nonces do not match!");
        }

        /* Subtract nonce and timestamp */
        byte[] payload = Arrays.copyOfRange(
            unencryptedResponseEnvelope,
            16, unencryptedResponseEnvelope.length
        );

        String json = new String(payload, StandardCharsets.UTF_8);
        try {
            return new Gson().fromJson(json, classOfT);
        } catch(Exception e){
            throw new UID2ClientException(
                "Failed to deserialize " + classOfT.getName() + ". Got " +
                e.getMessage()
            );
        }
    }

    public final class Tuple<T, U> {
        private T first;
        private U second;

        public Tuple(T t, U u) {
            this.first = t;
            this.second = u;
        }

        public T getFirst() {
            return first;
        }

        public U getSecond() {
            return second;
        }
    }
}
