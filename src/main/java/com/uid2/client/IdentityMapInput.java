package com.uid2.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IdentityMapInput {
    /**
     * @param emails a list of normalized or unnormalized email addresses
     * @return a IdentityMapInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static IdentityMapInput fromEmails(Iterable<String> emails) {
        return new IdentityMapInput(IdentityType.Email, emails, false);
    }

    /**
     * @param phones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return a IdentityMapInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static IdentityMapInput fromPhones(Iterable<String> phones) {
        return new IdentityMapInput(IdentityType.Phone, phones, false);
    }

    /**
     * @param hashedEmails a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return a IdentityMapInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static IdentityMapInput fromHashedEmails(Iterable<String> hashedEmails) {
        return new IdentityMapInput(IdentityType.Email, hashedEmails, true);
    }

    /**
     * @param hashedPhones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return a IdentityMapInput instance, to be used in {@link PublisherUid2Helper#createEnvelopeForTokenGenerateRequest}
     */
    public static IdentityMapInput fromHashedPhones(Iterable<String> hashedPhones) {
        return new IdentityMapInput(IdentityType.Phone, hashedPhones, true);
    }

    String getAsJsonString() {
        if (alreadyHashed) {
            return createAlreadyHashedJsonRequestForIdentityMap(identityType, emailsOrPhones);
        } else {
            return createHashedJsonRequestForIdentityMap(identityType, emailsOrPhones);
        }
    }

        private IdentityMapInput(IdentityType identityType, Iterable<String> emailsOrPhones, boolean alreadyHashed) {
        this.identityType = identityType;
        this.emailsOrPhones = emailsOrPhones;
        this.alreadyHashed = alreadyHashed;
    }


    private static String createJsonRequestForIdentityMap(String property, Iterable<String> values) {
        JsonObject json = new JsonObject();

        JsonArray jsonArray = new JsonArray();
        for (String value : values) {
            jsonArray.add(value);
        }

        json.add(property, jsonArray);
        return json.toString();
    }

    private String createHashedJsonRequestForIdentityMap(IdentityType identityType, Iterable<String> unhashedValues) {
        if (identityType == IdentityType.Email) {
            List<String> hashedNormalizedEmails = new ArrayList<>();
            for (String unnormalizedEmail : unhashedValues) {
                String hashedEmail = InputUtil.normalizeAndHashEmail(unnormalizedEmail);
                hashedNormalizedEmails.add(hashedEmail);
                hashedDiiToRawDii.put(hashedEmail, unnormalizedEmail);
            }
            return createJsonRequestForIdentityMap("email_hash", hashedNormalizedEmails);
        } else {  //phone
            List<String> hashedNormalizedPhones = new ArrayList<>();
            for (String phone : unhashedValues) {
                if (!InputUtil.isPhoneNumberNormalized(phone)) {
                    throw new IllegalArgumentException("phone number is not normalized: " + phone);
                }

                String hashedNormalizedPhone = InputUtil.getBase64EncodedHash(phone);
                hashedDiiToRawDii.put(hashedNormalizedPhone, phone);
                hashedNormalizedPhones.add(hashedNormalizedPhone);
            }
            return createJsonRequestForIdentityMap("phone_hash", hashedNormalizedPhones);
        }
    }

    private static String createAlreadyHashedJsonRequestForIdentityMap(IdentityType identityType, Iterable<String> hashedValues) {
        if (identityType == IdentityType.Email) {
            return createJsonRequestForIdentityMap("email_hash", hashedValues);
        } else {  //phone
            return createJsonRequestForIdentityMap("phone_hash", hashedValues);
        }
    }

    String getRawDii(String identifier) {
        if (alreadyHashed)
            return identifier;
        return hashedDiiToRawDii.get(identifier);
    }

    private final IdentityType identityType;
    private final Iterable<String> emailsOrPhones;
    private final boolean alreadyHashed;
    private final HashMap<String, String> hashedDiiToRawDii = new HashMap<>();
}
