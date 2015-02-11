package adnan.haber.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import adnan.haber.HaberActivity;
import adnan.haber.R;


public class SmileyChooser extends Fragment {


    public SmileyChooser() {
        // Required empty public constructor
    }


    public void onSmileyClick(View v) {
        String smiley = "";

        switch ( v.getId() ) {
            case R.id.thumbsdown:
                smiley = "(n)";
                break;

            case R.id.thumbsup:
                smiley = "(y)";
                break;

            case R.id.angel:
                smiley = "O:)";
                break;

            case R.id.ambivalent:
                smiley = "-.-";
                break;

            case R.id.wink:
                smiley = ";)";
                break;

            case R.id.largegasp:
                smiley = ":o";
                break;

            case R.id.ohnoes:
                smiley = ">.<";
                break;

            case R.id.naughty:
                smiley = ":666:";
                break;

            case R.id.halo:
                smiley = ":)";
                break;

            case R.id.laugh:
                smiley = ":D";
                break;

            case R.id.frown:
                smiley = ":(";
                break;

            case R.id.grin:
                smiley = "^^";
                break;

            case R.id.stickingouttongue:
                smiley = ":p";
                break;

            case R.id.confused:
                smiley = ":s";
                break;

            case R.id.undecided:
                smiley = ":/";
                break;

            case R.id.hot:
                smiley = "8)";
                break;

            case R.id.moneymouth:
                smiley = "$)";
                break;

            case R.id.lipsaresealed:
                smiley = ":x";
                break;
        }

        ((HaberActivity)getActivity()).appendMessage(" " + smiley + " ");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_smiley_chooser, container, false);
    }


}
