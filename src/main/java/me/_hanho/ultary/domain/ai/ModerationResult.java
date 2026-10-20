package me._hanho.ultary.domain.ai;

public record ModerationResult(Boolean safe, String category, String errorCode, String error, boolean retryable) {
    public static ModerationResult allowed() { return new ModerationResult(true, null, null, null, false); }
    public boolean failed() { return safe == null; }
}
