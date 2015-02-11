package adnan.haber.util;

/**
 * Created by Adnan on 26.11.2014..
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import adnan.haber.fragments.AdvancedPreferences;

public class Debug {
    private static String TAG = "Adnan";
    private static Context context;

    private static PrintWriter outputFile;
    private static File cFile;

    public static void Initialize(Context context) {
        Debug.context = context;

        try {
            outputFile = new PrintWriter(new FileOutputStream(makeNewFile()));
        } catch ( Exception er ) {
            log(er.toString());
        }
    }

    private static File makeNewFile() {
        String dir = Environment.getExternalStorageDirectory() + "/haber";
        (new File(dir)).mkdirs();

        cFile = new File(dir, "log" + Util.getRandomInt(100) + ".log");
        return cFile;
    }

    public static File getFile() {
        try {
            outputFile.flush();
            outputFile.close();

            File f = cFile;

            makeNewFile();

            return f;
        } catch ( Exception er ) {
            Debug.log(er);
            return null;
        }
    }

    public static void log(String msg) {
        if (AdvancedPreferences.IsDebug(context) ) {
            outputFile.println(msg);
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