package adnan.haber.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Adnan on 6.3.2015.
 */
public class AutoReply {
    private static final String PREFS = "autoreply";
    private static final String PREF_MESSAGE = "message";
    private static final String PREF_ENABLED = "enabled";
    private static final String PREF_MODE = "mode";

    public static final int MODE_EVERY_MESSAGE = 0;

    public static void setMode(Context context, int mode) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(PREF_MODE, mode);
        editor.apply();
    }


    public static int getMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_MODE, MODE_EVERY_MESSAGE);
    }

    public static void saveMessage(Context context, String message) {
        if ( message.trim().length() == 0 ) return;

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_MESSAGE, message);
        editor.apply();
    }

    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(PREF_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_ENABLED, false);
    }

    public static String getMessage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(PREF_MESSAGE, "nije definisana auto-reply poruka!");
    }
}
