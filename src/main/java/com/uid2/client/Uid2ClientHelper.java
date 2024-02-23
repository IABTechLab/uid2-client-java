package com.uid2.client;

import okhttp3.*;

import java.io.IOException;

public class Uid2ClientHelper {
    public Uid2ClientHelper(String uid2BaseUrl, String clientApiKey) {
        this.uid2BaseUrl = uid2BaseUrl;
        this.headers = getHeaders(clientApiKey);
    }

    static Headers getHeaders(String clientApiKey) {
        return new Headers.Builder()
                .add("Authorization", "Bearer " + clientApiKey)
                .add("X-UID2-Client-Version: java-" + Uid2Helper.getArtifactAndVersion())
                .build();
    }

    String makeRequest(EnvelopeV2 envelope, String urlSuffix) {
        return makeRequest(envelope.getEnvelope(), urlSuffix);
    }

    String makeRequest(String requestBody, String urlSuffix) {
        Request request = new Request.Builder()
                .url(uid2BaseUrl + urlSuffix)
                .headers(headers)
                .post(RequestBody.create(requestBody, FORM))
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Uid2Exception("Unexpected code " + response);
            }

            return getResponse(response);
        } catch (IOException e) {
            throw new Uid2Exception("error communicating with api endpoint", e);
        }
    }

    private static String getResponse(Response response) {
        String responseString;

        try {
            if(response == null) {
                throw new Uid2Exception("Response is null");
            }
            else {
                responseString = response.body() != null ? response.body().string() : response.toString();
                if (!response.isSuccessful()) {
                    throw new Uid2Exception("Unexpected code " + responseString);
                }
            }
            return responseString;
        } catch (IOException e) {
            throw new Uid2Exception("Error communicating with api endpoint", e);
        }
    }

    private final OkHttpClient client = new OkHttpClient();
    private final String uid2BaseUrl;
    private final Headers headers;
    private final static MediaType FORM = MediaType.get("application/x-www-form-urlencoded");

}
