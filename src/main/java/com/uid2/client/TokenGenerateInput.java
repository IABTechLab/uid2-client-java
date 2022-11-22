package com.uid2.client;

import com.google.gson.JsonObject;

import java.security.MessageDigest;

public class TokenGenerateInput {
    public static TokenGenerateInput fromEmail(String email) {
        return new TokenGenerateInput(IdentityType.Email, email, true);
    }

    public static TokenGenerateInput fromPhone(String phone) {
        return new TokenGenerateInput(IdentityType.Phone, phone, true);
    }

    public TokenGenerateInput withTransparencyAndConsentString(String tcString) {
        this.transparencyAndConsentString = tcString;
        return this;
    }

    TokenGenerateInput doNotHash() {
        needHash = false;
        return this;
    }

    String getAsJsonString() {
        if (needHash) {
            return createHashedJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString);
        } else {
            return createJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString);
        }
    }

    private TokenGenerateInput(IdentityType identityType, String emailOrPhone, boolean needHash) {
        this.identityType = identityType;
        this.emailOrPhone = emailOrPhone;
        this.needHash = needHash;
    }

    private static String getBase64EncodedHash(String input) {
        return InputUtil.byteArrayToBase64(getSha256Bytes(input));
    }

    private static byte[] getSha256Bytes(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input.getBytes());
            return md.digest();
        } catch (Exception e) {
            throw new Uid2Exception("Trouble Generating SHA256", e);
        }
    }


    private static String createJsonRequestForGenerateToken(IdentityType identityType, String value, String tcString) {
        final String property = (identityType == IdentityType.Email) ? "email" : "phone";
        return createJsonRequestForGenerateToken(property, value, tcString);
    }

    private static String createJsonRequestForGenerateToken(String property, String value, String tcString) {
        JsonObject json = new JsonObject();

        json.addProperty(property, value);
        if (tcString != null) {
            json.addProperty("tcf_consent_string", tcString);
        }

        return json.toString();
    }

    static String createHashedJsonRequestForGenerateToken(IdentityType identityType, String unhashedValue, String tcString) {
        if (identityType == IdentityType.Email) {
            String normalizedEmail = InputUtil.normalizeEmailString(unhashedValue);
            if (normalizedEmail == null) {
                throw new IllegalArgumentException("invalid email address");
            }
            String hashedNormalizedEmail = getBase64EncodedHash(normalizedEmail);
            return createJsonRequestForGenerateToken("email_hash", hashedNormalizedEmail, tcString);
        } else {  //phone
            if (!InputUtil.isPhoneNumberNormalized(unhashedValue)) {
                throw new IllegalArgumentException("phone number is not normalized");
            }

            String hashedNormalizedPhone = getBase64EncodedHash(unhashedValue);
            return createJsonRequestForGenerateToken("phone_hash", hashedNormalizedPhone, tcString);
        }
    }

    private final IdentityType identityType;
    private final String emailOrPhone;
    private boolean needHash;
    private String transparencyAndConsentString;
}
