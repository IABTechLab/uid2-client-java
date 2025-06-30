package com.uid2.client;

import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

public class Uid2ClientHelper {
    Uid2ClientHelper(String baseUrl, String clientApiKey) {
        this.baseUrl = baseUrl;
        this.headers = getHeaders(clientApiKey);
    }

    static Headers getHeaders(String clientApiKey) {
        return new Headers.Builder()
                .add("Authorization", "Bearer " + clientApiKey)
                .add("X-UID2-Client-Version: java-" + Uid2Helper.getArtifactAndVersion())
                .build();
    }

    Uid2Response makeRequest(String urlSuffix, EnvelopeV2 envelope) {
        return makeRequest(urlSuffix, getEnvelope(envelope));
    }

    Uid2Response makeRequest(String urlSuffix, String payload) {
        return makeRequest(RequestBody.create(payload, FORM), urlSuffix);
    }

    private static String getEnvelope(EnvelopeV2 envelope) {
        return envelope.getEnvelope();
    }

    Uid2Response makeBinaryRequest(String urlSuffix, EnvelopeV2 envelope) {
        return makeRequest(RequestBody.create(envelope.getBinaryEnvelope(), BINARY), urlSuffix);
    }

    Uid2Response makeRequest(RequestBody body, String urlSuffix) {
        Request request = new Request.Builder()
                .url(baseUrl + urlSuffix)
                .headers(headers)
                .post(body)
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

    private static Uid2Response getResponse(Response response) {
        Uid2Response uid2Response;

        try {
            if (response == null) {
                throw new Uid2Exception("Response is null");
            }
            else {
                if (responseIsBinary(response)) {
                    byte[] bytes = response.body() != null ? response.body().bytes() : new byte[0];
                    uid2Response = Uid2Response.fromBytes(bytes);
                } else {
                    String string = response.body() != null ? response.body().string() : response.toString();
                    uid2Response = Uid2Response.fromString(string);
                }

                if (!response.isSuccessful()) {
                    throw new Uid2Exception("Unexpected code " + response);
                }
            }
            return uid2Response;
        } catch (IOException e) {
            throw new Uid2Exception("Error communicating with api endpoint", e);
        }
    }

    private static boolean responseIsBinary(Response response) {
        return Objects.equals(response.headers().get("Content-Type"), "application/octet-stream");
    }

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;
    private final Headers headers;
    private final static MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
    private final static MediaType BINARY = MediaType.get("application/octet-stream");

}
