package adnan.haber;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import adnan.haber.util.Debug;

public class StopServiceBroadcast extends BroadcastReceiver {
    public StopServiceBroadcast() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (HaberActivity.InstanceExists()) {
            HaberActivity.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.getInstance());
                    builder.setMessage("Potvrdi izlazak");
                    builder.setTitle("Haber");
                    builder.setPositiveButton("Gasi haber", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                HaberService.StopService();
                            } catch ( Exception e ) {
                                Debug.log(e);
                            }

                            Debug.log("Finishing because of user interaction (haber shutting down from notification)");
                            HaberActivity.getInstance().finish();
                        }
                    });
                    builder.setNegativeButton("Ostani spojen", null);
                    builder.create().show();
                }
            });
            return;
        }

        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));


        Intent activity = new Intent(context, ConfirmLeaveActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activity);

    }
}
