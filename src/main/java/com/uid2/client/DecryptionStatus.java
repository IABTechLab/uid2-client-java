 package com.uid2.client;

public enum DecryptionStatus {
    SUCCESS,
    NOT_AUTHORIZED_FOR_MASTER_KEY, //note this error can occur for old tokens also, since old encryption keys are regularly deleted
    NOT_AUTHORIZED_FOR_KEY,
    NOT_INITIALIZED,
    INVALID_PAYLOAD,
    EXPIRED_TOKEN,
    KEYS_NOT_SYNCED,
    VERSION_NOT_SUPPORTED,
    INVALID_PAYLOAD_TYPE,
    INVALID_IDENTITY_SCOPE,
}
