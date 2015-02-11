package adnan.haber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopServiceBroadcast extends BroadcastReceiver {
    public StopServiceBroadcast() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, HaberService.class);
        context.stopService(service);
    }
}
