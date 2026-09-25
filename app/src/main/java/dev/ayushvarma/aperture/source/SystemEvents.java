package dev.ayushvarma.aperture.source;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.R;
import dev.ayushvarma.aperture.core.engine.ActivityKind;
import dev.ayushvarma.aperture.core.engine.IslandActivity;

/** P0.7 charging and low battery transients, registered while the overlay service runs. */
public final class SystemEvents extends BroadcastReceiver {
    private static final int[] LOW_LEVELS = {15, 5};
    private static final long CHARGING_MS = 3_000;

    private final Context ctx;
    private int lastLowWarning = 101;

    public SystemEvents(Context ctx) {
        this.ctx = ctx;
    }

    public void register() {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        ctx.registerReceiver(this, f, Context.RECEIVER_NOT_EXPORTED);
    }

    public void unregister() {
        ctx.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (action == null || !ApertureApp.of(ctx).settings().showSystemEvents()) return;
        int level = level();
        long now = System.currentTimeMillis();
        switch (action) {
            case Intent.ACTION_POWER_CONNECTED:
                lastLowWarning = 101;
                post("sys:charging", c.getString(R.string.island_charging), level, now);
                vibrate(VibrationEffect.Composition.PRIMITIVE_THUD);
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                post("sys:unplugged", c.getString(R.string.island_unplugged), level, now);
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                boolean charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                if (charging || level < 0) return;
                for (int threshold : LOW_LEVELS) {
                    if (level <= threshold && lastLowWarning > threshold) {
                        lastLowWarning = threshold;
                        post("sys:low", c.getString(R.string.island_low_battery), level, now);
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }

    private void post(String id, CharSequence title, int level, long now) {
        ApertureApp.of(ctx).hub().postTransient(IslandActivity.builder(id, ctx.getPackageName(), ActivityKind.SYSTEM)
                .title(title).shortText(level >= 0 ? level + "%" : null)
                .progress(level >= 0 ? level / 100f : -1f).createdAt(now).updatedAt(now).build(), CHARGING_MS);
    }

    private int level() {
        BatteryManager bm = ctx.getSystemService(BatteryManager.class);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    /**
     * Section 7.6: composition primitives when supported (rule H1). Touch usage makes Android apply the user's
     * touch-feedback setting, including off.
     */
    private void vibrate(int primitive) {
        if (!ApertureApp.of(ctx).settings().haptics()) return;
        Vibrator v = ctx.getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) return;
        VibrationAttributes touch = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH);
        if (v.areAllPrimitivesSupported(primitive)) {
            v.vibrate(VibrationEffect.startComposition().addPrimitive(primitive).compose(), touch);
        } else {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK), touch);
        }
    }
}
