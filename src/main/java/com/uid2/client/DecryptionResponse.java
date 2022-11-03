 package com.uid2.client;

import java.time.Instant;

public class DecryptionResponse {
    private final DecryptionStatus status;
    private final String uid;
    private final Instant established;
    private final Integer siteId;
    private final Integer siteKeySiteId;

    public DecryptionResponse(DecryptionStatus status, String uid, Instant established, Integer siteId, Integer siteKeySiteId) {
        this.status = status;
        this.uid = uid;
        this.established = established;
        this.siteId = siteId;
        this.siteKeySiteId = siteKeySiteId;
    }

    public boolean isSuccess() {
        return status == DecryptionStatus.SUCCESS;
    }

    public DecryptionStatus getStatus() {
        return status;
    }

    public String getUid() {
        return uid;
    }

    public Instant getEstablished() {
        return established;
    }

    public Integer getSiteId() { return siteId; }

    public Integer getSiteKeySiteId() { return siteKeySiteId; }

    public static DecryptionResponse makeError(DecryptionStatus status) {
        return new DecryptionResponse(status, null, Instant.MIN, null, null);
    }

    public static DecryptionResponse makeError(DecryptionStatus status, Instant established, Integer siteId, Integer siteKeySiteId) {
        return new DecryptionResponse(status, null, established, siteId, siteKeySiteId);
    }
}
