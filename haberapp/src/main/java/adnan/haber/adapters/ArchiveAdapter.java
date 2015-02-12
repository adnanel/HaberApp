package adnan.haber.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

import adnan.haber.Haber;
import adnan.haber.HaberActivity;
import adnan.haber.HaberService;
import adnan.haber.R;
import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.packets.PacketTimeStamp;
import adnan.haber.types.ArchiveItem;
import adnan.haber.types.ListChatItem;
import adnan.haber.util.Debug;
import adnan.haber.util.SmileyManager;
import adnan.haber.util.Util;

/**
 * Created by Adnan on 23.1.2015..
 */

public class ArchiveAdapter extends ArrayAdapter<ArchiveItem> {
    private List<ArchiveItem> items;
    private Context context;

    public ArchiveAdapter(Context context, List<ArchiveItem> items) {
        super(context, R.layout.single_chat_item, items);
        this.items = items;
        this.context = context;
    }


    @Override
    public int getCount() {
        return items.size();
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.single_archive_item, parent, false);


        ((TextView)rowView.findViewById(R.id.tvUser)).setText(Haber.getShortUsername(items.get(position).username));
        ((TextView)rowView.findViewById(R.id.tvMsgCount)).setText(items.get(position).messageCount + "");

        return rowView;
    }


}
