package com.uid2.client;

import java.time.Instant;
import java.util.*;

class KeyContainer implements IKeyContainer {

    private HashMap<Long, Key> keys;
    private HashMap<Integer, List<Key>> keysBySite;
    private Instant latestKeyExpiry;

    KeyContainer(List<Key> keyList)
    {
        this.keys = new HashMap<>();
        this.keysBySite = new HashMap<>();
        latestKeyExpiry = Instant.MIN;

        for (Key key : keyList) {
            this.keys.put(key.getId(), key);
            if (key.getSiteId() > 0) {
                keysBySite.computeIfAbsent(key.getSiteId(), k -> new ArrayList<Key>()).add(key);
            }
            if (key.getExpires().isAfter(latestKeyExpiry)) {
                latestKeyExpiry = key.getExpires();
            }
        }

        for(Map.Entry<Integer, List<Key>> entry : keysBySite.entrySet()) {
            entry.getValue().sort(Comparator.comparing(Key::getActivates));
        }
    }

    @Override
    public boolean isValid(Instant asOf) {
        return asOf.isBefore(latestKeyExpiry);
    }

    @Override
    public Key getKey(long id) {
        return keys.get(id);
    }

    @Override
    public Key getActiveSiteKey(int siteId, Instant now) {
        List<Key> siteKeys = keysBySite.get(siteId);
        if(siteKeys == null || siteKeys.isEmpty()) return null;
        int it = ListHelpers.upperBound(siteKeys, now, (ts, k) -> ts.isBefore(k.getActivates()));
        while(it > 0) {
            Key key = siteKeys.get(it-1);
            if(key.isActive(now)) {
                return key;
            }
            --it;
        }
        return null;
    }

}
