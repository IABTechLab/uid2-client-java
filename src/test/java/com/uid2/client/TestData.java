package com.uid2.client;

import com.google.gson.stream.JsonWriter;

import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class TestData {
    public static final long MASTER_KEY_ID = 164;
    public static final long SITE_KEY_ID = 165;
    public static final int SITE_ID = 9000;
    public static final int SITE_ID2 = 2;
    public static final int[] INT_MASTER_SECRET = new int[] { 139, 37, 241, 173, 18, 92, 36, 232, 165, 168, 23, 18, 38, 195, 123, 92, 160, 136, 185, 40, 91, 173, 165, 221, 168, 16, 169, 164, 38, 139, 8, 155 };
    public static final int[] INT_SITE_SECRET = new int[] { 32, 251, 7, 194, 132, 154, 250, 86, 202, 116, 104, 29, 131, 192, 139, 215, 48, 164, 11, 65, 226, 110, 167, 14, 108, 51, 254, 125, 65, 24, 23, 133 };
    public static final Instant NOW = Instant.now();
    public static final Key MASTER_KEY = new Key(MASTER_KEY_ID, -1, NOW.minus(1, ChronoUnit.DAYS), NOW, NOW.plus(1, ChronoUnit.DAYS), getMasterSecret());
    public static final Key SITE_KEY = new Key(SITE_KEY_ID, SITE_ID, NOW.minus(10, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), getSiteSecret());
    public static final String EXAMPLE_UID = "ywsvDNINiZOVSsfkHpLpSJzXzhr6Jx9Z/4Q0+lsEUvM=";
    public static final String EXAMPLE_PHONE_RAW_UID2_V3 = "BFOsW2SkK0egqbfyiALtpti5G/cG+PcEvjkoHl56rEV8";
    public static final String CLIENT_SECRET = "ioG3wKxAokmp+rERx6A4kM/13qhyolUXIu14WN16Spo=";

    public static byte[] getMasterSecret() {
        return intArrayToByteArray(INT_MASTER_SECRET);
    }

    public static byte[] getSiteSecret() {
        return intArrayToByteArray(INT_SITE_SECRET);
    }

    private static byte[] intArrayToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i] = (byte) (intArray[i] & 0xFF);
        }
        return byteArray;
    }

    public static byte[] getTestSecret(int value) {
        byte[] secret = new byte[32];
        Arrays.fill(secret, (byte)value);
        return secret;
    }

    public static String keySetToJson(Key ... keys) throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject();
        writer.name("body");
        writer.beginArray();
        for(Key k : keys) {
            writer.beginObject();
            writer.name("id"); writer.value(k.getId());
            writer.name("site_id"); writer.value(k.getSiteId());
            writer.name("created"); writer.value(k.getCreated().getEpochSecond());
            writer.name("activates"); writer.value(k.getActivates().getEpochSecond());
            writer.name("expires"); writer.value(k.getExpires().getEpochSecond());
            writer.name("secret"); writer.value(Base64.getEncoder().encodeToString(k.getSecret()));
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
        return sw.toString();
    }
}
