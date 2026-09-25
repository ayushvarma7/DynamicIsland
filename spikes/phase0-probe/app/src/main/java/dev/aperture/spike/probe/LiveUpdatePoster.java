package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;
import static dev.aperture.spike.probe.Probe.safe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

/** Posts a ProgressStyle notification that requests promotion, so the listener can report what it sees. */
final class LiveUpdatePoster {
    private static final String CHANNEL = "probe_live";
    private static final int ID = 4201;

    private LiveUpdatePoster() {}

    static void post(Context ctx, int progress) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Live update test",
                NotificationManager.IMPORTANCE_DEFAULT));
        Notification.ProgressStyle style = new Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgress(progress)
                .setProgressTrackerIcon(Icon.createWithResource(ctx, android.R.drawable.ic_menu_directions))
                .setProgressSegments(List.of(
                        new Notification.ProgressStyle.Segment(40).setColor(0xFF4285F4),
                        new Notification.ProgressStyle.Segment(60).setColor(0xFF34A853)))
                .setProgressPoints(List.of(new Notification.ProgressStyle.Point(40).setColor(0xFFEA4335)));
        PendingIntent open = PendingIntent.getActivity(ctx, 0, new Intent(ctx, ProbeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Probe ride arriving")
                .setContentText("Driver is 4 min away")
                .setOngoing(true)
                .setShortCriticalText("4 min")
                .setStyle(style)
                .setContentIntent(open);
        String via = requestPromotion(b);
        Notification n = b.build();
        JSONObject o = obj();
        put(o, "promotionRequestedVia", via);
        put(o, "hasPromotableCharacteristics", n.hasPromotableCharacteristics());
        put(o, "canPostPromotedNotifications", safe(nm::canPostPromotedNotifications));
        put(o, "flagsHexBeforeNotify", Integer.toHexString(n.flags));
        Probe.log("live_update_posting", o);
        nm.notify(ID, n);
    }

    /** API 36.1 may expose Builder.setRequestPromotedOngoing; the 36.0 SDK in this repo does not, so fall back to the extra. */
    private static String requestPromotion(Notification.Builder b) {
        try {
            Method m = Notification.Builder.class.getMethod("setRequestPromotedOngoing", boolean.class);
            m.invoke(b, true);
            return "Builder.setRequestPromotedOngoing";
        } catch (ReflectiveOperationException e) {
            Bundle extras = new Bundle();
            extras.putBoolean("android.requestPromotedOngoing", true);
            b.addExtras(extras);
            return "extra android.requestPromotedOngoing";
        }
    }

    static void cancel(Context ctx) {
        ctx.getSystemService(NotificationManager.class).cancel(ID);
    }

    /** Gate D7: a plain high-importance notification that normally pops up as a system heads-up. */
    static void postHeadsUp(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("probe_hun", "Heads-up test",
                NotificationManager.IMPORTANCE_HIGH));
        Notification n = new Notification.Builder(ctx, "probe_hun")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("Probe heads-up")
                .setContentText("Does the system pop this up?")
                .build();
        nm.notify(ID + 1, n);
        Probe.log("heads_up_posted", obj());
    }
}
