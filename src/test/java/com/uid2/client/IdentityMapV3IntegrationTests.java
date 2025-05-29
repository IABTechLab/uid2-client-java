package com.uid2.client;

import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


//most tests in this class require these env vars to be configured: UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY
@EnabledIfEnvironmentVariable(named = "UID2_BASE_URL", matches = "\\S+")
public class IdentityMapV3IntegrationTests {
    final private IdentityMapV3Client identityMapClient = new IdentityMapV3Client(System.getenv("UID2_BASE_URL"), System.getenv("UID2_API_KEY"), System.getenv("UID2_SECRET_KEY"));

    @Test
    public void identityMapEmails() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList("hopefully-not-opted-out@example.com", "somethingelse@example.com", "optout@example.com"));
        Response response = new Response(identityMapInput);

        response.assertMapped("hopefully-not-opted-out@example.com");
        response.assertMapped("somethingelse@example.com");

        response.assertUnmapped("optout", "optout@example.com");
    }

    @Test
    public void identityMapNothingUnmapped() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList("hopefully-not-opted-out@example.com", "somethingelse@example.com"));
        Response response = new Response(identityMapInput);

        response.assertMapped("hopefully-not-opted-out@example.com");
        response.assertMapped("somethingelse@example.com");
    }

    @Test
    public void identityMapNothingMapped() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Collections.singletonList("optout@example.com"));
        Response response = new Response(identityMapInput);

        response.assertUnmapped("optout", "optout@example.com");
    }


    @Test
    public void identityMapInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> IdentityMapV3Input.fromEmails(Arrays.asList("email@example.com", "this is not an email")));
    }

    @Test
    public void identityMapInvalidPhone() {
        assertThrows(IllegalArgumentException.class,
                () -> IdentityMapV3Input.fromPhones(Arrays.asList("+12345678901", "this is not a phone number")));
    }

    @Test
    public void identityMapInvalidHashedEmail() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedEmails(Collections.singletonList("this is not a hashed email"));

        Response response = new Response(identityMapInput);

        response.assertUnmapped("invalid identifier", "this is not a hashed email");
    }

    @Test
    public void identityMapInvalidHashedPhone() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedPhones(Collections.singletonList("this is not a hashed phone"));

        Response response = new Response(identityMapInput);
        response.assertUnmapped("invalid identifier", "this is not a hashed phone");
    }

    @Test
    public void identityMapHashedEmails() {
        String hashedEmail1 = InputUtil.normalizeAndHashEmail("hopefully-not-opted-out@example.com");
        String hashedEmail2 = InputUtil.normalizeAndHashEmail("somethingelse@example.com");
        String hashedOptedOutEmail = InputUtil.normalizeAndHashEmail("optout@example.com");

        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedEmails(Arrays.asList(hashedEmail1, hashedEmail2, hashedOptedOutEmail));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedEmail1);
        response.assertMapped(hashedEmail2);

        response.assertUnmapped("optout", hashedOptedOutEmail);
    }

    @Test
    public void identityMapDuplicateEmails() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList("JANE.SAOIRSE@gmail.com", "Jane.Saoirse@gmail.com", "JaneSaoirse+UID2@gmail.com", "janesaoirse@gmail.com", "JANE.SAOIRSE@gmail.com"));
        IdentityMapV3Response identityMapResponse = identityMapClient.generateIdentityMap(identityMapInput);

        HashMap<String, IdentityMapV3Response.MappedIdentity> mappedIdentities = identityMapResponse.getMappedIdentities();
        assertEquals(4, mappedIdentities.size()); //it's not 5 because the last email is an exact match to the first email

        String rawUid = mappedIdentities.get("JANE.SAOIRSE@gmail.com").getRawUid();
        assertEquals(rawUid, mappedIdentities.get("Jane.Saoirse@gmail.com").getRawUid());
        assertEquals(rawUid, mappedIdentities.get("JaneSaoirse+UID2@gmail.com").getRawUid());
        assertEquals(rawUid, mappedIdentities.get("janesaoirse@gmail.com").getRawUid());
    }


        @Test
    public void identityMapDuplicateHashedEmails() {
        String hashedEmail = InputUtil.normalizeAndHashEmail("hopefully-not-opted-out@example.com");
        String duplicateHashedEmail = hashedEmail;

        String hashedOptedOutEmail = InputUtil.normalizeAndHashEmail("optout@example.com");
        String duplicateOptedOutEmail = hashedOptedOutEmail;

        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedEmails(Arrays.asList(hashedEmail, duplicateHashedEmail, hashedOptedOutEmail, duplicateOptedOutEmail));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedEmail);
        response.assertMapped(duplicateHashedEmail);

        response.assertUnmapped("optout", hashedOptedOutEmail);
        response.assertUnmapped("optout", duplicateOptedOutEmail);
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
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromPhones(Arrays.asList("+12345678901", "+98765432109", "+00000000000"));
        Response response = new Response(identityMapInput);

        response.assertMapped("+12345678901");
        response.assertMapped("+98765432109");

        response.assertUnmapped("optout", "+00000000000");
    }

    @Test
    public void identityMapHashedPhones() {
        String hashedPhone1 = InputUtil.getBase64EncodedHash("+12345678901");
        String hashedPhone2 = InputUtil.getBase64EncodedHash("+98765432109");
        String hashedOptedOutPhone = InputUtil.getBase64EncodedHash("+00000000000");

        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromHashedPhones(Arrays.asList(hashedPhone1, hashedPhone2, hashedOptedOutPhone));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedPhone1);
        response.assertMapped(hashedPhone2);

        response.assertUnmapped("optout", hashedOptedOutPhone);
    }


    class Response {
        Response(IdentityMapV3Input identityMapInput) {
            identityMapResponse = identityMapClient.generateIdentityMap(identityMapInput);
        }

        void assertMapped(String dii) {
            IdentityMapV3Response.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(dii);
            assertNotNull(mappedIdentity);
            assertFalse(mappedIdentity.getRawUid().isEmpty());
            assertFalse(mappedIdentity.getBucketId().isEmpty());

            IdentityMapV3Response.UnmappedIdentity unmappedIdentity = identityMapResponse.getUnmappedIdentities().get(dii);
            assertNull(unmappedIdentity);
        }

        void assertUnmapped(String reason, String dii) {
            assertEquals(reason, identityMapResponse.getUnmappedIdentities().get(dii).getReason());

            IdentityMapV3Response.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(dii);
            assertNull(mappedIdentity);
        }


        private final IdentityMapV3Response identityMapResponse;
    }

    @Test
    public void identityMapEmailsUseOwnHttp() {
        IdentityMapV3Input identityMapInput = IdentityMapV3Input.fromEmails(Arrays.asList("hopefully-not-opted-out@example.com", "somethingelse@example.com", "optout@example.com"));

        final IdentityMapV3Helper identityMapHelper = new IdentityMapV3Helper(System.getenv("UID2_SECRET_KEY"));
        EnvelopeV2 envelopeV2 = identityMapHelper.createEnvelopeForIdentityMapRequest(identityMapInput);
        String uid2BaseUrl = System.getenv("UID2_BASE_URL");
        String clientApiKey = System.getenv("UID2_API_KEY");
        Headers headers = new Headers.Builder().add("Authorization", "Bearer " + clientApiKey).add("X-UID2-Client-Version: java-" + Uid2Helper.getArtifactAndVersion()).build();

        Request request = new Request.Builder().url(uid2BaseUrl + "/v2/identity/map").headers(headers)
                .post(RequestBody.create(envelopeV2.getEnvelope(), MediaType.get("application/x-www-form-urlencoded"))).build();

        OkHttpClient client = new OkHttpClient();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Uid2Exception("Unexpected code " + response);
            }

            String responseString = response.body() != null ? response.body().string() : response.toString();
            IdentityMapV3Response identityMapResponse = identityMapHelper.createIdentityMapResponse(responseString, envelopeV2, identityMapInput);

            IdentityMapV3Response.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get("hopefully-not-opted-out@example.com");
            assertFalse(mappedIdentity.getRawUid().isEmpty());
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
