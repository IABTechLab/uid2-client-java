package com.uid2.client;


public enum UnmappedIdentityReason {
    OPTOUT,
    INVALID_IDENTIFIER,
    UNKNOWN;

    public static UnmappedIdentityReason fromString(String reason) {
        if (reason.equals("optout")) {
            return OPTOUT;
        }
        if (reason.equals("invalid identifier")) {
            return INVALID_IDENTIFIER;
        }
        
        return UNKNOWN;
    }
}