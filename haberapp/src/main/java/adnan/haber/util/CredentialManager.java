package adnan.haber.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Adnan on 25.1.2015..
 */
public class CredentialManager {

    private static final String PREFS = "credentials";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_LOGIN_ON_STARTUP = "login_on_startup";

    public static void Save(Context context, String username, String password, boolean loginOnStartup) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();

        editor.putString(PREF_PASSWORD, password);
        editor.putString(PREF_USERNAME, username);
        editor.putBoolean(PREF_LOGIN_ON_STARTUP, loginOnStartup);

        editor.commit();
    }

    public static boolean ShouldLoginOnStartup(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        return sharedPreferences.getBoolean(PREF_LOGIN_ON_STARTUP, false);
    }

    public static String GetSavedUsername(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        return sharedPreferences.getString(PREF_USERNAME, "");
    }

    public static String GetSavedPassword(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        return sharedPreferences.getString(PREF_PASSWORD, "");
    }
}
