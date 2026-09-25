package dev.ayushvarma.aperture.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.core.engine.IslandEngine;
import dev.ayushvarma.aperture.core.geometry.GeometryCalculator;
import dev.ayushvarma.aperture.core.geometry.IslandGeometry;
import dev.ayushvarma.aperture.device.DisplayReader;
import dev.ayushvarma.aperture.overlay.Actions;
import dev.ayushvarma.aperture.overlay.IslandView;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.theme.IslandPalette;

/**
 * A looping, live preview of the island on a mock phone top. It runs the real renderer at the real geometry of
 * this phone, scaled down, so it always shows the chosen style, colors and motion (no video to go stale):
 * resting ring, then a song arrives, expands, collapses, and ends.
 */
final class LivePreview extends FrameLayout implements IslandSettings.Listener {
    // How long each state lasts: pill, expanded, pill again, resting ring, pause before the loop restarts.
    private static final long[] STEPS_MS = {2500, 4000, 1800, 1400, 600};

    private final IslandSettings settings;
    private final Actions actions;
    private final IslandView island;
    private final IslandEngine engine = new IslandEngine(System::currentTimeMillis);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Stage stage;
    private IslandGeometry geometry;
    private int step;
    private boolean running;

    private final Runnable next = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            switch (step) {
                case 0:
                    engine.upsert(DemoContent.song(getContext()));
                    break;
                case 1:
                    island.setExpanded(true);
                    break;
                case 2:
                    island.setExpanded(false);
                    break;
                case 3:
                    engine.remove(DemoContent.SONG_ID);
                    break;
                default:
                    break;
            }
            island.setState(engine.state());
            long delay = STEPS_MS[step];
            step = (step + 1) % STEPS_MS.length;
            handler.postDelayed(this, delay);
        }
    };

    /** Mock phone top: wallpaper gradient from the system palette, a status bar, the camera hole. */
    private final class Stage extends FrameLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Stage(Context c) {
            super(c);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            paint.setShader(new LinearGradient(0, 0, w, h,
                    getContext().getColor(android.R.color.system_accent1_600),
                    getContext().getColor(android.R.color.system_accent3_800), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            if (geometry == null) return;
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(geometry.compactHeight * 0.42f);
            paint.setTypeface(dev.ayushvarma.aperture.theme.Fonts.medium());
            String time = DateFormat.getTimeFormat(getContext()).format(new java.util.Date());
            c.drawText(time, geometry.expandedLeft + geometry.innerPadding, geometry.centerY + paint.getTextSize() * 0.35f, paint);
            float x = w - geometry.expandedLeft - geometry.innerPadding;
            float barH = geometry.compactHeight * 0.3f;
            for (int i = 0; i < 3; i++) {
                c.drawRoundRect(x - (i + 1) * barH * 0.9f, geometry.centerY - barH / 2, x - (i + 1) * barH * 0.9f + barH * 0.55f,
                        geometry.centerY + barH / 2, barH * 0.2f, barH * 0.2f, paint);
            }
            paint.setColor(0xFF000000);
            c.drawCircle(geometry.centerX, geometry.centerY, geometry.cutoutRadius, paint);
        }
    }

    LivePreview(Context ctx, IslandSettings settings) {
        super(ctx);
        this.settings = settings;
        this.actions = new Actions(ctx);
        stage = new Stage(ctx);
        island = new IslandView(ctx, new PreviewHost(), actions);
        stage.addView(island);
        addView(stage);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Ui.dp(getContext(), 20));
            }
        });
        configure();
    }

    private void configure() {
        geometry = GeometryCalculator.compute(DisplayReader.read(getContext()), settings.calibration());
        island.configure(geometry, IslandPalette.from(getContext(), settings.body() == IslandSettings.Body.TONAL), settings);
        stage.invalidate();
    }

    private int stageWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int stageHeight() {
        return geometry == null ? 600 : geometry.windowHeight + Math.round(geometry.compactHeight);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        float scale = w / (float) stageWidth();
        stage.measure(MeasureSpec.makeMeasureSpec(stageWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(stageHeight(), MeasureSpec.EXACTLY));
        stage.setPivotX(0);
        stage.setPivotY(0);
        stage.setScaleX(scale);
        stage.setScaleY(scale);
        setMeasuredDimension(w, Math.round(stageHeight() * scale));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        stage.layout(0, 0, stageWidth(), stageHeight());
        island.layout(0, 0, stageWidth(), stageHeight());
    }

    /** The preview is for looking at, not operating. */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        settings.addListener(this);
        running = true;
        step = 0;
        handler.postDelayed(next, 600);
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        settings.removeListener(this);
        actions.release();
        super.onDetachedFromWindow();
    }

    /** Pauses the loop while the screen is not visible (no animation work in the background). */
    void setRunning(boolean run) {
        if (run == running) return;
        running = run;
        handler.removeCallbacksAndMessages(null);
        if (run) {
            step = 0;
            engine.remove(DemoContent.SONG_ID);
            island.setState(engine.state());
            handler.postDelayed(next, 600);
        }
    }

    @Override
    public void onSettingsChanged() {
        configure();
    }

    private final class PreviewHost implements IslandView.Host {
        @Override
        public void onTouchRegion(Region region) {
        }

        @Override
        public void dismissTransient() {
        }

        @Override
        public void setExpanded(boolean expanded) {
            island.setExpanded(expanded);
        }

        @Override
        public void cycle(int direction) {
        }

        @Override
        public void reply(IslandActivity activity) {
        }

        @Override
        public void doubleTapIdle() {
        }

        @Override
        public void holdTransient() {
        }
    }
}
