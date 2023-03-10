 package com.uid2.client;

import java.time.Instant;

public class DecryptionResponse {
    private final DecryptionStatus status;
    private final String uid;
    private final Instant established;
    private final Integer siteId;
    private final Integer siteKeySiteId;

    DecryptionResponse(DecryptionStatus status, String uid, Instant established, Integer siteId, Integer siteKeySiteId) {
        this.status = status;
        this.uid = uid;
        this.established = established;
        this.siteId = siteId;
        this.siteKeySiteId = siteKeySiteId;
    }

    /**
     * @return whether the decryption was successful.
     */
    public boolean isSuccess() {
        return status == DecryptionStatus.SUCCESS;
    }

    /**
     * @return the decryption result status. See {@link DecryptionStatus}.
     */
    public DecryptionStatus getStatus() {
        return status;
    }

    /**
     * @return the raw UID.
     */
    public String getUid() {
        return uid;
    }

    public Instant getEstablished() {
        return established;
    }

    public Integer getSiteId() { return siteId; }

    public Integer getSiteKeySiteId() { return siteKeySiteId; }

    static DecryptionResponse makeError(DecryptionStatus status) {
        return new DecryptionResponse(status, null, Instant.MIN, null, null);
    }

    static DecryptionResponse makeError(DecryptionStatus status, Instant established, Integer siteId, Integer siteKeySiteId) {
        return new DecryptionResponse(status, null, established, siteId, siteKeySiteId);
    }
}
