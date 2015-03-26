package adnan.haber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import adnan.haber.util.Debug;

public class RestartServiceBroadcast extends BroadcastReceiver {
    public RestartServiceBroadcast() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (HaberActivity.InstanceExists()) {
            HaberActivity.getInstance().finish();
        }

        try {
            HaberService.StopService();
        } catch ( Exception er ) {
            Debug.log(er);
        }

        Intent splash = new Intent(context, SplashScreen.class);
        splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(splash);
    }
}
