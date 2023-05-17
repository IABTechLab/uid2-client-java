package com.uid2.client;

import okhttp3.*;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


public class PublisherUid2Client {
    /**
     * @param uid2BaseUrl     The <a href="https://unifiedid.com/docs/getting-started/gs-environments">UID2 Base URL</a>
     * @param clientApiKey    Your client API key
     * @param base64SecretKey Your client secret key
     */
    public PublisherUid2Client(String uid2BaseUrl, String clientApiKey, String base64SecretKey) {
        publisherUid2Helper = new PublisherUid2Helper(base64SecretKey);
        this.uid2BaseUrl = uid2BaseUrl;
        this.headers = getHeaders(clientApiKey);
    }

    /**
     * @param tokenGenerateInput represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-token-generate#unencrypted-json-body-parameters">/token/generate</a>
     * @return an IdentityTokens instance
     * @throws Uid2Exception if the response did not contain a "success" status, or the response code was not 200, or there was an error communicating with the provided UID2 Base URL
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
     * @param tokenGenerateInput represents the input required for <a href="https://unifiedid.com/docs/endpoints/post-token-generate#unencrypted-json-body-parameters">/token/generate</a>
     * @return a CompletableFuture of IdentityTokens instance. Completes exceptionally if the response did not contain a "success" status, or the response code was not 200, or there was an error communicating with the provided UID2 Base URL
     */
    public CompletableFuture<IdentityTokens> generateTokenAsync(TokenGenerateInput tokenGenerateInput) {
        EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(tokenGenerateInput);

        Request request = new Request.Builder()
                .url(uid2BaseUrl + "/v2/token/generate")
                .headers(headers)
                .post(RequestBody.create(envelope.getEnvelope(), FORM))
                .build();

        return makeAsyncCall(request, responseString -> publisherUid2Helper.createIdentityfromTokenGenerateResponse(responseString, envelope));
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

    /**
     * @param currentIdentity the current IdentityTokens instance, typically retrieved from a user's session
     * @return the refreshed IdentityTokens instance (with a new advertising token and updated expiry times). Typically, this will be used to replace the current identity in the user's session
     */
    public CompletableFuture<TokenRefreshResponse> refreshTokenAsync(IdentityTokens currentIdentity) {
        Request request = new Request.Builder()
                .url(uid2BaseUrl + "/v2/token/refresh")
                .headers(headers)
                .post(RequestBody.create(currentIdentity.getRefreshToken(), FORM))
                .build();

        return makeAsyncCall(request, (responseString) -> PublisherUid2Helper.createTokenRefreshResponse(responseString, currentIdentity));
    }

    private <T> CompletableFuture<T> makeAsyncCall(Request request, Function<String, T> mappingFunction) {
        CompletableFuture<T> responseCompletableFuture = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                responseCompletableFuture.completeExceptionally(new Uid2Exception("Error communication with API endpoint", e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final String responseString = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        responseCompletableFuture.completeExceptionally(new Uid2Exception("Unexpected code " + response + " " + responseString));
                    } else {
                        responseCompletableFuture.complete(mappingFunction.apply(responseString));
                    }
                } catch (Exception e){
                    responseCompletableFuture.completeExceptionally(new Uid2Exception("Error communication with API endpoint", e));
                } finally {
                    response.close();
                }
            }
        });
        return responseCompletableFuture;
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


