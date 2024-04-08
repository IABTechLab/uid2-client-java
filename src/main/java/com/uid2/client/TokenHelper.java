package com.uid2.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class TokenHelper {
    private final Uid2Helper uid2Helper;
    private final Uid2ClientHelper uid2ClientHelper;
    private final AtomicReference<KeyContainer> container = new AtomicReference<>(null);;

    TokenHelper(String baseUrl, String clientApiKey, String base64SecretKey) {
        this.uid2ClientHelper = new Uid2ClientHelper(baseUrl, clientApiKey);
        this.uid2Helper = new Uid2Helper(base64SecretKey);
    }

    DecryptionResponse decrypt(String token, Instant now, String domainNameFromBidRequest, ClientType clientType) {
        KeyContainer keyContainer = this.container.get();
        if (keyContainer == null) {
            return DecryptionResponse.makeError(DecryptionStatus.NOT_INITIALIZED);
        }

        if (!keyContainer.isValid(now)) {
            return DecryptionResponse.makeError(DecryptionStatus.KEYS_NOT_SYNCED);
        }

        try {
            return Uid2Encryption.decrypt(token, keyContainer, now, keyContainer.getIdentityScope(), domainNameFromBidRequest, clientType);
        } catch (Exception e) {
            return DecryptionResponse.makeError(DecryptionStatus.INVALID_PAYLOAD);
        }
    }

    EncryptionDataResponse encryptRawUidIntoToken(String rawUid, Instant now) {
        KeyContainer keyContainer = this.container.get();
        if (keyContainer == null) {
            return EncryptionDataResponse.makeError(EncryptionStatus.NOT_INITIALIZED);
        }

        if (!keyContainer.isValid(now)) {
            return EncryptionDataResponse.makeError(EncryptionStatus.KEYS_NOT_SYNCED);
        }

        return Uid2Encryption.encrypt(rawUid, keyContainer, keyContainer.getIdentityScope(), now);
    }

    RefreshResponse refresh(String urlSuffix) {
        try{
            EnvelopeV2 envelope = uid2Helper.createEnvelopeV2("".getBytes());
            String responseString = uid2ClientHelper.makeRequest(envelope, urlSuffix);
            byte[] response = uid2Helper.decrypt(responseString, envelope.getNonce()).getBytes();
            this.container.set(KeyParser.parse(new ByteArrayInputStream(response)));
            return RefreshResponse.makeSuccess();
        } catch (Exception ex) {
            return RefreshResponse.makeError(ex.getMessage());
        }
    }

    RefreshResponse refreshJson(String json) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            this.container.set(KeyParser.parse(inputStream));
            return RefreshResponse.makeSuccess();
        } catch (Exception ex) {
            return RefreshResponse.makeError(ex.getMessage());
        }
    }
}
