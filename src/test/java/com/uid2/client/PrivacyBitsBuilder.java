package com.uid2.client;

import java.util.BitSet;

class PrivacyBitsBuilder {
    private boolean legacyBit; // first bit, doesn't have a meaning any more
    private boolean isCstgDerived;
    private boolean isOptedOut;

    static PrivacyBitsBuilder Builder()
    {
        return new PrivacyBitsBuilder();
    }

    PrivacyBitsBuilder WithAllFlagsEnabled()
    {
        legacyBit = true;
        isCstgDerived = true;
        isOptedOut = true;

        return this;
    }

    PrivacyBitsBuilder WithAllFlagsDisabled()
    {
        legacyBit = false;
        isCstgDerived = false;
        isOptedOut = false;

        return this;
    }

    PrivacyBitsBuilder WithClientSideGenerated(boolean isCstgDerived)
    {
        this.isCstgDerived = isCstgDerived;
        return this;
    }

    PrivacyBitsBuilder WithOptedOut(boolean isOptedOut)
    {
        this.isOptedOut = isOptedOut;
        return this;
    }

    int Build()
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

    static int bitSetToInt(BitSet bitSet) {
        int intValue = 0;
        for (int i = 0; i < bitSet.length(); i++) {
            intValue += (bitSet.get(i) ? 1 : 0) << i;
        }
        return intValue;
    }
}
