package adnan.haber.util;

/**
 * Created by Adnan on 26.11.2014..
 */
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

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
        if ( context == null ) {
            return;
        }

        if (AdvancedPreferences.IsDebug(context) ) {
            try {
                outputFile.println(msg);
            } catch ( Exception er ) {
                //
            }

            Log.i(TAG, msg);
        }
    }


    public static void log(Exception e) {
        StringBuilder sb = new StringBuilder();
        e.printStackTrace(new PrintWriter(new LogWriter(sb)));
        log(sb.toString());
    }

    public static class LogWriter extends Writer {
        private StringBuilder builder;
        public LogWriter(StringBuilder builder) {
            super();
            this.builder = builder;
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void write(char[] buf, int offset, int count) throws IOException {
            builder.append(buf, offset, count);
        }
    }
}