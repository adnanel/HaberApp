package adnan.haber.util;

/**
 * Created by Adnan on 26.11.2014..
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;

import adnan.haber.fragments.AdvancedPreferences;

public class Debug {
    private static String TAG = "Adnan";
    private static Context context;

    public static void Initialize(Context context) {
        Debug.context = context;
        // todo old method was too heavy. find something else
    }

    public static void log(String msg) {
        if (AdvancedPreferences.IsDebug(context) ) {
            Log.i(TAG, msg);
        }
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