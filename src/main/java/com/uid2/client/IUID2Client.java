package com.uid2.client;

import java.time.Instant;

public interface IUID2Client {

    /**
     * Refreshes encryption keys. Call this regularly (eg every hour) to ensure keys are up to date.
     */
    void refresh() throws UID2ClientException;

    /**
     * Maps a set of PII or hashed PII to a UID.
     * @param request An object with the PII or hashed PII to be mapped
     * @return A payload with a list of mapped PII and their original identifiers, or the mappings that failed
     *         and the reasons for their failure.
     */
    MappingResponse mapIdentity(MappingRequest request) throws UID2ClientException;

    DecryptionResponse decrypt(String token, Instant now) throws UID2ClientException;

    /**
     * @param token The UID token to be decrypted into a raw UID
     * @return See {@link DecryptionResponse}
     */
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

    /**
     * @param rawUid The raw UID to be encrypted into a UID token
     * @return An EncryptionDataResponse, containing success status and the UID token
     */
    EncryptionDataResponse encrypt(String rawUid) throws UID2ClientException;

    /**
     * @deprecated
     * Use decrypt() instead
     */
    @Deprecated
    DecryptionDataResponse decryptData(String encryptedData) throws UID2ClientException;
}
