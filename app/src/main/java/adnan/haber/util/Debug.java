package adnan.haber.util;

/**
 * Created by Adnan on 26.11.2014..
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

public class Debug {
    public static boolean DebugBuild = true;
    public static String TAG = "Adnan";

    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;

    final static String PREFS = "log";
    final static String PREF_COUNT = "count";
    final static String PREF_ITEM  = "logitem";

    public static void Initialize(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized void SetDebugMode(boolean isDebug) {
        log("Logging disabled!");

        DebugBuild = isDebug;

        log("Logging enabled!");
    }

    public static File DumpToFile() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory()
                    + "/haber/");
            dir.mkdirs();

            File file = new File(dir, "log.dmp");
            PrintWriter output = new PrintWriter(file);

            int count = sharedPreferences.getInt(PREF_COUNT, 0);
            for ( int i = 0; i < count; i ++ ) {
                output.println(sharedPreferences.getString(PREF_ITEM + i, ""));
            }
            output.flush();

            return file;
        } catch ( Exception er ) {
            Debug.log(er);
            return null;
        }
    }

    public static void log(String msg) {
        if ( DebugBuild ) {
            Log.i(TAG, msg);
        }


        int count;
        editor.putInt(PREF_COUNT, count = sharedPreferences.getInt(PREF_COUNT, 0) + 1);
        editor.putString( PREF_ITEM + (count - 1), msg);

        editor.commit();
    }


    public static void log(Exception e) {
        String print = "Stack trace:\n";
        for ( StackTraceElement stack : e.getStackTrace() ) {
            print += stack.toString() + "\n";
        }
        print += "------------------\n";
        print += "cause: " + e.getCause();
        print += "class: " + e.getClass();
        print += "------------------\n";

        log( e.toString() + "\n\n" + print);
    }
}