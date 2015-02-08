package adnan.haber.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Adnan on 25.1.2015..
 */
public class CredentialManager {
    private static SharedPreferences sharedPreferences;
    private static final String PREFS = "credentials";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";

    public static void Initialize(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void Save(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PREF_PASSWORD, password);
        editor.putString(PREF_USERNAME, username);

        editor.commit();
    }

    public static String GetSavedUsername() {
        return sharedPreferences.getString(PREF_USERNAME, "");
    }

    public static String GetSavedPassword() {
        return sharedPreferences.getString(PREF_PASSWORD, "");
    }
}
