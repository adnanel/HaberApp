package adnan.haber;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import java.util.ArrayList;

import adnan.haber.adapters.ThemeAdapter;
import adnan.haber.types.Theme;
import adnan.haber.util.Debug;
import adnan.haber.util.JSON;
import adnan.haber.util.ThemeManager;


public class ThemeChooser extends ActionBarActivity {
    public static HaberActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_chooser);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ThemeChooserFragment())
                    .commit();
        }
    }


    public static class ThemeChooserFragment extends Fragment {
        RecyclerView recList;
        ThemeAdapter themeAdapter;
        ViewAnimator viewAnimator;

        public ThemeChooserFragment() {
            new ThemeDownloader() {
                @Override
                public void onPostExecute(ArrayList<Theme> themes) {
                    if ( themes == null ) {
                        viewAnimator.setDisplayedChild(2);
                    } else {
                        for ( Theme theme : themes )
                            themeAdapter.addItem(theme);
                        viewAnimator.setDisplayedChild(1);
                    }
                }
            }.execute();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_theme_chooser, container, false);

            viewAnimator = (ViewAnimator)rootView.findViewById(R.id.viewSwitcher2);
            recList = (RecyclerView) rootView.findViewById(R.id.themeList);
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);
            if ( themeAdapter == null ) {
                recList.setAdapter(themeAdapter = new ThemeAdapter(new ArrayList<Theme>(), new ThemeAdapter.OnThemeChangedListener() {

                    @Override
                    public void onApplyTheme(final Theme theme) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final ProgressDialog dialog = new ProgressDialog(getActivity());
                                final ThemeDownloader themeDownload = new ThemeDownloader() {
                                    @Override
                                    public void onPostExecute(ArrayList<Theme> themes) {
                                        dialog.dismiss();
                                        ThemeManager.ApplyTheme(themes.get(0));
                                        getActivity().finish();
                                        activity.finish();

                                        Intent intent = new Intent(getActivity(), HaberActivity.class);
                                        startActivity(intent);
                                    }
                                };

                                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                dialog.setTitle("Preuzimam...");
                                dialog.setButton(DialogInterface.BUTTON1, "Prekini!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        themeDownload.cancel(true);
                                    }
                                });
                                dialog.show();
                                themeDownload.execute(theme);
                            }
                        });
                    }
                }));
                viewAnimator.setDisplayedChild(0);
            } else {
                recList.setAdapter(themeAdapter);
                viewAnimator.setDisplayedChild(1);
            }


            return rootView;
        }


        private class ThemeDownloader extends AsyncTask<Theme, String, ArrayList<Theme>> {
            boolean isLoader = false;

            @Override
            protected ArrayList<Theme> doInBackground(Theme... params) {
                if ( params.length == 1 ) {
                    //download a specific theme
                    isLoader = true;
                    try {
                        ArrayList<Theme> result = new ArrayList<>();
                        result.add(JSON.GetTheme(params[0]));
                        return result;
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
                }
                try {
                    return JSON.getThemes();
                } catch ( Exception er ) {
                    Debug.log(er);
                }
                return null;
            }
        }
    }
}
