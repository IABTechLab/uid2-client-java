 package com.uid2.client;

import java.time.Instant;

public class DecryptionResponse {
    private final DecryptionStatus status;
    private final String uid;
    private final Instant established;
    private final Integer siteId;
    private final Integer siteKeySiteId;
    private final IdentityType identityType;
    private final Integer advertisingTokenVersion;
    private final boolean isClientSideGenerated;
    private final Instant expiry;


    DecryptionResponse(DecryptionStatus status, String uid, Instant established, Integer siteId, Integer siteKeySiteId, IdentityType identityType, Integer advertisingTokenVersion, boolean isClientSideGenerated, Instant expiry) {
        this.status = status;
        this.uid = uid;
        this.established = established;
        this.siteId = siteId;
        this.siteKeySiteId = siteKeySiteId;
        this.identityType = identityType;
        this.advertisingTokenVersion = advertisingTokenVersion;
        this.isClientSideGenerated = isClientSideGenerated;
        this.expiry = expiry;
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

    public Integer getAdvertisingTokenVersion() {
        return advertisingTokenVersion;
    }

    public IdentityType getIdentityType() {
        return identityType;
    }

    public boolean getIsClientSideGenerated() {
        return isClientSideGenerated;
    }

    public Instant getExpiry() {
        return expiry;
    }

    static DecryptionResponse makeError(DecryptionStatus status) {
        return new DecryptionResponse(status, null, Instant.MIN, null, null, null, null, false, Instant.MIN);
    }

    static DecryptionResponse makeError(DecryptionStatus status, Instant established, Integer siteId, Integer siteKeySiteId, IdentityType identityType, Integer advertisingTokenVersion, boolean isClientSideGenerated, Instant expiry) {
        return new DecryptionResponse(status, null, established, siteId, siteKeySiteId, identityType, advertisingTokenVersion, isClientSideGenerated, expiry);
    }
}
