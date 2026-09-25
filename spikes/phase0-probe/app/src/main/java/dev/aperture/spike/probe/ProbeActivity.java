package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;
import static dev.aperture.spike.probe.Probe.safe;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaRouter2;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for adb-driven measurements:
 *   am start -n dev.aperture.spike.probe/.ProbeActivity --es action probe|touch_target|post_live_update|
 *            cancel_live_update|post_heads_up|output_switcher|media_output_broadcast|request_post_notifications
 *            [--ez finish true] [--ei progress N] [--es pkg P]
 * Also the full-screen touch target for the overlay pass-through test (logs touch_activity).
 */
public class ProbeActivity extends Activity {
    private TouchTargetView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getWindow().setAttributes(lp);
        view = new TouchTargetView(this);
        setContentView(view);
        view.post(() -> handle(getIntent()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    private void handle(Intent intent) {
        String action = intent.getStringExtra("action");
        if (action == null) action = "probe";
        Probe.log("activity_action", "action", action);
        switch (action) {
            case "probe":
                runProbe();
                if (intent.getBooleanExtra("finish", false)) finish();
                break;
            case "request_post_notifications":
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                break;
            case "touch_target":
                break;
            case "post_live_update":
                LiveUpdatePoster.post(this, intent.getIntExtra("progress", 40));
                break;
            case "cancel_live_update":
                LiveUpdatePoster.cancel(this);
                break;
            case "post_heads_up":
                LiveUpdatePoster.postHeadsUp(this);
                break;
            case "output_switcher":
                Probe.log("output_switcher", "shown", safe(() -> MediaRouter2.getInstance(this).showSystemOutputSwitcher()));
                break;
            case "media_output_broadcast":
                sendMediaOutputBroadcast(intent.getStringExtra("pkg"));
                break;
            default:
                Probe.log("activity_action_unknown", "action", action);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Probe.log("permission_result", "granted", grantResults.length > 0 && grantResults[0] == 0);
    }

    private void runProbe() {
        JSONObject r = DeviceProbe.collect(this);
        put(r, "activityRootInsets", safe(() -> DeviceProbe.rootInsets(view)));
        File f = new File(getFilesDir(), "probe_activity.json");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(r.toString(2).getBytes(StandardCharsets.UTF_8));
            Probe.log("probe_written", put(put(obj(), "path", f.getAbsolutePath()), "bytes", f.length()));
        } catch (Exception e) {
            Probe.log("probe_write_error", "error", e.toString());
        }
    }

    /** Can a third-party app open SystemUI's media output dialog for another package? */
    private void sendMediaOutputBroadcast(String pkg) {
        Intent i = new Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                .setPackage("com.android.systemui")
                .putExtra("package_name", pkg == null ? getPackageName() : pkg);
        try {
            sendBroadcast(i);
            Probe.log("media_output_broadcast_sent", "pkg", pkg);
        } catch (RuntimeException e) {
            Probe.log("media_output_broadcast_error", "error", e.toString());
        }
    }

    @SuppressLint("ViewConstructor")
    static final class TouchTargetView extends View {
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

        TouchTargetView(Context ctx) {
            super(ctx);
            setBackgroundColor(0xFFBDBDBD);
            text.setColor(0xFF202124);
            text.setTextSize(40f);
            text.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawText("Aperture Probe: touch target", getWidth() / 2f, getHeight() / 2f, text);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                Probe.log("touch_activity", put(put(obj(), "x", e.getRawX()), "y", e.getRawY()));
            }
            return true;
        }
    }
}
