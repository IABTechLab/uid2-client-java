package com.uid2.client;

import java.util.List;

public final class MappingResponseBody {
    private List<MappedUid> mapped;
    private List<UnmappedUid> unmapped;

    public List<MappedUid> getMapped() {
        return this.mapped;
    }

    public List<UnmappedUid> getUnmapped() {
        return this.unmapped;
    }
}
