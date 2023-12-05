# UID2 SDK for Java

The UID 2 Project is subject to the IAB Tech Lab Intellectual Property Rights (IPR) Policy, and is managed by the IAB Tech Lab Addressability Working Group and [Privacy & Rearc Commit Group](https://iabtechlab.com/working-groups/privacy-rearc-commit-group/). Please review the [governance rules](https://github.com/IABTechLab/uid2-core/blob/master/Software%20Development%20and%20Release%20Procedures.md).

This document includes:
* [Who Is this SDK for?](#who-is-this-sdk-for)
* [Requirements](#requirements)
* [Install](#install)
* [Usage for DSPs](#usage-for-dsps)
* [Usage for Publishers](#usage-for-publishers)
   - [Basic Usage](#basic-usage)
   - [Advanced Usage](#advanced-usage)
* [Usage for UID2 Sharers](#usage-for-uid2-sharers)

## Who Is this SDK for?

This SDK simplifies integration with UID2 for Publishers, DSPs, and UID Sharers, as described in [UID2 Integration Guides](https://unifiedid.com/docs/category/integration-guides). 

## Requirements

This SDK requires Java version 1.8 or later.

## Install

To install the SDK, add the `uid2-client` dependency to your project's `pom.xml` file [as shown here](https://central.sonatype.com/artifact/com.uid2/uid2-client).

## Usage for DSPs

For an example of usage for DSPs, see [com.uid2.client.test.IntegrationExamples](https://github.com/IABTechLab/uid2-client-java/blob/master/src/test/java/com/uid2/client/test/IntegrationExamples.java).

## Usage for Publishers

As a publisher, there are two ways to use this SDK: 
1. [**Basic Usage**](#basic-usage) is for publishers who want to use this SDK's HTTP implementation (synchronous [OkHttp](https://square.github.io/okhttp/)).
2. [**Advanced Usage**](#advanced-usage) is for publishers who prefer to use their own HTTP library. 

For an example application that demonstrates both Basic and Advanced usage, see [Java UID2 Integration Example](https://github.com/UnifiedID2/uid2-examples/tree/main/publisher/uid2-java-test-site#readme).

### Basic Usage

If you're using the SDK's HTTP implementation, follow these steps.

1. Create an instance of PublisherUid2Client as an instance variable.
 
   `private final PublisherUid2Client publisherUid2Client = new PublisherUid2Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);`

2. Call a function that takes the user's email address or phone number as input and generates a `TokenGenerateResponse` object. The following example uses an email address:
 
   `TokenGenerateResponse tokenGenerateResponse = publisherUid2Client.generateTokenResponse(TokenGenerateInput.fromEmail(emailAddress).doNotGenerateTokensForOptedOut());`

   >IMPORTANT: Be sure to call this function only when you have obtained legal basis to convert the user’s [directly identifying information (DII)](https://unifiedid.com/docs/ref-info/glossary-uid#gl-dii) to UID2 tokens for targeted advertising.
   
   >IMPORTANT: Always apply `doNotGenerateTokensForOptedOut()`. This applies `policy=1` in the [/token/generate](https://unifiedid.com/docs/endpoints/post-token-generate#token-generation-policy) call. Support for `policy=0` will be removed soon.
#### Standard Integration

If you're using standard integration (client and server) (see [UID2 SDK for JavaScript Integration Guide](https://unifiedid.com/docs/guides/publisher-client-side)), follow this step:

* Send this identity as a JSON string back to the client (to use in the [identity field](https://unifiedid.com/docs/sdks/client-side-identity#initopts-object-void)) using the following:

    `tokenGenerateResponse.getIdentityJsonString()` //Note: this method returns `null` if the user has opted out, so be sure to handle that case.

#### Server-Only Integration

If you're using server-only integration (see [Publisher Integration Guide, Server-Only](https://unifiedid.com/docs/guides/custom-publisher-integration)):

1. Store this identity as a JSON string in the user's session, using the `tokenGenerateResponse.getIdentityJsonString()` function. This method returns `null` if the user has opted out, so be sure to handle that case.
2. To retrieve the user's UID2 token, use:

   ```
   IdentityTokens identity = tokenGenerateResponse.getIdentity();
   if (identity != null) { String advertisingToken = identity.getAdvertisingToken(); }
   ```
4. When the user accesses another page, or on a timer, determine whether a refresh is needed:
   1. Retrieve the identity JSON string from the user's session, and then call the following function that takes the identity information as input and generates an `IdentityTokens` object:

      `IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);`
   2. Determine if the identity can be refreshed (that is, the refresh token hasn't expired):

      ` if (identity == null || !identity.isRefreshable()) { we must no longer use this identity (for example, remove this identity from the user's session) }`
   3. Determine if a refresh is needed:

      `if (identity.isDueForRefresh()) {..}`
5. If needed, refresh the token and associated values:
 
   `TokenRefreshResponse tokenRefreshResponse = publisherUid2Client.refreshToken(identity);`
 
6. Store `tokenRefreshResponse.getIdentityJsonString()` in the user's session. If the user has opted out, this method returns `null`, indicating that the user's identity should be removed from the session. To confirm optout, you can use the `tokenRefreshResponse.isOptout()` function.

### Advanced Usage

1. Create an instance of PublisherUid2Helper as an instance variable:

    `private final PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);`
2. Call a function that takes the user's email address or phone number as input and creates a secure request data envelope. See [Encrypting requests](https://unifiedid.com/docs/getting-started/gs-encryption-decryption#encrypting-requests). The following example uses an email address:

    `EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail(emailAddress).doNotGenerateTokensForOptedOut());`
3. Using an HTTP client library of your choice, post this envelope to the [POST token/generate](https://unifiedid.com/docs/endpoints/post-token-generate) endpoint, including headers and body:
   1. Headers: Depending on your HTTP library, this might look something like the following:  
    
      `.putHeader("Authorization", "Bearer " + UID2_API_KEY)`  
      `.putHeader("X-UID2-Client-Version", PublisherUid2Helper.getVersionHeader())`
   2. Body: `envelope.getEnvelope()`
   >IMPORTANT: Be sure to call this endpoint only when you have obtained legal basis to convert the user’s [directly identifying information (DII)](https://unifiedid.com/docs/ref-info/glossary-uid#gl-dii) to UID2 tokens for targeted advertising.

   >IMPORTANT: Always apply `doNotGenerateTokensForOptedOut()`. This applies `policy=1` in the [/token/generate](https://unifiedid.com/docs/endpoints/post-token-generate#token-generation-policy) call. Support for `policy=0` will be removed soon.

4. If the HTTP response status code is _not_ 200, see [Response Status Codes](https://unifiedid.com/docs/endpoints/post-token-generate#response-status-codes) to determine next steps. Otherwise, convert the UID2 identity response content into a `TokenGenerateResponse` object:

   `TokenGenerateResponse tokenGenerateResponse = publisherUid2Helper.createTokenGenerateResponse({response body}, envelope);`

#### Standard Integration

If you're using standard integration (client and server) (see [UID2 SDK for JavaScript Integration Guide](https://unifiedid.com/docs/guides/publisher-client-side)):

* Send this identity as a JSON string back to the client (to use in the [identity field](https://unifiedid.com/docs/sdks/client-side-identity#initopts-object-void)) using the following:

    `tokenGenerateResponse.getIdentityJsonString() //Note: this method returns null if the user has opted out, so be sure to handle that case.`

#### Server-Only Integration

If you're using server-only integration (see [Publisher Integration Guide, Server-Only](https://unifiedid.com/docs/guides/custom-publisher-integration)):

1. Store this identity as a JSON string in the user's session, using: `tokenGenerateResponse.getIdentityJsonString()`. This method returns null if the user has opted out, so be sure to handle that case.
2. To retrieve the user's UID2 token, use:

   ```
   IdentityTokens identity = tokenGenerateResponse.getIdentity();
   if (identity != null) { String advertisingToken = identity.getAdvertisingToken(); }
   ```

3. When the user accesses another page, or on a timer, determine whether a refresh is needed:
   1. Retrieve the identity JSON string from the user's session, and then call the following function that generates an `IdentityTokens` object:
   
       `IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);`
   2. Determine if the identity can be refreshed (that is, the refresh token hasn't expired): 
    
      ` if (identity == null || !identity.isRefreshable()) { we must no longer use this identity (for example, remove this identity from the user's session) }`
   3. Determine if a refresh is needed:
   
      `if (identity.isDueForRefresh()) {..}`
4. If a refresh is needed, call the [POST token/refresh](https://unifiedid.com/docs/endpoints/post-token-refresh) endpoint, with:
   1. Headers (depending on your HTTP library, this might look something like):
    
      `.putHeader("Authorization", "Bearer " + UID2_API_KEY)`  
      `.putHeader("X-UID2-Client-Version", PublisherUid2Helper.getVersionHeader())`. 
   2. Body: `identity.getRefreshToken()`
5. If the refresh HTTP response status code is 200:

   `TokenRefreshResponse tokenRefreshResponse = PublisherUid2Helper.createTokenRefreshResponse({response body}, identity);`
6. Store `tokenRefreshResponse.getIdentityJsonString()` in the user's session. If the user has opted out, this method returns null, indicating that the user's identity should be removed from the session. To confirm optout, you can use the `tokenRefreshResponse.isOptout()` function.

## Usage for UID2 Sharers

A UID2 sharer is a participant that wants to share UID2s or EUIDs with another participant. Raw UID2s must be encrypted into UID2 tokens before sending them to another participant. For an example of usage, see [com.uid2.client.test.IntegrationExamples](https://github.com/IABTechLab/uid2-client-java/blob/master/src/test/java/com/uid2/client/test/IntegrationExamples.java) (runSharingExample method).

1. Use UID2ClientFactory.create() to create an IUID2Client reference:
 
   `private final IUID2Client client = UID2ClientFactory.create(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);`
2. Call IUID2Client.refresh once at startup, and then periodically (for example, every hour):

   `client.refresh();`
3. Senders: 
   1. Call the following:

      `EncryptionDataResponse encrypted = client.encrypt(rawUid);`
   2. If encryption succeeded, send the UID2 token to the receiver:   

      `if (encrypted.isSuccess()) {` send `encrypted.getEncryptedData()` to receiver`} else {`check `encrypted.getStatus()` for the failure reason} 
4. Receivers: 
   1. Call the following:

      `DecryptionResponse decrypted = client.decrypt(uidToken);`
   2. If decryption succeeded, use the raw UID2:
    
      `if (decrypted.isSuccess()) {`use `decrypted.getUid() } else {`check `decrypted.getStatus()` for the failure reason `}`
