// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

 package com.uid2.client.test;

import com.uid2.client.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class KeyGen {
    public static class Params
    {
        Instant tokenExpiry = Instant.now().plus(1, ChronoUnit.HOURS);

        public Params() {}
        public Params withTokenExpiry(Instant expiry) { tokenExpiry = expiry; return this; }
    }

    public static Params defaultParams() { return new Params(); }

    public static byte[] encrypt(String uid, Key masterKey, long siteId, Key siteKey) throws Exception {
        return encrypt(uid, masterKey, siteId, siteKey, defaultParams());
    }

    public static byte[] encrypt(String uid, Key masterKey, long siteId, Key siteKey, Params params) throws Exception {
        Random rd = new Random();
        byte[] uidBytes = uid.getBytes(StandardCharsets.UTF_8);
        ByteBuffer identityWriter = ByteBuffer.allocate(4 + 4 + uidBytes.length + 4 + 8);

        identityWriter.putInt((int) siteId);
        identityWriter.putInt(uidBytes.length);
        identityWriter.put(uidBytes);
        identityWriter.putInt(0);
        identityWriter.putLong(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());
        byte[] identityIv = new byte[16];
        rd.nextBytes(identityIv);
        byte[] encryptedIdentity = encrypt(identityWriter.array(), identityIv, siteKey.getSecret());

        ByteBuffer masterWriter = ByteBuffer.allocate(8 + 4 + encryptedIdentity.length);

        masterWriter.putLong(params.tokenExpiry.toEpochMilli());
        masterWriter.putInt((int) siteKey.getId());
        masterWriter.put(encryptedIdentity);

        byte[] masterIv = new byte[16];
        rd.nextBytes(masterIv);
        byte[] encryptedMasterPayload = encrypt(masterWriter.array(), masterIv, masterKey.getSecret());

        ByteBuffer rootWriter = ByteBuffer.allocate(1 + 4 + encryptedMasterPayload.length);
        rootWriter.put((byte) 2);
        rootWriter.putInt((int) masterKey.getId());
        rootWriter.put(encryptedMasterPayload);

        return rootWriter.array();
    }

    private static byte[] encrypt(byte[] data, byte[] iv, byte[] secret) throws Exception {
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        byte[] finalized = new byte[16 + encrypted.length];
        for (int i = 0; i < 16; i++) finalized[i] = iv[i];
        for (int i = 0; i < encrypted.length; i++) finalized[16 + i] = encrypted[i];
        return finalized;
    }
}
