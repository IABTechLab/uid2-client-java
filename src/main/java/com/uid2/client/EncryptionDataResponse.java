package com.uid2.client;

public class EncryptionDataResponse {
    private final EncryptionStatus status;
    private final String encryptedData;

    EncryptionDataResponse(EncryptionStatus status, String encryptedData) {
        this.status = status;
        this.encryptedData = encryptedData;
    }

    /**
     * @return whether the encryption was successful.
     */
    public boolean isSuccess() {
        return status == EncryptionStatus.SUCCESS;
    }

    /**
     * @return the encryption result status. See {@link EncryptionStatus}
     */
    public EncryptionStatus getStatus() { return status; }

    /**
     * @return the UID token
     */
    public String getEncryptedData() { return encryptedData; }

    static EncryptionDataResponse makeError(EncryptionStatus status) {
        return new EncryptionDataResponse(status, null);
    }
}
