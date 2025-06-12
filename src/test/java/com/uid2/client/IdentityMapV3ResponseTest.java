package com.uid2.client;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IdentityMapV3ResponseTest {
    private final static String SOME_EMAIL = "email1@example.com";

    @Test
    void mappedIdentity() {
        String email1 = "email1@example.com";
        String email2 = "email2@example.com";
        String phone1 = "+1234567890";
        String phone2 = "+0987654321";
        String hashedEmail1 = "email 1 hash";
        String hashedEmail2 = "email 2 hash";
        String hashedPhone1 = "phone 1 hash";
        String hashedPhone2 = "phone 2 hash";

        Instant email1RefreshFrom = Instant.parse("2025-01-01T00:00:01Z");
        Instant email2RefreshFrom = Instant.parse("2025-06-30T00:00:20Z");
        Instant phone1RefreshFrom = Instant.parse("2025-01-01T00:05:00Z");
        Instant phone2RefreshFrom = Instant.parse("2025-06-30T00:00:22Z");
        Instant hashedEmail1RefreshFrom = Instant.parse("2025-01-01T00:00:33Z");
        Instant hashedEmail2RefreshFrom = Instant.parse("2025-06-30T00:00:00Z");
        Instant hashedPhone1RefreshFrom = Instant.parse("2025-01-01T00:00:11Z");
        Instant hashedPhone2RefreshFrom = Instant.parse("2025-06-30T00:00:01Z");

        // Response from Operator
        String[] emailHashEntries = {
                mappedResponsePayloadEntry("email 1 current uid", "email 1 previous uid", email1RefreshFrom),
                mappedResponsePayloadEntry("email 2 current uid", "email 2 previous uid", email2RefreshFrom),
                mappedResponsePayloadEntry("hashed email 1 current uid", "hashed email 1 previous uid", hashedEmail1RefreshFrom),
                mappedResponsePayloadEntry("hashed email 2 current uid", "hashed email 2 previous uid", hashedEmail2RefreshFrom)
        };

        String[] phoneHashEntries = {
                mappedResponsePayloadEntry("phone 1 current uid", "phone 1 previous uid", phone1RefreshFrom),
                mappedResponsePayloadEntry("phone 2 current uid", "phone 2 previous uid", phone2RefreshFrom),
                mappedResponsePayloadEntry("hashed phone 1 current uid", "hashed phone 1 previous uid", hashedPhone1RefreshFrom),
                mappedResponsePayloadEntry("hashed phone 2 current uid", "hashed phone 2 previous uid", hashedPhone2RefreshFrom)
        };

        String responsePayload = mappedResponsePayload(emailHashEntries, phoneHashEntries);

        IdentityMapV3Input input = new IdentityMapV3Input()
                .withEmails(Arrays.asList(email1, email2))
                .withHashedEmails(Arrays.asList(hashedEmail1, hashedEmail2))
                .withPhones(Arrays.asList(phone1, phone2))
                .withHashedPhones(Arrays.asList(hashedPhone1, hashedPhone2));

        IdentityMapV3Response response = new IdentityMapV3Response(responsePayload, input);

        assertTrue(response.isSuccess());
        assertEquals(8, response.getMappedIdentities().size());
        assertEquals(0, response.getUnmappedIdentities().size());

        // Email
        IdentityMapV3Response.MappedIdentity rawEmailMapping1 = response.getMappedIdentities().get(email1);
        assertEquals("email 1 current uid", rawEmailMapping1.getCurrentRawUid());
        assertEquals("email 1 previous uid", rawEmailMapping1.getPreviousRawUid());
        assertEquals(email1RefreshFrom, rawEmailMapping1.getRefreshFrom());

        IdentityMapV3Response.MappedIdentity rawEmailMapping2 = response.getMappedIdentities().get(email2);
        assertEquals("email 2 current uid", rawEmailMapping2.getCurrentRawUid());
        assertEquals("email 2 previous uid", rawEmailMapping2.getPreviousRawUid());
        assertEquals(email2RefreshFrom, rawEmailMapping2.getRefreshFrom());

        // Phone
        IdentityMapV3Response.MappedIdentity rawPhoneMapping1 = response.getMappedIdentities().get(phone1);
        assertEquals("phone 1 current uid", rawPhoneMapping1.getCurrentRawUid());
        assertEquals("phone 1 previous uid", rawPhoneMapping1.getPreviousRawUid());
        assertEquals(phone1RefreshFrom, rawPhoneMapping1.getRefreshFrom());

        IdentityMapV3Response.MappedIdentity rawPhoneMapping2 = response.getMappedIdentities().get(phone2);
        assertEquals("phone 2 current uid", rawPhoneMapping2.getCurrentRawUid());
        assertEquals("phone 2 previous uid", rawPhoneMapping2.getPreviousRawUid());
        assertEquals(phone2RefreshFrom, rawPhoneMapping2.getRefreshFrom());

        // Hashed Email
        IdentityMapV3Response.MappedIdentity hashedEmailMapping1 = response.getMappedIdentities().get(hashedEmail1);
        assertEquals("hashed email 1 current uid", hashedEmailMapping1.getCurrentRawUid());
        assertEquals("hashed email 1 previous uid", hashedEmailMapping1.getPreviousRawUid());
        assertEquals(hashedEmail1RefreshFrom, hashedEmailMapping1.getRefreshFrom());

        IdentityMapV3Response.MappedIdentity hashedEmailMapping2 = response.getMappedIdentities().get(hashedEmail2);
        assertEquals("hashed email 2 current uid", hashedEmailMapping2.getCurrentRawUid());
        assertEquals("hashed email 2 previous uid", hashedEmailMapping2.getPreviousRawUid());
        assertEquals(hashedEmail2RefreshFrom, hashedEmailMapping2.getRefreshFrom());

        // Hashed Phone
        IdentityMapV3Response.MappedIdentity hashedPhoneMapping1 = response.getMappedIdentities().get(hashedPhone1);
        assertEquals("hashed phone 1 current uid", hashedPhoneMapping1.getCurrentRawUid());
        assertEquals("hashed phone 1 previous uid", hashedPhoneMapping1.getPreviousRawUid());
        assertEquals(hashedPhone1RefreshFrom, hashedPhoneMapping1.getRefreshFrom());

        IdentityMapV3Response.MappedIdentity hashedPhoneMapping2 = response.getMappedIdentities().get(hashedPhone2);
        assertEquals("hashed phone 2 current uid", hashedPhoneMapping2.getCurrentRawUid());
        assertEquals("hashed phone 2 previous uid", hashedPhoneMapping2.getPreviousRawUid());
        assertEquals(hashedPhone2RefreshFrom, hashedPhoneMapping2.getRefreshFrom());
    }

    @Test
    void unmappedIdentityReasonUnknown() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));

        IdentityMapV3Response response = new IdentityMapV3Response(unmappedResponsePayload("some new unmapped reason"), input);
        assertTrue(response.isSuccess());

        IdentityMapV3Response.UnmappedIdentity unmappedIdentity = response.getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.UNKNOWN, unmappedIdentity.getReason());
        assertEquals("some new unmapped reason", unmappedIdentity.getRawReason());
    }

    @Test
    void unmappedIdentityReasonOptout() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));

        IdentityMapV3Response response = new IdentityMapV3Response(unmappedResponsePayload("optout"), input);
        assertTrue(response.isSuccess());

        IdentityMapV3Response.UnmappedIdentity unmappedIdentity = response.getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.OPTOUT, unmappedIdentity.getReason());
        assertEquals("optout", unmappedIdentity.getRawReason());
    }

    @Test
    void unmappedIdentityReasonInvalid() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));

        IdentityMapV3Response response = new IdentityMapV3Response(unmappedResponsePayload("invalid identifier"), input);
        assertTrue(response.isSuccess());

        IdentityMapV3Response.UnmappedIdentity unmappedIdentity = response.getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.INVALID_IDENTIFIER, unmappedIdentity.getReason());
        assertEquals("invalid identifier", unmappedIdentity.getRawReason());
    }

    @Test
    void responseStatusNotSuccess() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));
        
        String failureResponsePayload = "{\"status\":\"error\",\"body\":{}}";
        
        Uid2Exception exception = assertThrows(Uid2Exception.class, () -> {
            new IdentityMapV3Response(failureResponsePayload, input);
        });
        
        assertEquals("Got unexpected identity map status: error", exception.getMessage());
    }

    private static String unmappedResponsePayload(String reason) {
        return "{\"status\":\"success\",\"body\":{\"email_hash\":[{\"e\":\"" + reason + "\"}]}}";
    }

    private static String mappedResponsePayload(String[] emailHashEntries, String[] phoneHashEntries) {
        return "{\"status\":\"success\",\"body\":{" +
                "\"email_hash\":[" + String.join(",", emailHashEntries) + "]," +
                "\"phone_hash\":[" + String.join(",", phoneHashEntries) + "]" +
                "}}";
    }

    private static String mappedResponsePayloadEntry(String currentUid, String previousUid, Instant refreshFrom) {
        return "{\"u\":\"" + currentUid + "\",\"p\":\"" + previousUid + "\",\"r\":" + refreshFrom.getEpochSecond() + "}";
    }
}