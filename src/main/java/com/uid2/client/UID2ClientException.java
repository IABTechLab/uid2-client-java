 package com.uid2.client;

public class UID2ClientException extends Exception {
    public UID2ClientException(Throwable cause) {
        super(cause);
    }

    public UID2ClientException(String message) {
        super(message);
    }

    public UID2ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
