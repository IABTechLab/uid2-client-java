package com.uid2.client;

import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


//most tests in this class require these env vars to be configured: UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY
@EnabledIfEnvironmentVariable(named = "UID2_BASE_URL", matches = "\\S+")
public class IdentityMapV3IntegrationTests {
    final private IdentityMapV3Client identityMapClient = new IdentityMapV3Client(System.getenv("UID2_BASE_URL"), System.getenv("UID2_API_KEY"), System.getenv("UID2_SECRET_KEY"));

    String mappedPhone = "+98765432109";
    String mappedPhone2 = "+12345678901";
    String optedOutPhone = "+00000000000";
    String mappedPhoneHash = InputUtil.getBase64EncodedHash(mappedPhone2);
    String optedOutPhoneHash = InputUtil.getBase64EncodedHash(optedOutPhone);

    String mappedEmail = "hopefully-not-opted-out@example.com";
    String optedOutEmail = "optout@example.com";
    String optedOutEmail2 = "somethingelse@example.com";
    String mappedEmailHash = InputUtil.normalizeAndHashEmail("mapped-email@example.com");
    String optedOutEmailHash = InputUtil.normalizeAndHashEmail(optedOutEmail);

    @Test
    public void identityMapEmails() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, optedOutEmail2, optedOutEmail));
        Response response = new Response(identityMapInput);

        response.assertMapped(mappedEmail);
        response.assertMapped(optedOutEmail2);

        response.assertUnmapped("OPTOUT", optedOutEmail);
    }

    @Test
    public void identityMapNothingUnmapped() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, optedOutEmail2));
        Response response = new Response(identityMapInput);

        response.assertMapped(mappedEmail);
        response.assertMapped(optedOutEmail2);
    }

    @Test
    public void identityMapNothingMapped() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Collections.singletonList(optedOutEmail));
        Response response = new Response(identityMapInput);

        response.assertUnmapped("OPTOUT", optedOutEmail);
    }


    @Test
    public void identityMapInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, "this is not an email")));
    }

    @Test
    public void identityMapInvalidPhone() {
        assertThrows(IllegalArgumentException.class,
                () -> IdentityMapV3Input.fromPhones(Arrays.asList(mappedPhone, "this is not a phone number")));
    }

    @Test
    public void identityMapInvalidHashedEmail() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedEmails(Collections.singletonList("this is not a hashed email"));

        Response response = new Response(identityMapInput);

        response.assertUnmapped("INVALID", "this is not a hashed email");
    }

    @Test
    public void identityMapInvalidHashedPhone() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedPhones(Collections.singletonList("this is not a hashed phone"));

        Response response = new Response(identityMapInput);
        response.assertUnmapped("INVALID", "this is not a hashed phone");
    }

    @Test
    public void identityMapHashedEmails() {
        String hashedEmail1 = InputUtil.normalizeAndHashEmail(mappedEmail);
        String hashedEmail2 = mappedEmailHash;
        String hashedOptedOutEmail = optedOutEmailHash;

        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedEmails(Arrays.asList(hashedEmail1, hashedEmail2, hashedOptedOutEmail));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedEmail1);
        response.assertMapped(hashedEmail2);

        response.assertUnmapped("OPTOUT", hashedOptedOutEmail);
    }

    @Test
    public void identityMapDuplicateEmails() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList("JANE.SAOIRSE@gmail.com", "Jane.Saoirse@gmail.com", "JaneSaoirse+UID2@gmail.com", "janesaoirse@gmail.com", "JANE.SAOIRSE@gmail.com"));
        IdentityMapV3Response identityMapResponse = identityMapClient.generateIdentityMap(identityMapInput);

        HashMap<String, IdentityMapV3Response.MappedIdentity> mappedIdentities = identityMapResponse.getMappedIdentities();
        assertEquals(4, mappedIdentities.size()); //it's not 5 because the last email is an exact match to the first email

        String rawUid = mappedIdentities.get("JANE.SAOIRSE@gmail.com").getCurrentRawUid();
        assertEquals(rawUid, mappedIdentities.get("Jane.Saoirse@gmail.com").getCurrentRawUid());
        assertEquals(rawUid, mappedIdentities.get("JaneSaoirse+UID2@gmail.com").getCurrentRawUid());
        assertEquals(rawUid, mappedIdentities.get("janesaoirse@gmail.com").getCurrentRawUid());
    }


        @Test
    public void identityMapDuplicateHashedEmails() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedEmails(Arrays.asList(mappedEmailHash, mappedEmailHash, optedOutEmailHash, optedOutEmailHash));
        Response response = new Response(identityMapInput);

        response.assertMapped(mappedEmailHash);

        response.assertUnmapped("OPTOUT", optedOutEmailHash);
    }

    @Test
    public void identityMapEmptyInput() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Collections.emptyList());
        IdentityMapV3Response identityMapResponse = identityMapClient.generateIdentityMap(identityMapInput);
        assertTrue(identityMapResponse.getMappedIdentities().isEmpty());
        assertTrue(identityMapResponse.getUnmappedIdentities().isEmpty());
    }


    @Test
    public void identityMapPhones() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromPhones(Arrays.asList(mappedPhone, mappedPhone2, optedOutPhone));
        Response response = new Response(identityMapInput);

        response.assertMapped(mappedPhone);
        response.assertMapped(mappedPhone2);

        response.assertUnmapped("OPTOUT", optedOutPhone);
    }

    @Test
    public void identityMapHashedPhones() {
        String hashedPhone1 = mappedPhoneHash;
        String hashedPhone2 = InputUtil.getBase64EncodedHash(mappedPhone);
        String hashedOptedOutPhone = optedOutPhoneHash;

        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedPhones(Arrays.asList(hashedPhone1, hashedPhone2, hashedOptedOutPhone));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedPhone1);
        response.assertMapped(hashedPhone2);

        response.assertUnmapped("OPTOUT", hashedOptedOutPhone);
    }

    @Test
    public void identityMapAllIdentityTypesInOneRequest() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input
                .fromEmails(Arrays.asList(mappedEmail, optedOutEmail))
                .withHashedEmails(Arrays.asList(mappedEmailHash, optedOutEmailHash))
                .withPhones(Arrays.asList(mappedPhone, optedOutPhone))
                .withHashedPhones(Arrays.asList(mappedPhoneHash, optedOutPhoneHash));

        Response response = new Response(identityMapInput);

        response.assertMapped(mappedEmail);
        response.assertMapped(mappedEmailHash);
        response.assertMapped(mappedPhone);
        response.assertMapped(mappedPhoneHash);

        response.assertUnmapped("OPTOUT", optedOutEmail);
        response.assertUnmapped("OPTOUT", optedOutEmailHash);
        response.assertUnmapped("OPTOUT", optedOutPhone);
        response.assertUnmapped("OPTOUT", optedOutPhoneHash);
    }

    @Test
    public void identityMapAllIdentityTypesInOneRequestAddedOneByOne() {
        IdentityMapV3Input identityMapInput = new IdentityMapV3Input();

        identityMapInput.withEmail(mappedEmail);
        identityMapInput.withPhone(optedOutPhone);
        identityMapInput.withHashedPhone(mappedPhoneHash);
        identityMapInput.withHashedEmail(optedOutEmailHash);

        Response response = new Response(identityMapInput);

        response.assertMapped(mappedEmail);
        response.assertMapped(mappedPhoneHash);

        response.assertUnmapped("OPTOUT", optedOutPhone);
        response.assertUnmapped("OPTOUT", optedOutEmailHash);
    }


    class Response {
        Response(IdentityMapV3Input identityMapInput) {
            identityMapResponse = identityMapClient.generateIdentityMap(identityMapInput);
        }

        void assertMapped(String dii) {
            IdentityMapV3Response.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(dii);
            assertNotNull(mappedIdentity);
            assertFalse(mappedIdentity.getCurrentRawUid().isEmpty());

            // Refresh from should be now or in the future, allow some slack for time between request and this assertion
            Instant aMinuteAgo = Instant.now().minusSeconds(60);
            assertTrue(mappedIdentity.getRefreshFrom().isAfter(aMinuteAgo));

            IdentityMapV3Response.UnmappedIdentity unmappedIdentity = identityMapResponse.getUnmappedIdentities().get(dii);
            assertNull(unmappedIdentity);
        }

        void assertUnmapped(String reason, String dii) {
            HashMap<String, IdentityMapV3Response.UnmappedIdentity> unmappedIdentities = identityMapResponse.getUnmappedIdentities();
            IdentityMapV3Response.UnmappedIdentity dii2 = unmappedIdentities.get(dii);
            assertEquals(reason, dii2.getReason());

            IdentityMapV3Response.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(dii);
            assertNull(mappedIdentity);
        }


        private final IdentityMapV3Response identityMapResponse;
    }

    @Test
    public void identityMapEmailsUseOwnHttp() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, optedOutEmail2, optedOutEmail));

        final IdentityMapV3Helper identityMapHelper = new IdentityMapV3Helper(System.getenv("UID2_SECRET_KEY"));
        EnvelopeV2 envelopeV2 = identityMapHelper.createEnvelopeForIdentityMapRequest(identityMapInput);
        String uid2BaseUrl = System.getenv("UID2_BASE_URL");
        String clientApiKey = System.getenv("UID2_API_KEY");
        Headers headers = new Headers.Builder().add("Authorization", "Bearer " + clientApiKey).add("X-UID2-Client-Version: java-" + Uid2Helper.getArtifactAndVersion()).build();

        Request request = new Request.Builder().url(uid2BaseUrl + "/v3/identity/map").headers(headers)
                .post(RequestBody.create(envelopeV2.getEnvelope(), MediaType.get("application/x-www-form-urlencoded"))).build();

        OkHttpClient client = new OkHttpClient();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Uid2Exception("Unexpected code " + response);
            }

            String responseString = response.body() != null ? response.body().string() : response.toString();
            IdentityMapV3Response identityMapResponse = identityMapHelper.createIdentityMapResponse(responseString, envelopeV2, identityMapInput);

            IdentityMapV3Response.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(mappedEmail);
            assertFalse(mappedIdentity.getCurrentRawUid().isEmpty());
        } catch (IOException e) {
            throw new Uid2Exception("error communicating with api endpoint", e);
        }
    }

    @Test
    public void identityMapBadUrl() {
         IdentityMapV3Client identityMapClient = new IdentityMapV3Client("https://operator-bad-url.uidapi.com", System.getenv("UID2_API_KEY"), System.getenv("UID2_SECRET_KEY"));
         IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Collections.singletonList("email@example.com"));
         assertThrows(Uid2Exception.class, () -> identityMapClient.generateIdentityMap(identityMapInput));
    }

    @Test
    public void identityMapBadApiKey() {
        IdentityMapV3Client identityMapClient = new IdentityMapV3Client(System.getenv("UID2_BASE_URL"), "bad-api-key", System.getenv("UID2_SECRET_KEY"));
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Collections.singletonList("email@example.com"));
        assertThrows(Uid2Exception.class, () -> identityMapClient.generateIdentityMap(identityMapInput));
    }

    @Test
    public void identityMapBadSecret() {
        IdentityMapV3Client identityMapClient = new IdentityMapV3Client(System.getenv("UID2_BASE_URL"), System.getenv("UID2_API_KEY"), "wJ0hP19QU4hmpB64Y3fV2dAed8t/mupw3sjN5jNRFzg=");
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Collections.singletonList("email@example.com"));
        assertThrows(Uid2Exception.class, () -> identityMapClient.generateIdentityMap(identityMapInput));
    }
}
