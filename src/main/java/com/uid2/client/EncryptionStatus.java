package com.uid2.client;

public enum EncryptionStatus {
    SUCCESS,
    /**
     * NOT_AUTHORIZED_FOR_MASTER_KEY: Indicates that the participant has been removed from the UID platform.
     */
    NOT_AUTHORIZED_FOR_MASTER_KEY,
    /**
     * NOT_AUTHORIZED_FOR_KEY: The participant has not been enabled for encryption.
     */
    NOT_AUTHORIZED_FOR_KEY,
    /**
     * NOT_INITIALIZED: IUID2Client.refresh() has not been called or did not succeed.
     */
    NOT_INITIALIZED,
    /**
     * KEYS_NOT_SYNCED: The encryption keys have expired. This can happen if IUID2Client.refresh() has not been called for a long time.
     */
    KEYS_NOT_SYNCED,
    /**
     * TOKEN_DECRYPT_FAILURE: This is a value returned by the legacy encryptData() method. Use encrypt() instead.
     */
    TOKEN_DECRYPT_FAILURE,
    /**
     * KEY_INACTIVE: This is a value returned by the legacy encryptData() method. Use encrypt() instead.
     */
    KEY_INACTIVE,
    /**
     * ENCRYPTION_FAILURE: An exception was thrown during encryption.
     */
    ENCRYPTION_FAILURE,
}
