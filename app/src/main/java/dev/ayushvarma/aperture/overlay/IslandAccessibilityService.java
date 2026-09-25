package dev.ayushvarma.aperture.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.AttachedSurfaceControl;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.IslandHub;
import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.core.engine.IslandEngine;
import dev.ayushvarma.aperture.core.engine.IslandState;
import dev.ayushvarma.aperture.core.geometry.GeometryCalculator;
import dev.ayushvarma.aperture.core.geometry.IslandGeometry;
import dev.ayushvarma.aperture.device.DisplayReader;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.source.SystemEvents;
import dev.ayushvarma.aperture.theme.IslandPalette;
import dev.ayushvarma.aperture.ui.ReplyActivity;

/**
 * Hosts the island window (ADR-001: TYPE_ACCESSIBILITY_OVERLAY, the only type that draws over the status bar).
 * Window strategy per ADR-002: one window sized for every presentation with a public touchable region that
 * follows the island, so taps elsewhere reach the app below and the window never resizes mid-animation.
 * Reads only window-state events (foreground package) and never window content.
 */
public final class IslandAccessibilityService extends AccessibilityService
        implements IslandView.Host, IslandEngine.Listener, IslandSettings.Listener, IslandHub.ExpandListener {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<String> cameraPackages = new HashSet<>();
    private final Set<String> inputMethods = new HashSet<>();
    private final Runnable autoCollapse = () -> setExpanded(false);
    private final Runnable themeReread = this::refresh;
    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private IslandView view;
    private Actions actions;
    private SystemEvents systemEvents;
    private final android.content.BroadcastReceiver screenReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context c, Intent i) {
            updateLocked();
        }
    };
    private IslandHub hub;
    private IslandSettings settings;
    private WindowInsets lastInsets;
    private boolean cameraForeground;

    @Override
    protected void onServiceConnected() {
        hub = ApertureApp.of(this).hub();
        settings = ApertureApp.of(this).settings();
        wm = getSystemService(WindowManager.class);
        actions = new Actions(this);
        view = new IslandView(this, this, actions);
        // Note: an overlay above the status bar receives no status-bar insets (measured: always 0 and "not
        // visible"), so its own insets cannot detect fullscreen apps. Only the privacy bounds are read here.
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            lastInsets = insets;
            return insets;
        });
        lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);
        lp.setTitle("Aperture island");
        loadCameraPackages();
        for (android.view.inputmethod.InputMethodInfo imi : getSystemService(
                android.view.inputmethod.InputMethodManager.class).getInputMethodList()) {
            inputMethods.add(imi.getPackageName());
        }
        refresh();
        wm.addView(view, lp);
        hub.addListener(this);
        hub.addExpandListener(this);
        settings.addListener(this);
        view.setState(hub.state());
        systemEvents = new SystemEvents(this);
        systemEvents.register();
        android.content.IntentFilter screen = new android.content.IntentFilter(Intent.ACTION_SCREEN_ON);
        screen.addAction(Intent.ACTION_SCREEN_OFF);
        screen.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, screen, RECEIVER_NOT_EXPORTED);
        updateLocked();
    }

    @Override
    public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if (hub != null) {
            hub.removeListener(this);
            hub.removeExpandListener(this);
        }
        if (settings != null) settings.removeListener(this);
        if (systemEvents != null) {
            systemEvents.unregister();
            unregisterReceiver(screenReceiver);
        }
        if (actions != null) actions.release();
        if (view != null && view.isAttachedToWindow()) wm.removeViewImmediate(view);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        // The shade, dialogs, the keyboard and our own settings screens don't change which app the user is in.
        if (pkg.equals("com.android.systemui") || pkg.equals(getPackageName()) || inputMethods.contains(pkg)) return;
        view.setForegroundPackage(pkg);
        boolean camera = cameraPackages.contains(pkg);
        if (camera != cameraForeground) {
            cameraForeground = camera;
            updateSuppression();
        }
    }

    @Override
    public void onInterrupt() {
        // No feedback to interrupt: the service only draws.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Colors are already fresh here (Phase 0 V3). Re-read once more shortly after, because contrast changes
        // deliver their callback before resources update.
        refresh();
        main.removeCallbacks(themeReread);
        main.postDelayed(themeReread, 500);
    }

    @Override
    public void onStateChanged(IslandState state) {
        view.setState(state);
        scheduleCollapse();
    }

    @Override
    public void onExpandRequest(String activityId) {
        IslandState s = hub.state();
        if (s.compact != null && s.compact.id.equals(activityId)) setExpanded(true);
    }

    @Override
    public void onSettingsChanged() {
        refresh();
    }

    /** Recomputes geometry and palette from live measurements (rules G5-G7, Section 9.5). */
    private void refresh() {
        IslandGeometry g = GeometryCalculator.compute(DisplayReader.read(this, lastInsets), settings.calibration());
        IslandPalette palette = IslandPalette.from(this, settings.body() == IslandSettings.Body.TONAL);
        if (lp.height != g.windowHeight) {
            lp.height = g.windowHeight;
            if (view.isAttachedToWindow()) wm.updateViewLayout(view, lp);
        }
        view.configure(g, palette, settings);
    }

    private void updateLocked() {
        view.setLocked(getSystemService(android.app.KeyguardManager.class).isKeyguardLocked());
    }

    private void updateSuppression() {
        view.setSuppressed(cameraForeground);
    }

    private void loadCameraPackages() {
        PackageManager pm = getPackageManager();
        for (String action : new String[]{MediaStore.ACTION_IMAGE_CAPTURE, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
                MediaStore.INTENT_ACTION_VIDEO_CAMERA}) {
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(action), 0)) {
                cameraPackages.add(ri.activityInfo.packageName);
            }
        }
    }

    private void scheduleCollapse() {
        main.removeCallbacks(autoCollapse);
        int seconds = settings.expandedSeconds();
        if (view.isExpanded() && seconds > 0) main.postDelayed(autoCollapse, seconds * 1000L);
    }

    // ---- IslandView.Host -----------------------------------------------------------------------------

    @Override
    public void onTouchRegion(Region region) {
        AttachedSurfaceControl sc = view.getRootSurfaceControl();
        if (sc != null) sc.setTouchableRegion(region);
    }

    @Override
    public void dismissTransient() {
        hub.engine().dismissTransient();
    }

    @Override
    public void setExpanded(boolean expanded) {
        view.setExpanded(expanded);
        scheduleCollapse();
    }

    @Override
    public void cycle(int direction) {
        hub.engine().cycle(direction);
    }

    @Override
    public void reply(IslandActivity activity) {
        hub.engine().holdTransient(60_000);
        startActivity(ReplyActivity.intent(this, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void doubleTapIdle() {
        switch (settings.doubleTap()) {
            case FLASHLIGHT:
                actions.toggleTorch();
                break;
            case RESUME_MEDIA:
                actions.resumeMedia();
                break;
            default:
                break;
        }
    }

    @Override
    public void holdTransient() {
        hub.engine().holdTransient(3_000);
    }
}
