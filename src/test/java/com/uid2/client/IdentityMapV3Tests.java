package com.uid2.client;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IdentityMapV3Tests {
    private final static String SOME_EMAIL = "test@example.com";

    @Test
    void identityMapV3UnmappedIdentityReasonUnknown() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));
        
        IdentityMapV3Response response = new IdentityMapV3Response(payloadJson("SOME_NEW_UNMAPPED_REASON"), input);
        
        IdentityMapV3Response.UnmappedIdentity unmappedIdentity = response.getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.UNKNOWN, unmappedIdentity.getReason());
        assertEquals("SOME_NEW_UNMAPPED_REASON", unmappedIdentity.getRawReason());
    }

    @Test
    void identityMapV3UnmappedIdentityReasonOptout() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));
        
        IdentityMapV3Response response = new IdentityMapV3Response(payloadJson("OPTOUT"), input);
        
        IdentityMapV3Response.UnmappedIdentity unmappedIdentity = response.getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.OPTOUT, unmappedIdentity.getReason());
        assertEquals("OPTOUT", unmappedIdentity.getRawReason());
    }

    @Test
    void identityMapV3UnmappedIdentityReasonInvalid() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));
        
        IdentityMapV3Response response = new IdentityMapV3Response(payloadJson("INVALID"), input);
        
        IdentityMapV3Response.UnmappedIdentity unmappedIdentity = response.getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.INVALID, unmappedIdentity.getReason());
        assertEquals("INVALID", unmappedIdentity.getRawReason());
    }

    @Test
    void identityMapV3UnmappedIdentityReasonCaseInsensitive() {
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(SOME_EMAIL));

        IdentityMapV3Response.UnmappedIdentity lowercaseOptout =
                new IdentityMapV3Response(payloadJson("optout"), input).getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.OPTOUT, lowercaseOptout.getReason());
        assertEquals("optout", lowercaseOptout.getRawReason());
        
        IdentityMapV3Response.UnmappedIdentity lowercaseInvalid = 
                new IdentityMapV3Response(payloadJson("invalid"), input).getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.INVALID, lowercaseInvalid.getReason());
        assertEquals("invalid", lowercaseInvalid.getRawReason());

        IdentityMapV3Response.UnmappedIdentity mixedOptout =
                new IdentityMapV3Response(payloadJson("OptOut"), input).getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.OPTOUT, mixedOptout.getReason());
        assertEquals("OptOut", mixedOptout.getRawReason());
        
        IdentityMapV3Response.UnmappedIdentity mixedInvalid = 
                new IdentityMapV3Response(payloadJson("InVaLiD"), input).getUnmappedIdentities().get(SOME_EMAIL);
        assertEquals(UnmappedIdentityReason.INVALID, mixedInvalid.getReason());
        assertEquals("InVaLiD", mixedInvalid.getRawReason());
    }

    @NotNull
    private static String payloadJson(String reason) {
        return "{\"status\":\"success\",\"body\":{\"email_hash\":[{\"e\":\"" + reason + "\"}]}}";
    }
}