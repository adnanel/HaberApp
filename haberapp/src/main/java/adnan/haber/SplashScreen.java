package adnan.haber;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import adnan.haber.util.Debug;


public class SplashScreen extends ActionBarActivity {
    private boolean started = false;
    private boolean canStart = true;

    void setStatus(String status) {
        View tv = findViewById(R.id.tvStatus);
        if ( tv == null ) return;
        ((TextView)tv).setText(status);
    }

    String getStatus() {
        return ((TextView)findViewById(R.id.tvStatus)).getText().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        setStatus("");

        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2500);
                } catch ( Exception er ) {
                    Debug.log(er);
                }
                SplashScreen.this.start();
            }
        }).start();

        if ( savedInstanceState == null ) {
            findViewById(R.id.imageView).startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_anim));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        try {
            bundle.putString("status", getStatus());
        } catch ( Exception e ) {
            Debug.log(e);
        }
    }

    void start() {
        if ( !canStart || started ) return;

        Intent intent = new Intent(this, HaberActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityOptionsCompat activityOps = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
        ActivityCompat.startActivity(this, intent, activityOps.toBundle());
        finish();
        started = true;
    }

}
