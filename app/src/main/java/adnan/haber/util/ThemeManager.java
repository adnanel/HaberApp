package adnan.haber.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import adnan.haber.types.Theme;

/**
 * Created by Adnan on 1.2.2015..
 */
public class ThemeManager {
    static SharedPreferences sharedPreferences;
    static final String PREFS = "themeManager";
    static final String PREF_CURRENT_VERSION = "cversion";
    static final String PREF_CURRENT_THEME = "ctheme";

    public static final String COLOR_CHAT_ACTIVE_STROKE = "color_chat_active_stroke";
    public static final String COLOR_CHAT_TAB_BACKGROUND = "color_chat_tab_background";
    public static final String COLOR_LEFT_DRAWER_BACKGROUND = "color_left_drawer_background";
    public static final String COLOR_CHAT_DIVIDER = "color_chat_divider";
    public static final String COLOR_CHAT_BACKGROUND = "color_chat_background";
    public static final String COLOR_CHAT_USER_BACKGROUND = "color_chat_user_background";
    public static final String COLOR_CHAT_ITEM_BACKGROUND = "color_chat_item_background";

    public static int GetColor(String color) {
        return sharedPreferences.getInt(color, Color.parseColor("#c6c6c6c6"));
    }

    public static void Initialize(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if ( !sharedPreferences.contains(PREF_CURRENT_VERSION) ) {
            try {
                ApplyTheme(JSON.GetDefaultTheme(context));
            } catch ( Exception er ) {
                Debug.log("CRITICAL!! FAILED TO LOAD DEFAULT THEME!");
            }
        }
    }

    public static void ApplyTheme(Theme theme) {
        Debug.log("Applying theme " + theme.name);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PREF_CURRENT_THEME, theme.name);
        editor.putString(PREF_CURRENT_VERSION, "1");

        editor.putInt(COLOR_CHAT_ACTIVE_STROKE, theme.COLOR_CHAT_ACTIVE_STROKE);
        editor.putInt(COLOR_CHAT_TAB_BACKGROUND, theme.COLOR_CHAT_TAB_BACKGROUND);
        editor.putInt(COLOR_LEFT_DRAWER_BACKGROUND, theme.COLOR_LEFT_DRAWER_BACKGROUND);
        editor.putInt(COLOR_CHAT_DIVIDER, theme.COLOR_CHAT_DIVIDER);
        editor.putInt(COLOR_CHAT_BACKGROUND, theme.COLOR_CHAT_BACKGROUND);
        editor.putInt(COLOR_CHAT_ITEM_BACKGROUND, theme.COLOR_CHAT_ITEM_BACKGROUND);
        editor.putInt(COLOR_CHAT_USER_BACKGROUND, theme.COLOR_CHAT_USER_BACKGROUND);

        editor.commit();
    }
}
