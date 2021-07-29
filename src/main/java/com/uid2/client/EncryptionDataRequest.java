package com.uid2.client;

import java.time.Instant;

public class EncryptionDataRequest {
    private byte[] data = null;
    private Integer siteId = null;
    private Key key = null;
    private String advertisingToken = null;
    private byte[] iv = null;
    private Instant now = null;

    public EncryptionDataRequest() {}

    public EncryptionDataRequest(byte[] data) {
        this.data = data;
    }

    public static EncryptionDataRequest forData(byte[] data) { return new EncryptionDataRequest(data); }

    public EncryptionDataRequest withData(byte[] data) { this.data = data; return this; }
    public EncryptionDataRequest withSiteId(int siteId) { this.siteId = siteId; return this; }
    public EncryptionDataRequest withKey(Key key) { this.key = key; return this; }
    public EncryptionDataRequest withAdvertisingToken(String token) { this.advertisingToken = token; return this; }
    public EncryptionDataRequest withInitializationVector(byte[] iv) { this.iv = iv; return this; }
    public EncryptionDataRequest withNow(Instant now) { this.now = now; return this; }

    public byte[] getData() { return data; }
    public Integer getSiteId() { return siteId; }
    public Key getKey() { return key; }
    public String getAdvertisingToken() { return advertisingToken; }
    public byte[] getInitializationVector() { return iv; }
    public Instant getNow() { return now == null ? Instant.now() : now; }
}
