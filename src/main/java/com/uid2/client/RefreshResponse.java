package com.uid2.client;

public class RefreshResponse {
    private RefreshResponse(boolean success, String reason)
    {
        this.success = success;
        this.reason = reason;
    }

    static RefreshResponse makeSuccess()
    {
        return new RefreshResponse(true, "");
    }

    static RefreshResponse makeError(String reason)
    {
        return new RefreshResponse(false, reason);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public String getReason()
    {
        return reason;
    }

    private final boolean success;
    private final String reason;
}
