
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

## Examples

* See com.uid2.client.test.IntegrationExamples for example usage for DSPs.
* See [uid2-examples](https://github.com/UnifiedID2/uid2-examples/tree/main/publisher) for example usage for Publishers.

## Usage
To use this SDK as a Publisher with your own HTTP client library:
1. Create an instance of PublisherUid2Helper as a class variable. 

    `private final publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);`
1. When a user authenticates and authorizes the creation of a UID2:

    `Envelope envelope = publisherUid2Helper.createEnvelope(IdentityInput.fromEmail(emailAddress));`
1. Using the HTTP client library of your choice, post this envelope to the [/v2/token/generate](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md) endpoint, with:
   1. Header: `Authorization: Bearer {UID2_API_KEY}`
   1. Body: `envelope.getEnvelope()`
1. If the HTTP response status code is 200:

   `IdentityTokens identity = publisherUid2Helper.createIdentityfromTokenGenerateResponse({response body}, envelope.getNonce());`
1. Store this identity as a JSON string in the user's session state, using:

   `identity.getJsonString()`
1. When the user accesses another page, or on a timer, determine whether a refresh is needed:
   1. Retrieve the identity from the user's session state via:
   
      `IdentityTokens identity = IdentityTokens.fromJsonString(identityJsonString);`
   1. Determine if the identity is refreshable (ie, the refresh token hasn't expired): 
    
      ` if (identity == null || !identity.isRefreshable()) { user will need to reauthenticate }`
   1. Determine if a refresh is needed:
   
      `if (identity.isDueForRefresh()) {..}`
1. If a refresh is needed, post to the [/v2/token/refresh](https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-refresh.md) endpoint, with:
   1. Header: `Authorization: Bearer {UID2_API_KEY}`
   2. Body: `identity.getRefreshToken()`
1. If the refresh HTTP response status code is 200:

   `IdentityTokens refreshedIdentity = PublisherUid2Helper.createIdentityFromTokenRefreshResponse({response body}, identity); `
1. If `refreshedIdentity` is null, the user has opted out and you must no longer use their identity tokens.
1. Otherwise, store `refreshedIdentity.getJsonString()` in the user's session state. 
