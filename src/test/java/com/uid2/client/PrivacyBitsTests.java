package com.uid2.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrivacyBitsTests {
    @Test
    public void ReadsClientSideGeneratedBit_WhenFalse() {
        int bits = PrivacyBitsBuilder.Builder().WithAllFlagsEnabled().WithClientSideGenerated(false).Build();
        assertFalse(new PrivacyBits(bits).isClientSideGenerated());
    }

    @Test
    public void ReadsClientSideGeneratedBit_WhenTrue() {
        int bits = PrivacyBitsBuilder.Builder().WithAllFlagsEnabled().WithClientSideGenerated(true).Build();
        assertTrue(new PrivacyBits(bits).isClientSideGenerated());
    }
}
