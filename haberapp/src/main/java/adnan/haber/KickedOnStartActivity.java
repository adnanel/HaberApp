package adnan.haber;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import adnan.haber.util.CredentialManager;
import adnan.haber.util.Debug;


public class KickedOnStartActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kicked_on_start);

        if ( !Haber.IsGuest() ) {
            findViewById(R.id.btLogin).setVisibility(View.GONE);
        }

        TextView tvMessage = (TextView)findViewById(R.id.tvGuestMessage);
        if ( getIntent().hasExtra("user") ) {
            tvMessage.setText(String.format(getString(R.string.kicked_message), getIntent().getStringExtra("user"), getIntent().getStringExtra("reason")));
        } else {
            if ( !Haber.IsGuest() ) {
                tvMessage.setVisibility(View.GONE);
            }
        }
    }

    public void login(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Login");
        final View view = getLayoutInflater().inflate(R.layout.login, null);

        ((EditText)view.findViewById(R.id.editText2)).setText(CredentialManager.GetSavedUsername(KickedOnStartActivity.this));
        ((EditText)view.findViewById(R.id.editText3)).setText(CredentialManager.GetSavedPassword(KickedOnStartActivity.this));
        ((CheckBox)view.findViewById(R.id.cbRememberMe)).setChecked(CredentialManager.GetSavedPassword(KickedOnStartActivity.this).length() > 0);

        builder.setView(view);
        builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();

                String password;
                String username;

                username = ((EditText)view.findViewById(R.id.editText2)).getText().toString();
                password = ((EditText) view.findViewById(R.id.editText3)).getText().toString();

                if ( ((CheckBox)view.findViewById(R.id.cbRememberMe)).isChecked())
                    CredentialManager.Save(KickedOnStartActivity.this, username, password);
                else
                    CredentialManager.Save(KickedOnStartActivity.this, "", "");

                try {
                    Haber.Disconnect();

                    Haber.setUser(username);
                    Haber.setPassword(password);
                    Haber.setIsGuest(false);
                } catch ( Exception e ) {
                    Debug.log(e);
                }


                HaberService.RestartService(KickedOnStartActivity.this);
                Intent intent = new Intent(KickedOnStartActivity.this, SplashScreen.class);
                finish();
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Prekid", null);
        builder.create().show();
    }

    public void closeApp(View v) {
        try {
            Haber.setIsGuest(true);
            Haber.setUser("");
            Haber.setPassword("");
        } catch ( Exception er ) {
            Debug.log(er);
        }
        finish();
    }
}
