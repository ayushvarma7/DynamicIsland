package dev.aperture.spike.probe;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Reads Settings keys the way the product would, recording "DENIED" when SettingsProvider refuses
 * (hidden keys without @Readable throw SecurityException for apps targeting S+), and "<unset>" when absent.
 */
final class SettingsReader {
    private SettingsReader() {}

    static Object system(Context c, String key) {
        return read(c.getContentResolver(), 's', key);
    }

    static Object secure(Context c, String key) {
        return read(c.getContentResolver(), 'e', key);
    }

    static Object global(Context c, String key) {
        return read(c.getContentResolver(), 'g', key);
    }

    private static Object read(ContentResolver cr, char table, String key) {
        try {
            String v;
            switch (table) {
                case 's':
                    v = Settings.System.getString(cr, key);
                    break;
                case 'e':
                    v = Settings.Secure.getString(cr, key);
                    break;
                default:
                    v = Settings.Global.getString(cr, key);
                    break;
            }
            return v == null ? "<unset>" : v;
        } catch (SecurityException e) {
            return "DENIED";
        } catch (Throwable t) {
            return "ERR " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }
}
