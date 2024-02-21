package com.uid2.client;

import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;


//most tests in this class require these env vars to be configured: UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY
@EnabledIfEnvironmentVariable(named = "UID2_BASE_URL", matches = "\\S+")
public class IdentityMapIntegrationTests {
    final private IdentityMapClient identityMapClient = new IdentityMapClient(System.getenv("UID2_BASE_URL"), System.getenv("UID2_API_KEY"), System.getenv("UID2_SECRET_KEY"));

    @Test
    public void identityMapEmails() {
        IdentityMapInput identityMapInput = IdentityMapInput.fromEmails(Arrays.asList("hopefully-not-opted-out@example.com", "somethingelse@example.com", "optout@example.com"));
        Response response = new Response(identityMapInput);

        response.assertMapped("hopefully-not-opted-out@example.com");
        response.assertMapped("somethingelse@example.com");

        response.assertUnmapped("optout", "optout@example.com");
    }

    @Test
    public void identityMapInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> IdentityMapInput.fromEmails(Arrays.asList("email@example.com", "this is not an email")));
    }

    @Test
    public void identityMapInvalidPhone() {
        assertThrows(IllegalArgumentException.class,
                () -> IdentityMapInput.fromPhones(Arrays.asList("+12345678901", "this is not a phone number")));
    }

    @Test
    public void identityMapInvalidHashedEmail() {
        IdentityMapInput identityMapInput = IdentityMapInput.fromHashedEmails(Collections.singletonList("this is not a hashed email"));

        Response response = new Response(identityMapInput);

        response.assertUnmapped("invalid identifier", "this is not a hashed email");
    }

    @Test
    public void identityMapInvalidHashedPhone() {
        IdentityMapInput identityMapInput = IdentityMapInput.fromHashedPhones(Collections.singletonList("this is not a hashed phone"));

        Response response = new Response(identityMapInput);
        response.assertUnmapped("invalid identifier", "this is not a hashed phone");
    }


    @Test
    public void identityMapHashedEmails() {
        String hashedEmail1 = InputUtil.normalizeAndHashEmail("hopefully-not-opted-out@example.com");
        String hashedEmail2 = InputUtil.normalizeAndHashEmail("somethingelse@example.com");
        String hashedOptedOutEmail = InputUtil.normalizeAndHashEmail("optout@example.com");

        IdentityMapInput identityMapInput = IdentityMapInput.fromHashedEmails(Arrays.asList(hashedEmail1, hashedEmail2, hashedOptedOutEmail));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedEmail1);
        response.assertMapped(hashedEmail2);

        response.assertUnmapped("optout", hashedOptedOutEmail);
    }

    @Test
    public void identityMapPhones() {
        IdentityMapInput identityMapInput = IdentityMapInput.fromPhones(Arrays.asList("+12345678901", "+98765432109", "+00000000000"));
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

        IdentityMapInput identityMapInput = IdentityMapInput.fromHashedPhones(Arrays.asList(hashedPhone1, hashedPhone2, hashedOptedOutPhone));
        Response response = new Response(identityMapInput);

        response.assertMapped(hashedPhone1);
        response.assertMapped(hashedPhone2);

        response.assertUnmapped("optout", hashedOptedOutPhone);
    }


    class Response {
        Response(IdentityMapInput identityMapInput) {
            identityMapResponse = identityMapClient.generateIdentityMap(identityMapInput);
        }

        void assertMapped(String dii) {
            IdentityMapResponse.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(dii);
            assertNotNull(mappedIdentity);
            assertFalse(mappedIdentity.getRawUid().isEmpty());
            assertFalse(mappedIdentity.getBucketId().isEmpty());

            IdentityMapResponse.UnmappedIdentity unmappedIdentity = identityMapResponse.getUnmappedIdentities().get(dii);
            assertNull(unmappedIdentity);
        }

        void assertUnmapped(String reason, String dii) {
            assertEquals(reason, identityMapResponse.getUnmappedIdentities().get(dii).getReason());

            IdentityMapResponse.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get(dii);
            assertNull(mappedIdentity);
        }


        private final IdentityMapResponse identityMapResponse;
    }

    @Test
    public void identityMapEmailsUseOwnHttp() {
        IdentityMapInput identityMapInput = IdentityMapInput.fromEmails(Arrays.asList("hopefully-not-opted-out@example.com", "somethingelse@example.com", "optout@example.com"));

        final IdentityMapHelper identityMapHelper = new IdentityMapHelper(System.getenv("UID2_SECRET_KEY"));
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
            IdentityMapResponse identityMapResponse = identityMapHelper.createIdentityMapResponse(responseString, envelopeV2, identityMapInput);

            IdentityMapResponse.MappedIdentity mappedIdentity = identityMapResponse.getMappedIdentities().get("hopefully-not-opted-out@example.com");
            assertFalse(mappedIdentity.getRawUid().isEmpty());
        } catch (IOException e) {
            throw new Uid2Exception("error communicating with api endpoint", e);
        }
    }

    @Test
    public void identityMapBadUrl() {
         IdentityMapClient identityMapClient = new IdentityMapClient("https://operator-bad-url.uidapi.com", System.getenv("UID2_API_KEY"), System.getenv("UID2_SECRET_KEY"));
         IdentityMapInput identityMapInput = IdentityMapInput.fromEmails(Collections.singletonList("email@example.com"));
         assertThrows(Uid2Exception.class, () -> identityMapClient.generateIdentityMap(identityMapInput));
    }

    @Test
    public void identityMapBadApiKey() {
        IdentityMapClient identityMapClient = new IdentityMapClient(System.getenv("UID2_BASE_URL"), "bad-api-key", System.getenv("UID2_SECRET_KEY"));
        IdentityMapInput identityMapInput = IdentityMapInput.fromEmails(Collections.singletonList("email@example.com"));
        assertThrows(Uid2Exception.class, () -> identityMapClient.generateIdentityMap(identityMapInput));
    }

    @Test
    public void identityMapBadSecret() {
        IdentityMapClient identityMapClient = new IdentityMapClient(System.getenv("UID2_BASE_URL"), System.getenv("UID2_API_KEY"), "wJ0hP19QU4hmpB64Y3fV2dAed8t/mupw3sjN5jNRFzg=");
        IdentityMapInput identityMapInput = IdentityMapInput.fromEmails(Collections.singletonList("email@example.com"));
        assertThrows(Uid2Exception.class, () -> identityMapClient.generateIdentityMap(identityMapInput));
    }
}
