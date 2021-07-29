// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

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
