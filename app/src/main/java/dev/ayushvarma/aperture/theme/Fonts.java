package dev.ayushvarma.aperture.theme;

import android.graphics.Typeface;

/**
 * Gate D6: the Pixel system Google Sans families (measured resolving on Pixel 8), used at explicit weights so
 * the island and the settings app share one typeface. Falls back to the system default on other phones.
 */
public final class Fonts {
    private static Typeface base;
    private static Typeface regular;
    private static Typeface medium;
    private static Typeface semibold;
    private static Typeface bold;

    private Fonts() {}

    private static synchronized Typeface base() {
        if (base == null) base = first("google-sans-flex", "google-sans-text", "google-sans");
        return base;
    }

    public static synchronized Typeface regular() {
        if (regular == null) regular = Typeface.create(base(), 400, false);
        return regular;
    }

    public static synchronized Typeface medium() {
        if (medium == null) medium = Typeface.create(base(), 500, false);
        return medium;
    }

    /** Key values on the island (times, names, ETAs): heavier weights read at a glance. */
    public static synchronized Typeface semibold() {
        if (semibold == null) semibold = Typeface.create(base(), 600, false);
        return semibold;
    }

    public static synchronized Typeface bold() {
        if (bold == null) bold = Typeface.create(base(), 700, false);
        return bold;
    }

    /** True when the Pixel font resolved; shown in the Device Report. */
    public static boolean isGoogleSans() {
        return !base().equals(Typeface.DEFAULT);
    }

    private static Typeface first(String... families) {
        for (String f : families) {
            Typeface t = Typeface.create(f, Typeface.NORMAL);
            if (t != null && !t.equals(Typeface.DEFAULT)) return t;
        }
        return Typeface.DEFAULT;
    }
}
