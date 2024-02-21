package com.uid2.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InputUtilTests {
    @Test
    void emailProducesCorrectHash() {
        //from https://unifiedid.com/docs/getting-started/gs-normalization-encoding#normalization-examples-for-email
        assertEmail("MyEmail@example.com", "FsGNM28LJQ8OLZB0Us65ZYp07NrovJSGTCMSKnLMJ6U=");
        assertEmail("MYEMAIL@example.com", "FsGNM28LJQ8OLZB0Us65ZYp07NrovJSGTCMSKnLMJ6U=");

        assertEmail("JANESAOIRSE@example.com", "1mcOepIAfxtf94Xx/IHlOqbT170GvfXEc83HKGwoS20=");
        assertEmail("JaneSaoirse@example.com", "1mcOepIAfxtf94Xx/IHlOqbT170GvfXEc83HKGwoS20=");

        assertEmail("JaneSaoirse+UID2@example.com", "bhQ2aMIGWT1ey4p7JyavdNlIQ4pe11/rrcv0u1jrxCc=");

        assertEmail("JANE.SAOIRSE@gmail.com", "ku4mBX7Z3qJTXWyLFB1INzkyR2WZGW4ANSJUiW21iI8=");
        assertEmail("Jane.Saoirse@gmail.com", "ku4mBX7Z3qJTXWyLFB1INzkyR2WZGW4ANSJUiW21iI8=");
        assertEmail("JaneSaoirse+UID2@gmail.com", "ku4mBX7Z3qJTXWyLFB1INzkyR2WZGW4ANSJUiW21iI8=");

    }

    void assertEmail(String email, String expectedHash) {
        String producedHash = InputUtil.normalizeAndHashEmail(email);
        assertEquals(expectedHash, producedHash);
    }

}
