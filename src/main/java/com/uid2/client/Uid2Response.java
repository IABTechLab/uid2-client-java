package com.uid2.client;

public class Uid2Response {
    String asString;
    byte[] asBytes;

    private Uid2Response(String asString, byte[] asBytes) {
        this.asString = asString;
        this.asBytes = asBytes;
    }

    public static Uid2Response fromString(String asString) {
        return new Uid2Response(asString, null);
    }

    public static Uid2Response fromBytes(byte[] asBytes) {
        return new Uid2Response(null, asBytes);
    }

    public String getAsString() {
        return asString;
    }

    public byte[] getAsBytes() {
        return asBytes;
    }

    public boolean isBinary() {
        return asBytes != null;
    }
}
