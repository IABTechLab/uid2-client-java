package com.uid2.client;

import java.time.Instant;

public class DecryptionDataResponse {
    private final DecryptionStatus status;
    private final byte[] decryptedData;
    private final Instant encryptedAt;

    public DecryptionDataResponse(DecryptionStatus status, byte[] decryptedData, Instant encryptedAt) {
        this.status = status;
        this.decryptedData = decryptedData;
        this.encryptedAt = encryptedAt;
    }

    public boolean isSuccess() {
        return status == DecryptionStatus.SUCCESS;
    }

    public DecryptionStatus getStatus() {
        return status;
    }

    public byte[] getDecryptedData() {
        return decryptedData;
    }

    public Instant getEncryptedAt() {
        return encryptedAt;
    }

    public static DecryptionDataResponse makeError(DecryptionStatus status) {
        return new DecryptionDataResponse(status, null, Instant.MIN);
    }
}
