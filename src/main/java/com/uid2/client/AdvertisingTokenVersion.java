package com.uid2.client;

public enum AdvertisingTokenVersion {
    //showing as "AHA..." in the Base64 Encoding (Base64 'H' is 000111 and 112 is 01110000)
    V3(112),
    //showing as "AIA..." in the Base64URL Encoding ('H' is followed by 'I' hence
    //this choice for the next token version) (Base64 'I' is 001000 and 128 is 10000000)
    V4(128);

    int version;
    AdvertisingTokenVersion(int v) {
        version = v;
    }
    public int value() {
        return version;
    }

}
