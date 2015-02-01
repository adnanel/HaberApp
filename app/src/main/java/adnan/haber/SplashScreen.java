package adnan.haber;

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
    public void setStatus(String status) {
        View tv = findViewById(R.id.tvStatus);
        if ( tv == null ) return;
        ((TextView)tv).setText(status);
    }

    public String getStatus() {
        return ((TextView)findViewById(R.id.tvStatus)).getText().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        LeftDrawer.initialize(this);
        ChatSaver.Initialize(this);
        Debug.Initialize(this);

        if ( HaberService.isConnected ) {
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

    public void start() {
        Intent intent = new Intent(this, HaberActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityOptionsCompat activityOps = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
        ActivityCompat.startActivity(this, intent, activityOps.toBundle());
        finish();
    }

    int counter = 0;
    @Override
    public void onMessageReceived(Chat chat, Message message) {
        counter++;
        if ( counter >= 25 ) {
            start();
        }
    }

    @Override
    public void onRoomJoined(Chat chat) {

    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, String... params) {

    }

    @Override
    public void onSoftDisconnect() {

    }
}
