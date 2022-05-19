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

public class DecryptionResponse {
    private final DecryptionStatus status;
    private final String uid;
    private final Instant established;
    private final Integer siteId;

    public DecryptionResponse(DecryptionStatus status, String uid, Instant established, Integer siteId) {
        this.status = status;
        this.uid = uid;
        this.established = established;
        this.siteId = siteId;
    }

    public boolean isSuccess() {
        return status == DecryptionStatus.SUCCESS;
    }

    public DecryptionStatus getStatus() {
        return status;
    }

    public String getUid() {
        return uid;
    }

    public Instant getEstablished() {
        return established;
    }

    public Integer getSiteId() { return siteId; }

    public static DecryptionResponse makeError(DecryptionStatus status) {
        return new DecryptionResponse(status, null, Instant.MIN, null);
    }

    public static DecryptionResponse makeError(DecryptionStatus status, Instant established, Integer siteId) {
        return new DecryptionResponse(status, null, established, siteId);
    }
}
