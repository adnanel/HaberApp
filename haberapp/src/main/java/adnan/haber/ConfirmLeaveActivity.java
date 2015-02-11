package adnan.haber;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class ConfirmLeaveActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_leave);
    }


    public void shutDown(View v) {
        Intent service = new Intent(this, HaberService.class);
        stopService(service);

        finish();
    }

    public void stayConnected(View v) {
        finish();
    }
}
