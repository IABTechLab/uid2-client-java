package com.uid2.client;

public class RefreshResponse {
    private RefreshResponse(boolean success, String reason)
    {
        this.success = success;
        this.reason = reason;
    }

    public static RefreshResponse makeSuccess()
    {
        return new RefreshResponse(true, "");
    }

    public static RefreshResponse makeError(String reason)
    {
        return new RefreshResponse(false, reason);
    }

    public boolean isSuccess()
    {
        return success;
    }

    private final boolean success;
    private final String reason;
}
