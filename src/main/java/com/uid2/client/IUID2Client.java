package com.uid2.client;

import java.time.Instant;

public interface IUID2Client {

    void refresh() throws UID2ClientException;

    DecryptionResponse decrypt(String token, Instant now) throws UID2ClientException;

    default DecryptionResponse decrypt(String token) throws UID2ClientException
    {
        return decrypt(token, Instant.now());
    }

    EncryptionDataResponse encryptData(EncryptionDataRequest request) throws UID2ClientException;
    DecryptionDataResponse decryptData(String encryptedData) throws UID2ClientException;
}
