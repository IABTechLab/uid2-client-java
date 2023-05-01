package com.uid2.client;

import com.google.gson.annotations.SerializedName;

public final class MappedUid {
    private String identifier;

    @SerializedName("advertising_id")
    private String advertisingId;

    @SerializedName("bucket_id")
    private String bucketId;    

    public String getIdentifier() {
        return this.identifier;
    }

    public String getAdvertisingId() {
        return this.advertisingId;
    }

    public String getBucketId() {
        return this.bucketId;
    }
}
