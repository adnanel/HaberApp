package adnan.haber;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import org.jivesoftware.smack.Chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import adnan.haber.fragments.AboutFragment;
import adnan.haber.types.Rank;
import adnan.haber.util.ChatSaver;
import adnan.haber.util.CredentialManager;
import adnan.haber.util.Debug;
import adnan.haber.util.RankIconManager;

public class LeftDrawer extends PreferenceFragment {
    static SharedPreferences preferences;

    ArrayList<Preference> onlineUsers = new ArrayList<Preference>();
    PreferenceCategory prefcat;


    public static void initialize(Context context) {
        String preference = context.getPackageName() + "_preferences";

        preferences = context.getSharedPreferences(preference, Context.MODE_PRIVATE);
    }


    public static LeftDrawer newInstance() {
        LeftDrawer fragment = new LeftDrawer();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        view.setBackgroundColor(Color.parseColor("#FF3C3F41"));
        if ( prefcat == null ) {
            Preference btLogin = new Preference(getActivity());
            btLogin.setKey(Haber.IsGuest() ? "login" : "logout");
            btLogin.setTitle(Haber.IsGuest() ? "Login" : "Logout");
            btLogin.setOrder(-1);
            getPreferenceScreen().addPreference(btLogin);

            prefcat = new PreferenceCategory(getActivity());
            prefcat.setOrder(0);
            prefcat.setTitle("Online korisnici:");
            getPreferenceScreen().addPreference(prefcat);
        }

        refreshOnlineList();

        return view;
    }

    public void refreshOnlineList() {
        Object[] users = HaberService.haberChat.getOccupants().toArray();
        Arrays.sort(users, new Comparator<Object>() {
            @Override
            public int compare(Object lhs, Object rhs) {
                Rank r1 = HaberService.GetRankForUser((String)rhs);
                Rank r2 = HaberService.GetRankForUser((String)lhs);
                int n1 = r1.toInt();
                int n2 = r2.toInt();
                return n1 > n2 ? 1 : n1 < n2 ? -1 : 0;
            }
        });

        prefcat.removeAll();
        onlineUsers.clear();

        for ( Object obj : users ) {
            String user = (String)obj;
            if ( Haber.getShortUsername(user).equals(Haber.getUsername()))
                continue;

            boolean found = false;
            for ( Preference pref : onlineUsers ) {
                if ( pref.getKey().equals(user) ) {
                    found = true;
                    break;
                }
            }
            if ( found ) continue;

            Preference pref = new Preference(getActivity());

            pref.setKey(user);

            Rank rank = HaberService.GetRankForUser(user);
            String span = "</guest>";
            if ( rank == Rank.Moderator || rank == Rank.Admin ) {
                span = "</moderator>";
            } else if ( rank == Rank.Adnan ) {
                span = "</adnan>";
            } else if ( rank == Rank.User ) {
                span = "</user>";
            } else if ( rank == Rank.Enil ) {
                span = "</enil>";
            } else if ( rank == Rank.Berina ) {
                span = "</berina>";
            } else if ( rank == Rank.Mathilda ) {
                span = "</mathilda>";
            } else if ( rank == Rank.Alma ) {
                span = "</alma>";
            } else if ( rank == Rank.Memi ) {
                span = "</memi>";
            }

            Spannable spannable = RankIconManager.getSpanned(getActivity(), span + "  " + Haber.getShortUsername(user));
            //todo add other ranks here

            pref.setTitle( spannable );
            if ( pref.getTitle().equals(Haber.getUsername()) ) continue;

            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    HaberActivity activity = (HaberActivity) getActivity();

                    for (Object o : activity.chatThreads.entrySet()) {
                        Map.Entry pairs = (Map.Entry) o;
                        if (((HaberActivity.ChatThread) pairs.getValue()).getUser().equals(preference.getKey())) {
                            activity.onRoomJoined((Chat) pairs.getKey());
                            return false;
                        }

                    }

                    Haber.StartChat(preference.getKey());
                    return false;
                }
            });

            prefcat.addPreference(pref);
            onlineUsers.add(pref);
        }
    }


    public LeftDrawer() {
        //required empty constructor


    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        this.addPreferencesFromResource(R.xml.left_drawer);

        new Thread() {
            Thread thread;

            @Override
            public void run() {
                thread = this;
                while ( !this.isInterrupted() ) {
                    try {
                        Thread.sleep(1000);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    refreshOnlineList();
                                } catch ( Exception e ) {
                                    Debug.log(e);
                                    thread.interrupt();
                                }
                            }
                        });
                    } catch ( Exception e ) {
                        break;
                    }
                }
            }
        }.start();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if ( preference.getKey().equals("login") ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Login");
            final View view = getActivity().getLayoutInflater().inflate(R.layout.login, null);

            ((EditText)view.findViewById(R.id.editText2)).setText(CredentialManager.GetSavedUsername());
            ((EditText)view.findViewById(R.id.editText3)).setText(CredentialManager.GetSavedPassword());
            ((CheckBox)view.findViewById(R.id.cbRememberMe)).setChecked(CredentialManager.GetSavedPassword().length() > 0);

            builder.setView(view);
            builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();

                    String password;
                    String username;

                    Haber.setUser(username = ((EditText)view.findViewById(R.id.editText2)).getText().toString());
                    Haber.setPassword(password = ((EditText) view.findViewById(R.id.editText3)).getText().toString());
                    Haber.setIsGuest(false);

                    if ( ((CheckBox)view.findViewById(R.id.cbRememberMe)).isChecked())
                        CredentialManager.Save(username, password);

                    try {
                        Haber.Disconnect();
                    } catch ( Exception e ) {
                        Debug.log(e);
                    }

                    Haber.setUser(username);
                    Haber.setPassword(password);
                    Haber.setIsGuest(false);

                    HaberService.RestartService(getActivity());
                    Intent intent = new Intent(getActivity(), SplashScreen.class);
                    getActivity().finish();
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Prekid", null);
            builder.create().show();
        } else if ( preference.getKey().equals("logout") ) {
            Haber.setIsGuest(true);
            Haber.setPassword("");

            HaberService.RestartService(getActivity());
            Intent intent = new Intent(getActivity(), SplashScreen.class);
            getActivity().finish();
            startActivity(intent);
        } else if ( preference.getKey().equals("advanced")) {
            Intent intent = new Intent(getActivity(), AdvancedPrefsActivity.class);
            ActivityOptionsCompat activityOps = ActivityOptionsCompat.makeCustomAnimation(getActivity(), R.anim.slide_in_right, R.anim.slide_out_left);
            ActivityCompat.startActivity(getActivity(), intent, activityOps.toBundle());
        }

        return true;
    }
}
