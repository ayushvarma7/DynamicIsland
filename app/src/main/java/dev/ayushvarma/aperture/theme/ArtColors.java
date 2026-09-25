package dev.ayushvarma.aperture.theme;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Section 7.1 "Content" color for media: a vivid accent picked from the album art, then lifted so it stays
 * readable on the island's dark body (contrast guard: bright, not neon). Falls back when the art is grey.
 */
public final class ArtColors {
    private static Bitmap lastArt;
    private static int lastAccent;

    private ArtColors() {}

    public static synchronized int accent(Bitmap art, int fallback) {
        if (art == null || art.isRecycled()) return fallback;
        if (art == lastArt) return lastAccent == 0 ? fallback : lastAccent;
        lastArt = art;
        lastAccent = pick(art);
        return lastAccent == 0 ? fallback : lastAccent;
    }

    private static int pick(Bitmap art) {
        Bitmap small = Bitmap.createScaledBitmap(art.copy(Bitmap.Config.ARGB_8888, false), 24, 24, true);
        float[] hsv = new float[3];
        float bestScore = 0;
        float[] best = null;
        for (int y = 0; y < small.getHeight(); y++) {
            for (int x = 0; x < small.getWidth(); x++) {
                Color.colorToHSV(small.getPixel(x, y), hsv);
                if (hsv[1] < 0.28f || hsv[2] < 0.25f) continue;
                // Prefer saturated, mid-to-bright colors; very dark or washed-out pixels don't carry the mood.
                float score = hsv[1] * (1f - Math.abs(hsv[2] - 0.75f));
                if (score > bestScore) {
                    bestScore = score;
                    best = hsv.clone();
                }
            }
        }
        if (best == null) return 0;
        best[1] = Math.min(best[1], 0.72f);
        best[2] = Math.max(best[2], 0.92f);
        return Color.HSVToColor(best);
    }

    private static Bitmap lumArt;
    private static float lumValue;

    /** Mean relative luminance (0..1) of the art, used to size the scrim so white text stays at 4.5:1. */
    public static synchronized float luminance(Bitmap art) {
        if (art == null || art.isRecycled()) return 0f;
        if (art == lumArt) return lumValue;
        Bitmap small = Bitmap.createScaledBitmap(art.copy(Bitmap.Config.ARGB_8888, false), 12, 12, true);
        double sum = 0;
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) sum += Color.luminance(small.getPixel(x, y));
        }
        lumArt = art;
        lumValue = (float) (sum / 144);
        return lumValue;
    }

    /**
     * Scrim alpha over a background of luminance {@code l} so white text reaches 4.5:1:
     * (1 + 0.05) / (l * (1 - a) + 0.05) >= 4.5  =>  l * (1 - a) <= 0.183.
     */
    public static float scrimFor(float l) {
        if (l <= 0.183f) return 0.45f;
        return Math.max(0.45f, Math.min(0.92f, 1f - 0.183f / l));
    }

    /** Black or white, whichever reads better on the accent (for the glyph on the play button). */
    public static int onAccent(int accent) {
        double lum = 0.2126 * Color.red(accent) + 0.7152 * Color.green(accent) + 0.0722 * Color.blue(accent);
        return lum > 140 ? 0xFF111111 : 0xFFFFFFFF;
    }
}
