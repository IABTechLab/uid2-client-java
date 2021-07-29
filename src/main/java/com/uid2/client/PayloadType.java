package com.uid2.client;

enum PayloadType {
    ENCRYPTED_DATA(128);

    public final int value;
    PayloadType(int value) { this.value = value; }
}
