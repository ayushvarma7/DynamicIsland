package dev.ayushvarma.aperture.core.format;

/** Timer, call and media time strings. */
public final class TimeText {
    private TimeText() {}

    /** "0:07", "4:05", "1:02:03". Negative input is treated as zero. */
    public static String clock(long ms) {
        long total = Math.max(0, ms) / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return h + ":" + two(m) + ":" + two(s);
        return m + ":" + two(s);
    }

    /** Compact countdown: rounds up so a timer never shows 0:00 while still running. */
    public static String countdown(long remainingMs) {
        return clock(remainingMs <= 0 ? 0 : remainingMs + 999);
    }

    private static String two(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }
}
