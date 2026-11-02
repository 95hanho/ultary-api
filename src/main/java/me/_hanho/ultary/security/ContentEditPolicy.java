package me._hanho.ultary.security;

/** Reserved service account. Nickname changes must never grant this permission. */
public final class ContentEditPolicy {
    private ContentEditPolicy() {}

    public static boolean canEdit(Long actor, Long author) {
        return actor != null && (actor == 100L || actor.equals(author));
    }
}
