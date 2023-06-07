package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.time.Instant;
import java.util.Objects;

public class IdentityTokens {
  /**
   * Creates an IdentityTokens instance
   * @param jsonString a JSON string generated from a previous call to {@link #getJsonString}. Typically, this string is stored in the user's session.
   * @return an IdentityTokens instance
   */
  static public IdentityTokens fromJsonString(String jsonString) {
    Objects.requireNonNull(jsonString, "jsonString must not be null");

    try {
      return fromJson(new Gson().fromJson(jsonString, JsonObject.class));
    } catch (JsonSyntaxException e) {
      throw new Uid2Exception("invalid json string", e);
    } catch (NullPointerException e) {
      throw new Uid2Exception("missing field in json string", e);
    }
  }

  /**
   * @return whether this identity is due to be refreshed. If true, a call to <a href="https://unifiedid.com/docs/endpoints/post-token-refresh">/token/refresh</a> is due.
   */
  public boolean isDueForRefresh() {
    return isDueForRefreshImpl(Instant.now());
  }

  /**
   * @return the advertising token. This token can be sent to the SSP for bidding.
   */
  public String getAdvertisingToken() { return advertisingToken; }

  /**
   * @return the refresh token. This is used as the POST body in <a href="https://unifiedid.com/docs/endpoints/post-token-refresh">/token/refresh</a>
   */
  public String getRefreshToken() { return refreshToken; }

  /**
   * @return the identity as represented by a JSON string. This should be sent back to the client if using <a href="https://unifiedid.com/docs/guides/publisher-client-side">standard integration</a>,
   * or stored in the user's session if using <a href="https://unifiedid.com/docs/guides/custom-publisher-integration">Server-Only integration.</a>
   */
  public String getJsonString() {return jsonString;} //this ensures we make newly added fields available, even before this class is updated.

  /**
   * @return whether the identity is refreshable. If false, the refresh token has expired. This means the identity should be removed from the user's session and must no longer be used.
   */
  public boolean isRefreshable() { return isRefreshableImpl(Instant.now()); }

  static IdentityTokens fromJson(JsonObject json) {
    return new IdentityTokens(getJsonString(json, "advertising_token"), getJsonString(json, "refresh_token"), getRefreshResponseKey(json),
            getInstant(json, "identity_expires"), getInstant(json, "refresh_expires"), getInstant(json, "refresh_from"), json.toString());
  }


  boolean isRefreshableImpl(Instant timestamp) {
    Instant refreshExpires = getRefreshExpires();
    if (refreshExpires == null || timestamp.isAfter(refreshExpires)) {
      return false;
    }
    return getRefreshToken() != null;
  }


  boolean isDueForRefreshImpl(Instant timestamp) {
    return timestamp.isAfter(getRefreshFrom()) || hasIdentityExpired(timestamp);
  }

  boolean hasIdentityExpired(Instant timestamp) {
    return timestamp.isAfter(getIdentityExpires());
  }

  String getRefreshResponseKey() { return refreshResponseKey; }

  /**
   * @return Instant that indicated when the advertising token expires
   */
  public Instant getIdentityExpires() { return identityExpires; }

  /**
   * @return Instant that indicates when the refresh token expires
   */
  public Instant getRefreshExpires() { return refreshExpires; } //indicates when the refresh token expires

  /**
   * @return Instant that indicated when identity is due for refresh
   */
  public Instant getRefreshFrom() { return refreshFrom; }


  private IdentityTokens(String advertisingToken, String refreshToken, String refreshResponseKey, Instant identityExpires,
                        Instant refreshExpires, Instant refreshFrom, String jsonString) {
    this.advertisingToken = advertisingToken;
    this.refreshToken = refreshToken;
    this.identityExpires = identityExpires;
    this.refreshExpires = refreshExpires;
    this.refreshFrom = refreshFrom;
    this.refreshResponseKey = refreshResponseKey;
    this.jsonString = jsonString;
    //note we are intentionally skipping user_token as that is likely to be deprecated
  }

  static private Instant getInstant(JsonObject json, String key) {
    return Instant.ofEpochMilli(json.get(key).getAsLong());
  }
  static private String getJsonString(JsonObject json, String key) {
    JsonElement keyElem = json.get(key);
    return keyElem.getAsString();
  }

  static private String getRefreshResponseKey(JsonObject json) {
    JsonElement keyElem = json.get("refresh_response_key");
    if (keyElem == null) {
      return null; //temporary to allow saved v1 sessions
    }
    return keyElem.getAsString();
  }


  private final String advertisingToken;
  private final String refreshToken;
  private final String refreshResponseKey;
  private final Instant identityExpires;
  private final Instant refreshExpires;
  private final Instant refreshFrom;
  private final String jsonString;

}


