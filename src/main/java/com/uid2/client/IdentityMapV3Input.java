package com.uid2.client;

import com.google.gson.annotations.SerializedName;

import java.util.*;

public class IdentityMapV3Input {
    /**
     * @param emails a list of normalized or unnormalized email addresses
     * @return a IdentityMapV3Input instance, to be used in {@link IdentityMapHelper#createEnvelopeForIdentityMapRequest}
     */
    public static IdentityMapV3Input fromEmails(List<String> emails) {
        return new IdentityMapV3Input().withEmails(emails);
    }

    /**
     * @param hashedEmails a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return an IdentityMapV3Input instance
     */
    public static IdentityMapV3Input fromHashedEmails(List<String> hashedEmails) {
        return new IdentityMapV3Input().withHashedEmails(hashedEmails);
    }

    /**
     * @param phones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return an IdentityMapV3Input instance
     */
    public static IdentityMapV3Input fromPhones(List<String> phones) {
        return new IdentityMapV3Input().withPhones(phones);
    }

    /**
     * @param hashedPhones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return an IdentityMapV3Input instance
     */
    public static IdentityMapV3Input fromHashedPhones(List<String> hashedPhones) {
        return new IdentityMapV3Input().withHashedPhones(hashedPhones);
    }

    private transient final Map<String, List<String>> diiMappings = new HashMap<>();

    @SerializedName("email_hash")
    private final List<Identity> hashedEmails = new ArrayList<>();

    @SerializedName("phone_hash")
    private final List<Identity> hashedPhones = new ArrayList<>();

    private IdentityMapV3Input() {}

    public IdentityMapV3Input withHashedEmails(List<String> hashedEmails) {
        for (String hashedEmail : hashedEmails) {
            this.hashedEmails.add(new Identity(hashedEmail));
            addToDiiMappings(hashedEmail, hashedEmail);
        }
        return this;
    }

    public IdentityMapV3Input withHashedPhones(List<String> hashedPhones) {
        for (String hashedPhone : hashedPhones) {
            this.hashedPhones.add(new Identity(hashedPhone));
            addToDiiMappings(hashedPhone, hashedPhone);
        }
        return this;
    }

    public IdentityMapV3Input withEmails(List<String> emails) {
        for (String email : emails) {
            String hash = InputUtil.normalizeAndHashEmail(email);
            this.hashedEmails.add(new Identity(hash));
            addToDiiMappings(hash, email);
        }
        return this;
    }

    public IdentityMapV3Input withPhones(List<String> phones) {
        for (String phone : phones) {
            if (!InputUtil.isPhoneNumberNormalized(phone)) {
                throw new IllegalArgumentException("phone number is not normalized: " + phone);
            }

            String hash = InputUtil.getBase64EncodedHash(phone);
            this.hashedPhones.add(new Identity(hash));
            addToDiiMappings(hash, phone);
        }
        return this;
    }

    private void addToDiiMappings(String hashedDii, String rawDii) {
        diiMappings.computeIfAbsent(hashedDii, k -> new ArrayList<>()).add(rawDii);
    }

    List<String> getRawDiis(String identityType, int i) {
        return diiMappings.get(getEncodedDii(identityType, i));
    }

    private String getEncodedDii(String identityType, int i) {
        switch (identityType) {
            case "email_hash": return hashedEmails.get(i).identity;
            case "phone_hash": return hashedPhones.get(i).identity;
        }
        throw new Uid2Exception("Unexpected identity type: " + identityType);
    }


    private static class Identity {
        @SerializedName("i")
        private final String identity;

        public Identity(String value) {
            this.identity = value;
        }
    }
}
