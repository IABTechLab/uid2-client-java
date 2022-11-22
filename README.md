
The UID 2 Project is subject to Tech Lab IPR’s Policy and is managed by the IAB Tech Lab Addressability Working Group and Privacy & Rearc Commit Group. Please review the governance rules [here](https://github.com/IABTechLab/uid2-core/blob/master/Software%20Development%20and%20Release%20Procedures.md).

# Requirements

* Java 1.8 or later


## Installation

Add this dependency to your project's POM:


```
        <dependency>
            <groupId>com.uid2</groupId>
            <artifactId>uid2-client</artifactId>
            <version>4.0.0</version>
        </dependency>
```

## Usage for DSPs

See com.uid2.client.test.IntegrationExamples for example usage for DSPs.

## Usage for Publishers
You can use this SDK with your own HTTP client library, as outlined in the steps below. An example application is at [uid2-examples](https://github.com/UnifiedID2/uid2-examples/tree/main/publisher).


1. Create an instance of PublisherUid2Helper as an instance variable. 

    `private final PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);`
2. When a user authenticates and authorizes the creation of a UID2:

    `EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail(emailAddress));`
3. Using an HTTP client library of your choice, post this envelope to the [/v2/token/generate](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md) endpoint, with:
   1. Header: `Authorization: Bearer {UID2_API_KEY}`
   2. Body: `envelope.getEnvelope()`
4. If the HTTP response status code is _not_ 200, see [Response Status Codes](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md#response-status-codes) to determine next steps. Otherwise:

   `IdentityTokens identity = publisherUid2Helper.createIdentityfromTokenGenerateResponse({response body}, envelope);`

### Standard Integration
If you're using [standard (client-side) integration](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/guides/publisher-client-side.md):

1. Send this identity as a JSON string back to the client (to use in the [identity field](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/sdks/client-side-identity.md#initopts-object-void)) using: `identity.getJsonString()`
### Server-Only Integration
If you're using [server-only integration](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/guides/custom-publisher-integration.md):
1. Store this identity as a JSON string in the user's session, using: `identity.getJsonString()`
2. To use the user's UID2 token, use `identity.getAdvertisingToken()`

3. When the user accesses another page, or on a timer, determine whether a refresh is needed:
   1. Retrieve the identity JSON string from the user's session, then call:
   
       `IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);`
   2. Determine if the identity is refreshable (ie, the refresh token hasn't expired): 
    
      ` if (identity == null || !identity.isRefreshable()) { we must no longer use this identity (eg, remove this identity from the user's session) }`
   3. Determine if a refresh is needed:
   
      `if (identity.isDueForRefresh()) {..}`
4. If a refresh is needed, post to the [/v2/token/refresh](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-refresh.md) endpoint, with:
   1. Header: `Authorization: Bearer {UID2_API_KEY}`
   2. Body: `identity.getRefreshToken()`
5. If the refresh HTTP response status code is 200:

   `IdentityTokens refreshedIdentity = PublisherUid2Helper.createIdentityFromTokenRefreshResponse({response body}, identity); `
6. If `refreshedIdentity` is null, the user has opted out, and you must no longer use their identity tokens.
7. Otherwise, replace the identity that was previously stored in the user's session with: `refreshedIdentity.getJsonString()`. 
