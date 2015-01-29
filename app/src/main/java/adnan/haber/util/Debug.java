package adnan.haber.util;

/**
 * Created by Adnan on 26.11.2014..
 */
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

public class Debug {
    public static boolean DebugBuild = true;
    public static String TAG = "Adnan";

    static FileWriter outputStream;

    static {
        try {
            //todo ako je release build, outputStream staviti na neki log file
        } catch ( Exception er ) {
            log(er);
        }
    }

    public static void Finalize() {
        try {
            if ( outputStream != null ) {
                outputStream.flush();
                outputStream.close();
            }
        } catch ( Exception er ) {
            log(er);
        }
    }

    public static void log(String msg) {
        if ( DebugBuild ) {
            if ( outputStream != null ) {
                try {
                    Log.i(TAG, msg);
                    outputStream.write(msg);
                } catch (Exception e) {
                    outputStream = null;
                    log(msg);
                }
            } else
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