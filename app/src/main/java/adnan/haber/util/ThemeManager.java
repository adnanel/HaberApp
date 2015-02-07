 package adnan.haber.util;

/*

 No more themes, maybe later.


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;

import adnan.haber.types.Theme;

/**
 * Created by Adnan on 1.2.2015..

public class ThemeManager {
    static SharedPreferences sharedPreferences;
    static final String PREFS = "themeManager";
    static final String PREF_CURRENT_VERSION = "cversion";
    static final String PREF_CURRENT_THEME = "ctheme";

    public static final String COLOR_CHAT_ACTIVE_STROKE = "color_chat_active_stroke";
    public static final String COLOR_TAB_COUNTER_CIRCLE_STROKE = "color_tab_counter_circle_stroke";

    public static final String CHAT_TAB_BACKGROUND = "chat_tab_background";
    public static final String LEFT_DRAWER_BACKGROUND = "left_drawer_background";
    public static final String CHAT_DIVIDER = "chat_divider";
    public static final String CHAT_BACKGROUND = "chat_background";
    public static final String CHAT_USER_BACKGROUND = "chat_user_background";
    public static final String CHAT_ITEM_BACKGROUND = "chat_item_background";
    public static final String CHAT_MARK            = "chat_mark";
    public static final String TAB_COUNTER_CIRCLE   = "tab_counter_circle";

    public static final String BUTTON_SEND_MESSAGE = "drawable_button_send_message";
    public static final String BUTTON_SMILEY_CHOOSER = "drawable_button_smiley_chooser";

    public static int GetColor(String resource) {
        try {
            return Color.parseColor(sharedPreferences.getString(resource, "#c6c6c6c6"));
        } catch ( IllegalArgumentException ex ) {
            Debug.log(ex);
            return Color.RED;
        }
    }

    public static Drawable GetDrawable(String resource) {
        try {
            int color = Color.parseColor(sharedPreferences.getString(resource, "#c6c6c6c6"));
            return new ColorDrawable(color);
        } catch ( IllegalArgumentException ex ) {
            Debug.log("Failed to parse color!!!");

            String path = sharedPreferences.getString(resource, "");
            File drawable = new File(path);
            if ( drawable.exists() )
                return Drawable.createFromPath(path);
            else
                return new ColorDrawable(Color.RED);
        }
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

        editor.putString(COLOR_CHAT_ACTIVE_STROKE, theme.COLOR_CHAT_ACTIVE_STROKE);
        editor.putString(CHAT_TAB_BACKGROUND, theme.CHAT_TAB_BACKGROUND);
        editor.putString(LEFT_DRAWER_BACKGROUND, theme.LEFT_DRAWER_BACKGROUND);
        editor.putString(CHAT_DIVIDER, theme.CHAT_DIVIDER);
        editor.putString(CHAT_BACKGROUND, theme.CHAT_BACKGROUND);
        editor.putString(CHAT_ITEM_BACKGROUND, theme.CHAT_ITEM_BACKGROUND);
        editor.putString(CHAT_USER_BACKGROUND, theme.CHAT_USER_BACKGROUND);
        editor.putString(CHAT_MARK, theme.CHAT_MARK);
        editor.putString(TAB_COUNTER_CIRCLE, theme.TAB_COUNTER_CIRCLE);
        editor.putString(COLOR_TAB_COUNTER_CIRCLE_STROKE, theme.COLOR_TAB_COUNTER_CIRCLE_STROKE);


        editor.commit();
    }
}

*/
