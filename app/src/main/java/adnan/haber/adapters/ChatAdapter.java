package adnan.haber.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jivesoftware.smack.packet.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adnan.haber.Haber;
import adnan.haber.HaberActivity;
import adnan.haber.HaberService;
import adnan.haber.R;
import adnan.haber.types.ListChatItem;
import adnan.haber.util.Debug;
import adnan.haber.util.SmileyManager;

/**
 * Created by Adnan on 23.1.2015..
 */

public class ChatAdapter extends ArrayAdapter<ListChatItem> {
    List<ListChatItem> items;
    HaberActivity context;
    CommandBarListener commandBarListener;

    public ChatAdapter(HaberActivity context, List<ListChatItem> items, CommandBarListener cmdListener) {
        super(context, R.layout.single_chat_item, items);
        this.items = items;
        this.context = context;
        this.commandBarListener = cmdListener;

    }

    @Override
    public int getCount() {
        return items.size();
    }

    public void putDivider(String msg) {
        items.add(new ListChatItem(msg));
        notifyDataSetChanged();
    }

    public static boolean makeLinksClickable(Context context, Spannable spannable) {
        boolean hasChanges = false;

        Matcher matcher = Patterns.WEB_URL.matcher(spannable);
        while (matcher.find()) {
            boolean set = true;
            for (URLSpan span : spannable.getSpans(matcher.start(),
                    matcher.end(), URLSpan.class))
                if (spannable.getSpanStart(span) >= matcher.start()
                        && spannable.getSpanEnd(span) <= matcher.end())
                    spannable.removeSpan(span);
                else {
                    set = false;
                    break;
                }
            if (set) {
                hasChanges = true;
                spannable.setSpan(new URLSpan(spannable.subSequence(matcher.start(), matcher.end()).toString()),
                        matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return hasChanges;
    }

    public void addItem(Message msg) {
        ListChatItem item = new ListChatItem();
        item.rank = HaberService.GetRankForUser(msg.getFrom());

        try {
            item.author = Haber.getShortUsername(msg.getFrom());
        } catch ( Exception er ) {
            Debug.log(er);
        }
        if ( msg.getBody() == null ) {
            Debug.log("Blocking message with null body!" + msg.toXML());
            return;
        }
        if ( msg.getBody().trim().length() == 0 ) {
            //or maybe show subject instead? (haber announcement je subject, no body)
            return;
        }

        item.message = msg.getBody();
        item.id      = msg.getPacketID();

        try {
            // todo
            String time = msg.toXML().toString();
            time = time.substring(time.indexOf("stamp=\"") + "stamp=\"".length());
            time = time.substring(0, time.indexOf("\"") + 1);

            int year = Integer.parseInt(time.substring(0, time.indexOf("-")));
            time = time.substring(time.indexOf("-") + 1);

            int month = Integer.parseInt(time.substring(0, time.indexOf("-")));
            time = time.substring(time.indexOf("-") + 1);

            int day = Integer.parseInt(time.substring(0, time.indexOf("T")));
            time = time.substring(time.indexOf("T") + 1);

            item.time = "Davno :o";
        } catch ( Exception e ) {
            Date date = new Date();
            item.time = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
        }

        item.time = "";
        items.add(item);
        notifyDataSetChanged();
    }

    static ViewSwitcher lastSwitcher = null;


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = null;
        if ( items.get(position).isSpacer ) {
            rowView = inflater.inflate(R.layout.single_divider_item, parent, false);

            ((TextView) rowView.findViewById(R.id.tvDivider)).setText(items.get(position).message);
        } else {
            TextView tvUsername;
            TextView tvMessage;

            rowView = inflater.inflate(R.layout.single_chat_item, parent, false);


            (tvUsername = (TextView) rowView.findViewById(R.id.tvName)).setText(items.get(position).author);

            tvMessage = (TextView) rowView.findViewById(R.id.tvMessage);
            tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

            //mark name
            String string = " " + items.get(position).message + " ";
            Spannable msg = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(string));
            String mark = "@" + Haber.getUsername();
            if ( string.contains(mark) ) {
                int start = string.indexOf(mark);
                int end = string.indexOf(mark) + mark.length();
                msg.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.NickMarkColor)), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            //mark name without guest character
            if ( Haber.IsGuest() ) {
                mark = "@" + Haber.getUsername().substring(1);
                if ( string.contains(mark) ) {
                    int start = string.indexOf(mark);
                    int end = string.indexOf(mark) + mark.length();
                    msg.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.NickMarkColor)), start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            //smileys and urls
            SpannableStringBuilder strBuilder = new SpannableStringBuilder(msg);
            makeLinksClickable(context, strBuilder);
            URLSpan[] urls = strBuilder.getSpans(0, msg.length(), URLSpan.class);

            for(URLSpan cspan : urls) {
                makeLinkClickable(strBuilder, cspan);
            }

            strBuilder = SmileyManager.getSmiledText(context, strBuilder);

            tvMessage.setText(strBuilder);
            tvMessage.setIncludeFontPadding(false);

            ((TextView) rowView.findViewById(R.id.tvTime)).setText(items.get(position).time);

            if ( items.get(position).author.equals(Haber.getShortUsername(Haber.getUsername()))) {
                try {
                    rowView.findViewById(R.id.background).setBackgroundResource(R.drawable.adnan_bg);
                } catch ( Exception er ) {
                    Debug.log(er);
                }
            }

            final ViewSwitcher switcher = (ViewSwitcher)rowView.findViewById(R.id.viewSwitcher);
            if ( !items.get(position).author.equals(Haber.getUsername())) {
                rowView.findViewById(R.id.nameCarry).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ( lastSwitcher != null ) {
                            View btBack = lastSwitcher.findViewById(R.id.btBack);
                            if ( btBack != null ) btBack.performClick();
                        }

                        lastSwitcher = switcher;

                        LayoutInflater inflater = (LayoutInflater) context
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        final View view = inflater.inflate(R.layout.single_user_menu, null);
                        switcher.addView(view);
                        switcher.setInAnimation(context, R.anim.slide_in_left);
                        switcher.setOutAnimation(context, R.anim.slide_out_right);
                        switcher.setDisplayedChild(1);

                        view.findViewById(R.id.btBack).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                switcher.setBackgroundColor(0);
                                switcher.removeView(view);
                            }
                        });

                        view.findViewById(R.id.btReply).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                commandBarListener.onReply(items.get(position).author);

                                switcher.setBackgroundColor(0);
                                switcher.removeView(view);
                            }
                        });

                        view.findViewById(R.id.btStartPrivate).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                commandBarListener.onPrivateMessage(items.get(position).author);

                                switcher.setBackgroundColor(0);
                                switcher.removeView(view);
                            }
                        });

                        view.findViewById(R.id.btKick).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                commandBarListener.onKick(items.get(position).author);

                                switcher.setBackgroundColor(0);
                                switcher.removeView(view);
                            }
                        });
                        view.findViewById(R.id.btKick).setVisibility(HaberService.CanKick() ? View.VISIBLE : View.GONE);
                    }
                });
            }
        }

        return rowView;
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
    {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        final String url = strBuilder.subSequence(start, end).toString();

        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                Toast.makeText(context, "Učitavam...", Toast.LENGTH_SHORT).show();
                context.openUrl(url);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }


    public interface CommandBarListener {
        public abstract void onKick(String user);
        public abstract void onReply(String user);
        public abstract void onPrivateMessage(String user);
    }
}
