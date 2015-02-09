package adnan.haber.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import adnan.haber.Haber;
import adnan.haber.HaberActivity;
import adnan.haber.R;
import adnan.haber.adapters.ChatAdapter;
import adnan.haber.fragments.AboutFragment;
import adnan.haber.util.ChatSaver;
import adnan.haber.util.Debug;
import adnan.haber.util.Updater;


public class AdvancedPreferences extends PreferenceFragment {
    public boolean shouldInvalidatedChatAdapters = false;

    @Override
    public void onDestroy() {
        if ( shouldInvalidatedChatAdapters ) {
            ChatAdapter.invalidateAll(getActivity());
        }

        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        shouldInvalidatedChatAdapters = false;

        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.advanced_prefs);

        SwitchPreference pref = (SwitchPreference)this.getPreferenceScreen().findPreference("ownMessageAlignRight");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                shouldInvalidatedChatAdapters = true;
                return true;
            }
        });
    }

    public static boolean ShouldVibrate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrate", true);
    }

    public static boolean ShouldVibrateOnPublic(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrateonpublic", true);
    }

    public static boolean ShouldVibrateOnActive(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrateonactive", true);
    }

    public static boolean ShouldAlignOwnMessagesRight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ownMessageAlignRight", true);
    }

    public static boolean ShouldVibrateOnReply(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrateonreply", true);
    }

    public static boolean ShouldSwitchToNewTab(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNewChat", true);
    }


    private static boolean IsDebug(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("debugMode", true);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if ( preference.getKey().equals("git") ) {
            String url = "https://github.com/adnanel/HaberApp";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } else if ( preference.getKey().equals("about") ) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = getActivity().getLayoutInflater().inflate(R.layout.about, null);
                    final AboutFragment frag = (AboutFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.aboutFragment);

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
                    Debug.log("finishing due to cache clear...");
                    getActivity().finish();
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Prekid", null);
            builder.create().show();
        } else if ( preference.getKey().equals("debugMode")) {
            Debug.SetDebugMode(IsDebug(getActivity()));
        } else if ( preference.getKey().equals("version") ) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Provjeravam verziju...", Toast.LENGTH_SHORT).show();
                }
            });
            Updater.CheckForUpdates(getActivity());
        } else if (preference.getKey().equals("reportbug") ) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = getActivity().getLayoutInflater().inflate(R.layout.report, null);
                    final ReportFragment frag = (ReportFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.fragReport);

                    builder.setTitle("Prijavi bug");
                    builder.setNegativeButton("Prekid", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.remove(frag);
                            transaction.commit();
                        }
                    });

                    builder.setPositiveButton("Posalji", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if ( 1 < 2 ) return;

                            String bug = ((EditText)frag.getView().findViewById(R.id.editText5)).getText().toString();
                            String report = ((EditText)frag.getView().findViewById(R.id.editText4)).getText().toString();;

                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("message/rfc822");
                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"adnanel94@gmail.com"});
                            i.putExtra(Intent.EXTRA_SUBJECT, "Bug report!");
                            i.putExtra(Intent.EXTRA_TEXT   , "Username: " + Haber.getUsername() +
                                    "\nBug: " + bug +
                                    "\nKako rekonstruisati: " + report);

                            File file = null;//Debug.DumpToFile();
                            if (!file.exists() || !file.canRead()) {
                                Toast.makeText(getActivity(), "Attachment Error", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Uri uri = Uri.parse("file://" + file);
                            i.putExtra(Intent.EXTRA_STREAM, uri);

                            try {
                                startActivity(Intent.createChooser(i, "Izaberi mail app..."));
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(getActivity(), "Nije nadjen ni jedan mail klijent!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    builder.setCancelable(false);
                    builder.setView(view);
                    builder.create().show();
                }
            });
        }

        return false;
    }
}
