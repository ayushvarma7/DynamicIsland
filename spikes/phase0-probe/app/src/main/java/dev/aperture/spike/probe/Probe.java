package dev.aperture.spike.probe;

import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** JSON and logging helpers. Every measurement goes to logcat tag APERTURE_PROBE as "event {json}". */
final class Probe {
    static final String TAG = "APERTURE_PROBE";
    private static final int CHUNK = 3500;

    private Probe() {}

    interface Supplier<T> {
        T get() throws Exception;
    }

    static JSONObject obj() {
        return new JSONObject();
    }

    static JSONObject put(JSONObject o, String key, Object value) {
        Object v = value;
        if (v instanceof Float) v = (double) (Float) v;
        if (v instanceof Double && (((Double) v).isNaN() || ((Double) v).isInfinite())) v = v.toString();
        try {
            o.put(key, v == null ? JSONObject.NULL : v);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    /** Runs a probe step; any throwable becomes an "ERR ..." string so one failure never hides the rest. */
    static Object safe(Supplier<?> s) {
        try {
            Object v = s.get();
            return v == null ? JSONObject.NULL : v;
        } catch (Throwable t) {
            return "ERR " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    static JSONArray add(JSONArray a, double v) {
        try {
            a.put(v);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return a;
    }

    static void putSafe(JSONObject o, String key, Supplier<?> s) {
        put(o, key, safe(s));
    }

    static void log(String event, JSONObject payload) {
        String body = payload.toString();
        if (body.length() <= CHUNK) {
            Log.i(TAG, event + " " + body);
            return;
        }
        int parts = (body.length() + CHUNK - 1) / CHUNK;
        for (int i = 0; i < parts; i++) {
            String part = body.substring(i * CHUNK, Math.min(body.length(), (i + 1) * CHUNK));
            Log.i(TAG, String.format(Locale.US, "%s [%d/%d] %s", event, i + 1, parts, part));
        }
    }

    static void log(String event, String key, Object value) {
        log(event, put(obj(), key, value));
    }

    static String hex(int color) {
        return String.format(Locale.US, "#%08X", color);
    }

    static JSONObject rect(Rect r) {
        if (r == null) return null;
        JSONObject o = obj();
        put(o, "l", r.left);
        put(o, "t", r.top);
        put(o, "r", r.right);
        put(o, "b", r.bottom);
        return o;
    }

    static JSONObject rectF(RectF r) {
        JSONObject o = obj();
        put(o, "l", r.left);
        put(o, "t", r.top);
        put(o, "r", r.right);
        put(o, "b", r.bottom);
        return o;
    }

    static JSONArray rects(List<Rect> list) {
        JSONArray a = new JSONArray();
        if (list != null) for (Rect r : list) a.put(rect(r));
        return a;
    }

    static JSONObject insets(Insets i) {
        JSONObject o = obj();
        put(o, "l", i.left);
        put(o, "t", i.top);
        put(o, "r", i.right);
        put(o, "b", i.bottom);
        return o;
    }
}
