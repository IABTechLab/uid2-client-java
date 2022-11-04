package com.uid2.client;

public class UID2ClientFactory {
    public static IUID2Client create(String endpoint, String authKey, String secretKey) {
        return new UID2Client(endpoint, authKey, secretKey, IdentityScope.UID2);
    }
}
