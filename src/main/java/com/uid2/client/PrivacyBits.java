package com.uid2.client;

import java.util.BitSet;

 class PrivacyBits {
    // Bit 0 is legacy and is no longer in use
    private final int bitClientSideGenerated = 1;

    private final BitSet bits;

    PrivacyBits(int bitsAsInt)
    {
        bits = new BitSet();
        int index = 0;
        while (bitsAsInt != 0) {
            if ((bitsAsInt & 1) == 1) {
                bits.set(index); // Set the corresponding bit to 1
            }
            bitsAsInt >>= 1; // Right shift to get next bit
            index++;
        }
    }

    boolean isClientSideGenerated() {
        return bits.get(bitClientSideGenerated);
    }
}
