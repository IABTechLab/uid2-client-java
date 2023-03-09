package com.uid2.client;

import java.time.Instant;

public interface IUID2Client {

    void refresh() throws UID2ClientException;

    DecryptionResponse decrypt(String token, Instant now) throws UID2ClientException;

    default DecryptionResponse decrypt(String token) throws UID2ClientException
    {
        return decrypt(token, Instant.now());
    }

    /**
     * @deprecated
     * Use encrypt() instead
     */
    @Deprecated
    EncryptionDataResponse encryptData(EncryptionDataRequest request) throws UID2ClientException;

    EncryptionDataResponse encrypt(String rawUid) throws UID2ClientException;

    /**
     * @deprecated
     * Use decrypt() instead
     */
    @Deprecated
    DecryptionDataResponse decryptData(String encryptedData) throws UID2ClientException;
}
