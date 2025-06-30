package com.uid2.client;

import com.google.gson.annotations.SerializedName;

import java.util.*;

public class IdentityMapV3Input {
    /**
     * @param emails a list of normalized or unnormalized email addresses
     * @return a IdentityMapV3Input instance, to be used in {@link IdentityMapV3Helper#createEnvelopeForIdentityMapRequest}
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

    // Transient as this should not be part of the serialized JSON payload we send to UID2 Operator
    private transient final Map<String, List<String>> hashedDiiToRawDii = new HashMap<>();

    @SerializedName("email_hash")
    private final List<String> hashedEmails = new ArrayList<>();

    @SerializedName("phone_hash")
    private final List<String> hashedPhones = new ArrayList<>();

    // We never send unhashed emails or phone numbers in the SDK, but they are required fields in the API request
    @SerializedName("email")
    private List<String> emails = Collections.unmodifiableList(new ArrayList<>());
    @SerializedName("phone")
    private List<String> phones = Collections.unmodifiableList(new ArrayList<>());

    public IdentityMapV3Input() {}

    /**
     * @param hashedEmails a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withHashedEmails(List<String> hashedEmails) {
        for (String hashedEmail : hashedEmails) {
            withHashedEmail(hashedEmail);
        }
        return this;
    }

    /**
     * @param hashedEmail a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withHashedEmail(String hashedEmail) {
        this.hashedEmails.add(hashedEmail);
        addToDiiMappings(hashedEmail, hashedEmail);
        return this;
    }

    /**
     * @param hashedPhones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withHashedPhones(List<String> hashedPhones) {
        for (String hashedPhone : hashedPhones) {
            withHashedPhone(hashedPhone);
        }
        return this;
    }

    /**
     * @param hashedPhone a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withHashedPhone(String hashedPhone) {
        this.hashedPhones.add(hashedPhone);
        addToDiiMappings(hashedPhone, hashedPhone);
        return this;
    }

    /**
     * @param emails a list of normalized or unnormalized email addresses
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withEmails(List<String> emails) {
        for (String email : emails) {
            withEmail(email);
        }
        return this;
    }

    /**
     * @param email a normalized or unnormalized email address
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withEmail(String email) {
        String hashedEmail = InputUtil.normalizeAndHashEmail(email);
        this.hashedEmails.add(hashedEmail);
        addToDiiMappings(hashedEmail, email);
        return this;
    }

    /**
     * @param phones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withPhones(List<String> phones) {
        for (String phone : phones) {
            withPhone(phone);
        }
        return this;
    }

    /**
     * @param phone a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return this IdentityMapV3Input instance
     */
    public IdentityMapV3Input withPhone(String phone) {
        if (!InputUtil.isPhoneNumberNormalized(phone)) {
            throw new IllegalArgumentException("phone number is not normalized: " + phone);
        }

        String hashedPhone = InputUtil.getBase64EncodedHash(phone);
        this.hashedPhones.add(hashedPhone);
        addToDiiMappings(hashedPhone, phone);
        return this;
    }

    List<String> getInputDiis(String identityType, int i) {
        return hashedDiiToRawDii.get(getHashedDii(identityType, i));
    }

    private void addToDiiMappings(String hashedDii, String rawDii) {
        hashedDiiToRawDii.computeIfAbsent(hashedDii, k -> new ArrayList<>()).add(rawDii);
    }

    private String getHashedDii(String identityType, int i) {
        switch (identityType) {
            case "email_hash": return hashedEmails.get(i);
            case "phone_hash": return hashedPhones.get(i);
        }
        throw new Uid2Exception("Unexpected identity type: " + identityType);
    }
}
