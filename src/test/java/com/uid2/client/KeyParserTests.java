package com.uid2.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;


public class KeyParserTests {
    private static final int DEFAULT_TOKEN_EXPIRY_SECONDS = 2592000;

    @Test
    public void parseKeyListSharingEndpoint() {
        String s = "{ \"body\": { " +
                "\"caller_site_id\": 11, " +
                "\"master_keyset_id\": 1, " +
                "\"default_keyset_id\": 99999, " +
                "\"token_expiry_seconds\": 1728000, " +
                "\"keys\": [ " +
                    "{ " +
                        "\"id\": 3, " +
                        "\"keyset_id\": 99999, " +
                        "\"created\": 1609459200, " +
                        "\"activates\": 1609459210, " +
                        "\"expires\": 1893456000, " +
                        "\"secret\": \"o8HsvkwJ5Ulnrd0uui3GpukpwDapj+JLqb7qfN/GJKo=\"" +
                    "}, " +
                    "{ " +
                        "\"id\": 2, " +
                        "\"keyset_id\": 1, " +
                        "\"created\": 1609458200, " +
                        "\"activates\": 1609459220, " +
                        "\"expires\": 1893457000, " +
                        "\"secret\": \"DD67xF8OFmbJ1/lMPQ6fGRDbJOT4kXErrYWcKdFfCUE=\"" +
                    "} " +
                "] " +
                "}, " +
                "\"status\": \"success\" }";

        KeyContainer keyContainer = parse(s);

        assertEquals(11, keyContainer.getCallerSiteId());
        assertEquals(1728000, keyContainer.getTokenExpirySeconds());

        Key masterKey = keyContainer.getMasterKey(Instant.now());
        assertNotNull(masterKey);
        assertEquals(2, masterKey.getId());

        Key defaultKey = keyContainer.getDefaultKey(Instant.now());
        assertNotNull(defaultKey);
        assertEquals(3, defaultKey.getId());

        Key key3 = keyContainer.getKey(3);
        assertNotNull(key3);
        assertEquals(99999, key3.getKeysetId());
        assertEquals(Instant.ofEpochSecond(1609459200), key3.getCreated());
        assertEquals(Instant.ofEpochSecond(1609459210), key3.getActivates());
        assertEquals(Instant.ofEpochSecond(1893456000), key3.getExpires());
        assertEquals("o8HsvkwJ5Ulnrd0uui3GpukpwDapj+JLqb7qfN/GJKo=", InputUtil.byteArrayToBase64(key3.getSecret()));

        Key key2 = keyContainer.getKey(2);
        assertNotNull(key2);
        assertEquals(1, key2.getKeysetId());
        assertEquals(Instant.ofEpochSecond(1609458200), key2.getCreated());
        assertEquals(Instant.ofEpochSecond(1609459220), key2.getActivates());
        assertEquals(Instant.ofEpochSecond(1893457000), key2.getExpires());
        assertEquals("DD67xF8OFmbJ1/lMPQ6fGRDbJOT4kXErrYWcKdFfCUE=", InputUtil.byteArrayToBase64(key2.getSecret()));
    }

    @Test
    public void parseErrorKeyList() {
        assertThrows(Exception.class, () -> parse("{\"status\": \"error\"}"));
        assertThrows(Exception.class, () -> parse("{\"body\": \"error\"}"));
        assertThrows(Exception.class, () -> parse("{\"body\": [1, 2, 3]}"));
        assertThrows(Exception.class, () -> parse("{\"body\": [{}]}"));
        assertThrows(Exception.class, () -> parse("{\"body\": [{\"id\": \"test\"}]}"));
        assertThrows(Exception.class, () -> parse("{\"body\": [{\"id\": 5}]}"));
    }

    @Test
    public void parseWithNullTokenExpirySecondField() {
        String s = "{ \"body\": { " +
                "\"caller_site_id\": 11, " +
                "\"token_expiry_seconds\": null " +
                "}, " +
                "\"status\": \"success\" }";

        KeyContainer keyContainer = parse(s);

        assertEquals(11, keyContainer.getCallerSiteId());
        assertEquals(DEFAULT_TOKEN_EXPIRY_SECONDS, keyContainer.getTokenExpirySeconds());
    }

    @Test
    public void parseWithMissingTokenExpirySecondField() {
        String s = "{ \"body\": { " +
                "\"caller_site_id\": 11 " +
                "}, " +
                "\"status\": \"success\" }";

        KeyContainer keyContainer = parse(s);

        assertEquals(11, keyContainer.getCallerSiteId());
        assertEquals(DEFAULT_TOKEN_EXPIRY_SECONDS, keyContainer.getTokenExpirySeconds());
    }

    private KeyContainer parse(String str) {
        InputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        return KeyParser.parse(inputStream);
    }
}
