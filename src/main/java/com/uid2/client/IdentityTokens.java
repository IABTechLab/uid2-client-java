package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.time.Instant;
import java.util.Objects;

public class IdentityTokens {
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

  public boolean isDueForRefresh() {
    return isDueForRefreshImpl(Instant.now());
  }
  public String getAdvertisingToken() { return advertisingToken; }

  public String getRefreshToken() { return refreshToken; }
  public String getJsonString() {return jsonString;} //this ensures we make newly added fields available, even before this class is updated.
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

  Instant getIdentityExpires() { return identityExpires; } //indicates when the advertising token expires

  Instant getRefreshExpires() { return refreshExpires; } //indicates when the refresh token expires

  Instant getRefreshFrom() { return refreshFrom; }


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


