package com.uid2.client;

import com.google.gson.JsonObject;

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

    /**
     * @deprecated This method is deprecated and no longer has any effect. It is kept for backwards compatibility.
     * The policy parameter has been removed from the /token/generate endpoint.
     */
    @Deprecated
    public TokenGenerateInput doNotGenerateTokensForOptedOut() {
        // No-op: kept for backwards compatibility only
        return this;
    }

    String getAsJsonString() {
        if (alreadyHashed) {
            return createAlreadyHashedJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString);
        } else if (needHash) {
            return createHashedJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString);
        } else {
            return createJsonRequestForGenerateToken(identityType, emailOrPhone, transparencyAndConsentString);
        }
    }

    private TokenGenerateInput(IdentityType identityType, String emailOrPhone, boolean needHash, boolean alreadyHashed) {
        this.identityType = identityType;
        this.emailOrPhone = emailOrPhone;
        this.needHash = needHash;
        this.alreadyHashed = alreadyHashed;
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
            String hashedNormalizedEmail = InputUtil.normalizeAndHashEmail(unhashedValue);
            return createJsonRequestForGenerateToken("email_hash", hashedNormalizedEmail, tcString);
        } else {  //phone
            if (!InputUtil.isPhoneNumberNormalized(unhashedValue)) {
                throw new IllegalArgumentException("phone number is not normalized");
            }

            String hashedNormalizedPhone = InputUtil.getBase64EncodedHash(unhashedValue);
            return createJsonRequestForGenerateToken("phone_hash", hashedNormalizedPhone, tcString);
        }
    }

    static String createAlreadyHashedJsonRequestForGenerateToken(IdentityType identityType, String hashedValue, String tcString) {
        if (identityType == IdentityType.Email) {
            return createJsonRequestForGenerateToken("email_hash", hashedValue, tcString);
        } else {  //phone
            return createJsonRequestForGenerateToken("phone_hash", hashedValue, tcString);
        }
    }

    private final IdentityType identityType;
    private final String emailOrPhone;
    private boolean needHash;
    private final boolean alreadyHashed;
    private String transparencyAndConsentString;


}
