package com.uid2.client;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.Assert.*;


public class KeyParserTests {
    @Test
    public void parseKeyListSharingEndpoint() throws Exception
    {
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
        Key masterKey = keyContainer.getMasterKey(Instant.now());
        assertNotNull(masterKey);
        assertEquals(2, masterKey.getId());

        Key defaultKey = keyContainer.getDefaultKey(Instant.now());
        assertNotNull(defaultKey);
        assertEquals(3, defaultKey.getId());
        assertEquals(1728000, keyContainer.getTokenExpirySeconds());

        Key key = keyContainer.getKey(3);
        assertNotNull(key);
        assertEquals(99999, key.getKeysetId());
        assertEquals(Instant.ofEpochSecond(1609459200), key.getCreated());
        assertEquals(Instant.ofEpochSecond(1609459210), key.getActivates());
        assertEquals(Instant.ofEpochSecond(1893456000), key.getExpires());
        assertEquals("o8HsvkwJ5Ulnrd0uui3GpukpwDapj+JLqb7qfN/GJKo=", InputUtil.byteArrayToBase64(key.getSecret()));

        key = keyContainer.getKey(2);
        assertNotNull(key);
        assertEquals(1, key.getKeysetId());
        assertEquals(Instant.ofEpochSecond(1609458200), key.getCreated());
        assertEquals(Instant.ofEpochSecond(1609459220), key.getActivates());
        assertEquals(Instant.ofEpochSecond(1893457000), key.getExpires());
        assertEquals("DD67xF8OFmbJ1/lMPQ6fGRDbJOT4kXErrYWcKdFfCUE=", InputUtil.byteArrayToBase64(key.getSecret()));
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

    private KeyContainer parse(String str) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        return KeyParser.parse(inputStream);
    }
}
