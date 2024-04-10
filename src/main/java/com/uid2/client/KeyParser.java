package com.uid2.client;

import com.google.gson.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


class KeyParser {
    static KeyContainer parse(InputStream stream) {
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        JsonElement bodyElement = json.get("body");
        if (bodyElement.isJsonArray()) { // key/latest response, which is now become legacy. We can remove this block once all tests use key/sharing JSON instead
            List<Key> keys = new ArrayList<>();
            JsonArray body = json.getAsJsonArray("body");
            for (JsonElement item : body) {
                JsonObject obj = item.getAsJsonObject();
                long id = obj.get("id").getAsLong();
                int siteId = obj.get("site_id").getAsInt();
                Instant created = Instant.ofEpochSecond(obj.get("created").getAsLong());
                Instant activates = Instant.ofEpochSecond(obj.get("activates").getAsLong());
                Instant expires = Instant.ofEpochSecond(obj.get("expires").getAsLong());
                byte[] secret = Base64.getDecoder().decode(obj.get("secret").getAsString());
                keys.add(new Key(id, siteId, created, activates, expires, secret));
            }
            return new KeyContainer(keys);
        }
        else { //key/sharing response
            JsonObject body = json.get("body").getAsJsonObject();

            int callerSiteId = getAsInt(body,"caller_site_id");
            int masterKeysetId = getAsInt(body,"master_keyset_id");
            int defaultKeysetId = getAsInt(body,"default_keyset_id");
            long maxBidstreamLifetimeSeconds = getAsLongOrDefault(body, "max_bidstream_lifetime_seconds", Long.MAX_VALUE);
            long maxSharingLifetimeSeconds = getAsLongOrDefault(body, "max_sharing_lifetime_seconds", Long.MAX_VALUE);
            long allowClockSkewSeconds = getAsLongOrDefault(body, "allow_clock_skew_seconds", 1800);
            IdentityScope identityScope = getAsIdentityScopeOrDefault(body, "identity_scope", IdentityScope.UID2);

            long tokenExpirySeconds = getAsLongOrDefault(body,"token_expiry_seconds", 0);
            if (tokenExpirySeconds == 0) {
                final short defaultTokenExpiryDays = 30;
                tokenExpirySeconds = defaultTokenExpiryDays * 24 * 60 * 60;
            }

            JsonArray keysJson = isNull(body.get("keys")) ? new JsonArray() : body.get("keys").getAsJsonArray();

            List<Key> keys = new ArrayList<>();
            for (JsonElement element : keysJson) {
                JsonObject item = element.getAsJsonObject();
                Key key = Key.createKeysetKey(
                        item.get("id").getAsLong(),
                        getAsInt(item, "keyset_id"),
                        Instant.ofEpochSecond(item.get("created").getAsLong()),
                        Instant.ofEpochSecond(item.get("activates").getAsLong()),
                        Instant.ofEpochSecond(item.get("expires").getAsLong()),
                        Base64.getDecoder().decode(item.get("secret").getAsString())
                );
                keys.add(key);
            }

            return new KeyContainer(callerSiteId, masterKeysetId, defaultKeysetId, tokenExpirySeconds, keys, identityScope, maxBidstreamLifetimeSeconds, maxSharingLifetimeSeconds, allowClockSkewSeconds);
        }
    }

    static private int getAsInt(JsonObject body, String memberName) {
        JsonElement element = body.get(memberName);
        return isNull(element) ? 0 : element.getAsInt();
    }

    static private long getAsLongOrDefault(JsonObject body, String memberName, long defaultVal) {
        JsonElement element = body.get(memberName);
        return isNull(element) ? defaultVal : element.getAsLong();
    }

    static private IdentityScope getAsIdentityScopeOrDefault(JsonObject body, String memberName, IdentityScope defaultVal) {
        JsonElement element = body.get(memberName);
        return isNull(element) ?  defaultVal : body.get("identity_scope").getAsString().equals("EUID") ? IdentityScope.EUID : IdentityScope.UID2;
    }

    static private boolean isNull(JsonElement jo) {
        return jo == null || jo.isJsonNull();
    }
}
