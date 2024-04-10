package com.uid2.client;

import com.uid2.client.PrivacyBitsBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrivacyBitsBuilderTests {
    private final PrivacyBitsBuilder builder = PrivacyBitsBuilder.Builder();

    @Test
    public void WithAllFlagsEnabled()
    {
        int privacyBits = builder.WithAllFlagsEnabled().Build();
        assertEquals(7, privacyBits);
    }

    @Test
    public void WithAllFlagsDisabled()
    {
        int privacyBits = builder.WithAllFlagsDisabled().Build();
        assertEquals(0, privacyBits);
    }

    @Test
    public void AllDisabledByDefault()
    {
        int privacyBits = builder.Build();
        assertEquals(0, privacyBits);
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, 6",
            "false, false, 0",
            "true, false, 2",
            "false, true, 4"
    })
    public void SetsPrivacyBitCombinations(boolean isClientSideGenerated, boolean isOptedOut, int expected)
    {
        int privacyBits = builder.WithOptedOut(isOptedOut).WithClientSideGenerated(isClientSideGenerated).Build();
        assertEquals(expected, privacyBits);
    }
}
