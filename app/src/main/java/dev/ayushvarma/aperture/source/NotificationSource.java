package dev.ayushvarma.aperture.source;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.app.RemoteInput;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.IslandHub;
import dev.ayushvarma.aperture.R;
import dev.ayushvarma.aperture.core.engine.ActivityKind;
import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.core.format.TimeText;
import dev.ayushvarma.aperture.settings.IslandSettings;

/**
 * Notification access: turns other apps' notifications into island activities and hosts the media tracker.
 * WhatsApp, Messages, Gmail and the like arrive as transient heads-ups (with inline reply when the app offers it);
 * calls (CallStyle, or category CALL for older apps) become call activities; promoted Live Updates, Google Clock
 * timers and downloads become persistent activities. Media-style notifications are skipped: the media session
 * carries the same information with exact position and controls.
 */
public final class NotificationSource extends NotificationListenerService implements IslandSettings.Listener {
    private static final String CLOCK_PKG = "com.google.android.deskclock";
    private static final String EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText";

    private static volatile NotificationSource instance;

    private final Map<String, String> keyToId = new HashMap<>();
    private final Set<String> seenKeys = new HashSet<>();
    private MediaTracker media;
    private IslandHub hub;
    private IslandSettings settings;

    /** True while the system has us bound; the health card in the app reads this. */
    public static boolean isConnected() {
        return instance != null;
    }

    /** Swipe-away from the island dismisses the real notification too. */
    public static void dismiss(String key) {
        NotificationSource s = instance;
        if (s != null && key != null) s.cancelNotification(key);
    }

    /** The live reply action for a notification, read fresh so it is never a stale copy. */
    public static Notification.Action replyAction(String key) {
        NotificationSource s = instance;
        if (s == null || key == null) return null;
        StatusBarNotification[] found = s.getActiveNotifications(new String[]{key});
        if (found == null || found.length == 0) return null;
        return findReply(found[0].getNotification());
    }

    @Override
    public void onListenerConnected() {
        instance = this;
        hub = ApertureApp.of(this).hub();
        settings = ApertureApp.of(this).settings();
        media = new MediaTracker(this, new ComponentName(this, NotificationSource.class));
        media.start();
        settings.addListener(this);
        StatusBarNotification[] active = getActiveNotifications();
        if (active == null) return;
        RankingMap rm = getCurrentRanking();
        for (StatusBarNotification sbn : active) handle(sbn, rm, true);
    }

    @Override
    public void onListenerDisconnected() {
        if (settings != null) settings.removeListener(this);
        if (media != null) media.stop();
        for (String id : keyToId.values()) hub.remove(id);
        keyToId.clear();
        seenKeys.clear();
        instance = null;
        // Section 8 health: ask to be bound again rather than silently staying dead.
        requestRebind(new ComponentName(this, NotificationSource.class));
    }

    /** App picker or toggles changed: drop what is no longer allowed and re-read media sessions. */
    @Override
    public void onSettingsChanged() {
        String own = getPackageName();
        hub.engine().removeIf(a -> !own.equals(a.pkg) && (!settings.isAllowed(a.pkg)
                || (a.kind == ActivityKind.MEDIA && !settings.showMedia())));
        media.republish();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        handle(sbn, rankingMap, false);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        String id = keyToId.remove(sbn.getKey());
        seenKeys.remove(sbn.getKey());
        if (id != null) hub.remove(id);
    }

    private void handle(StatusBarNotification sbn, RankingMap rm, boolean initial) {
        if (getPackageName().equals(sbn.getPackageName())) return;
        if (!settings.isAllowed(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        Bundle ex = n.extras;
        String template = ex.getString(Notification.EXTRA_TEMPLATE, "");
        if (template.contains("MediaStyle")) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        Ranking ranking = new Ranking();
        boolean ranked = rm != null && rm.getRanking(sbn.getKey(), ranking);
        boolean update = !seenKeys.add(sbn.getKey());
        long now = System.currentTimeMillis();

        int callType = ex.getInt(Notification.EXTRA_CALL_TYPE, Notification.CallStyle.CALL_TYPE_UNKNOWN);
        boolean categoryCall = Notification.CATEGORY_CALL.equals(n.category);
        if (callType != Notification.CallStyle.CALL_TYPE_UNKNOWN || (categoryCall && ((n.flags & Notification.FLAG_ONGOING_EVENT) != 0 || n.fullScreenIntent != null))) {
            handleCall(sbn, n, callType, now);
            return;
        }

        boolean promoted = (Build.VERSION.SDK_INT >= 36 && (n.flags & Notification.FLAG_PROMOTED_ONGOING) != 0)
                || template.endsWith("ProgressStyle") || template.endsWith("MetricStyle");
        MetricInfo metric = template.endsWith("MetricStyle") ? metric(n) : null;
        boolean chronometer = ex.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || (metric != null && metric.baseMs > 0);
        boolean countDown = metric != null && metric.baseMs > 0 ? metric.countDown
                : ex.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN);
        int max = ex.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
        boolean ongoing = (n.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        if (promoted || ongoing) {
            ActivityKind kind;
            if (Notification.CATEGORY_ALARM.equals(n.category) && n.fullScreenIntent != null) kind = ActivityKind.ALARM;
            else if (metric != null && metric.time) kind = metric.countDown ? ActivityKind.TIMER : ActivityKind.STOPWATCH;
            else if (chronometer && countDown) kind = ActivityKind.TIMER;
            else if (chronometer && CLOCK_PKG.equals(sbn.getPackageName())) kind = ActivityKind.STOPWATCH;
            else if (promoted) kind = ActivityKind.LIVE_UPDATE;
            else if (max > 0) kind = ActivityKind.DOWNLOAD;
            else return; // ordinary foreground-service notifications stay off the island
            publishPersistent(sbn, n, kind, chronometer, countDown, metric, now);
            return;
        }

        if (initial || !settings.showNotifications()) return;
        if (ranked && !ranking.matchesInterruptionFilter()) return; // DND
        if (ranked && ranking.getImportance() < NotificationManager.IMPORTANCE_DEFAULT) return;
        if (update && (n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0) return;
        if (settings.ignoreWhileScreenOff() && !getSystemService(android.os.PowerManager.class).isInteractive()) return;

        publishTransient(sbn, n, ranked ? ranking : null, now);
    }

    private void publishTransient(StatusBarNotification sbn, Notification n, Ranking ranking, long now) {
        Payload p = base(sbn, n);
        CharSequence[] tt = titleText(n);
        CharSequence title = tt[0];
        CharSequence text = tt[1];
        if (isLocked()) {
            int override = ranking == null ? NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE
                    : ranking.getLockscreenVisibilityOverride();
            if (n.visibility == Notification.VISIBILITY_SECRET || override == Notification.VISIBILITY_SECRET) return;
            if (!allowPrivateOnLockScreen() || n.visibility == Notification.VISIBILITY_PRIVATE
                    || override == Notification.VISIBILITY_PRIVATE) {
                if (n.publicVersion != null) {
                    CharSequence[] pub = titleText(n.publicVersion);
                    title = pub[0] != null ? pub[0] : p.appName;
                    text = pub[1];
                } else {
                    title = p.appName;
                    text = getString(R.string.island_new_notification);
                }
                p.redacted = true;
            }
        }
        if (title == null && text == null) return;
        if (settings.hideSensitive() && looksLikeOneTimeCode(title, text)) return;
        if (!p.redacted) p.replyAction = findReply(n);
        String id = "notif:" + sbn.getKey();
        keyToId.put(sbn.getKey(), id);
        int chars = (title == null ? 0 : title.length()) + (text == null ? 0 : text.length());
        int base = settings.notificationSeconds();
        long durationMs = Math.min(base + 3, base + chars / 40) * 1000L;
        hub.postTransient(IslandActivity.builder(id, sbn.getPackageName(), ActivityKind.NOTIFICATION)
                .title(title).text(text).accentColor(n.color).payload(p).createdAt(now).updatedAt(now).build(),
                durationMs);
    }

    private void publishPersistent(StatusBarNotification sbn, Notification n, ActivityKind kind, boolean chronometer,
                                   boolean countDown, MetricInfo metric, long now) {
        Payload p = base(sbn, n);
        CharSequence[] tt = titleText(n);
        Bundle ex = n.extras;
        CharSequence shortText = ex.getCharSequence(EXTRA_SHORT_CRITICAL_TEXT);
        if (metric != null) {
            // MetricStyle posts no title or text: the metric's label ("Pasta") and value carry the meaning.
            if (tt[0] == null) tt[0] = metric.label;
            if (shortText == null) shortText = metric.text;
        }
        long chronometerBase = !chronometer ? 0 : metric != null && metric.baseMs > 0 ? metric.baseMs : n.when;
        int max = ex.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
        int progress = ex.getInt(Notification.EXTRA_PROGRESS, 0);
        boolean indeterminate = ex.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE);
        float fraction = max > 0 && !indeterminate ? Math.max(0f, Math.min(1f, progress / (float) max)) : -1f;
        String id = kind.name().toLowerCase(Locale.ROOT) + ":" + sbn.getKey();
        keyToId.put(sbn.getKey(), id);
        IslandActivity prev = hub.engine().get(id);
        hub.upsert(IslandActivity.builder(id, sbn.getPackageName(), kind)
                .title(tt[0]).text(tt[1]).shortText(shortText)
                .progress(fraction).chronometer(chronometerBase, countDown).accentColor(n.color)
                .payload(p).createdAt(prev == null ? now : prev.createdAt).updatedAt(now).build());
    }

    private void handleCall(StatusBarNotification sbn, Notification n, int callType, long now) {
        Bundle ex = n.extras;
        Payload p = base(sbn, n);
        p.answer = ex.getParcelable(Notification.EXTRA_ANSWER_INTENT, PendingIntent.class);
        p.decline = ex.getParcelable(Notification.EXTRA_DECLINE_INTENT, PendingIntent.class);
        p.hangUp = ex.getParcelable(Notification.EXTRA_HANG_UP_INTENT, PendingIntent.class);
        if (n.actions != null) {
            // Apps that don't use CallStyle: recognise the buttons by their labels.
            for (Notification.Action a : n.actions) {
                String label = a.title == null ? "" : a.title.toString().toLowerCase(Locale.ROOT);
                if (p.answer == null && (label.contains("answer") || label.contains("accept"))) p.answer = a.actionIntent;
                else if (p.decline == null && (label.contains("decline") || label.contains("reject"))) p.decline = a.actionIntent;
                else if (p.hangUp == null && (label.contains("hang up") || label.contains("end"))) p.hangUp = a.actionIntent;
            }
        }
        boolean incoming = callType == Notification.CallStyle.CALL_TYPE_INCOMING
                || callType == Notification.CallStyle.CALL_TYPE_SCREENING
                || (callType == Notification.CallStyle.CALL_TYPE_UNKNOWN && p.answer != null);
        CharSequence caller = null;
        Person person = ex.getParcelable(Notification.EXTRA_CALL_PERSON, Person.class);
        if (person != null) {
            caller = person.getName();
            if (person.getIcon() != null) p.art = bitmapOf(person.getIcon());
        }
        CharSequence[] tt = titleText(n);
        if (caller == null) caller = tt[0];
        String id = "call:" + sbn.getKey();
        keyToId.put(sbn.getKey(), id);
        IslandActivity prev = hub.engine().get(id);
        hub.upsert(IslandActivity.builder(id, sbn.getPackageName(),
                        incoming ? ActivityKind.INCOMING_CALL : ActivityKind.ONGOING_CALL)
                .title(caller).text(tt[1]).chronometer(incoming ? 0 : n.when, false)
                .payload(p).createdAt(prev == null ? now : prev.createdAt).updatedAt(now).build());
    }

    /** The metric an Android 17 MetricStyle notification leads with, as plain island data. */
    private static final class MetricInfo {
        CharSequence label;
        /** Wall-clock zero of a running timer (when it ends) or stopwatch (when it started); 0 if not running. */
        long baseMs;
        boolean countDown;
        /** True for timers and stopwatches, running or paused. */
        boolean time;
        /** Display text for anything that doesn't tick: a paused time, a number, a word, a date. */
        CharSequence text;
    }

    /**
     * Android 17 MetricStyle: Google Clock posts timers and stopwatches this way, with no title, text or
     * chronometer extras. Reads the critical metric (or the first one) through the public API.
     */
    private MetricInfo metric(Notification n) {
        if (Build.VERSION.SDK_INT < 37) return null;
        try {
            Notification.Style style = Notification.Builder.recoverBuilder(this, n).getStyle();
            if (!(style instanceof Notification.MetricStyle)) return null;
            Notification.MetricStyle ms = (Notification.MetricStyle) style;
            Notification.Metric m = ms.getCriticalMetric();
            if (m == null && !ms.getMetrics().isEmpty()) m = ms.getMetrics().get(0);
            if (m == null) return null;
            MetricInfo info = new MetricInfo();
            info.label = m.getLabel();
            Notification.Metric.MetricValue v = m.getValue();
            if (v instanceof Notification.Metric.TimeDifference) {
                Notification.Metric.TimeDifference t = (Notification.Metric.TimeDifference) v;
                info.time = true;
                info.countDown = t.isTimer();
                java.time.Duration paused = t.getPausedDuration();
                if (paused != null) {
                    long ms0 = paused.toMillis();
                    info.text = t.isTimer() ? TimeText.countdown(ms0) : TimeText.clock(ms0);
                } else if (t.getZeroTime() != null) {
                    info.baseMs = t.getZeroTime().toEpochMilli();
                } else if (t.getZeroElapsedRealtime() != null) {
                    info.baseMs = System.currentTimeMillis() + (t.getZeroElapsedRealtime() - SystemClock.elapsedRealtime());
                }
            } else if (v instanceof Notification.Metric.FixedText) {
                Notification.Metric.FixedText t = (Notification.Metric.FixedText) v;
                info.text = withUnit(t.getValue(), t.getUnit());
            } else if (v instanceof Notification.Metric.FixedInt) {
                Notification.Metric.FixedInt t = (Notification.Metric.FixedInt) v;
                info.text = withUnit(String.valueOf(t.getValue()), t.getUnit());
            } else if (v instanceof Notification.Metric.FixedFloat) {
                Notification.Metric.FixedFloat t = (Notification.Metric.FixedFloat) v;
                java.text.NumberFormat f = java.text.NumberFormat.getNumberInstance();
                f.setMinimumFractionDigits(t.getMinFractionDigits());
                f.setMaximumFractionDigits(t.getMaxFractionDigits());
                info.text = withUnit(f.format(t.getValue()), t.getUnit());
            } else if (v instanceof Notification.Metric.FixedTime) {
                info.text = java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                        .format(((Notification.Metric.FixedTime) v).getValue());
            } else if (v instanceof Notification.Metric.FixedDate) {
                info.text = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                        .format(((Notification.Metric.FixedDate) v).getValue());
            }
            return info;
        } catch (RuntimeException e) {
            return null; // a malformed style: fall back to the plain extras
        }
    }

    private static CharSequence withUnit(CharSequence value, CharSequence unit) {
        if (value == null) return null;
        return unit == null || unit.length() == 0 ? value : value + " " + unit;
    }

    /** Title and body, preferring the latest message for MessagingStyle (WhatsApp, Messages, Telegram...). */
    private CharSequence[] titleText(Notification n) {
        Bundle ex = n.extras;
        CharSequence title = ex.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (text == null) text = ex.getCharSequence(Notification.EXTRA_TEXT);
        try {
            Notification.Style style = Notification.Builder.recoverBuilder(this, n).getStyle();
            if (style instanceof Notification.MessagingStyle) {
                Notification.MessagingStyle ms = (Notification.MessagingStyle) style;
                List<Notification.MessagingStyle.Message> messages = ms.getMessages();
                if (!messages.isEmpty()) {
                    Notification.MessagingStyle.Message last = messages.get(messages.size() - 1);
                    CharSequence sender = last.getSenderPerson() == null ? null : last.getSenderPerson().getName();
                    CharSequence conversation = ms.getConversationTitle();
                    if (ms.isGroupConversation() && conversation != null) {
                        title = conversation;
                        text = sender == null ? last.getText() : sender + ": " + last.getText();
                    } else {
                        if (sender != null) title = sender;
                        text = last.getText();
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // A malformed style falls back to the plain title/text extras read above.
        }
        return new CharSequence[]{title, text};
    }

    private Payload base(StatusBarNotification sbn, Notification n) {
        Payload p = new Payload();
        p.notificationKey = sbn.getKey();
        p.open = n.contentIntent;
        PackageManager pm = getPackageManager();
        try {
            p.appIcon = pm.getApplicationIcon(sbn.getPackageName());
            p.appName = pm.getApplicationLabel(pm.getApplicationInfo(sbn.getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            p.appName = sbn.getPackageName();
            if (n.getSmallIcon() != null) p.appIcon = n.getSmallIcon().loadDrawable(this);
        }
        Icon large = n.getLargeIcon();
        if (large != null) p.art = bitmapOf(large);
        return p;
    }

    private Bitmap bitmapOf(Icon icon) {
        Drawable d = icon.loadDrawable(this);
        if (d == null) return null;
        if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();
        int size = Math.max(1, Math.min(256, Math.max(d.getIntrinsicWidth(), d.getIntrinsicHeight())));
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        return b;
    }

    private static final java.util.regex.Pattern OTP_WORDS = java.util.regex.Pattern.compile(
            "(?i)\\b(otp|code|passcode|verification|verify|one[- ]time|2fa|pin)\\b");
    private static final java.util.regex.Pattern OTP_DIGITS = java.util.regex.Pattern.compile("(?<![\\d.,])\\d{4,8}(?![\\d.,])");

    /**
     * Android only tells system apps which notifications are sensitive, so recognise one-time codes the way a
     * person would: a 4-8 digit number next to words like "code", "OTP" or "verification".
     */
    static boolean looksLikeOneTimeCode(CharSequence title, CharSequence text) {
        String all = (title == null ? "" : title) + " " + (text == null ? "" : text);
        return OTP_WORDS.matcher(all).find() && OTP_DIGITS.matcher(all).find();
    }

    static Notification.Action findReply(Notification n) {
        if (n.actions == null) return null;
        for (Notification.Action a : n.actions) {
            RemoteInput[] inputs = a.getRemoteInputs();
            if (inputs == null) continue;
            for (RemoteInput ri : inputs) {
                if (ri.getAllowFreeFormInput()) return a;
            }
        }
        return null;
    }

    private boolean isLocked() {
        return getSystemService(KeyguardManager.class).isKeyguardLocked();
    }

    /** Section 6.6.6: honour "Sensitive notifications" off (Pixel 8 in Phase 0 had it off). */
    private boolean allowPrivateOnLockScreen() {
        try {
            return !"0".equals(Settings.Secure.getString(getContentResolver(), "lock_screen_allow_private_notifications"));
        } catch (SecurityException e) {
            return false;
        }
    }
}
