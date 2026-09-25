package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.hex;
import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;
import static dev.aperture.spike.probe.Probe.putSafe;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Phase 0 listener spike: proves which Live Update / ProgressStyle / CallStyle fields a third-party
 * listener can read. Privacy: other apps' text is logged as length only; structure keys are logged in full.
 */
public class ProbeListener extends NotificationListenerService {
    /** adb shell am broadcast -a dev.aperture.spike.probe.LISTENER_CMD : re-dump without touching the grant. */
    static final String ACTION_DUMP = "dev.aperture.spike.probe.LISTENER_CMD";

    private BroadcastReceiver dumpReceiver;

    @Override
    public void onListenerConnected() {
        dumpAll("listener_connected");
        MediaSessionManager msm = getSystemService(MediaSessionManager.class);
        msm.addOnActiveSessionsChangedListener(this::dumpMedia, new ComponentName(this, ProbeListener.class));
        dumpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                dumpAll("listener_dump");
            }
        };
        registerReceiver(dumpReceiver, new IntentFilter(ACTION_DUMP), Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onListenerDisconnected() {
        if (dumpReceiver != null) {
            unregisterReceiver(dumpReceiver);
            dumpReceiver = null;
        }
        Probe.log("listener_disconnected", obj());
    }

    private void dumpAll(String event) {
        StatusBarNotification[] active = getActiveNotifications();
        Probe.log(event, "active", active == null ? 0 : active.length);
        if (active != null) {
            RankingMap rm = getCurrentRanking();
            for (StatusBarNotification sbn : active) logSbn("listener_active", sbn, rm);
        }
        dumpMedia(getSystemService(MediaSessionManager.class)
                .getActiveSessions(new ComponentName(this, ProbeListener.class)));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        logSbn("listener_posted", sbn, rankingMap);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        Probe.log("listener_removed", put(put(obj(), "pkg", sbn.getPackageName()), "reason", reason));
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
        Probe.log("listener_interruption_filter", "filter", interruptionFilter);
    }

    private void logSbn(String event, StatusBarNotification sbn, RankingMap rm) {
        Notification n = sbn.getNotification();
        boolean own = getPackageName().equals(sbn.getPackageName());
        JSONObject o = obj();
        put(o, "pkg", sbn.getPackageName());
        put(o, "id", sbn.getId());
        put(o, "ongoing", sbn.isOngoing());
        put(o, "clearable", sbn.isClearable());
        put(o, "flagsHex", Integer.toHexString(n.flags));
        put(o, "FLAG_PROMOTED_ONGOING", (n.flags & Notification.FLAG_PROMOTED_ONGOING) != 0);
        putSafe(o, "hasPromotableCharacteristics", n::hasPromotableCharacteristics);
        put(o, "groupSummary", (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0);
        put(o, "onlyAlertOnce", (n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
        put(o, "category", n.category);
        put(o, "channel", n.getChannelId());
        put(o, "visibility", n.visibility);
        put(o, "hasPublicVersion", n.publicVersion != null);
        put(o, "hasCustomViews", n.contentView != null || n.bigContentView != null || n.headsUpContentView != null);
        put(o, "extras", extras(n.extras, own));
        putSafe(o, "style", () -> style(n));
        if (rm != null) {
            Ranking r = new Ranking();
            if (rm.getRanking(sbn.getKey(), r)) put(o, "ranking", ranking(r));
        }
        Probe.log(event, o);
    }

    private static JSONObject extras(Bundle ex, boolean own) {
        JSONObject keys = obj();
        for (String k : ex.keySet()) {
            Object v = ex.get(k);
            String type = v == null ? "null" : v.getClass().getSimpleName();
            String lk = k.toLowerCase(Locale.US);
            boolean structural = lk.contains("progress") || lk.contains("promot") || lk.contains("calltype")
                    || lk.contains("template") || lk.contains("colorized") || lk.contains("shortcritical")
                    || lk.contains("chronometer") || lk.contains("showwhen") || lk.contains("metric");
            if (own || structural) {
                put(keys, k, type + "=" + describe(v));
            } else if (v instanceof CharSequence) {
                put(keys, k, type + "(len " + ((CharSequence) v).length() + ")");
            } else {
                put(keys, k, type);
            }
        }
        return keys;
    }

    private static String describe(Object v) {
        if (v instanceof Bundle) return "Bundle" + ((Bundle) v).keySet();
        if (v instanceof Parcelable[]) {
            Parcelable[] a = (Parcelable[]) v;
            return "Parcelable[" + a.length + "]" + (a.length > 0 && a[0] instanceof Bundle ? ((Bundle) a[0]).keySet() : "");
        }
        if (v instanceof List) return "List(" + ((List<?>) v).size() + ")";
        String s = String.valueOf(v);
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private Object style(Notification n) {
        Notification.Style s = Notification.Builder.recoverBuilder(this, n).getStyle();
        if (s == null) return "none";
        JSONObject o = obj();
        put(o, "class", s.getClass().getName());
        if (s instanceof Notification.ProgressStyle) {
            Notification.ProgressStyle ps = (Notification.ProgressStyle) s;
            put(o, "progress", ps.getProgress());
            put(o, "max", ps.getProgressMax());
            put(o, "indeterminate", ps.isProgressIndeterminate());
            put(o, "styledByProgress", ps.isStyledByProgress());
            JSONArray segs = new JSONArray();
            for (Notification.ProgressStyle.Segment seg : ps.getProgressSegments()) {
                segs.put(put(put(put(obj(), "length", seg.getLength()), "color", hex(seg.getColor())), "id", seg.getId()));
            }
            put(o, "segments", segs);
            JSONArray pts = new JSONArray();
            for (Notification.ProgressStyle.Point p : ps.getProgressPoints()) {
                pts.put(put(put(put(obj(), "position", p.getPosition()), "color", hex(p.getColor())), "id", p.getId()));
            }
            put(o, "points", pts);
            put(o, "trackerIcon", ps.getProgressTrackerIcon() != null);
            put(o, "startIcon", ps.getProgressStartIcon() != null);
            put(o, "endIcon", ps.getProgressEndIcon() != null);
        }
        return o;
    }

    private static JSONObject ranking(Ranking r) {
        JSONObject o = obj();
        put(o, "importance", r.getImportance());
        put(o, "rank", r.getRank());
        put(o, "matchesInterruptionFilter", r.matchesInterruptionFilter());
        put(o, "lockscreenVisibilityOverride", r.getLockscreenVisibilityOverride());
        put(o, "suppressedVisualEffectsHex", Integer.toHexString(r.getSuppressedVisualEffects()));
        put(o, "ambient", r.isAmbient());
        put(o, "suspended", r.isSuspended());
        put(o, "conversation", r.isConversation());
        put(o, "channel", r.getChannel() == null ? null : r.getChannel().getId());
        put(o, "smartActions", r.getSmartActions().size());
        put(o, "smartReplies", r.getSmartReplies().size());
        return o;
    }

    private void dumpMedia(List<MediaController> controllers) {
        JSONArray a = new JSONArray();
        if (controllers != null) {
            for (MediaController c : controllers) {
                JSONObject o = obj();
                put(o, "pkg", c.getPackageName());
                put(o, "playbackType", c.getPlaybackInfo().getPlaybackType());
                PlaybackState ps = c.getPlaybackState();
                if (ps != null) {
                    put(o, "state", ps.getState());
                    put(o, "positionMs", ps.getPosition());
                    put(o, "lastPositionUpdateTime", ps.getLastPositionUpdateTime());
                    put(o, "elapsedRealtimeNow", SystemClock.elapsedRealtime());
                    put(o, "speed", ps.getPlaybackSpeed());
                    put(o, "actionsHex", Long.toHexString(ps.getActions()));
                    JSONArray ca = new JSONArray();
                    for (PlaybackState.CustomAction act : ps.getCustomActions()) ca.put(String.valueOf(act.getName()));
                    put(o, "customActions", ca);
                }
                MediaMetadata md = c.getMetadata();
                if (md != null) {
                    put(o, "durationMs", md.getLong(MediaMetadata.METADATA_KEY_DURATION));
                    put(o, "hasArtBitmap", md.getBitmap(MediaMetadata.METADATA_KEY_ART) != null
                            || md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null);
                    put(o, "hasArtUri", md.getString(MediaMetadata.METADATA_KEY_ART_URI) != null
                            || md.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) != null);
                    CharSequence title = md.getText(MediaMetadata.METADATA_KEY_TITLE);
                    put(o, "titleLen", title == null ? 0 : title.length());
                }
                a.put(o);
            }
        }
        Probe.log("media_sessions", "sessions", a);
    }
}
