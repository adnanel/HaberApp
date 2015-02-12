package adnan.haber;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import android.widget.Toast;

import org.jivesoftware.smack.Chat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import adnan.haber.fragments.ReportFragment;
import adnan.haber.types.Rank;
import adnan.haber.util.CredentialManager;
import adnan.haber.util.Debug;
import adnan.haber.util.RankIconManager;

public class LeftDrawer extends PreferenceFragment {
    public static final String TAG  = "Left_drawer_fragment";

    private Context context;

    private ArrayList<Preference> onlineUsers = new ArrayList<Preference>();
    private PreferenceCategory prefcat;

    public static LeftDrawer newInstance(Context context) {
        LeftDrawer instance = new LeftDrawer();
        instance.context = context;
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(Color.parseColor("#ff3c3f41"));

        if ( prefcat == null ) {
            final Preference btLogin = new Preference(getActivity());
            if ( Haber.IsGuest() ) {
                btLogin.setKey("login");
                btLogin.setTitle("Login");
            } else {
                btLogin.setKey("logout");
                btLogin.setSummary(Haber.getUsername());
                new Thread() {
                    @Override
                    public void run() {
                        this.setPriority(Thread.MIN_PRIORITY);
                        while ( !this.isInterrupted() ) {
                            try {
                                if ( Haber.isConnected() ) break;
                                Thread.sleep(100);
                            } catch ( Exception er ) {
                                Debug.log(er);
                            }
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btLogin.setTitle(RankIconManager.getSpanned(getActivity(), Haber.getUsername(), "Logout"));
                            }
                        });
                    }
                }.start();
            }

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

    void refreshOnlineList() {
        if ( HaberService.getHaberChat() == null ) {
            Debug.log("HaberService isn't running...");
            return;
        }

        Object[] users = HaberService.getHaberChat().getOccupants().toArray();
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
            pref.setLayoutResource(R.layout.online_user_list_item);

            Spannable span = RankIconManager.getSpanned(getActivity(), user);

            pref.setTitle( span );
            if ( pref.getTitle().equals(Haber.getUsername()) ) continue;

            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    for (Object o : HaberActivity.getInstance().chatThreads.entrySet()) {
                        Map.Entry pairs = (Map.Entry) o;
                        if (((HaberActivity.ChatThread) pairs.getValue()).getUser().equals(preference.getKey())) {
                            HaberActivity.getInstance().onRoomJoined((Chat) pairs.getKey(), true);
                            return false;
                        }

                    }

                    Haber.StartChat(preference.getKey());
                    try {
                        HaberActivity.getInstance().closeLeftDrawer();
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
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
                        Thread.sleep(3500);
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
        if ( preference.getKey().equals("themes") ) {
            /*Intent intent = new Intent(getActivity(), ThemeChooser.class);
            ActivityOptionsCompat activityOps = ActivityOptionsCompat.makeCustomAnimation(getActivity(), R.anim.slide_in_right, R.anim.slide_out_left);
            ActivityCompat.startActivity(getActivity(), intent, activityOps.toBundle());
            ThemeChooser.activity = (HaberActivity)getActivity();
            */
        } else if ( preference.getKey().equals("login") ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Login");
            final View view = getActivity().getLayoutInflater().inflate(R.layout.login, null);

            ((EditText)view.findViewById(R.id.editText2)).setText(CredentialManager.GetSavedUsername(getActivity()));
            ((EditText)view.findViewById(R.id.editText3)).setText(CredentialManager.GetSavedPassword(getActivity()));
            ((CheckBox)view.findViewById(R.id.cbRememberMe)).setChecked(CredentialManager.GetSavedPassword(getActivity()).length() > 0);

            builder.setView(view);
            builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Debug.log("Finishing activity due to login...");
                    getActivity().finish();

                    final String username = ((EditText)view.findViewById(R.id.editText2)).getText().toString();
                    final String password = ((EditText) view.findViewById(R.id.editText3)).getText().toString();


                    if ( ((CheckBox)view.findViewById(R.id.cbRememberMe)).isChecked())
                        CredentialManager.Save(getActivity(), username, password, ((CheckBox)view.findViewById(R.id.cbLoginOnStartup)).isChecked());
                    else
                        CredentialManager.Save(getActivity(), "", "", false);



                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Haber.Disconnect();
                                Haber.setUser(username);
                                Haber.setPassword(password);
                                Haber.setIsGuest(false);
                            } catch ( Exception e ) {
                                Debug.log(e);
                            }


                            HaberService.RestartService(getActivity());
                            Intent intent = new Intent(getActivity(), SplashScreen.class);
                            startActivity(intent);
                        }
                    }.start();

                }
            });
            builder.setNegativeButton("Prekid", null);
            builder.create().show();
        } else if ( preference.getKey().equals("logout") ) {
            Haber.setIsGuest(true);
            Haber.setPassword("");

            HaberService.RestartService(getActivity());
            Intent intent = new Intent(getActivity(), SplashScreen.class);
            Debug.log("finishing activity duo to logout...");
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
