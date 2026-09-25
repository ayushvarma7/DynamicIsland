package dev.ayushvarma.aperture.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;

import dev.ayushvarma.aperture.R;
import dev.ayushvarma.aperture.core.engine.ActivityKind;
import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.source.Payload;

/** A sample song for previews: a generated sunset cover (so album-art colors show) and live playback. */
final class DemoContent {
    static final String SONG_ID = "demo:song";
    private static Bitmap cover;

    private DemoContent() {}

    static synchronized Bitmap cover() {
        if (cover != null) return cover;
        int s = 512;
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, s, s, new int[]{0xFFFF7A3D, 0xFFE0306E, 0xFF4B2A8A},
                null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, s, s, p);
        p.setShader(new RadialGradient(s * 0.68f, s * 0.36f, s * 0.26f, 0xFFFFD166, 0x00FFD166, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.68f, s * 0.36f, s * 0.26f, p);
        p.setShader(null);
        p.setColor(0x55200A30);
        c.drawRect(0, s * 0.72f, s, s, p);
        cover = b;
        return b;
    }

    static IslandActivity song(Context ctx) {
        long now = System.currentTimeMillis();
        Payload p = new Payload();
        p.art = cover();
        p.appIcon = ctx.getApplicationInfo().loadIcon(ctx.getPackageManager());
        p.appName = ctx.getString(R.string.app_name);
        p.durationMs = 204_000;
        p.positionMs = 61_000;
        p.positionUpdateElapsedMs = SystemClock.elapsedRealtime();
        p.speed = 1f;
        return IslandActivity.builder(SONG_ID, ctx.getPackageName(), ActivityKind.MEDIA)
                .title(ctx.getString(R.string.demo_song_title)).text(ctx.getString(R.string.demo_song_artist))
                .playing(true).payload(p).createdAt(now).updatedAt(now).build();
    }
}
