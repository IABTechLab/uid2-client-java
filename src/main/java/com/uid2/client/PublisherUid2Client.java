package com.uid2.client;

import okhttp3.*;

import java.io.*;


public class PublisherUid2Client {
    /**
     * @param uid2BaseUrl     The <a href="https://github.com/UnifiedID2/uid2docs/tree/main/api/v2#environments">UID2 Base URL</a>
     * @param clientApiKey    Your client API key
     * @param base64SecretKey Your client secret key
     */
    public PublisherUid2Client(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        publisherUid2Helper = new PublisherUid2Helper(base64SecretKey);
        this.uid2BaseUrl = uid2BaseUrl;
        this.headers = getHeaders(clientApiKey);
    }

    /**
     * @param tokenGenerateInput represents the input required for <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/endpoints/post-token-generate.md#unencrypted-json-body-parameters">/token/generate</a>
     * @return an IdentityTokens instance
     */
    public IdentityTokens generateToken(TokenGenerateInput tokenGenerateInput) {
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(tokenGenerateInput);

        Request request = new Request.Builder()
                .url(uid2BaseUrl + "/v2/token/generate")
                .headers(headers)
                .post(RequestBody.create(envelope.getEnvelope(), FORM))
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Uid2Exception("Unexpected code " + response);
            }

            String responseString = response.body().string();
            return publisherUid2Helper.createIdentityfromTokenGenerateResponse(responseString, envelope);
        } catch (IOException e) {
            throw new Uid2Exception("error communicating with api endpoint", e);
        }
    }

    /**
     * @param currentIdentity the current IdentityTokens instance, typically retrieved from a user's session
     * @return the refreshed IdentityTokens instance (with a new advertising token and updated expiry times). Typically, this will be used to replace the current identity in the user's session
     */
    public TokenRefreshResponse refreshToken(IdentityTokens currentIdentity) {
        Request request = new Request.Builder()
                .url(uid2BaseUrl + "/v2/token/refresh")
                .headers(headers)
                .post(RequestBody.create(currentIdentity.getRefreshToken(), FORM))
                .build();


        try (Response response = client.newCall(request).execute()) {
            final String responseString = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Uid2Exception("Unexpected code " + response + " " + responseString);
            }

            return PublisherUid2Helper.createTokenRefreshResponse(responseString, currentIdentity);
        } catch (IOException e) {
            throw new Uid2Exception("error communicating with api endpoint", e);
        }
    }

    private static Headers getHeaders(String clientApiKey) {
        return new Headers.Builder()
                .add("Authorization", "Bearer " + clientApiKey)
                .add("X-UID2-Client-Version: java-" + PublisherUid2Helper.getArtifactAndVersion())
                .build();
    }

    private final OkHttpClient client = new OkHttpClient();
    private final PublisherUid2Helper publisherUid2Helper;
    private final String uid2BaseUrl;
    private final Headers headers;
    private final static MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
}

