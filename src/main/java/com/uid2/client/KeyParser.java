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
        if (bodyElement.isJsonArray()) { // key/latest response, which will become legacy
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
            long tokenExpirySeconds = getAsLong(body,"token_expiry_seconds");
            if (tokenExpirySeconds == 0) {
                final short defaultTokenExpiryDays = 30;
                tokenExpirySeconds = defaultTokenExpiryDays * 24 * 60 * 60;
            }

            JsonArray keysJson = body.get("keys").getAsJsonArray();

            List<Key> keys = new ArrayList<>();
            for (JsonElement element : keysJson) {
                JsonObject item = element.getAsJsonObject();
                Key key = Key.createKeysetKey(
                        item.get("id").getAsLong(),
                        item.get("keyset_id").getAsInt(),
                        Instant.ofEpochSecond(item.get("created").getAsLong()),
                        Instant.ofEpochSecond(item.get("activates").getAsLong()),
                        Instant.ofEpochSecond(item.get("expires").getAsLong()),
                        Base64.getDecoder().decode(item.get("secret").getAsString())
                );
                keys.add(key);
            }

            return new KeyContainer(callerSiteId, masterKeysetId, defaultKeysetId, tokenExpirySeconds, keys);
        }
    }

    static private int getAsInt(JsonObject body, String memberName) {
        JsonElement element = body.get(memberName);
        return element == null ? 0 : element.getAsInt();
    }
    static private long getAsLong(JsonObject body, String memberName) {
        JsonElement element = body.get(memberName);
        return element == null ? 0 : element.getAsLong();
    }
}
