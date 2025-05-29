package com.uid2.client;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class IdentityMapV3Input {
    /**
     * @param emails a list of normalized or unnormalized email addresses
     * @return a IdentityMapInput instance, to be used in {@link IdentityMapHelper#createEnvelopeForIdentityMapRequest}
     */
    public static IdentityMapV3Input fromEmails(Iterable<String> emails) {
        return new IdentityMapV3Input(IdentityType.Email, emails, false);
    }

    /**
     * @param phones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return an IdentityMapInput instance
     */
    public static IdentityMapV3Input fromPhones(Iterable<String> phones) {
        return new IdentityMapV3Input(IdentityType.Phone, phones, false);
    }

    /**
     * @param hashedEmails a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return an IdentityMapInput instance
     */
    public static IdentityMapV3Input fromHashedEmails(Iterable<String> hashedEmails) {
        return new IdentityMapV3Input(IdentityType.Email, hashedEmails, true);
    }

    /**
     * @param hashedPhones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return an IdentityMapInput instance
     */
    public static IdentityMapV3Input fromHashedPhones(Iterable<String> hashedPhones) {
        return new IdentityMapV3Input(IdentityType.Phone, hashedPhones, true);
    }

    private IdentityMapV3Input(IdentityType identityType, Iterable<String> emailsOrPhones, boolean alreadyHashed) {
        if (identityType == IdentityType.Email) {
            hashedNormalizedEmails = new ArrayList<>();
            for (String email : emailsOrPhones) {
                if (alreadyHashed) {
                    hashedNormalizedEmails.add(new Identity(email));
                } else {
                    String hashedEmail = InputUtil.normalizeAndHashEmail(email);
                    hashedNormalizedEmails.add(new Identity(hashedEmail));
                    addHashedToRawDiiMapping(hashedEmail, email);
                }
            }
        } else {  //phone
            hashedNormalizedPhones = new ArrayList<>();
            for (String phone : emailsOrPhones) {
                if (alreadyHashed) {
                    hashedNormalizedPhones.add(new Identity(phone));
                } else {
                    if (!InputUtil.isPhoneNumberNormalized(phone)) {
                        throw new IllegalArgumentException("phone number is not normalized: " + phone);
                    }

                    String hashedNormalizedPhone = InputUtil.getBase64EncodedHash(phone);
                    addHashedToRawDiiMapping(hashedNormalizedPhone, phone);
                    hashedNormalizedPhones.add(new Identity(hashedNormalizedPhone));
                }
            }
        }
    }

    private void addHashedToRawDiiMapping(String hashedDii, String rawDii) {
        hashedDiiToRawDiis.computeIfAbsent(hashedDii, k -> new ArrayList<>()).add(rawDii);
    }


    List<String> getRawDiis(String identifier) {
        final boolean wasInputAlreadyHashed = hashedDiiToRawDiis.isEmpty();
        if (wasInputAlreadyHashed)
            return Collections.singletonList(identifier);
        return hashedDiiToRawDiis.get(identifier);
    }

    @SerializedName("email_hash")
    private List<Identity> hashedNormalizedEmails;
    @SerializedName("phone_hash")
    private List<Identity> hashedNormalizedPhones;

    private final transient HashMap<String, List<String>> hashedDiiToRawDiis = new HashMap<>();

    private static class Identity {
        @SerializedName("i")
        private final String i;

        public Identity(String value) {
            this.i = value;
        }
    }
}
