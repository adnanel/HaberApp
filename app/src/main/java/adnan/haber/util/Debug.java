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

public class Debug {
    private static boolean DebugBuild = true;
    private static String TAG = "Adnan";

    public static void Initialize(Context context) {
        // todo old method was too heavy. find something else
    }

    public static synchronized void SetDebugMode(boolean isDebug) {
        log("Logging disabled!");

        DebugBuild = isDebug;

        log("Logging enabled!");
    }


    public static void log(String msg) {
        if ( DebugBuild ) {
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