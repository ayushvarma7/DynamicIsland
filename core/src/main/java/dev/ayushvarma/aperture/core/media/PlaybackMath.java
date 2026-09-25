package dev.ayushvarma.aperture.core.media;

/** Section 8 media progress: {@code position + (now - lastPositionUpdateTime) * speed}, clamped to duration. */
public final class PlaybackMath {
    private PlaybackMath() {}

    /**
     * @param lastUpdateElapsedMs {@code PlaybackState.getLastPositionUpdateTime()} (elapsedRealtime timebase)
     * @param durationMs          track duration, or {@code <= 0} when the app didn't publish one
     * @return the current position in ms, never negative, never past the duration
     */
    public static long position(long positionMs, long lastUpdateElapsedMs, float speed, boolean playing,
                                long nowElapsedMs, long durationMs) {
        long pos = positionMs;
        if (playing && lastUpdateElapsedMs > 0) {
            pos += (long) ((nowElapsedMs - lastUpdateElapsedMs) * speed);
        }
        if (pos < 0) pos = 0;
        if (durationMs > 0 && pos > durationMs) pos = durationMs;
        return pos;
    }

    /** Progress 0..1, or -1 when duration is missing: the bar must then be hidden, never frozen. */
    public static float fraction(long positionMs, long durationMs) {
        if (durationMs <= 0) return -1f;
        return Math.max(0f, Math.min(1f, positionMs / (float) durationMs));
    }
}
