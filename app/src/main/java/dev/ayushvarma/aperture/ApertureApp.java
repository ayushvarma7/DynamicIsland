package dev.ayushvarma.aperture;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

import dev.ayushvarma.aperture.settings.IslandSettings;

/** Process-wide singletons. Sources, the overlay and the settings UI all talk through these. */
public final class ApertureApp extends Application {
    private IslandHub hub;
    private IslandSettings settings;

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        settings = new IslandSettings(this);
        hub = new IslandHub();
    }

    public static ApertureApp of(android.content.Context c) {
        return (ApertureApp) c.getApplicationContext();
    }

    public IslandHub hub() {
        return hub;
    }

    public IslandSettings settings() {
        return settings;
    }
}
