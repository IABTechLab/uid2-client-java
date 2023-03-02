 package com.uid2.client;

import java.time.Instant;
import java.util.Base64;

class Key {
    private final long id;
    private final int siteId;
    private final int keysetId;
    private final Instant created;
    private final Instant activates;
    private final Instant expires;
    private final byte[] secret;

    public Key(long id, int siteId, Instant created, Instant activates, Instant expires, byte[] secret) {  //for legacy /key/latest
        this.id = id;
        this.siteId = siteId;
        this.created = created;
        this.activates = activates;
        this.expires = expires;
        this.secret = secret;
        this.keysetId = 0;
    }

    public static Key createKeysetKey(long id, int keysetId, Instant created, Instant activates, Instant expires, byte[] secret)
    {
        return new Key(id, created, activates, expires, secret, keysetId);
    }

    private Key(long id, Instant created, Instant activates, Instant expires, byte[] secret, int keysetId)
    {
        this.id = id;
        this.keysetId = keysetId;
        this.created = created;
        this.activates = activates;
        this.expires = expires;
        this.secret = secret;
        this.siteId = 0;
    }


    public long getId() {
        return id;
    }

    public int getSiteId() { return siteId; }
    public int getKeysetId() { return keysetId;}

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

