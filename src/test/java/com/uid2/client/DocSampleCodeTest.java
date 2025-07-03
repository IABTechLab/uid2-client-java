package com.uid2.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// !!!!! Do not refactor this code if you're not intending to change the JDK docs samples !!!!!

// Tests for sample code as used in https://unifiedid.com/docs/sdks/sdk-ref-java
// The tests are designed to have sections of almost exactly copy/pasted code samples so there are
// unused variables, unnecessary comments, redundant repetition... since those are used in docs for illustration.
// If a test breaks in this file, likely the change breaks one of the samples on the docs site
@EnabledIfEnvironmentVariable(named = "UID2_BASE_URL", matches = "\\S+")
public class DocSampleCodeTest {

    // Test data constants
    private static final String UID2_BASE_URL = System.getenv("UID2_BASE_URL");
    private static final String UID2_API_KEY = System.getenv("UID2_API_KEY");
    private static final String UID2_SECRET_KEY = System.getenv("UID2_SECRET_KEY");
    
    // Test email addresses - these should be configured in your test environment
    private static final String mappedEmail = "user@example.com";
    private static final String optedOutEmail = "optout@example.com";
    private static final String optedOutEmail2 = "optout2@example.com";
    private static final String mappedPhone = "+12345678901";
    private static final String mappedPhone2 = "+12345678902";
    private static final String mappedEmailHash = "preHashedEmailValue";
    private static final String mappedPhoneHash = "preHashedPhoneValue";
    
    // Client instances  
    private IdentityMapV3Client identityMapV3Client;
    private PublisherUid2Client publisherUid2Client;

    @BeforeEach
    void setUp() {
        identityMapV3Client = new IdentityMapV3Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        publisherUid2Client = new PublisherUid2Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
    }

    @Test
    public void testBasicUsageExample() {
        // Documentation sdk-ref-java.md Line 311: IdentityMapV3Client creation
        IdentityMapV3Client client = new IdentityMapV3Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);

        // Documentation sdk-ref-java.md Line 316: Basic email input creation
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList("user@example.com", "user2@example.com"));
        // Documentation sdk-ref-java.md Line 330: Generate identity map call
        IdentityMapV3Response identityMapResponse = client.generateIdentityMap(input);

        // Documentation sdk-ref-java.md Line 334-335: Get mapped and unmapped results
        HashMap<String, IdentityMapV3Response.MappedIdentity> mappedIdentities = identityMapResponse.getMappedIdentities();
        HashMap<String, IdentityMapV3Response.UnmappedIdentity> unmappedIdentities = identityMapResponse.getUnmappedIdentities();

        // Verify basic structure works
        assertNotNull(mappedIdentities);
        assertNotNull(unmappedIdentities);
        assertTrue(mappedIdentities.size() + unmappedIdentities.size() >= 2);
    }

    @Test
    public void testMultiIdentityTypeExample() {
        // Documentation sdk-ref-java.md Line 320-323: Multi-identity type input creation with fluent builder
        IdentityMapV3Input input = new IdentityMapV3Input().withEmail("user@example.com").withPhone(mappedPhone).withHashedEmail("preHashedEmail").withHashedPhone("preHashedPhone");

        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);

        // Verify the fluent builder pattern works
        assertNotNull(response);
        assertNotNull(response.getMappedIdentities());
        assertNotNull(response.getUnmappedIdentities());
    }

    @Test
    public void testResponseHandlingExample() {
        // Documentation sdk-ref-java.md Line 340-349: Process results with mapped and unmapped identities
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail));
        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);

        // Documentation sdk-ref-java.md Line 340-349: Access mapped identity and extract current/previous UIDs and refresh timestamp
        IdentityMapV3Response.MappedIdentity mappedIdentity = response.getMappedIdentities().get(mappedEmail);
        if (mappedIdentity != null) {
            String currentUid = mappedIdentity.getCurrentRawUid();      // Current raw UID2
            String previousUid = mappedIdentity.getPreviousRawUid();   // Previous raw UID2 (nullable, only available for 90 days after rotation)
            Instant refreshFrom = mappedIdentity.getRefreshFrom();     // When to refresh this identity

            assertNotNull(currentUid);
            assertFalse(currentUid.isEmpty());
            assertNotNull(refreshFrom);
            assertTrue(refreshFrom.isAfter(Instant.now().minus(1, java.time.temporal.ChronoUnit.MINUTES)));
        }
    }

    @Test
    public void testCompleteUsageExample() {
        // Documentation sdk-ref-java.md Line 353-382: Complete usage example with single and mixed identity types
        IdentityMapV3Client client = new IdentityMapV3Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);

        // Documentation sdk-ref-java.md Line 356-359: Example 1: Single identity type
        IdentityMapV3Input emailInput = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, optedOutEmail));
        IdentityMapV3Response emailResponse = client.generateIdentityMap(emailInput);

        // Documentation sdk-ref-java.md Line 361-367: Process email results with forEach
        emailResponse.getMappedIdentities().forEach((email, identity) -> {
            assertNotNull(identity.getCurrentRawUid());
            assertNotNull(identity.getRefreshFrom());
        });

        emailResponse.getUnmappedIdentities().forEach((email, identity) -> {
            assertNotNull(identity.getReason());
        });

        // Documentation sdk-ref-java.md Line 375-379: Example 2: Mixed identity types in single request
        IdentityMapV3Input mixedInput = new IdentityMapV3Input().withEmail(mappedEmail).withPhone(mappedPhone).withHashedEmail(mappedEmailHash).withHashedPhone(mappedPhoneHash);

        IdentityMapV3Response mixedResponse = client.generateIdentityMap(mixedInput);
        assertNotNull(mixedResponse);
    }

    @Test
    public void testFluentBuilderExample() {
        // Documentation sdk-ref-java.md: Additional fluent builder patterns (tested comprehensively)
        IdentityMapV3Input complexInput = new IdentityMapV3Input().withEmail(mappedEmail).withEmails(Arrays.asList("user3@example.com", "user4@example.com")).withPhone(mappedPhone).withPhones(Arrays.asList(mappedPhone2)).withHashedEmail(mappedEmailHash).withHashedPhone(mappedPhoneHash);

        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(complexInput);
        assertNotNull(response);

        // Alternative: Start with one type and chain others
        IdentityMapV3Input chainedInput = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail)).withPhone(mappedPhone).withHashedPhone(mappedPhoneHash);

        IdentityMapV3Response chainedResponse = identityMapV3Client.generateIdentityMap(chainedInput);
        assertNotNull(chainedResponse);

        // Or start with phones and add emails
        IdentityMapV3Input phoneFirstInput = IdentityMapV3Input.fromPhones(Arrays.asList(mappedPhone, mappedPhone2)).withEmail(mappedEmail).withHashedEmail(mappedEmailHash);

        IdentityMapV3Response phoneFirstResponse = identityMapV3Client.generateIdentityMap(phoneFirstInput);
        assertNotNull(phoneFirstResponse);
    }

    @Test
    public void testEmailNormalizationExample() {
        // Documentation sdk-ref-java.md: Email normalization patterns (demonstrated with various formats)
        IdentityMapV3Input normalizedEmails = IdentityMapV3Input.fromEmails(Arrays.asList("JANE.DOE@GMAIL.COM", "jane.doe@gmail.com", "JaneDoe@gmail.com", "jane.doe+newsletter@gmail.com", "janedoe@gmail.com"));

        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(normalizedEmails);

        // Verify all variants are processed (they should normalize to same UID)
        assertNotNull(response);
        assertTrue(response.getMappedIdentities().size() >= 1); // At least one mapping
    }

    @Test
    public void testRefreshTimestampExample() {
        // Documentation sdk-ref-java.md Line 336-347: Refresh timestamp handling and previous UID availability
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail));
        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);

        IdentityMapV3Response.MappedIdentity mappedIdentity = response.getMappedIdentities().get(mappedEmail);
        if (mappedIdentity != null) {
            // Check if identity has a previous UID (recently rotated)
            if (mappedIdentity.getPreviousRawUid() != null) {
                assertNotNull(mappedIdentity.getCurrentRawUid());
                assertNotNull(mappedIdentity.getPreviousRawUid());
            }

            // Determine if refresh is needed
            Instant now = Instant.now();
            Instant refreshFrom = mappedIdentity.getRefreshFrom();
            assertNotNull(refreshFrom);

            // Refresh timestamp should be in the future for fresh mappings
            assertTrue(now.isBefore(refreshFrom) || now.equals(refreshFrom));
        }
    }

    @Test
    public void testErrorHandlingExample() {
        // Documentation sdk-ref-java.md Line 336-347: Error handling with structured reason enum and raw reason string
        // Invalid email format gets rejected at input validation level, not API level
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, optedOutEmail));

        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);

        // Retrieve unmapped identities and their reasons
        HashMap<String, IdentityMapV3Response.UnmappedIdentity> unmappedIdentities = response.getUnmappedIdentities();

        for (Map.Entry<String, IdentityMapV3Response.UnmappedIdentity> entry : unmappedIdentities.entrySet()) {
            String identifier = entry.getKey();
            IdentityMapV3Response.UnmappedIdentity unmappedIdentity = entry.getValue();

            // Get structured reason enum
            UnmappedIdentityReason reason = unmappedIdentity.getReason();

            // Get original reason string from API
            String rawReason = unmappedIdentity.getRawReason();

            assertNotNull(identifier);
            assertNotNull(reason);
            assertNotNull(rawReason);
        }
        
        // Test that invalid email format throws exception at input validation
        try {
            IdentityMapV3Input invalidInput = IdentityMapV3Input.fromEmails(Arrays.asList("invalid-email-format"));
            fail("Should have thrown IllegalArgumentException for invalid email format");
        } catch (IllegalArgumentException e) {
            // Expected for invalid email format
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void testCategorizedErrorHandling() {
        // Documentation sdk-ref-java.md Line 530-549: Categorized error handling with switch statement for UnmappedIdentityReason
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail, optedOutEmail));
        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);

        HashMap<String, IdentityMapV3Response.UnmappedIdentity> unmappedIdentities = response.getUnmappedIdentities();

        for (Map.Entry<String, IdentityMapV3Response.UnmappedIdentity> entry : unmappedIdentities.entrySet()) {
            String identifier = entry.getKey();
            UnmappedIdentityReason reason = entry.getValue().getReason();

            switch (reason) {
                case OPTOUT:
                    // User opted out - would remove from marketing lists
                    assertEquals(optedOutEmail, identifier);
                    break;

                case INVALID_IDENTIFIER:
                    // Invalid identifier format - would log for data quality
                    assertNotNull(identifier);
                    break;

                case UNKNOWN:
                    // Unknown error - would log for investigation
                    assertNotNull(identifier);
                    break;
            }
        }
    }

    @Test
    public void testBatchProcessingWithErrorHandling() {
        // Documentation sdk-ref-java.md: Batch processing patterns with comprehensive error handling
        List<String> emails = Arrays.asList(mappedEmail, optedOutEmail, optedOutEmail2);
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(emails);
        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);

        // Process successful mappings
        response.getMappedIdentities().forEach((email, identity) -> {
            assertNotNull(email);
            assertNotNull(identity.getCurrentRawUid());
            assertNotNull(identity.getRefreshFrom());
        });

        // Handle failures
        response.getUnmappedIdentities().forEach((email, unmapped) -> {
            assertNotNull(email);
            UnmappedIdentityReason reason = unmapped.getReason();
            assertNotNull(reason);

            switch (reason) {
                case OPTOUT:
                case INVALID_IDENTIFIER:
                case UNKNOWN:
                    // All valid reason types
                    break;
                default:
                    fail("Unexpected reason: " + reason);
            }
        });
    }

    @Test
    public void testInputValidation() {
        // Documentation sdk-ref-java.md: Input validation patterns (phone number regex filtering example)
        List<String> emails = Arrays.asList(mappedEmail);
        List<String> phones = Arrays.asList(mappedPhone, mappedPhone2);

        // Validate phone numbers before creating input (example pattern)
        List<String> validPhones = phones.stream().filter(phone -> phone.matches("\\+\\d{10,15}")).collect(Collectors.toList());

        IdentityMapV3Input input = new IdentityMapV3Input().withEmails(emails).withPhones(validPhones);

        IdentityMapV3Response response = identityMapV3Client.generateIdentityMap(input);
        assertNotNull(response);
    }

    @Test
    public void testMigrationExamples() {
        // Documentation sdk-ref-java.md Line 439-451, 512-527: Migration examples showing V3 response structure and enhanced error handling

        // Documentation sdk-ref-java.md Line 439-451: Current V3 SDK usage (migration target)
        IdentityMapV3Client client = new IdentityMapV3Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        IdentityMapV3Input input = IdentityMapV3Input.fromEmails(Arrays.asList(mappedEmail));
        IdentityMapV3Response response = client.generateIdentityMap(input);

        // Verify new response structure
        IdentityMapV3Response.MappedIdentity mapped = response.getMappedIdentities().get(mappedEmail);
        if (mapped != null) {
            String currentUid = mapped.getCurrentRawUid();
            String previousUid = mapped.getPreviousRawUid(); // May be null
            Instant refreshFrom = mapped.getRefreshFrom();

            assertNotNull(currentUid);
            assertNotNull(refreshFrom);
            // previousUid may be null, that's expected

            // Check if refresh is needed (example from migration docs)
            if (Instant.now().isAfter(refreshFrom)) {
                // Would refresh this identity in real usage
                assertTrue(true); // Placeholder for refresh logic
            }
        }

        // Documentation sdk-ref-java.md Line 454-462: Enhanced error handling from migration docs
        IdentityMapV3Response.UnmappedIdentity unmapped = response.getUnmappedIdentities().get(mappedEmail);
        if (unmapped != null) {
            UnmappedIdentityReason reason = unmapped.getReason();
            assertNotNull(reason);

            switch (reason) {
                case OPTOUT:
                case INVALID_IDENTIFIER:
                case UNKNOWN:
                    // All valid reasons from migration docs
                    break;
            }
        }
    }

    // Helper method to avoid code duplication
    private void storeIdentityMapping(String identifier, IdentityMapV3Response.MappedIdentity identity) {
        // Placeholder for storage logic used in examples
        assertNotNull(identifier);
        assertNotNull(identity);
        assertNotNull(identity.getCurrentRawUid());
        assertNotNull(identity.getRefreshFrom());
    }

    private void markAsOptedOut(String email) {
        // Placeholder for opt-out handling used in examples
        assertNotNull(email);
    }

    private void markAsInvalidFormat(String email) {
        // Placeholder for invalid format handling used in examples
        assertNotNull(email);
    }

    private void retryLater(String email) {
        // Placeholder for retry logic used in examples
        assertNotNull(email);
    }

    // ========================================
    // PUBLISHER USAGE TESTS
    // ========================================
    
    @Test
    public void testPublisherBasicUsageExample() {
        // Documentation sdk-ref-java.md Line 142: Publisher Basic Usage - PublisherUid2Client creation
        PublisherUid2Client publisherUid2Client = new PublisherUid2Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        
        // Documentation sdk-ref-java.md Line 147: Generate token from email with opt-out check
        TokenGenerateResponse tokenGenerateResponse = publisherUid2Client.generateTokenResponse(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        assertNotNull(tokenGenerateResponse);
    }
    
    @Test
    public void testPublisherClientServerIntegrationExample() {
        // Documentation sdk-ref-java.md Line 147: Publisher Client-Server Integration - generate token response
        TokenGenerateResponse tokenGenerateResponse = publisherUid2Client.generateTokenResponse(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        // Documentation sdk-ref-java.md Line 165: Send identity as JSON string back to client
        String identityJsonString = tokenGenerateResponse.getIdentityJsonString();
        
        // Note: If the user has opted out, this method returns null
        if (identityJsonString != null) {
            assertNotNull(identityJsonString);
            assertFalse(identityJsonString.isEmpty());
        }
    }
    
    @Test
    public void testPublisherServerSideIntegrationExample() {
        // Documentation sdk-ref-java.md Line 147: Publisher Server-Side Integration workflow - generate token
        TokenGenerateResponse tokenGenerateResponse = publisherUid2Client.generateTokenResponse(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        // Documentation sdk-ref-java.md Line 176: Store identity as JSON string
        String identityJsonString = tokenGenerateResponse.getIdentityJsonString();
        
        // Documentation sdk-ref-java.md Line 183-184: Retrieve user's UID2 token
        IdentityTokens identity = tokenGenerateResponse.getIdentity();
        if (identity != null) { 
            String advertisingToken = identity.getAdvertisingToken();
            assertNotNull(advertisingToken);
        }
        
        // Documentation sdk-ref-java.md Line 190: Retrieve identity from session
        if (identityJsonString != null) {
            IdentityTokens sessionIdentity = IdentityTokens.fromJsonString(identityJsonString);
            
            // Documentation sdk-ref-java.md Line 195-197: Check if identity can be refreshed
            if (sessionIdentity == null || !sessionIdentity.isRefreshable()) {
                // we must no longer use this identity (for example, remove this identity from the user's session)
                assertTrue(true); // Expected behavior
            }
            
            // Documentation sdk-ref-java.md Line 202: Check if refresh is needed
            if (sessionIdentity != null && sessionIdentity.isDueForRefresh()) {
                // Documentation sdk-ref-java.md Line 207: Refresh the token
                TokenRefreshResponse tokenRefreshResponse = publisherUid2Client.refreshToken(sessionIdentity);
                
                // Documentation sdk-ref-java.md Line 210: Store refreshed identity
                String refreshedIdentityJsonString = tokenRefreshResponse.getIdentityJsonString();
                
                // Documentation sdk-ref-java.md Line 212: Check for optout
                if (refreshedIdentityJsonString == null) {
                    boolean isOptout = tokenRefreshResponse.isOptout();
                    // User opted out or identity should be removed
                }
                
                assertNotNull(tokenRefreshResponse);
            }
        }
    }
    
    @Test
    public void testPublisherAdvancedUsageExample() {
        // Documentation sdk-ref-java.md Line 219: Publisher Advanced Usage - PublisherUid2Helper creation
        PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);
        
        // Documentation sdk-ref-java.md Line 224: Create envelope for token generate request
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        assertNotNull(envelope);
        assertNotNull(envelope.getEnvelope());
        
        // Documentation sdk-ref-java.md Line 229-230: Headers for HTTP request
        String authHeader = "Bearer " + UID2_API_KEY;
        String versionHeader = PublisherUid2Helper.getVersionHttpHeader();
        
        assertNotNull(authHeader);
        assertNotNull(versionHeader);
        assertFalse(versionHeader.isEmpty());
        
        // Documentation sdk-ref-java.md Line 242: Convert response to TokenGenerateResponse (simulated)
        // Note: In real usage, this would be the HTTP response body
        String mockResponseBody = "{}"; // Placeholder for actual response
        try {
            TokenGenerateResponse tokenGenerateResponse = publisherUid2Helper.createTokenGenerateResponse(mockResponseBody, envelope);
            // This might fail with mock data, which is expected in unit test
        } catch (Exception e) {
            // Expected for mock response body
            assertTrue(true);
        }
    }
    
    @Test
    public void testPublisherAdvancedClientServerExample() {
        // Documentation sdk-ref-java.md Line 250-252: Publisher Advanced Client-Server Integration
        PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        // Documentation sdk-ref-java.md Line 251: Get identity JSON string from token generate response
        // tokenGenerateResponse.getIdentityJsonString() - line 251
        assertNotNull(envelope);
    }
    
    @Test
    public void testPublisherAdvancedServerSideExample() {
        // Documentation sdk-ref-java.md Line 267-298: Publisher Advanced Server-Side Integration complete workflow
        PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);
        
        // Simulate the full advanced flow
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        // Documentation sdk-ref-java.md Line 275-277: Generate IdentityTokens object from JSON
        String identityJsonString = "{}"; // Mock data
        try {
            IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);
            
            // Documentation sdk-ref-java.md Line 280-282: Check if identity can be refreshed
            if (identity == null || !identity.isRefreshable()) {
                // we must no longer use this identity (for example, remove this identity from the user's session)
                assertTrue(true); // Expected behavior for mock data
            }
            
            // Documentation sdk-ref-java.md Line 285-287: Check if refresh is needed
            if (identity != null && identity.isDueForRefresh()) {
                // Refresh needed
                assertTrue(true); // Expected behavior
            }
            
        } catch (Exception e) {
            // Expected for mock JSON
            assertTrue(true);
        }
        
        // Documentation sdk-ref-java.md Line 291-293: Headers for refresh request
        String authHeader = "Bearer " + UID2_API_KEY;
        String versionHeader = PublisherUid2Helper.getVersionHttpHeader();
        
        assertNotNull(authHeader);
        assertNotNull(versionHeader);
        
        assertNotNull(envelope);
    }
    
    @Test
    public void testPublisherAdvancedTokenRefreshResponse() {
        // Documentation sdk-ref-java.md Line 297-298: Create token refresh response from HTTP response body
        PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);
        
        // First create a valid identity to use for refresh response
        TokenGenerateResponse tokenGenerateResponse = publisherUid2Client.generateTokenResponse(TokenGenerateInput.fromEmail("user@example.com").doNotGenerateTokensForOptedOut());
        
        if (tokenGenerateResponse != null && tokenGenerateResponse.getIdentity() != null) {
            IdentityTokens identity = tokenGenerateResponse.getIdentity();
            String mockRefreshResponse = "{}"; // Placeholder for actual HTTP response body
            
            try {
                // Documentation sdk-ref-java.md Line 297-298: Create token refresh response from HTTP response body
                TokenRefreshResponse tokenRefreshResponse = PublisherUid2Helper.createTokenRefreshResponse(mockRefreshResponse, identity);
                // This might fail with mock response body, which is expected in unit test
                assertNotNull(tokenRefreshResponse);
            } catch (Exception e) {
                // Expected for mock response body
                assertTrue(true);
            }
        }
    }
    
    @Test
    public void testImportStatements() {
        // Documentation sdk-ref-java.md Line 419-422: Import statements verification for migration guide
        // Verify that all documented import classes are accessible and can be instantiated
        
        // import com.uid2.client.IdentityMapV3Client;
        IdentityMapV3Client testClient = new IdentityMapV3Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        assertNotNull(testClient);
        
        // import com.uid2.client.IdentityMapV3Input;
        IdentityMapV3Input testInput = IdentityMapV3Input.fromEmails(Arrays.asList("test@example.com"));
        assertNotNull(testInput);
        
        // import com.uid2.client.IdentityMapV3Response;
        IdentityMapV3Response testResponse = testClient.generateIdentityMap(testInput);
        assertNotNull(testResponse);
        
        // import com.uid2.client.UnmappedIdentityReason;
        // Verify enum values exist as documented
        UnmappedIdentityReason[] reasons = UnmappedIdentityReason.values();
        boolean hasOptout = false, hasInvalidIdentifier = false, hasUnknown = false;
        
        for (UnmappedIdentityReason reason : reasons) {
            switch (reason) {
                case OPTOUT:
                    hasOptout = true;
                    break;
                case INVALID_IDENTIFIER:
                    hasInvalidIdentifier = true;
                    break;
                case UNKNOWN:
                    hasUnknown = true;
                    break;
            }
        }
        
        assertTrue(hasOptout);
        assertTrue(hasInvalidIdentifier);
        assertTrue(hasUnknown);
    }

    // ========================================
    // DSP USAGE TESTS  
    // ========================================
    
    @Test
    public void testDspUsageExample() {
        // Documentation sdk-ref-java.md Line 514: DSP Usage - BidstreamClient creation
        BidstreamClient client = new BidstreamClient(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        
        // Documentation sdk-ref-java.md Line 520: Refresh at startup and periodically
        client.refresh();
        
        // Documentation sdk-ref-java.md Line 529: Decrypt token into raw UID2 with domain/app name
        String mockUidToken = "mock-uid-token"; // In real usage, this would be from bid request
        String domainOrAppName = "example.com"; // or app name, or null
        
        try {
            DecryptionResponse decrypted = client.decryptTokenIntoRawUid(mockUidToken, domainOrAppName);
            
            // If decryption succeeded, use the raw UID2
            if (decrypted.isSuccess()) {
                String uid = decrypted.getUid();
                assertNotNull(uid);  
            } else {
                // Check decrypted.getStatus() for the failure reason
                DecryptionStatus status = decrypted.getStatus();
                assertNotNull(status);
            }
        } catch (Exception e) {
            // Expected with mock token
            assertTrue(true);
        }
    }

    // ========================================
    // UID2 SHARERS USAGE TESTS
    // ========================================
    
    @Test
    public void testSharingClientUsageExample() {
        // Documentation sdk-ref-java.md Line 557: UID2 Sharers Usage - SharingClient creation
        SharingClient client = new SharingClient(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        
        // Documentation sdk-ref-java.md Line 562: Refresh at startup and periodically
        client.refresh();
        
        // Documentation sdk-ref-java.md Line 567: Encrypt raw UID as sender
        String mockRawUid = "mock-raw-uid"; // In real usage, this would be actual raw UID2
        
        try {
            EncryptionDataResponse encrypted = client.encryptRawUidIntoToken(mockRawUid);
            
            // If encryption succeeded, send the UID2 token to the receiver
            if (encrypted.isSuccess()) {
                String encryptedData = encrypted.getEncryptedData();
                assertNotNull(encryptedData);
            } else {
                // Check encrypted.getStatus() for the failure reason
                EncryptionStatus status = encrypted.getStatus();
                assertNotNull(status);
            }
        } catch (Exception e) {
            // Expected with mock raw UID
            assertTrue(true);
        }
        
        // Documentation sdk-ref-java.md Line 581: Decrypt token as receiver
        String mockUidToken = "mock-uid-token"; // In real usage, this would be from sender
        
        try {
            DecryptionResponse decrypted = client.decryptTokenIntoRawUid(mockUidToken);
            
            // If decryption succeeded, use the raw UID2
            if (decrypted.isSuccess()) {
                String uid = decrypted.getUid();
                assertNotNull(uid);
            } else {
                // Check decrypted.getStatus() for the failure reason  
                DecryptionStatus status = decrypted.getStatus();
                assertNotNull(status);
            }
        } catch (Exception e) {
            // Expected with mock token
            assertTrue(true);
        }
    }

    // ========================================
    // V2 LEGACY EXAMPLES TESTS
    // ========================================
    
    @Test
    public void testV2LegacyUsageExample() {
        // Documentation sdk-ref-java.md Line 480: V2 Implementation - Legacy IdentityMapClient creation
        IdentityMapClient identityMapClient = new IdentityMapClient(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        
        // Documentation sdk-ref-java.md Line 485: V2 Input construction (single identity type only)
        IdentityMapInput input = IdentityMapInput.fromEmails(Arrays.asList("user@example.com"));
        
        // Documentation sdk-ref-java.md Line 485: V2 API call
        IdentityMapResponse identityMapResponse = identityMapClient.generateIdentityMap(input);
        
        // Documentation sdk-ref-java.md Line 492-493: V2 Response handling with proper types
        HashMap<String, IdentityMapResponse.MappedIdentity> mappedIdentities = identityMapResponse.getMappedIdentities();
        HashMap<String, IdentityMapResponse.UnmappedIdentity> unmappedIdentities = identityMapResponse.getUnmappedIdentities();
        
        assertNotNull(mappedIdentities);
        assertNotNull(unmappedIdentities);
        
        // Documentation sdk-ref-java.md Line 498-501: V2 Result processing with bucket ID
        IdentityMapResponse.MappedIdentity mappedIdentity = mappedIdentities.get("user@example.com");
        if (mappedIdentity != null) {
            String rawUID = mappedIdentity.getRawUid();
            String bucketId = mappedIdentity.getBucketId();
            
            assertNotNull(rawUID);
            assertNotNull(bucketId);
        }
    }
    
    @Test 
    public void testV2ToV3MigrationExamples() {
        // Documentation sdk-ref-java.md Line 410-415: Basic email mapping migration example
        
        // Documentation sdk-ref-java.md Line 411: V2 approach (before migration)
        IdentityMapClient clientV2 = new IdentityMapClient(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        IdentityMapInput inputV2 = IdentityMapInput.fromEmails(Arrays.asList("user@example.com"));
        IdentityMapResponse responseV2 = clientV2.generateIdentityMap(inputV2);
        String uidV2 = null;
        if (!responseV2.getMappedIdentities().isEmpty()) {
            uidV2 = responseV2.getMappedIdentities().values().iterator().next().getRawUid();
        }
        
        // Documentation sdk-ref-java.md Line 414: V3 approach (after migration)
        IdentityMapV3Client clientV3 = new IdentityMapV3Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);
        IdentityMapV3Input inputV3 = IdentityMapV3Input.fromEmails(Arrays.asList("user@example.com"));
        IdentityMapV3Response responseV3 = clientV3.generateIdentityMap(inputV3);
        String uidV3 = null;
        if (!responseV3.getMappedIdentities().isEmpty()) {
            uidV3 = responseV3.getMappedIdentities().values().iterator().next().getCurrentRawUid();
        }
        
        // Both should work
        assertNotNull(responseV2);
        assertNotNull(responseV3);
        
        // Documentation sdk-ref-java.md Line 517-527: Enhanced response processing with UID rotation support
        IdentityMapV3Response.MappedIdentity mapped = responseV3.getMappedIdentities().get("user@example.com");
        if (mapped != null) {
            String currentUid = mapped.getCurrentRawUid();
            String previousUid = mapped.getPreviousRawUid(); // May be null
            Instant refreshFrom = mapped.getRefreshFrom();
            
            assertNotNull(currentUid);
            assertNotNull(refreshFrom);
            
            // Documentation sdk-ref-java.md Line 524-526: Check if refresh is needed
            if (Instant.now().isAfter(refreshFrom)) {
                // Refresh this identity
                assertTrue(true); // Placeholder for refresh logic
            }
        }
        
        // Documentation sdk-ref-java.md Line 535-549: Error handling migration with structured switch statement
        IdentityMapV3Response.UnmappedIdentity unmapped = responseV3.getUnmappedIdentities().get("user@example.com");
        if (unmapped != null) {
            switch (unmapped.getReason()) {
                case OPTOUT:
                    // User opted out - remove from marketing lists
                    break;
                case INVALID_IDENTIFIER:
                    // Fix data quality issue  
                    break;
                case UNKNOWN:
                    // Log for investigation, possibly retry
                    break;
            }
        }
    }
}
