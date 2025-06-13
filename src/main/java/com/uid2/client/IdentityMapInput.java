package com.uid2.client;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class IdentityMapInput {
    /**
     * @param emails a list of normalized or unnormalized email addresses
     * @return a IdentityMapInput instance, to be used in {@link IdentityMapHelper#createEnvelopeForIdentityMapRequest}
     */
    public static IdentityMapInput fromEmails(Iterable<String> emails) {
        return new IdentityMapInput(IdentityType.Email, emails, false);
    }

    /**
     * @param phones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> phone number
     * @return an IdentityMapInput instance
     */
    public static IdentityMapInput fromPhones(Iterable<String> phones) {
        return new IdentityMapInput(IdentityType.Phone, phones, false);
    }

    /**
     * @param hashedEmails a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#email-address-hash-encoding">hashed</a> email address
     * @return an IdentityMapInput instance
     */
    public static IdentityMapInput fromHashedEmails(Iterable<String> hashedEmails) {
        return new IdentityMapInput(IdentityType.Email, hashedEmails, true);
    }

    /**
     * @param hashedPhones a <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-normalization">normalized</a> and <a href="https://unifiedid.com/docs/getting-started/gs-normalization-encoding#phone-number-hash-encoding">hashed</a> phone number
     * @return an IdentityMapInput instance
     */
    public static IdentityMapInput fromHashedPhones(Iterable<String> hashedPhones) {
        return new IdentityMapInput(IdentityType.Phone, hashedPhones, true);
    }

    private IdentityMapInput(IdentityType identityType, Iterable<String> emailsOrPhones, boolean alreadyHashed) {
        if (identityType == IdentityType.Email) {
            hashedNormalizedEmails = new ArrayList<>();
            for (String email : emailsOrPhones) {
                if (alreadyHashed) {
                    hashedNormalizedEmails.add(email);
                } else {
                    String hashedEmail = InputUtil.normalizeAndHashEmail(email);
                    hashedNormalizedEmails.add(hashedEmail);
                    addHashedToRawDiiMapping(hashedEmail, email);
                }
            }
        } else {  //phone
            hashedNormalizedPhones = new ArrayList<>();
            for (String phone : emailsOrPhones) {
                if (alreadyHashed) {
                    hashedNormalizedPhones.add(phone);
                } else {
                    if (!InputUtil.isPhoneNumberNormalized(phone)) {
                        throw new IllegalArgumentException("phone number is not normalized: " + phone);
                    }

                    String hashedNormalizedPhone = InputUtil.getBase64EncodedHash(phone);
                    addHashedToRawDiiMapping(hashedNormalizedPhone, phone);
                    hashedNormalizedPhones.add(hashedNormalizedPhone);
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
    private List<String> hashedNormalizedEmails;
    @SerializedName("phone_hash")
    private List<String> hashedNormalizedPhones;

    // We never send unhashed emails or phone numbers in the SDK, but they are required fields in the API request
    @SerializedName("email")
    private List<String> emails = Collections.unmodifiableList(new ArrayList<>());
    @SerializedName("phone")
    private List<String> phones = Collections.unmodifiableList(new ArrayList<>());

    private final transient HashMap<String, List<String>> hashedDiiToRawDiis = new HashMap<>();
}
