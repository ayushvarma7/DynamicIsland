package dev.ayushvarma.aperture.source;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.res.ResourcesCompat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.IslandHub;
import dev.ayushvarma.aperture.core.engine.ActivityKind;
import dev.ayushvarma.aperture.core.engine.IslandActivity;

/**
 * Mirrors every active media session (YouTube Music, Spotify, podcast apps, browsers) into MEDIA activities.
 * Needs notification-listener access (that is what grants getActiveSessions). Paused sessions stay for
 * 10 minutes at priority 10 (Section 6.3), then leave the island.
 */
final class MediaTracker {
    private static final long PAUSED_TTL_MS = 10 * 60_000L;

    private final Context ctx;
    private final ComponentName listener;
    private final IslandHub hub;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Map<String, Tracked> tracked = new HashMap<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = this::onSessions;

    private final class Tracked extends MediaController.Callback {
        final MediaController controller;
        final String id;
        Bitmap art;
        String artKey;
        final Runnable expire;

        Tracked(MediaController c) {
            controller = c;
            id = "media:" + c.getPackageName();
            expire = () -> hub.remove(id);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            publish(this);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            publish(this);
        }

        @Override
        public void onSessionDestroyed() {
            drop(controller.getPackageName());
        }
    }

    MediaTracker(Context ctx, ComponentName listener) {
        this.ctx = ctx;
        this.listener = listener;
        this.hub = ApertureApp.of(ctx).hub();
    }

    void start() {
        MediaSessionManager msm = ctx.getSystemService(MediaSessionManager.class);
        msm.addOnActiveSessionsChangedListener(sessionsListener, listener, main);
        onSessions(msm.getActiveSessions(listener));
    }

    void stop() {
        ctx.getSystemService(MediaSessionManager.class).removeOnActiveSessionsChangedListener(sessionsListener);
        for (String pkg : new HashSet<>(tracked.keySet())) drop(pkg);
    }

    /** Re-evaluates every tracked session after a settings change. */
    void republish() {
        for (Tracked t : tracked.values()) publish(t);
    }

    private void onSessions(List<MediaController> controllers) {
        Set<String> live = new HashSet<>();
        if (controllers != null) {
            for (MediaController c : controllers) {
                String pkg = c.getPackageName();
                live.add(pkg);
                Tracked t = tracked.get(pkg);
                if (t != null && t.controller.getSessionToken().equals(c.getSessionToken())) continue;
                if (t != null) drop(pkg);
                t = new Tracked(c);
                tracked.put(pkg, t);
                c.registerCallback(t, main);
                publish(t);
            }
        }
        for (String pkg : new HashSet<>(tracked.keySet())) {
            if (!live.contains(pkg)) drop(pkg);
        }
    }

    private void drop(String pkg) {
        Tracked t = tracked.remove(pkg);
        if (t == null) return;
        t.controller.unregisterCallback(t);
        main.removeCallbacks(t.expire);
        hub.remove(t.id);
    }

    private void publish(Tracked t) {
        if (!ApertureApp.of(ctx).settings().showMedia()
                || !ApertureApp.of(ctx).settings().isAllowed(t.controller.getPackageName())) {
            hub.remove(t.id);
            return;
        }
        MediaController c = t.controller;
        PlaybackState ps = c.getPlaybackState();
        MediaMetadata md = c.getMetadata();
        int st = ps == null ? PlaybackState.STATE_NONE : ps.getState();
        CharSequence title = md == null ? null : first(md, MediaMetadata.METADATA_KEY_DISPLAY_TITLE, MediaMetadata.METADATA_KEY_TITLE);
        if (title == null || st == PlaybackState.STATE_NONE || st == PlaybackState.STATE_STOPPED
                || st == PlaybackState.STATE_ERROR) {
            main.removeCallbacks(t.expire);
            hub.remove(t.id);
            return;
        }
        boolean playing = st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
                || st == PlaybackState.STATE_FAST_FORWARDING || st == PlaybackState.STATE_REWINDING;
        CharSequence artist = first(md, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST, MediaMetadata.METADATA_KEY_ALBUM);
        loadArt(t, md);

        Payload p = new Payload();
        p.controller = c;
        p.art = t.art;
        p.open = c.getSessionActivity();
        p.durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
        p.positionMs = ps.getPosition();
        p.positionUpdateElapsedMs = ps.getLastPositionUpdateTime();
        p.speed = ps.getPlaybackSpeed() == 0f ? 1f : ps.getPlaybackSpeed();
        appInfo(p, c.getPackageName());
        List<PlaybackState.CustomAction> custom = ps.getCustomActions();
        if (custom != null && !custom.isEmpty()) {
            PlaybackState.CustomAction a = custom.get(0);
            p.customAction = a.getAction();
            p.customActionName = a.getName();
            p.customActionIcon = packageDrawable(c.getPackageName(), a.getIcon());
        }
        long now = System.currentTimeMillis();
        IslandActivity prev = hub.engine().get(t.id);
        hub.upsert(IslandActivity.builder(t.id, c.getPackageName(), ActivityKind.MEDIA)
                .title(title).text(artist).playing(playing).payload(p)
                .createdAt(prev == null ? now : prev.createdAt).updatedAt(now).build());
        main.removeCallbacks(t.expire);
        if (!playing) main.postDelayed(t.expire, PAUSED_TTL_MS);
    }

    /** Bitmap art first (Spotify, YouTube Music publish one); otherwise load the URI off the main thread. */
    private void loadArt(Tracked t, MediaMetadata md) {
        Bitmap bmp = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (bmp == null) bmp = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (bmp == null) bmp = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (bmp != null) {
            t.art = bmp;
            t.artKey = null;
            return;
        }
        String uri = firstString(md, MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_ART_URI,
                MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI);
        if (uri == null) {
            t.art = null;
            return;
        }
        if (uri.equals(t.artKey)) return;
        t.artKey = uri;
        t.art = null;
        io.execute(() -> {
            Bitmap loaded = null;
            try (InputStream in = ctx.getContentResolver().openInputStream(Uri.parse(uri))) {
                if (in != null) loaded = BitmapFactory.decodeStream(in);
            } catch (Exception | OutOfMemoryError ignored) {
                // Unreadable URIs (e.g. http) simply mean no art; the app icon is shown instead.
            }
            Bitmap result = loaded;
            main.post(() -> {
                if (uri.equals(t.artKey) && tracked.get(t.controller.getPackageName()) == t) {
                    t.art = result;
                    publish(t);
                }
            });
        });
    }

    private void appInfo(Payload p, String pkg) {
        PackageManager pm = ctx.getPackageManager();
        try {
            p.appIcon = pm.getApplicationIcon(pkg);
            p.appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
        } catch (PackageManager.NameNotFoundException ignored) {
            p.appName = pkg;
        }
    }

    private Drawable packageDrawable(String pkg, int resId) {
        if (resId == 0) return null;
        try {
            return ResourcesCompat.getDrawable(ctx.getPackageManager().getResourcesForApplication(pkg), resId, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static CharSequence first(MediaMetadata md, String... keys) {
        if (md == null) return null;
        for (String k : keys) {
            CharSequence v = md.getText(k);
            if (v != null && v.length() > 0) return v;
        }
        return null;
    }

    private static String firstString(MediaMetadata md, String... keys) {
        for (String k : keys) {
            String v = md.getString(k);
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }
}
