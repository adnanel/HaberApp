package adnan.haber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopServiceBroadcast extends BroadcastReceiver {
    public StopServiceBroadcast() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        Intent activity = new Intent(context, ConfirmLeaveActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activity);

    }
}
