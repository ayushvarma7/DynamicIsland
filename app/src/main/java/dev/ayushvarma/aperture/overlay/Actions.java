package dev.ayushvarma.aperture.overlay;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;

/** Everything the island does to the outside world. Runs in the accessibility service's context. */
public final class Actions {
    private final Context ctx;
    private String torchId;
    private boolean torchOn;

    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (cameraId.equals(torchId)) torchOn = enabled;
        }
    };

    public Actions(Context ctx) {
        this.ctx = ctx;
        CameraManager cm = ctx.getSystemService(CameraManager.class);
        try {
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) {
                    torchId = id;
                    break;
                }
            }
        } catch (CameraAccessException ignored) {
            torchId = null;
        }
        if (torchId != null) cm.registerTorchCallback(torchCallback, new Handler(Looper.getMainLooper()));
    }

    public void release() {
        if (torchId != null) ctx.getSystemService(CameraManager.class).unregisterTorchCallback(torchCallback);
    }

    /**
     * Sends an app's PendingIntent. A system-bound accessibility service may start activities from the
     * background, but since Android 14 the sender must opt in to lending that privilege.
     */
    boolean send(PendingIntent pi) {
        if (pi == null) return false;
        try {
            ActivityOptions opts = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            pi.send(ctx, 0, null, null, null, null, opts.toBundle());
            return true;
        } catch (PendingIntent.CanceledException e) {
            return false;
        }
    }

    void startActivity(Intent intent) {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /**
     * Opens SystemUI's output picker for the playing app (Phase 0 V6: works for other packages, not public
     * API). Falls back to the public volume panel when the receiver is missing.
     */
    void outputSwitcher(String pkg) {
        Intent i = new Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                .setPackage("com.android.systemui").putExtra("package_name", pkg);
        if (!ctx.getPackageManager().queryBroadcastReceivers(i, 0).isEmpty()) {
            ctx.sendBroadcast(i);
        } else {
            startActivity(new Intent(Settings.Panel.ACTION_VOLUME));
        }
    }

    boolean hasTorch() {
        return torchId != null;
    }

    void toggleTorch() {
        if (torchId == null) return;
        try {
            ctx.getSystemService(CameraManager.class).setTorchMode(torchId, !torchOn);
        } catch (CameraAccessException ignored) {
            // Camera in use by another app: the torch is unavailable, nothing to do.
        }
    }

    /** Phase 0 V5: resume the last media app the way a Bluetooth headset does. */
    void resumeMedia() {
        AudioManager am = ctx.getSystemService(AudioManager.class);
        long t = SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0));
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0));
    }

    static void haptic(View v, int constant, boolean enabled) {
        if (enabled) v.performHapticFeedback(constant);
    }
}
