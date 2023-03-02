package com.uid2.client;

import java.time.Instant;
import java.util.*;

class KeyContainer {

    private final HashMap<Long, Key> keys = new HashMap<>();
    private final HashMap<Integer, List<Key>> keysBySite = new HashMap<>(); //for legacy /key/latest
    private final HashMap<Integer, List<Key>> keysByKeyset = new HashMap<>();
    private Instant latestKeyExpiry;
    private int callerSiteId;
    private int masterKeysetId;
    private int defaultKeysetId;
    private long tokenExpirySeconds;


    KeyContainer(List<Key> keyList)
    {
        latestKeyExpiry = Instant.MIN;

        for (Key key : keyList) {
            this.keys.put(key.getId(), key);
            if (key.getSiteId() > 0) {
                keysBySite.computeIfAbsent(key.getSiteId(), k -> new ArrayList<>()).add(key);
            }
            if (key.getExpires().isAfter(latestKeyExpiry)) {
                latestKeyExpiry = key.getExpires();
            }
        }

        for(Map.Entry<Integer, List<Key>> entry : keysBySite.entrySet()) {
            entry.getValue().sort(Comparator.comparing(Key::getActivates));
        }
    }

    KeyContainer(int callerSiteId, int masterKeysetId, int defaultKeysetId, long tokenExpirySeconds, List<Key> keyList) {
        this.callerSiteId = callerSiteId;
        this.masterKeysetId = masterKeysetId;
        this.defaultKeysetId = defaultKeysetId;
        this.tokenExpirySeconds = tokenExpirySeconds;

        for (Key key : keyList) {
            this.keys.put(key.getId(), key);
            if (key.getKeysetId() > 0) {
                keysByKeyset.computeIfAbsent(key.getKeysetId(), k -> new ArrayList<>()).add(key);
            }
            if (latestKeyExpiry == null || key.getExpires().isAfter(latestKeyExpiry)) {
                latestKeyExpiry = key.getExpires();
            }
        }

        for(Map.Entry<Integer, List<Key>> entry : keysByKeyset.entrySet()) {
            entry.getValue().sort(Comparator.comparing(Key::getActivates));
        }
    }


    public boolean isValid(Instant asOf) {
        return asOf.isBefore(latestKeyExpiry);
    }

    public Key getKey(long id) {
        return keys.get(id);
    }

    public Key getDefaultKey(Instant now)
    {
        return getKeysetActiveKey(defaultKeysetId, now);
    }

    public Key getMasterKey(Instant now)
    {
        return getKeysetActiveKey(masterKeysetId, now);
    }

    private Key getKeysetActiveKey(int keysetId, Instant now)
    {
        List<Key> keyset = keysByKeyset.get(keysetId);
        return getLatestKey(keyset, now);
    }

    private Key getLatestKey(List<Key> keys, Instant now)
    {
        if(keys == null || keys.isEmpty())
            return null;
        int it = ListHelpers.upperBound(keys, now, (ts, k) -> ts.isBefore(k.getActivates()));
        while(it > 0) {
            Key key = keys.get(it-1);
            if(key.isActive(now)) {
                return key;
            }
            --it;
        }
        return null;
    }

    public Key getActiveSiteKey(int siteId, Instant now) {
        List<Key> siteKeys = keysBySite.get(siteId);
        return getLatestKey(siteKeys, now);
    }

    public int getCallerSiteId() {
        return callerSiteId;
    }

    public long getTokenExpirySeconds() {
        return tokenExpirySeconds;
    }
}
