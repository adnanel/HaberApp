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

import adnan.haber.util.ChatSaver;
import adnan.haber.util.Debug;


public class SplashScreen extends ActionBarActivity implements Haber.HaberListener {
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

        Debug.Initialize(this); // <- must be first
        LeftDrawer.initialize(this);
        ChatSaver.Initialize(this);

        if ( Haber.isConnected() ) {
            start();
            return;
        }

        Intent intent = new Intent(this, HaberService.class);
        startService(intent);
        HaberService.addHaberListener(this);

        if ( savedInstanceState == null ) {
            findViewById(R.id.imageView).startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_anim));
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HaberService.removeHaberListener(this);
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

    @Override
    public void onStatusChanged(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setStatus(status);


            }
        });

    }


    @Override
    public void onLoggedIn(MultiUserChat haberChat) {

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

    private int counter = 0;
    @Override
    public void onMessageReceived(Chat chat, Message message) {
        counter++;
        if ( counter >= 26 ) {
            start();
        }
        Debug.log(counter + "");
    }

    @Override
    public void onRoomJoined(Chat chat) {

    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, final String... params) {
        if ( started ) return;

        if ( params.length == 2 && ( event == Haber.ChatEvent.Banned || event == Haber.ChatEvent.Kicked ) ) {
            canStart = false;
        }
    }

    @Override
    public void onSoftDisconnect() {
        finish();
    }

}
