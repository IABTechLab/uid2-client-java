package com.uid2.client;

import com.google.gson.JsonObject;

import java.security.MessageDigest;

public class TokenGenerateInput {
    /**
     * @param email a normalized or unnormalized email address
     * @return a TokenGenerateInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static TokenGenerateInput fromEmail(String email) {
        return new TokenGenerateInput(IdentityType.Email, email, true, false);
    }

    /**
     * @param phone a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return a TokenGenerateInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static TokenGenerateInput fromPhone(String phone) {
        return new TokenGenerateInput(IdentityType.Phone, phone, true, false);
    }

    /**
     * @param hashedEmail a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return a TokenGenerateInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static TokenGenerateInput fromHashedEmail(String hashedEmail) {
        return new TokenGenerateInput(IdentityType.Email, hashedEmail, false, true);
    }

    /**
     * @param hashedPhone a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return a TokenGenerateInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static TokenGenerateInput fromHashedPhone(String hashedPhone) {
        return new TokenGenerateInput(IdentityType.Phone, hashedPhone, false, true);
    }

    /**
     * @param tcString a <a href="https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework">Transparency and Consent String</a>, which is a requirement for EUID but not for UID2
     * @return a TokenGenerateInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public TokenGenerateInput withTransparencyAndConsentString(String tcString) {
        this.transparencyAndConsentString = tcString;
        return this;
    }

    TokenGenerateInput doNotHash() {
        needHash = false;
        return this;
    }

    public TokenGenerateInput doNotGenerateTokensForOptedOut() {
        generateForOptedOut = false;
        return this;
    }

    String getAsJsonString() {
        if (alreadyHashed) {
            return createAlreadyHashedJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString, generateForOptedOut);
        } else if (needHash) {
            return createHashedJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString, generateForOptedOut);
        } else {
            return createJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString, generateForOptedOut);
        }
    }

    private TokenGenerateInput(IdentityType identityType, String emailOrPhone, boolean needHash, boolean alreadyHashed) {
        this.identityType = identityType;
        this.emailOrPhone = emailOrPhone;
        this.needHash = needHash;
        this.alreadyHashed = alreadyHashed;
    }



    private static String createJsonRequestForGenerateToken(IdentityType identityType, String value, String tcString, boolean generateForOptedOut) {
        final String property = (identityType == IdentityType.Email) ? "email" : "phone";
        return createJsonRequestForGenerateToken(property, value, tcString, generateForOptedOut);
    }

    private static String createJsonRequestForGenerateToken(String property, String value, String tcString, boolean generateForOptedOut) {
        JsonObject json = new JsonObject();

        json.addProperty(property, value);
        if (tcString != null) {
            json.addProperty("tcf_consent_string", tcString);
        }
        if(!generateForOptedOut){
            json.addProperty("optout_check", 1);
        }
        return json.toString();
    }

    static String createHashedJsonRequestForGenerateToken(IdentityType identityType, String unhashedValue, String tcString, boolean generateForOptedOut) {
        if (identityType == IdentityType.Email) {
            String normalizedEmail = InputUtil.normalizeEmailString(unhashedValue);
            if (normalizedEmail == null) {
                throw new IllegalArgumentException("invalid email address");
            }
            String hashedNormalizedEmail = InputUtil.getBase64EncodedHash(normalizedEmail);
            return createJsonRequestForGenerateToken("email_hash", hashedNormalizedEmail, tcString, generateForOptedOut);
        } else {  //phone
            if (!InputUtil.isPhoneNumberNormalized(unhashedValue)) {
                throw new IllegalArgumentException("phone number is not normalized");
            }

            String hashedNormalizedPhone = InputUtil.getBase64EncodedHash(unhashedValue);
            return createJsonRequestForGenerateToken("phone_hash", hashedNormalizedPhone, tcString, generateForOptedOut);
        }
    }

    static String createAlreadyHashedJsonRequestForGenerateToken(IdentityType identityType, String hashedValue, String tcString, boolean generateForOptedOut) {
        if (identityType == IdentityType.Email) {
            return createJsonRequestForGenerateToken("email_hash", hashedValue, tcString, generateForOptedOut);
        } else {  //phone
            return createJsonRequestForGenerateToken("phone_hash", hashedValue, tcString, generateForOptedOut);
        }
    }

    private final IdentityType identityType;
    private final String emailOrPhone;
    private boolean needHash;
    private final boolean alreadyHashed;
    private boolean generateForOptedOut = true;
    private String transparencyAndConsentString;


}
