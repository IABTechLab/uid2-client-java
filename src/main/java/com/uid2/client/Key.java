 package com.uid2.client;

import java.time.Instant;
import java.util.Base64;

public class Key {
    private final long id;
    private final int siteId;
    private final Instant created;
    private final Instant activates;
    private final Instant expires;
    private final byte[] secret;

    public Key(long id, int siteId, Instant created, Instant activates, Instant expires, byte[] secret) {
        this.id = id;
        this.siteId = siteId;
        this.created = created;
        this.activates = activates;
        this.expires = expires;
        this.secret = secret;
    }

    public long getId() {
        return id;
    }

    public int getSiteId() { return siteId; }

    public Instant getCreated() {
        return created;
    }

    public Instant getActivates() { return activates; }

    public Instant getExpires() {
        return expires;
    }

    public byte[] getSecret() {
        return secret;
    }

    public boolean isActive(Instant asOf) {
        return !activates.isAfter(asOf) && asOf.isBefore(expires);
    }

    @Override
    public String toString() {
        return "Key{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", created=" + created +
                ", activates=" + activates +
                ", expires=" + expires +
                ", secret=" + Base64.getEncoder().encodeToString(secret) +
                '}';
    }
}
