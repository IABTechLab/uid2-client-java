package com.uid2.client.builder;

import java.util.BitSet;

public class PrivacyBitsBuilder {
    private boolean legacyBit; // first bit, doesn't have a meaning any more
    private boolean isCstgDerived;
    private boolean isOptedOut;

    public static PrivacyBitsBuilder Builder()
    {
        return new PrivacyBitsBuilder();
    }

    public PrivacyBitsBuilder WithAllFlagsEnabled()
    {
        legacyBit = true;
        isCstgDerived = true;
        isOptedOut = true;

        return this;
    }

    public PrivacyBitsBuilder WithAllFlagsDisabled()
    {
        legacyBit = false;
        isCstgDerived = false;
        isOptedOut = false;

        return this;
    }

    public PrivacyBitsBuilder WithClientSideGenerated(boolean isCstgDerived)
    {
        this.isCstgDerived = isCstgDerived;
        return this;
    }

    public PrivacyBitsBuilder WithOptedOut(boolean isOptedOut)
    {
        this.isOptedOut = isOptedOut;
        return this;
    }

    public int Build()
    {
        return FlagsToInt(new boolean[] { legacyBit, isCstgDerived, isOptedOut });
    }

    private static int FlagsToInt(boolean[] flags)
    {
        BitSet bitSet = new BitSet(flags.length);
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) {
                bitSet.set(i);
            }
        }
        return bitSetToInt(bitSet);
    }

    public static int bitSetToInt(BitSet bitSet) {
        int intValue = 0;
        for (int i = 0; i < bitSet.length(); i++) {
            intValue += (bitSet.get(i) ? 1 : 0) << i;
        }
        return intValue;
    }
}
