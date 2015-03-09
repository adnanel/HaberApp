package adnan.haber.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import adnan.haber.HaberService;
import adnan.haber.R;

/**
 * Created by Adnan on 24.1.2015..
 */
public class Updater {
    private final static int version = 108;
    private static AlertDialog dialog;


    public static void CheckForUpdates(final Activity activity, final boolean shouldToast ) {
        new Thread() {
            @Override
            public void run() {
                this.setPriority(MIN_PRIORITY);

                Looper.prepare();
                try {
                    final JSON.UpdateInfo info = JSON.getUpdateInfo();
                    if ( Integer.parseInt(info.version) > version ) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle("Nova verzija dostupna!");

                        String changeLog = Util.DownloadString(info.changeLogUrl);

                        View view = activity.getLayoutInflater().inflate(R.layout.changelog, null);
                        ((TextView)view.findViewById(R.id.tvChangeLog)).setText(Html.fromHtml(changeLog));

                        builder.setView(view);
                        //deprecated: builder.setMessage(info.changeLog);

                        builder.setPositiveButton("Download!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final ProgressDialog progressDialog = new ProgressDialog(activity);
                                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                                        final Downloader downloader = new Downloader() {
                                            @Override
                                            public void onProgressUpdate(Integer... values) {
                                                progressDialog.setProgress(values[0]);
                                                if (values[0] == 100) {
                                                    progressDialog.dismiss();
                                                    try {
                                                        Intent intent = new Intent(activity, HaberService.class);
                                                        activity.stopService(intent);
                                                    } catch ( Exception e ) {
                                                        Debug.log(e);
                                                    }

                                                    Debug.log("finishing activity (updater)");
                                                    activity.finish();

                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setDataAndType(Uri.fromFile(new File(apkDir)), "application/vnd.android.package-archive");
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    activity.startActivity(intent);
                                                }

                                            }

                                            @Override
                                            public void onPostExecute(String param) {
                                                if ( param == null ) {
                                                    //success!
                                                    return;
                                                }
                                                progressDialog.dismiss();
                                                Debug.log(param);
                                                Toast.makeText(activity, param, Toast.LENGTH_LONG).show();
                                            }
                                        };


                                        progressDialog.setTitle("Downloadujem...");
                                        progressDialog.setProgress(0);
                                        progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Prekini", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                progressDialog.dismiss();
                                                downloader.cancel(true);
                                            }
                                        });


                                        String path = (Environment.getExternalStorageDirectory() + "/haberTemp/");
                                        (new File(path)).mkdirs();
                                        File file = new File(path, info.version + ".apk");

                                        downloader.execute(info.apkUrl, file.getAbsolutePath());
                                        progressDialog.show();

                                    }
                                });
                            }
                        });
                        builder.setNegativeButton("Nah..", null);
                        builder.setCancelable(false);


                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog = builder.create();
                                dialog.show();
                            }
                        });

                    } else {
                        if ( shouldToast )
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity, "Nema nove verzije!", Toast.LENGTH_SHORT).show();
                                }
                            });
                    }
                } catch ( Exception e ) {
                    Debug.log(e);
                }
            }
        }.start();
    }

    public static class Downloader extends AsyncTask<String, Integer, String> {
        String apkDir;

        @Override
        protected String doInBackground(String... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                output = new FileOutputStream(apkDir = params[1], false);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        if ( progress != 100 )
                            publishProgress(progress);
                    }
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            onProgressUpdate(100);
            return null;
        }
    }
}
