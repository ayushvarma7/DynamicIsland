package dev.ayushvarma.aperture.theme;

import android.graphics.Typeface;

/** Gate D6: Pixel system Google Sans families when they resolve (measured on Pixel 8), else the default. */
public final class Fonts {
    private static Typeface regular;
    private static Typeface medium;

    private Fonts() {}

    public static synchronized Typeface regular() {
        if (regular == null) regular = first(Typeface.NORMAL, "google-sans-flex", "google-sans-text", "google-sans");
        return regular;
    }

    public static synchronized Typeface medium() {
        if (medium == null) {
            Typeface base = first(Typeface.NORMAL, "google-sans-medium", "google-sans-text-medium");
            medium = base != null && !base.equals(Typeface.DEFAULT) ? base : Typeface.create(regular(), 500, false);
        }
        return medium;
    }

    /** True when the Pixel font resolved; shown in the Device Report. */
    public static boolean isGoogleSans() {
        return !regular().equals(Typeface.DEFAULT);
    }

    private static Typeface first(int style, String... families) {
        for (String f : families) {
            Typeface t = Typeface.create(f, style);
            if (t != null && !t.equals(Typeface.DEFAULT)) return t;
        }
        return Typeface.DEFAULT;
    }
}
