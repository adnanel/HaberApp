package adnan.haber.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import adnan.haber.Haber;
import adnan.haber.HaberActivity;
import adnan.haber.HaberService;
import adnan.haber.R;
import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.packets.PacketTimeStamp;
import adnan.haber.types.ListChatItem;
import adnan.haber.types.MessageDirection;
import adnan.haber.util.Debug;
import adnan.haber.util.SmileyManager;
import adnan.haber.util.Util;

/**
 * Created by Adnan on 23.1.2015..
 */

public class ChatAdapter extends ArrayAdapter<ListChatItem> {
    private List<ListChatItem> items;
    private HaberActivity context;
    private CommandBarListener commandBarListener;
    private boolean isPrivate = false;
    private static String ownUsername = null;

    private static ArrayList<ChatAdapter> allAdapters = new ArrayList<ChatAdapter>();

    public ChatAdapter(HaberActivity context, List<ListChatItem> items, CommandBarListener cmdListener, boolean isPrivate) {
        super(context, R.layout.single_chat_item, items);
        this.items = items;
        this.context = context;
        this.commandBarListener = cmdListener;
        this.isPrivate = isPrivate;

        addAdapter(this);

    }

    public static synchronized void invalidateAll(final HaberActivity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( AdvancedPreferences.ShouldUseBalloons(activity))
                    activity.setListViewDivider(0);
                else
                    activity.setListViewDivider(1);

                for ( ChatAdapter adapter : allAdapters )
                    adapter.notifyDataSetChanged();
            }
        });
    }

    private synchronized void addAdapter(ChatAdapter adapter) {
        allAdapters.add(adapter);
    }

    private synchronized void removeAdapter(ChatAdapter adapter) {
        allAdapters.remove(adapter);
    }

    @Override
    public void finalize() throws Throwable {
        removeAdapter(this);
        super.finalize();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public void remove(ListChatItem item) {
        items.remove(item);
        notifyDataSetChanged();
    }

    public ListChatItem putDivider(String msg) {
        ListChatItem item = new ListChatItem(msg);
        if ( items.size() > 0 )
            item.time = items.get(items.size() - 1).time;

        items.add(item);
        notifyDataSetChanged();
        return item;
    }

    private static void makeLinksClickable(Spannable spannable) {
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
                spannable.setSpan(new URLSpan(spannable.subSequence(matcher.start(), matcher.end()).toString()),
                        matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public void addItem(Message msg) {
        addItem(msg, true);
        notifyDataSetChanged();
    }

    public void removeMessagesFromUser(String user) {
        ArrayList<ListChatItem> queue = new ArrayList<>();
        for ( ListChatItem item : items ) {
            if ( item.author == null ) continue;

            if ( item.author.equals(user) ) {
                queue.add(item);
            }
        }
        for (ListChatItem item : queue)
            items.remove(item);

        notifyDataSetChanged();
    }

    public void addItem(Message msg, boolean shouldNotifyDataSetChanged) {

        ListChatItem item = new ListChatItem();
        item.rank = HaberService.GetRankForUser(msg.getFrom());
        item.direction = msg.getSubject();

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


        for ( ListChatItem citem : items ) {
            if ( citem.isSpacer ) continue;

            if ( citem.id.equals(item.id) ) {
                Debug.log("Blocking duplicate message");
                return;
            }
        }

        for (PacketExtension ex : msg.getExtensions() ) {
            if ( ex instanceof PacketTimeStamp ) {
                item.time = Util.getDate(((PacketTimeStamp) ex).getTime());
                break;
            }
        }
        items.add(item);

        if ( shouldNotifyDataSetChanged ) {
            notifyDataSetChanged();
        }
    }

    private static ViewSwitcher lastSwitcher = null;

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(items, new Comparator<ListChatItem>() {
            @Override
            public int compare(ListChatItem lhs, ListChatItem rhs) {
                if ( lhs.time == null || rhs.time == null ) return 1;

                if ( lhs.time.getTime() < rhs.time.getTime() ) return -1;
                if ( lhs.time.getTime() > rhs.time.getTime() ) return 1;
                return 0;
            }
        });
        super.notifyDataSetChanged();
    }

    private boolean isMyMessage(ListChatItem item) {
        return (item.direction.equals(MessageDirection.OUTGOING) || Haber.getShortUsername(Haber.getUsername()).equals(Haber.getShortUsername(item.author)));
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView;
        if ( items.get(position).isSpacer ) {
            rowView = inflater.inflate(R.layout.single_divider_item, parent, false);

            ((TextView) rowView.findViewById(R.id.tvDivider)).setText(items.get(position).message);
        } else {
            TextView tvUsername;
            TextView tvMessage;

            ownUsername = Haber.getShortUsername(Haber.getUsername());

            if ( AdvancedPreferences.ShouldUseBalloons(context) ) {
                if (isMyMessage(items.get(position)) && AdvancedPreferences.ShouldAlignOwnMessagesRight(context) )
                    rowView = inflater.inflate(R.layout.single_own_balloon_chat_item, parent, false);
                else
                    rowView = inflater.inflate(R.layout.single_balloon_chat_item, parent, false);

            } else {
                if (isMyMessage(items.get(position)) && AdvancedPreferences.ShouldAlignOwnMessagesRight(context))
                    rowView = inflater.inflate(R.layout.single_own_chat_item, parent, false);
                else
                    rowView = inflater.inflate(R.layout.single_chat_item, parent, false);

                if (isMyMessage(items.get(position))) {
                    try {
                        rowView.findViewById(R.id.background).setBackgroundResource(R.drawable.adnan_bg);
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
                }
            }

            tvUsername = (TextView) rowView.findViewById(R.id.tvName);
            if ( tvUsername != null )
                tvUsername.setText(items.get(position).author);

            tvMessage = (TextView) rowView.findViewById(R.id.tvMessage);
            tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

            //mark name
            String string = " " + items.get(position).message + " ";
            Spannable msg = Spannable.Factory.getInstance().newSpannable(string);
            String mark = "@" + Haber.getUsername();
            if ( string.toUpperCase().contains(mark.toUpperCase()) ) {
                int start = string.toUpperCase().indexOf(mark.toUpperCase());
                int end = start + mark.length();
                msg.setSpan(new BackgroundColorSpan(Color.parseColor("#ff9e3e67")), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            //mark name without guest character
            if ( Haber.IsGuest() ) {
                mark = "@" + Haber.getUsername().substring(1);
                if ( string.toUpperCase().contains(mark.toUpperCase()) ) {
                    int start = string.toUpperCase().indexOf(mark.toUpperCase());
                    int end = start + mark.length();
                    msg.setSpan(new BackgroundColorSpan(Color.parseColor("#ff9e3e67")), start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            //smileys and urls
            SpannableStringBuilder strBuilder = new SpannableStringBuilder(msg);
            makeLinksClickable(strBuilder);
            URLSpan[] urls = strBuilder.getSpans(0, msg.length(), URLSpan.class);

            for(URLSpan cspan : urls) {
                makeLinkClickable(strBuilder, cspan);
            }

            strBuilder = SmileyManager.getSmiledText(context, strBuilder);

            tvMessage.setText(strBuilder);
            tvMessage.setIncludeFontPadding(false);

            TextView tvTime = (TextView) rowView.findViewById(R.id.tvTime);
            (tvTime).setText(Util.dateToFormat("HH:mm", items.get(position).time));



            final ViewSwitcher switcher = (ViewSwitcher)rowView.findViewById(R.id.viewSwitcher);
            if ( !items.get(position).author.equals(Haber.getUsername())) {
                View nameCarry = rowView.findViewById(R.id.nameCarry);
                if ( nameCarry != null ) {
                    nameCarry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (lastSwitcher != null) {
                                View btBack = lastSwitcher.findViewById(R.id.btBack);
                                if (btBack != null) btBack.performClick();
                            }

                            lastSwitcher = switcher;

                            LayoutInflater inflater = (LayoutInflater) context
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                            final View view = inflater.inflate(R.layout.single_user_menu, switcher, false);

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


                            if (!Haber.IsOnline(Haber.getFullUsername(items.get(position).author))) {
                                view.findViewById(R.id.btStartPrivate).setVisibility(View.GONE);
                            } else {
                                view.findViewById(R.id.btStartPrivate).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        commandBarListener.onPrivateMessage(Haber.getFullUsername(items.get(position).author));

                                        switcher.setBackgroundColor(0);
                                        switcher.removeView(view);
                                    }
                                });
                            }

                            if (isPrivate)
                                view.findViewById(R.id.btStartPrivate).setVisibility(View.GONE);

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

        }

        return rowView;
    }

    void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
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
