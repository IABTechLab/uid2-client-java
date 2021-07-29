package com.uid2.client;

public class EncryptionDataResponse {
    private final EncryptionStatus status;
    private final String encryptedData;

    public EncryptionDataResponse(EncryptionStatus status, String encryptedData) {
        this.status = status;
        this.encryptedData = encryptedData;
    }

    public boolean isSuccess() {
        return status == EncryptionStatus.SUCCESS;
    }
    public EncryptionStatus getStatus() { return status; }
    public String getEncryptedData() { return encryptedData; }

    public static EncryptionDataResponse makeError(EncryptionStatus status) {
        return new EncryptionDataResponse(status, null);
    }
}
