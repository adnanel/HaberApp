package adnan.haber.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import adnan.haber.HaberActivity;
import adnan.haber.R;
import adnan.haber.fragments.AboutFragment;
import adnan.haber.util.ChatSaver;
import adnan.haber.util.Debug;
import adnan.haber.util.Updater;


public class AdvancedPreferences extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.advanced_prefs);

    }

    public static boolean ShouldVibrate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrate", true);
    }

    public static boolean IsDebug(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("debugMode", true);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if ( preference.getKey().equals("about") ) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = getActivity().getLayoutInflater().inflate(R.layout.about, null);
                    final AboutFragment frag = (AboutFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.aboutFragment);

                    builder.setTitle("O aplikaciji");
                    builder.setNeutralButton("Zatvori", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.remove(frag);
                            transaction.commit();
                        }
                    });

                    builder.setCancelable(false);
                    builder.setView(view);
                    builder.create().show();
                }
            });
        } else if ( preference.getKey().equals("clearCache") ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Potvrdi brisanje");
            builder.setMessage("U kešu se nalazi:\n" + ChatSaver.getSavedLobbyMessagesCount() + " haber poruka\n" + ChatSaver.getSavedMessagesCount() + " privatnih poruka.");
            builder.setPositiveButton("Obriši", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ChatSaver.ClearCache();
                    Intent intent = new Intent(getActivity(), HaberActivity.class);
                    getActivity().finish();
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Prekid", null);
            builder.create().show();
        } else if ( preference.getKey().equals("debugMode")) {
            Debug.SetDebugMode( IsDebug(getActivity() ) );
        } else if ( preference.getKey().equals("version") ) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Provjeravam verziju...", Toast.LENGTH_SHORT).show();
                }
            });
            Updater.CheckForUpdates(getActivity());
        }

        return false;
    }
}
