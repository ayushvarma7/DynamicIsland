package dev.ayushvarma.aperture.overlay;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

import dev.ayushvarma.aperture.R;
import dev.ayushvarma.aperture.core.engine.ActivityKind;
import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.core.engine.IslandState;
import dev.ayushvarma.aperture.core.format.TimeText;
import dev.ayushvarma.aperture.core.geometry.IslandGeometry;
import dev.ayushvarma.aperture.core.media.PlaybackMath;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.source.Payload;
import dev.ayushvarma.aperture.theme.IslandPalette;

/**
 * Draws the island and runs its motion. The container (position, size, corner) moves on spatial springs that
 * always start from the current value and velocity (Section 7.3 rules 1 and 5). Content fades out on an effects
 * spring and the new content fades in once the container is 60 % of the way to its target (rules 2-3), so the
 * pill never disappears or flashes.
 */
@SuppressLint("ViewConstructor")
final class IslandView extends View {
    interface Host {
        void onTouchRegion(Region region);

        void dismissTransient();

        void setExpanded(boolean expanded);

        void cycle(int direction);

        void reply(IslandActivity activity);

        void doubleTapIdle();

        /** Keeps a transient on screen while the user is touching it. */
        void holdTransient();
    }

    enum Presentation { HIDDEN, IDLE, COMPACT, CARD }

    /** What is drawn: a presentation plus the activity it shows. */
    private static final class Content {
        final Presentation presentation;
        final IslandActivity activity;

        Content(Presentation presentation, IslandActivity activity) {
            this.presentation = presentation;
            this.activity = activity;
        }

        boolean sameSlot(Content o) {
            return o != null && presentation == o.presentation
                    && (activity == null ? o.activity == null : o.activity != null && activity.id.equals(o.activity.id));
        }
    }

    private interface Hit {
        void run();
    }

    private final Host host;
    private final Actions actions;
    private final Glyphs glyphs = new Glyphs();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint image = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final RectF tmp = new RectF();
    private final Path clip = new Path();
    private final Matrix matrix = new Matrix();
    private final List<RectF> hitRects = new ArrayList<>();
    private final List<Hit> hitActions = new ArrayList<>();
    private final GestureDetector gestures;

    private final FloatValueHolder left = new FloatValueHolder();
    private final FloatValueHolder top = new FloatValueHolder();
    private final FloatValueHolder width = new FloatValueHolder();
    private final FloatValueHolder height = new FloatValueHolder();
    private final FloatValueHolder radius = new FloatValueHolder();
    private final FloatValueHolder bodyAlpha = new FloatValueHolder();
    private final FloatValueHolder contentAlpha = new FloatValueHolder(1f);
    private final FloatValueHolder bubbleScale = new FloatValueHolder();
    private final List<SpringAnimation> spatial = new ArrayList<>();
    private final List<SpringAnimation> effects = new ArrayList<>();
    private final SpringAnimation leftAnim;
    private final SpringAnimation topAnim;
    private final SpringAnimation widthAnim;
    private final SpringAnimation heightAnim;
    private final SpringAnimation radiusAnim;
    private final SpringAnimation bodyAlphaAnim;
    private final SpringAnimation contentAlphaAnim;
    private final SpringAnimation bubbleAnim;

    private IslandGeometry g;
    private IslandPalette palette;
    private IslandSettings settings;
    private IslandState state = IslandState.EMPTY;
    private boolean expanded;
    private boolean suppressed;
    private boolean initialized;
    private Content rendered;
    private Content pending;
    private float startDistance;
    private final RectF target = new RectF();
    private float targetRadius;
    private final Region region = new Region();
    private final RectF lastRegion = new RectF();

    IslandView(Context ctx, Host host, Actions actions) {
        super(ctx);
        this.host = host;
        this.actions = actions;
        leftAnim = spring(left, true);
        topAnim = spring(top, true);
        widthAnim = spring(width, true);
        heightAnim = spring(height, true);
        radiusAnim = spring(radius, true);
        bodyAlphaAnim = spring(bodyAlpha, false);
        contentAlphaAnim = spring(contentAlpha, false);
        bubbleAnim = spring(bubbleScale, true);
        gestures = new GestureDetector(ctx, new Gestures());
        stroke.setStyle(Paint.Style.STROKE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private SpringAnimation spring(FloatValueHolder holder, boolean isSpatial) {
        SpringAnimation a = new SpringAnimation(holder);
        a.setSpring(new SpringForce());
        a.setMinimumVisibleChange(isSpatial ? DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
                : DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
        a.addUpdateListener((anim, value, velocity) -> onFrame());
        a.addEndListener((anim, canceled, value, velocity) -> onFrame());
        (isSpatial ? spatial : effects).add(a);
        return a;
    }

    // ---- Inputs ------------------------------------------------------------------------------------

    void configure(IslandGeometry geometry, IslandPalette palette, IslandSettings settings) {
        this.g = geometry;
        this.palette = palette;
        this.settings = settings;
        applyMotion();
        retarget();
    }

    void setState(IslandState s) {
        IslandActivity oldCompact = state.compact;
        state = s;
        if (expanded && (s.compact == null || oldCompact == null || !s.compact.id.equals(oldCompact.id))) {
            expanded = false;
        }
        retarget();
    }

    void setExpanded(boolean e) {
        if (expanded == e) return;
        expanded = e;
        retarget();
    }

    boolean isExpanded() {
        return expanded;
    }

    /** Fullscreen, camera app, landscape: the island steps aside (Section 6.6.5). */
    void setSuppressed(boolean s) {
        if (suppressed == s) return;
        suppressed = s;
        retarget();
    }

    /** Section 7.3 motion schemes. Stiffness/damping per scheme; reduced motion snaps (rule 9). */
    private void applyMotion() {
        float stiffness;
        float damping;
        switch (settings.motion()) {
            case STANDARD:
                stiffness = 700f;
                damping = 0.9f;
                break;
            case FLUID:
                stiffness = 195f; // ~0.45 s response, near-critical
                damping = 0.88f;
                break;
            default:
                stiffness = 380f; // expressive spatial default: visible overshoot
                damping = 0.8f;
                break;
        }
        for (SpringAnimation a : spatial) a.getSpring().setStiffness(stiffness).setDampingRatio(damping);
        for (SpringAnimation a : effects) a.getSpring().setStiffness(1600f).setDampingRatio(1f);
    }

    // ---- Targeting ---------------------------------------------------------------------------------

    private Content decide() {
        if (g == null || suppressed || !g.visible) return new Content(Presentation.HIDDEN, null);
        IslandActivity t = state.transientActivity;
        if (t != null) {
            // System events (charging, battery) are glanceable pills; messages expand into a card.
            return new Content(t.kind == ActivityKind.SYSTEM ? Presentation.COMPACT : Presentation.CARD, t);
        }
        IslandActivity c = state.compact;
        if (c != null) {
            boolean card = expanded || c.kind == ActivityKind.INCOMING_CALL;
            return new Content(card ? Presentation.CARD : Presentation.COMPACT, c);
        }
        if (settings.idleStyle() == IslandSettings.IdleStyle.HIDDEN && !settings.calibrationPreview()) {
            return new Content(Presentation.HIDDEN, null);
        }
        return new Content(Presentation.IDLE, null);
    }

    private void retarget() {
        if (g == null) return;
        Content next = decide();
        float hc = g.compactHeight;
        float alpha = 1f;
        switch (next.presentation) {
            case CARD:
                target.set(g.expandedLeft, g.expandedTop, g.expandedLeft + g.expandedWidth,
                        g.expandedTop + cardHeight(next.activity));
                targetRadius = g.expandedCorner;
                break;
            case COMPACT:
                target.set(g.compactLeft(), g.compactTop(), g.compactLeft() + g.compactWidth(), g.compactTop() + hc);
                targetRadius = hc / 2f;
                break;
            case IDLE:
                target.set(g.centerX - hc / 2f, g.compactTop(), g.centerX + hc / 2f, g.compactTop() + hc);
                targetRadius = hc / 2f;
                boolean ring = settings.idleStyle() == IslandSettings.IdleStyle.RING || settings.calibrationPreview();
                alpha = ring ? 1f : 0f;
                break;
            default:
                // Collapse into the camera hole, then fade (rule 6).
                target.set(g.centerX - g.cutoutRadius, g.centerY - g.cutoutRadius, g.centerX + g.cutoutRadius,
                        g.centerY + g.cutoutRadius);
                targetRadius = g.cutoutRadius;
                alpha = 0f;
                break;
        }
        boolean snap = !initialized || !ValueAnimator.areAnimatorsEnabled();
        if (snap) {
            for (SpringAnimation a : spatial) a.cancel();
            left.setValue(target.left);
            top.setValue(target.top);
            width.setValue(target.width());
            height.setValue(target.height());
            radius.setValue(targetRadius);
            bodyAlpha.setValue(alpha);
            initialized = true;
        } else {
            startDistance = distance();
            leftAnim.animateToFinalPosition(target.left);
            topAnim.animateToFinalPosition(target.top);
            widthAnim.animateToFinalPosition(target.width());
            heightAnim.animateToFinalPosition(target.height());
            radiusAnim.animateToFinalPosition(targetRadius);
            bodyAlphaAnim.animateToFinalPosition(alpha);
        }
        boolean bubbles = next.presentation == Presentation.COMPACT && !state.minimal.isEmpty();
        if (snap) bubbleScale.setValue(bubbles ? 1f : 0f);
        else bubbleAnim.animateToFinalPosition(bubbles ? 1f : 0f);

        if (rendered == null || next.sameSlot(rendered) || snap) {
            // Same slot (an update such as a new progress value): swap in place, no fade.
            rendered = next;
            pending = null;
            contentAlpha.setValue(1f);
        } else {
            pending = next;
            contentAlphaAnim.animateToFinalPosition(0f);
        }
        updateDescription();
        onFrame();
    }

    private float distance() {
        return Math.abs(target.left - left.getValue()) + Math.abs(target.top - top.getValue())
                + Math.abs(target.width() - width.getValue()) + Math.abs(target.height() - height.getValue());
    }

    private void onFrame() {
        if (pending != null) {
            float progress = startDistance < 1f ? 1f : 1f - distance() / startDistance;
            if (contentAlpha.getValue() <= 0.05f && progress >= 0.6f) {
                rendered = pending;
                pending = null;
                // This can run inside the fade-out spring's own end callback, where restarting that spring
                // is dropped (the new content then stayed invisible). Start the fade-in on the next frame.
                postOnAnimation(() -> contentAlphaAnim.animateToFinalPosition(1f));
            }
        }
        updateRegion();
        invalidate();
    }

    private float cardHeight(IslandActivity a) {
        if (a == null) return g.detentSmall;
        switch (a.kind) {
            case MEDIA:
            case INCOMING_CALL:
            case ONGOING_CALL:
                return g.detentMedium;
            case NOTIFICATION:
                Payload p = payload(a);
                return p != null && (p.replyAction != null || p.open != null) ? g.detentMedium : g.detentSmall;
            default:
                return a.progress >= 0 || a.chronometerBaseMs > 0 ? g.detentMedium : g.detentSmall;
        }
    }

    /** Touchable area = island (and bubbles) + 6 dp slop; empty when nothing can be touched. */
    private void updateRegion() {
        currentBody(body);
        Content c = rendered;
        boolean touchable = c != null && c.presentation != Presentation.HIDDEN;
        if (!touchable) {
            if (!lastRegion.isEmpty()) {
                lastRegion.setEmpty();
                region.setEmpty();
                host.onTouchRegion(region);
            }
            return;
        }
        float slop = 6 * getResources().getDisplayMetrics().density;
        tmp.set(body);
        if (bubbleScale.getValue() > 0.01f) {
            float extent = g.bubbleGap + g.compactHeight;
            tmp.left -= state.minimal.size() > 1 ? extent : 0;
            tmp.right += extent;
        }
        tmp.inset(-slop, -slop);
        tmp.top = Math.max(0, tmp.top);
        if (Math.abs(tmp.left - lastRegion.left) + Math.abs(tmp.right - lastRegion.right)
                + Math.abs(tmp.top - lastRegion.top) + Math.abs(tmp.bottom - lastRegion.bottom) < 2f) {
            return;
        }
        lastRegion.set(tmp);
        region.set(Math.round(tmp.left), Math.round(tmp.top), Math.round(tmp.right), Math.round(tmp.bottom));
        host.onTouchRegion(region);
    }

    private void currentBody(RectF out) {
        out.set(left.getValue(), top.getValue(), left.getValue() + Math.max(0, width.getValue()),
                top.getValue() + Math.max(0, height.getValue()));
    }

    // ---- Drawing -----------------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas c) {
        if (g == null || palette == null) return;
        hitRects.clear();
        hitActions.clear();
        currentBody(body);
        float r = Math.min(radius.getValue(), Math.min(body.width(), body.height()) / 2f);
        float a = clamp01(bodyAlpha.getValue());
        Content content = rendered;
        boolean card = content != null && content.presentation == Presentation.CARD;

        if (a > 0.01f) {
            fill.setColor(card ? palette.expandedBody : palette.body);
            fill.setAlpha(Math.round(255 * a));
            c.drawRoundRect(body, r, r, fill);
            if (card && settings.body() == IslandSettings.Body.BLACK) {
                stroke.setColor(palette.keyline);
                stroke.setStrokeWidth(dp(1));
                c.drawRoundRect(body, r, r, stroke);
            }
        }
        drawBubbles(c);
        if (settings.calibrationPreview()) {
            stroke.setColor(palette.primary);
            stroke.setStrokeWidth(dp(1));
            c.drawCircle(g.centerX, g.centerY, g.cutoutRadius, stroke);
        }
        if (content == null || content.activity == null || content.presentation == Presentation.HIDDEN) return;

        float ca = clamp01(contentAlpha.getValue());
        if (ca < 0.01f) return;
        int save = c.saveLayerAlpha(body, Math.round(255 * ca));
        clip.reset();
        clip.addRoundRect(body, r, r, Path.Direction.CW);
        c.clipPath(clip);
        if (card) drawCard(c, content.activity);
        else drawCompact(c, content.activity);
        c.restoreToCount(save);
        scheduleTicks(content.activity, card);
    }

    /** Redraw only while something visibly changes (Section 12: zero idle wakeups). */
    private void scheduleTicks(IslandActivity a, boolean card) {
        boolean bars = a.kind == ActivityKind.MEDIA && a.playing;
        if (bars) {
            postInvalidateOnAnimation();
        } else if (a.chronometerBaseMs > 0) {
            postInvalidateDelayed(500);
        }
        if (card && a.kind == ActivityKind.MEDIA && a.playing) postInvalidateDelayed(250);
    }

    private void drawCompact(Canvas c, IslandActivity a) {
        float hc = g.compactHeight;
        float cy = body.centerY();
        float icon = hc - 2 * g.gap - dp(4);
        float leadEnd = g.centerX - g.keepOutRadius;
        float trailStart = g.centerX + g.keepOutRadius;
        boolean leadingRoom = leadEnd - body.left >= icon + dp(4);
        float iconCx = leadingRoom ? body.left + g.gap + dp(2) + icon / 2f : trailStart + icon / 2f;
        drawLeading(c, a, iconCx, cy, icon);
        float trailLeft = leadingRoom ? trailStart : trailStart + icon + dp(6);
        float trailRight = body.right - g.gap - dp(4);
        drawTrailing(c, a, trailLeft, trailRight, cy, hc);
    }

    private void drawLeading(Canvas c, IslandActivity a, float cx, float cy, float size) {
        Payload p = payload(a);
        switch (a.kind) {
            case INCOMING_CALL:
            case ONGOING_CALL:
                if (p != null && p.art != null) {
                    drawCircleBitmap(c, p.art, cx, cy, size / 2f);
                } else {
                    fill.setColor(palette.accept);
                    c.drawCircle(cx, cy, size / 2f, fill);
                    fill.setColor(0xFF000000);
                    glyphs.phone(c, cx, cy, size * 0.6f, fill, false);
                }
                return;
            case SYSTEM:
                fill.setColor(a.id.startsWith("sys:low") ? palette.error : palette.accept);
                glyphs.bolt(c, cx, cy, size * 0.8f, fill);
                return;
            default:
                if (p != null && p.art != null) {
                    if (a.kind == ActivityKind.MEDIA) drawRoundBitmap(c, p.art, cx, cy, size, size * 0.28f);
                    else drawCircleBitmap(c, p.art, cx, cy, size / 2f);
                } else if (p != null && p.appIcon != null) {
                    drawDrawable(c, p.appIcon, cx, cy, size);
                } else {
                    fill.setColor(palette.primary);
                    c.drawCircle(cx, cy, size / 4f, fill);
                }
        }
    }

    private void drawTrailing(Canvas c, IslandActivity a, float l, float r, float cy, float hc) {
        if (r - l < dp(8)) return;
        float cx = r - Math.min(r - l, hc * 0.9f) / 2f;
        switch (a.kind) {
            case MEDIA:
                if (a.playing) drawBars(c, cx, cy, hc * 0.42f, palette.primary);
                else {
                    fill.setColor(palette.onSurfaceVariant);
                    glyphs.pause(c, cx, cy, hc * 0.4f, fill);
                }
                return;
            case INCOMING_CALL:
                fill.setColor(palette.accept);
                float pulse = 0.85f + 0.15f * (float) Math.sin(SystemClock.uptimeMillis() / 180.0);
                glyphs.phone(c, cx, cy, hc * 0.45f * pulse, fill, false);
                postInvalidateOnAnimation();
                return;
            case SYSTEM:
                drawText(c, a.shortText != null ? a.shortText : a.title, l, r, cy, hc * 0.36f,
                        a.id.startsWith("sys:low") ? palette.error : palette.accept, true);
                return;
            case NOTIFICATION:
                drawText(c, a.title, l, r, cy, hc * 0.32f, palette.onSurface, true);
                return;
            default:
                CharSequence label = trailingLabel(a);
                if (label != null) {
                    drawText(c, label, l, r, cy, hc * 0.36f, palette.primary, true);
                } else if (a.progress >= 0) {
                    drawRing(c, cx, cy, hc * 0.26f, a.progress);
                }
        }
    }

    /** Compact trailing text: live chronometer first, then the app's short critical text, then percent. */
    private CharSequence trailingLabel(IslandActivity a) {
        if (a.chronometerBaseMs > 0) {
            long now = System.currentTimeMillis();
            return a.countDown ? TimeText.countdown(a.chronometerBaseMs - now) : TimeText.clock(now - a.chronometerBaseMs);
        }
        if (a.shortText != null && a.shortText.length() > 0) return a.shortText;
        if (a.progress >= 0 && a.kind == ActivityKind.DOWNLOAD) return Math.round(a.progress * 100) + "%";
        return null;
    }

    private void drawCard(Canvas c, IslandActivity a) {
        Payload p = payload(a);
        float pad = g.innerPadding;
        float hc = g.compactHeight;
        float rowTop = body.top;
        float icon = hc - 2 * g.gap;
        float leadCx = body.left + pad + icon / 2f;
        drawLeading(c, a, leadCx, rowTop + hc / 2f, icon);
        // Trailing slot keeps the compact content in place (Section 3.2 rule 3: same info, same place).
        float trailStart = g.centerX + g.keepOutRadius;
        if (a.kind != ActivityKind.NOTIFICATION) drawTrailing(c, a, trailStart, body.right - pad, rowTop + hc / 2f, hc);

        float textLeft = body.left + pad;
        float textRight = body.right - pad;
        float y = rowTop + hc + dp(10);
        if (a.kind == ActivityKind.NOTIFICATION && p != null && p.appName != null) {
            drawTextTop(c, p.appName, textLeft + icon + dp(10), g.centerX - g.keepOutRadius - dp(4), rowTop + hc / 2f
                    - dp(8), dp(12), palette.onSurfaceVariant, false);
        }
        CharSequence title = a.title;
        if (a.kind == ActivityKind.INCOMING_CALL && title == null) title = getContext().getString(R.string.island_incoming_call);
        y = drawTextTop(c, title, textLeft, textRight, y, dp(17), palette.onSurface, true) + dp(4);
        CharSequence sub = a.text;
        if ((a.kind == ActivityKind.INCOMING_CALL || a.kind == ActivityKind.ONGOING_CALL) && p != null && p.appName != null) {
            sub = a.kind == ActivityKind.INCOMING_CALL
                    ? getContext().getString(R.string.island_call_from, p.appName) : p.appName;
        }
        if (sub != null) y = drawTextTop(c, sub, textLeft, textRight, y, dp(14), palette.onSurfaceVariant, false) + dp(6);

        float bottom = body.bottom - pad;
        float buttonH = dp(44);
        switch (a.kind) {
            case MEDIA:
                drawMediaControls(c, a, p, textLeft, textRight, y, bottom, buttonH);
                break;
            case INCOMING_CALL:
                if (p != null) {
                    float mid = (textLeft + textRight) / 2f;
                    button(c, textLeft, bottom - buttonH, mid - dp(6), bottom, palette.error, null, 2,
                            () -> { actions.send(p.decline); host.setExpanded(false); });
                    button(c, mid + dp(6), bottom - buttonH, textRight, bottom, palette.accept, null, 1,
                            () -> actions.send(p.answer));
                }
                break;
            case ONGOING_CALL:
                if (p != null && p.hangUp != null) {
                    float w = dp(120);
                    button(c, textRight - w, bottom - buttonH, textRight, bottom, palette.error, null, 2,
                            () -> actions.send(p.hangUp));
                }
                break;
            case NOTIFICATION:
                if (p != null) {
                    float x = textRight;
                    if (p.open != null) {
                        float w = dp(96);
                        button(c, x - w, bottom - buttonH, x, bottom, palette.buttonSecondary,
                                getContext().getString(R.string.island_open), 0, () -> open(a));
                        x -= w + dp(8);
                    }
                    if (p.replyAction != null) {
                        float w = dp(96);
                        button(c, x - w, bottom - buttonH, x, bottom, palette.primaryContainer,
                                getContext().getString(R.string.island_reply), 0, () -> host.reply(a));
                    }
                }
                break;
            default:
                if (a.progress >= 0) drawBar(c, textLeft, textRight, bottom - dp(6), a.progress);
                break;
        }
    }

    private void drawMediaControls(Canvas c, IslandActivity a, Payload p, float l, float r, float y, float bottom,
                                   float buttonH) {
        if (p == null) return;
        long now = SystemClock.elapsedRealtime();
        long pos = PlaybackMath.position(p.positionMs, p.positionUpdateElapsedMs, p.speed, a.playing, now, p.durationMs);
        float fraction = PlaybackMath.fraction(pos, p.durationMs);
        float barY = bottom - buttonH - dp(22);
        if (fraction >= 0) {
            // Missing duration hides the bar (Section 8): never a frozen bar.
            drawBar(c, l, r, barY, fraction);
            text.setTypeface(palette.typeface);
            text.setTextSize(dp(11));
            text.setColor(palette.onSurfaceVariant);
            text.setTextAlign(Paint.Align.LEFT);
            c.drawText(TimeText.clock(pos), l, barY + dp(16), text);
            text.setTextAlign(Paint.Align.RIGHT);
            c.drawText(TimeText.clock(p.durationMs), r, barY + dp(16), text);
            text.setTextAlign(Paint.Align.LEFT);
            // Tapping the bar seeks (handled in onTouchEvent); remember where it is this frame.
            scrubRect.set(l, barY - dp(14), r, barY + dp(14));
            scrubDuration = p.durationMs;
            scrubController = p.controller;
            scrubLeft = l;
            scrubSpan = r - l;
        } else {
            scrubController = null;
        }
        MediaController mc = p.controller;
        if (mc == null) return;
        MediaController.TransportControls tc = mc.getTransportControls();
        float cy = bottom - buttonH / 2f;
        float mid = (l + r) / 2f;
        float step = dp(64);
        float s = dp(26);
        fill.setColor(palette.onSurface);
        glyphs.next(c, mid - step, cy, s, fill, false);
        tmp.set(mid - step - buttonH / 2, cy - buttonH / 2, mid - step + buttonH / 2, cy + buttonH / 2);
        addHit(tmp, tc::skipToPrevious);
        fill.setColor(palette.primaryContainer);
        c.drawCircle(mid, cy, buttonH / 2f + dp(4), fill);
        fill.setColor(palette.onPrimaryContainer);
        if (a.playing) glyphs.pause(c, mid, cy, s, fill);
        else glyphs.play(c, mid, cy, s, fill);
        tmp.set(mid - buttonH / 2, cy - buttonH / 2, mid + buttonH / 2, cy + buttonH / 2);
        addHit(tmp, a.playing ? tc::pause : tc::play);
        fill.setColor(palette.onSurface);
        glyphs.next(c, mid + step, cy, s, fill, true);
        tmp.set(mid + step - buttonH / 2, cy - buttonH / 2, mid + step + buttonH / 2, cy + buttonH / 2);
        addHit(tmp, tc::skipToNext);
        // Output switcher on the right, the app's first custom action (like/thumbs up) on the left.
        fill.setColor(palette.onSurfaceVariant);
        glyphs.output(c, r - buttonH / 2, cy, dp(22), fill);
        tmp.set(r - buttonH, cy - buttonH / 2, r, cy + buttonH / 2);
        addHit(tmp, () -> actions.outputSwitcher(mc.getPackageName()));
        if (p.customAction != null && p.customActionIcon != null) {
            Drawable d = p.customActionIcon.mutate();
            d.setTint(palette.onSurfaceVariant);
            drawDrawable(c, d, l + buttonH / 2, cy, dp(24));
            tmp.set(l, cy - buttonH / 2, l + buttonH, cy + buttonH / 2);
            String action = p.customAction;
            addHit(tmp, () -> tc.sendCustomAction(action, null));
        }
    }

    private void button(Canvas c, float l, float t, float r, float b, int color, CharSequence label, int glyph,
                        Runnable action) {
        tmp.set(l, t, r, b);
        fill.setColor(color);
        c.drawRoundRect(tmp, tmp.height() / 2, tmp.height() / 2, fill);
        int on = color == palette.primaryContainer ? palette.onPrimaryContainer
                : color == palette.buttonSecondary ? palette.onSurface : 0xFF000000;
        if (label != null) {
            drawText(c, label, l + dp(8), r - dp(8), tmp.centerY(), dp(15), on, false, true);
        } else {
            fill.setColor(on);
            glyphs.phone(c, tmp.centerX(), tmp.centerY(), dp(24), fill, glyph == 2);
        }
        addHit(tmp, action);
    }

    private void addHit(RectF r, Runnable action) {
        hitRects.add(new RectF(r));
        hitActions.add(action == null ? () -> { } : action::run);
    }

    private void drawBubbles(Canvas c) {
        float s = bubbleScale.getValue();
        if (s < 0.01f || state.minimal.isEmpty()) return;
        float hc = g.compactHeight;
        for (int i = 0; i < state.minimal.size(); i++) {
            IslandActivity m = state.minimal.get(i);
            float cx = i == 0 ? body.right + g.bubbleGap + hc / 2f : body.left - g.bubbleGap - hc / 2f;
            float cy = body.centerY();
            float rr = hc / 2f * clamp01(s);
            fill.setColor(palette.body);
            fill.setAlpha(255);
            c.drawCircle(cx, cy, rr, fill);
            // Minimal shows live data when there is any (Section 3.2 rule 5), else the app icon.
            if (m.progress >= 0) {
                drawRing(c, cx, cy, rr * 0.6f, m.progress);
            } else if (m.kind == ActivityKind.MEDIA && m.playing) {
                drawBars(c, cx, cy, rr * 0.9f, palette.primary);
            } else {
                drawLeading(c, m, cx, cy, rr * 1.3f);
            }
            tmp.set(cx - rr, cy - rr, cx + rr, cy + rr);
            addHit(tmp, () -> open(m));
        }
    }

    /** Gate D5 option A: procedural bars from playback state, never frozen (static only when paused). */
    private void drawBars(Canvas c, float cx, float cy, float h, int color) {
        fill.setColor(color);
        float t = SystemClock.uptimeMillis() / 1000f;
        float w = h * 0.16f;
        float gap = h * 0.12f;
        float total = 4 * w + 3 * gap;
        float x = cx - total / 2f;
        for (int i = 0; i < 4; i++) {
            float level = 0.35f + 0.65f * Math.abs((float) Math.sin(t * (3.1f + i * 0.9f) + i * 1.7f));
            float bh = h * level;
            tmp.set(x, cy - bh / 2f, x + w, cy + bh / 2f);
            c.drawRoundRect(tmp, w / 2, w / 2, fill);
            x += w + gap;
        }
    }

    private void drawRing(Canvas c, float cx, float cy, float r, float progress) {
        stroke.setStrokeWidth(r * 0.35f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setColor(palette.track);
        c.drawCircle(cx, cy, r, stroke);
        stroke.setColor(palette.primary);
        tmp.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(tmp, -90, 360 * clamp01(progress), false, stroke);
        stroke.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawBar(Canvas c, float l, float r, float cy, float fraction) {
        float h = dp(4);
        tmp.set(l, cy - h / 2, r, cy + h / 2);
        fill.setColor(palette.track);
        c.drawRoundRect(tmp, h / 2, h / 2, fill);
        tmp.right = l + (r - l) * clamp01(fraction);
        fill.setColor(palette.primary);
        c.drawRoundRect(tmp, h / 2, h / 2, fill);
    }

    private void drawText(Canvas c, CharSequence s, float l, float r, float cy, float size, int color, boolean alignRight) {
        drawText(c, s, l, r, cy, size, color, alignRight, false);
    }

    private void drawText(Canvas c, CharSequence s, float l, float r, float cy, float size, int color,
                          boolean alignRight, boolean center) {
        if (s == null || r - l <= 0) return;
        text.setTypeface(palette.typefaceMedium);
        text.setTextSize(Math.min(size, capSize(size)));
        text.setColor(color);
        text.setFontFeatureSettings("'tnum'");
        CharSequence e = TextUtils.ellipsize(s, text, r - l, TextUtils.TruncateAt.END);
        float w = text.measureText(e, 0, e.length());
        float x = center ? (l + r - w) / 2f : alignRight ? r - w : l;
        Paint.FontMetrics fm = text.getFontMetrics();
        c.drawText(e, 0, e.length(), x, cy - (fm.ascent + fm.descent) / 2f, text);
        text.setFontFeatureSettings(null);
    }

    /** Draws one line from its top edge; returns the bottom. */
    private float drawTextTop(Canvas c, CharSequence s, float l, float r, float top, float size, int color, boolean medium) {
        if (s == null || r - l <= 0) return top;
        text.setTypeface(medium ? palette.typefaceMedium : palette.typeface);
        text.setTextSize(size);
        text.setColor(color);
        CharSequence e = TextUtils.ellipsize(s, text, r - l, TextUtils.TruncateAt.END);
        Paint.FontMetrics fm = text.getFontMetrics();
        c.drawText(e, 0, e.length(), l, top - fm.ascent, text);
        return top + fm.descent - fm.ascent;
    }

    /** Section 7.4: compact text is capped at 1.3x font scale because the geometry is fixed. */
    private float capSize(float size) {
        float scale = getResources().getConfiguration().fontScale;
        return scale > 1.3f ? size * 1.3f / scale : size;
    }

    private void drawCircleBitmap(Canvas c, Bitmap b, float cx, float cy, float r) {
        drawRoundBitmap(c, b, cx, cy, 2 * r, r);
    }

    private void drawRoundBitmap(Canvas c, Bitmap b, float cx, float cy, float size, float corner) {
        if (b.isRecycled()) return;
        BitmapShader shader = new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = size / Math.min(b.getWidth(), b.getHeight());
        matrix.setScale(scale, scale);
        matrix.postTranslate(cx - b.getWidth() * scale / 2f, cy - b.getHeight() * scale / 2f);
        shader.setLocalMatrix(matrix);
        image.setShader(shader);
        tmp.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        c.drawRoundRect(tmp, corner, corner, image);
        image.setShader(null);
    }

    private void drawDrawable(Canvas c, Drawable d, float cx, float cy, float size) {
        d.setBounds(Math.round(cx - size / 2), Math.round(cy - size / 2), Math.round(cx + size / 2), Math.round(cy + size / 2));
        d.draw(c);
    }

    // ---- Touch -------------------------------------------------------------------------------------

    private final RectF scrubRect = new RectF();
    private MediaController scrubController;
    private long scrubDuration;
    private float scrubLeft;
    private float scrubSpan;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
            if (expanded) host.setExpanded(false);
            return false;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP && scrubController != null && rendered != null
                && rendered.presentation == Presentation.CARD && scrubRect.contains(e.getX(), e.getY())) {
            float f = clamp01((e.getX() - scrubLeft) / scrubSpan);
            scrubController.getTransportControls().seekTo((long) (f * scrubDuration));
            Actions.haptic(this, HapticFeedbackConstants.CLOCK_TICK, settings.haptics());
            return true;
        }
        gestures.onTouchEvent(e);
        return true;
    }

    private final class Gestures extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            if (state.transientActivity != null) host.holdTransient();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Content c = rendered;
            if (c == null || c.presentation == Presentation.IDLE) return false;
            for (int i = hitRects.size() - 1; i >= 0; i--) {
                if (hitRects.get(i).contains(e.getX(), e.getY())) {
                    Actions.haptic(IslandView.this, HapticFeedbackConstants.CONFIRM, settings.haptics());
                    hitActions.get(i).run();
                    return true;
                }
            }
            if (c.activity != null) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.CONFIRM, settings.haptics());
                open(c.activity);
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (rendered != null && rendered.presentation == Presentation.IDLE) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.CONFIRM, settings.haptics());
                host.doubleTapIdle();
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (rendered != null && rendered.presentation == Presentation.COMPACT) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.GESTURE_START, settings.haptics());
                host.setExpanded(true);
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            Content c = rendered;
            if (c == null) return false;
            boolean vertical = Math.abs(vy) > Math.abs(vx);
            if (vertical && vy > 0 && c.presentation == Presentation.COMPACT) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.GESTURE_START, settings.haptics());
                host.setExpanded(true);
            } else if (vertical && vy < 0 && c.presentation == Presentation.CARD) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.GESTURE_END, settings.haptics());
                if (state.transientActivity != null) host.dismissTransient();
                else host.setExpanded(false);
            } else if (!vertical && c.presentation == Presentation.COMPACT) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.SEGMENT_TICK, settings.haptics());
                host.cycle(vx < 0 ? 1 : -1);
            } else if (!vertical && c.presentation == Presentation.CARD && state.transientActivity != null) {
                Actions.haptic(IslandView.this, HapticFeedbackConstants.REJECT, settings.haptics());
                Payload p = payload(state.transientActivity);
                if (p != null) dev.ayushvarma.aperture.source.NotificationSource.dismiss(p.notificationKey);
                host.dismissTransient();
            } else {
                return false;
            }
            return true;
        }
    }

    private void open(IslandActivity a) {
        Payload p = payload(a);
        if (p != null && actions.send(p.open)) {
            if (state.transientActivity == a) host.dismissTransient();
            host.setExpanded(false);
        }
    }

    // ---- Accessibility (Section 7.7) -----------------------------------------------------------------

    private void updateDescription() {
        Content c = pending != null ? pending : decide();
        IslandActivity a = c.activity;
        if (a == null) {
            setContentDescription(getContext().getString(R.string.island_idle_description));
            return;
        }
        StringBuilder sb = new StringBuilder();
        Payload p = payload(a);
        if (p != null && p.appName != null) sb.append(p.appName).append(". ");
        if (a.title != null) sb.append(a.title).append(". ");
        if (a.text != null) sb.append(a.text);
        setContentDescription(sb.toString());
    }

    // ---- Utilities ---------------------------------------------------------------------------------

    private static Payload payload(IslandActivity a) {
        return a != null && a.payload instanceof Payload ? (Payload) a.payload : null;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
