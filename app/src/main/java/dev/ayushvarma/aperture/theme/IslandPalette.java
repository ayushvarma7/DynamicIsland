package dev.ayushvarma.aperture.theme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;

/**
 * Section 7.1 System color mode: the tokens the OS already generated (android.R.color.system_*_dark), so the
 * island matches Quick Settings, including contrast level and every Dynamic Color style. The body is always
 * black; color comes from content. Rebuild from the service's own Resources on configuration change
 * (Phase 0 V3: Resources.getSystem() never sees theme overlays).
 */
public final class IslandPalette {
    public final int body;
    public final int expandedBody;
    public final int primary;
    public final int onSurface;
    public final int onSurfaceVariant;
    public final int primaryContainer;
    public final int onPrimaryContainer;
    public final int buttonSecondary;
    public final int tertiary;
    public final int error;
    public final int accept;
    public final int keyline;
    public final int track;
    /** The wallpaper is light (supports dark text), so translucent cards need more opacity to stay readable. */
    public final boolean lightWallpaper;
    public final Typeface typeface;
    public final Typeface typefaceMedium;

    private IslandPalette(Context ctx, boolean tonalBody) {
        Resources r = ctx.getResources();
        primary = r.getColor(android.R.color.system_primary_dark, null);
        onSurface = r.getColor(android.R.color.system_on_surface_dark, null);
        onSurfaceVariant = r.getColor(android.R.color.system_on_surface_variant_dark, null);
        primaryContainer = r.getColor(android.R.color.system_primary_container_dark, null);
        onPrimaryContainer = r.getColor(android.R.color.system_on_primary_container_dark, null);
        buttonSecondary = r.getColor(android.R.color.system_surface_container_highest_dark, null);
        tertiary = r.getColor(android.R.color.system_tertiary_dark, null);
        error = r.getColor(android.R.color.system_error_dark, null);
        body = Color.BLACK;
        expandedBody = tonalBody ? r.getColor(android.R.color.system_surface_container_high_dark, null) : Color.BLACK;
        accept = blend(0xFF34C759, primary, 0.15f);
        keyline = withAlpha(primary, 0.32f);
        track = withAlpha(primary, 0.24f);
        boolean light = false;
        try {
            android.app.WallpaperColors wc = android.app.WallpaperManager.getInstance(ctx)
                    .getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM);
            light = wc != null && (wc.getColorHints() & android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
        } catch (RuntimeException e) {
            light = false;
        }
        lightWallpaper = light;
        typeface = Fonts.regular();
        typefaceMedium = Fonts.medium();
    }

    public static IslandPalette from(Context ctx, boolean tonalBody) {
        return new IslandPalette(ctx, tonalBody);
    }

    public static int withAlpha(int color, float alpha) {
        return (Math.round(alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    static int blend(int a, int b, float t) {
        int r = Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }
}
