package com.uid2.client;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class AdvertiserAndDataProviderTests {
    final AdvertiserAndDataProviderUid2Client advertiserAndDataProviderUid2Client = new AdvertiserAndDataProviderUid2Client(System.getenv("UID2_BASE_URL"), System.getenv("UID2_API_KEY"), System.getenv("UID2_SECRET_KEY"));

    @Test //this test requires these env vars to be configured: UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY
    public void identityMap() {
        IdentityMapResponse identityMapResponse = advertiserAndDataProviderUid2Client.generateIdentityMap(IdentityMapInput.fromEmails(Arrays.asList("hopefully-not-opted-out@example.com", "somethingelse@example.com", "optout@example.com")));

        List<IdentityMapResponse.MappedIdentity> mappedIdentityList = identityMapResponse.getMappedIdentities();
        List<IdentityMapResponse.UnmappedIdentity> unnmappedIdentityList = identityMapResponse.getUnmappedIdentities();

        assertFalse(getRawUid(mappedIdentityList, "hopefully-not-opted-out@example.com").isEmpty());
        assertFalse(getRawUid(mappedIdentityList, "somethingelse@example.com").isEmpty());
        assertNull(getRawUid(mappedIdentityList, "optout@example.com"));

        assertEquals("optout", getReason(unnmappedIdentityList, "optout@example.com"));
        assertNull(getReason(unnmappedIdentityList, "hopefully-not-opted-out@example.com"));
        assertNull(getReason(unnmappedIdentityList, "somethingelse@example.com"));

        //todo - test phones, hashed emails, hashed phones, bucket ids
    }

    String getRawUid(List<IdentityMapResponse.MappedIdentity> mappedIdentityList, String unhashedEmail) {
        for (IdentityMapResponse.MappedIdentity identity : mappedIdentityList) {
            if (IdentityMapInput.normalizeAndHashEmail(unhashedEmail).equals(identity.getIdentifier())) {
                return identity.getRawUid();
            }
        }
        return null;
    }

    String getReason(List<IdentityMapResponse.UnmappedIdentity> unmappedIdentityList, String unhashedEmail) {
        for (IdentityMapResponse.UnmappedIdentity identity : unmappedIdentityList) {
            if (IdentityMapInput.normalizeAndHashEmail(unhashedEmail).equals(identity.getIdentifier())) {
                return identity.getReason();
            }
        }
        return null;
    }
}
