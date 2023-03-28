# UID2 Java SDK

The UID 2 Project is subject to the IAB Tech Lab Intellectual Property Rights (IPR) Policy, and is managed by the IAB Tech Lab Addressability Working Group and [Privacy & Rearc Commit Group](https://iabtechlab.com/working-groups/privacy-rearc-commit-group/). Please review the [governance rules](https://github.com/IABTechLab/uid2-core/blob/master/Software%20Development%20and%20Release%20Procedures.md).

This document includes:
* [Who is this SDK for?](#who-is-this-sdk-for)
* [Requirements](#requirements)
* [Installation](#installation)
* [Usage for DSPs](#usage-for-dsps)
* [Usage for Publishers](#usage-for-publishers)
   - [Basic Usage](#basic-usage)
   - [Advanced Usage](#advanced-usage)
* [## Usage for UID Sharers](#usage-for-uid-sharers)

## Who is this SDK for?
This SDK simplifies integration with UID2 for Publishers, DSPs, and UID Sharers, as described in the [UID2 Integration Guides](https://github.com/IABTechLab/uid2docs/blob/main/api/v2/guides/summary-guides.md). 

## Requirements

* This SDK requires Java version 1.8 or later.

## Installation

Add this dependency to your project's POM:

```
        <dependency>
            <groupId>com.uid2</groupId>
            <artifactId>uid2-client</artifactId>
            <version>4.1.2</version>
        </dependency>
```

## Usage for DSPs

For an example of usage for DSPs, see [com.uid2.client.test.IntegrationExamples](https://github.com/IABTechLab/uid2-client-java/blob/master/src/test/java/com/uid2/client/test/IntegrationExamples.java).

## Usage for Publishers

As a publisher, there are two ways to use this SDK: 
1. [**Basic Usage**](#basic-usage) is for publishers who are happy to use this SDK's HTTP implementation (synchronous [OkHttp](https://square.github.io/okhttp/)).
2. [**Advanced Usage**](#advanced-usage) is for publishers who prefer to use their own HTTP library. 

For an example application that demonstrates both Basic and Advanced usage, see [uid2-examples](https://github.com/UnifiedID2/uid2-examples/tree/main/publisher/uid2-java-test-site).

### Basic Usage

If you're using the SDK's HTTP implementation, follow these steps.

1. Create an instance of PublisherUid2Client as an instance variable.
 
   `private final PublisherUid2Client publisherUid2Client = new PublisherUid2Client(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);`

2. When the user has authenticated, and has authorized the creation of a UID2, run a function that takes the user's email address or phone number as input and generates an `IdentityTokens` object. The following example uses an email address:
 
   `IdentityTokens identity = publisherUid2Client.generateToken(TokenGenerateInput.fromEmail(emailAddress));`
 
#### Standard Integration

If you're using [standard (client and server) integration](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/guides/publisher-client-side.md), follow this step:

* Send this identity as a JSON string back to the client (to use in the [identity field](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/sdks/client-side-identity.md#initopts-object-void)) using: `identity.getJsonString()`

#### Server-Only Integration

If you're using [server-only integration](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/guides/custom-publisher-integration.md):

1. Store this identity as a JSON string in the user's session, using the `identity.getJsonString()` function.
2. To use the user's UID2 token, use the `identity.getAdvertisingToken()` function.
3. When the user accesses another page, or on a timer, determine whether a refresh is needed:
   1. Retrieve the identity JSON string from the user's session, and then call the following function that takes the identity information as input and generates an `IdentityTokens` object:

      `IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);`
   2. Determine if the identity can be refreshed (that is, the refresh token hasn't expired):

      ` if (identity == null || !identity.isRefreshable()) { we must no longer use this identity (for example, remove this identity from the user's session) }`
   3. Determine if a refresh is needed:

      `if (identity.isDueForRefresh()) {..}`
4. If needed, refresh the token and associated values:
 
   `TokenRefreshResponse tokenRefreshResponse = publisherUid2Client.refreshToken(identity);`
 
5. You should then store `tokenRefreshResponse.getIdentityJsonString()` in the user's session. If the user has opted out, this method returns null, indicating that the user's identity should be removed from their session. (Optout can be confirmed via `tokenRefreshResponse.isOptout()`.)

### Advanced Usage

1. Create an instance of PublisherUid2Helper as an instance variable:

    `private final PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);`
2. When the user has authenticated, and has authorized the creation of a UID2, run a function that takes the user's email address or phone number as input and creates a secure request data envelope. See [Encrypting requests](https://github.com/IABTechLab/uid2docs/blob/main/api/v2/getting-started/gs-encryption-decryption.md#encrypting-requests). The following example uses an email address:

    `EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail(emailAddress));`
3. Using an HTTP client library of your choice, post this envelope to the [/v2/token/generate](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md) endpoint, including headers and body:
   1. Headers: Depending on your HTTP library, this might look something like the following:  
    
      `.putHeader("Authorization", "Bearer " + UID2_API_KEY)`  
      `.putHeader("X-UID2-Client-Version", PublisherUid2Helper.getVersionHeader())`
   2. Body: `envelope.getEnvelope()`
4. If the HTTP response status code is _not_ 200, see [Response Status Codes](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md#response-status-codes) to determine next steps. Otherwise, convert the UID2 identity response content into an `IdentityTokens` object:

   `IdentityTokens identity = publisherUid2Helper.createIdentityfromTokenGenerateResponse({response body}, envelope);`

#### Standard Integration

If you're using [standard (client and server) integration](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/guides/publisher-client-side.md):

1. Send this identity as a JSON string back to the client (to use in the [identity field](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/sdks/client-side-identity.md#initopts-object-void)) using: `identity.getJsonString()`

#### Server-Only Integration

If you're using [server-only integration](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/guides/custom-publisher-integration.md):
1. Store this identity as a JSON string in the user's session, using: `identity.getJsonString()`
2. To use the user's UID2 token, use `identity.getAdvertisingToken()`

3. When the user accesses another page, or on a timer, determine whether a refresh is needed:
   1. Retrieve the identity JSON string from the user's session, and then call the following function that generates an `IdentityTokens` object:
   
       `IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);`
   2. Determine if the identity can be refreshed (that is, the refresh token hasn't expired): 
    
      ` if (identity == null || !identity.isRefreshable()) { we must no longer use this identity (for example, remove this identity from the user's session) }`
   3. Determine if a refresh is needed:
   
      `if (identity.isDueForRefresh()) {..}`
4. If a refresh is needed, call the [POST token/refresh](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-refresh.md) endpoint, with:
   1. Headers (depending on your HTTP library, this might look something like):
    
      `.putHeader("Authorization", "Bearer " + UID2_API_KEY)`  
      `.putHeader("X-UID2-Client-Version", PublisherUid2Helper.getVersionHeader())`. 
   2. Body: `identity.getRefreshToken()`
5. If the refresh HTTP response status code is 200:

   `TokenRefreshResponse tokenRefreshResponse = PublisherUid2Helper.createTokenRefreshResponse({response body}, identity);`
6. You should then store `tokenRefreshResponse.getIdentityJsonString()` in the user's session. If the user has opted out, this method returns null, indicating that the user's identity should be removed from their session (you can confirm user opt-out with the `tokenRefreshResponse.isOptout()` function).

## Usage for UID Sharers

A UID2 Sharer is a participant that wants to share UID2s or EUIDs with another participant. Raw UIDs must be encrypted into UID tokens before sending them to another participant. For an example of usage, see [com.uid2.client.test.IntegrationExamples](https://github.com/IABTechLab/uid2-client-java/blob/master/src/test/java/com/uid2/client/test/IntegrationExamples.java) (runSharingExample method).

1. Use UID2ClientFactory.create() to create an IUID2Client reference:
 
   `private final IUID2Client client = UID2ClientFactory.create(UID2_BASE_URL, UID2_API_KEY, UID2_SECRET_KEY);`
2. Call IUID2Client.refresh once at startup, and then periodically (for example, every hour):

   `client.refresh();`
3. Senders: 
   1. Call the following:

      `EncryptionDataResponse encrypted = client.encrypt(rawUid);`
   2. If encryption succeeded, send the UID token to the receiver:   

      `if (encrypted.isSuccess()) {` send `encrypted.getEncryptedData()` to receiver`} else {`check `encrypted.getStatus()` for the failure reason} 
4. Receivers: 
   1. Call the following:

      `DecryptionResponse decrypted = client.decrypt(uidToken);`
   2. If decryption succeeded, use the raw UID:
    
      `if (decrypted.isSuccess()) {`use `decrypted.getUid() } else {`check `decrypted.getStatus()` for the failure reason `}`
