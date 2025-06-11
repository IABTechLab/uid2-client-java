package com.uid2.client;


public enum UnmappedIdentityReason {
    OPTOUT,
    INVALID,
    UNKNOWN;

    public static UnmappedIdentityReason fromString(String reason) {
        for (UnmappedIdentityReason knownReason : values()) {
            if (knownReason.name().equals(reason.toUpperCase())) {
                return knownReason;
            }
        }
        
        return UNKNOWN;
    }
}