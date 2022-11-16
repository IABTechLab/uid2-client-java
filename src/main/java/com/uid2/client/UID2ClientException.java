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

//use Uid2Exception for unchecked exceptions, which can be more convenient for consumers in large code bases
 class Uid2Exception extends RuntimeException {
     Uid2Exception(String message) {
         super(message);
     }

     Uid2Exception(String message, Exception cause) {
         super(message, cause);
     }
 }

