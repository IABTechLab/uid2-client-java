package com.uid2.client;

enum PayloadType {
    ENCRYPTED_DATA(128),
    ENCRYPTED_DATA_V3(96);

    public final int value;
    PayloadType(int value) { this.value = value; }
}
