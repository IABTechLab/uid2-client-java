package com.uid2.client;

import com.google.gson.JsonObject;

import java.security.MessageDigest;

public class IdentityInput {
    static public IdentityInput fromEmail(String email) {
        return new IdentityInput(IdentityType.Email, email, true);
    }

    static public IdentityInput fromPhone(String phone) {
        return new IdentityInput(IdentityType.Phone, phone, true);
    }

    public IdentityInput withTcfConsentString(String tcf) {
        this.tcf = tcf;
        return this;
    }

    IdentityInput doNotHash() {
        hash = false;
        return this;
    }

    String getAsJsonString() {
        if (hash) {
            return createHashedJsonRequestForGenerateToken(identityType, emailOrPhone, tcf);
        } else {
            return createJsonRequestForGenerateToken(identityType, emailOrPhone, tcf);
        }
    }

    private IdentityInput(IdentityType identityType, String emailOrPhone, boolean hash) {
        this.identityType = identityType;
        this.emailOrPhone = emailOrPhone;
        this.hash = hash;
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


    private static String createJsonRequestForGenerateToken(IdentityType identityType, String value, String tcf) {
        final String property = (identityType == IdentityType.Email) ? "email" : "phone";
        return createJsonRequestForGenerateToken(property, value, tcf);
    }

    private static String createJsonRequestForGenerateToken(String property, String value, String tcf) {
        JsonObject json = new JsonObject();

        json.addProperty(property, value);
        if (tcf != null) {
            json.addProperty("tcf_consent_string", tcf);
        }

        return json.toString();
    }

    static String createHashedJsonRequestForGenerateToken(IdentityType identityType, String unhashedValue, String tcf) {
        if (identityType == IdentityType.Email) {
            String normalizedEmail = InputUtil.normalizeEmailString(unhashedValue);
            if (normalizedEmail == null) {
                throw new IllegalArgumentException("invalid email address");
            }
            String hashedNormalizedEmail = getBase64EncodedHash(normalizedEmail);
            return createJsonRequestForGenerateToken("email_hash", hashedNormalizedEmail, tcf);
        } else {  //phone
            if (!InputUtil.isPhoneNumberNormalized(unhashedValue)) {
                throw new IllegalArgumentException("phone number is not normalized");
            }

            String hashedNormalizedPhone = getBase64EncodedHash(unhashedValue);
            return createJsonRequestForGenerateToken("phone_hash", hashedNormalizedPhone, tcf);
        }
    }

    private final IdentityType identityType;
    private final String emailOrPhone;
    private boolean hash;
    private String tcf;
}
