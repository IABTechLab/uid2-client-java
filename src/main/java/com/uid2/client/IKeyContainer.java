package com.uid2.client;

import java.time.Instant;

interface IKeyContainer {
    boolean isValid(Instant now);
    Key getKey(long id);
    Key getActiveSiteKey(int siteId, Instant now);
}
