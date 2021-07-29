package com.uid2.client;

public enum EncryptionStatus {
    SUCCESS,
    NOT_AUTHORIZED_FOR_KEY,
    NOT_INITIALIZED,
    KEYS_NOT_SYNCED,
    TOKEN_DECRYPT_FAILURE,
    KEY_INACTIVE,
    ENCRYPTION_FAILURE,
}
