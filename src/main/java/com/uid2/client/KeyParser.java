 package com.uid2.client;

import com.google.gson.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class KeyParser {
    static KeyContainer parse(InputStream stream) throws Exception {
        List<Key> keys = new ArrayList<>();
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
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
}
