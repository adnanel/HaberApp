package adnan.haber.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
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
import adnan.haber.util.ChatSaver;
import adnan.haber.util.Debug;
import adnan.haber.util.Updater;


public class AdvancedPreferences extends PreferenceFragment {
    public boolean shouldInvalidatedChatAdapters = false;

    @Override
    public void onDestroy() {
        if ( shouldInvalidatedChatAdapters ) {
            if ( HaberActivity.getInstance() != null )
                ChatAdapter.invalidateAll(HaberActivity.getInstance());
        }

        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        shouldInvalidatedChatAdapters = false;

        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.advanced_prefs);

        CheckBoxPreference pref = (CheckBoxPreference)this.getPreferenceScreen().findPreference("ownMessageAlignRight");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                shouldInvalidatedChatAdapters = true;
                return true;
            }
        });

        (this.getPreferenceScreen().findPreference("balloonchatitems")).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                shouldInvalidatedChatAdapters = true;
                return true;
            }
        });
    }

    public static boolean AutoReconnectEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("autoReconnect", false);
    }

    public static boolean ShouldBlink(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("blinkTab", true);
    }
    public static boolean ShowJoinedLeftNotifications(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("showJoinedLeft", false);
    }
    public static boolean ShouldClearNotifications(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("deleteNotificationsEnabled", true);
    }


    public static int GetStatusChangeTimeout(Context context) {
        try {
            return 1000 * Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("notificationTimeout", "1"));
        } catch ( Exception er ) {
            return 5000;
        }
    }

    public static boolean ShouldVibrate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrate", true);
    }

    public static boolean ShouldVibrateInService(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrateinbackground", true);
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

    public static boolean ShouldUseBalloons(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("balloonchatitems", true);
    }

    public static boolean IsDebug(Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("debugMode", true);
        } catch ( Exception er ) {
            return true;
        }
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

                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.remove(frag);
                            transaction.commit();
                        }
                    });
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
        } else if ( preference.getKey().equals("version") ) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Provjeravam verziju...", Toast.LENGTH_SHORT).show();
                }
            });
            Updater.CheckForUpdates(getActivity(), true);
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
                            String bug = ((EditText)frag.getView().findViewById(R.id.editText5)).getText().toString();
                            String report = ((EditText)frag.getView().findViewById(R.id.editText4)).getText().toString();;

                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("message/rfc822");
                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"adnanel94@gmail.com"});
                            i.putExtra(Intent.EXTRA_SUBJECT, "Bug report!");
                            i.putExtra(Intent.EXTRA_TEXT   , "Username: " + Haber.getUsername() +
                                    "\nBug: " + bug +
                                    "\nKako rekonstruisati: " + report);

                            File file = Debug.getFile();
                            if (!file.exists() || !file.canRead()) {
                                Toast.makeText(getActivity(), "Attachment Error", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Uri uri = Uri.parse("file://" + file);
                            i.putExtra(Intent.EXTRA_STREAM, uri);

                            try {
                                startActivity(Intent.createChooser(i, "Izaberi mail app..."));
                            } catch (android.content.ActivityNotFoundException ex) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "Nije nadjen ni jedan mail klijent!", Toast.LENGTH_SHORT).show();
                                    }

                                });
                            }

                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.remove(frag);
                            transaction.commit();
                        }
                    });

                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.remove(frag);
                            transaction.commit();
                        }
                    });
                    builder.setView(view);
                    builder.create().show();
                }
            });
        }

        return false;
    }
}
