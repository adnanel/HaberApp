package adnan.haber;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import adnan.haber.adapters.ChatAdapter;
import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.fragments.SmileyChooser;
import adnan.haber.types.ListChatItem;
import adnan.haber.util.ChatSaver;
import adnan.haber.util.Debug;
import adnan.haber.util.Updater;
import adnan.haber.util.Util;


public class HaberActivity extends ActionBarActivity implements Haber.HaberListener {
    HashMap<Chat, ChatThread> chatThreads = new HashMap<>();
    private ChatThread mainChatThread;
    private ListView chatListView;
    private AlertDialog smileyDialog;

    private boolean vibrationLock = true;
    private static HaberActivity instance = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private float lastTranslate = 0.0f;
    private DrawerLayout mDrawerLayout;

    private RelativeLayout rlContent;

    private ChatAdapter.CommandBarListener cmdListener = new ChatAdapter.CommandBarListener() {

        @Override
        public void onKick(final String user) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.this);
                    builder.setTitle("Razlog?");
                    final EditText etReason = new EditText(HaberActivity.this);
                    builder.setView(etReason);
                    builder.setPositiveButton("Kick!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            HaberService.KickUser(user, etReason.getText().toString());
                        }
                    });
                    builder.setNegativeButton("Prekini", null);
                    builder.create().show();
                }
            });
        }

        @Override
        public void onReply(String user) {
            appendMessage("@" + Haber.getShortUsername(user) + " ");
        }

        @Override
        public void onPrivateMessage(String user) {
            for (Object o : chatThreads.entrySet()) {
                Map.Entry pairs = (Map.Entry) o;
                if (((HaberActivity.ChatThread) pairs.getValue()).getUser().equals(user)) {
                    ((ChatThread) pairs.getValue()).tabView.performClick();
                    return;
                }

            }

            Haber.StartChat(user);
        }
    };

    private Runnable sendMessage = new Runnable() {
        @Override
        public void run() {
            final EditText editText = (EditText)findViewById(R.id.editText);
            String str = editText.getText().toString();
            if ( str.length() == 0 ) return;
            while ( str.charAt(0) == '\n' && str.length() > 1 ) str = str.substring(1);
            while ( str.charAt(str.length() - 1) == '\n' && str.length() > 1 ) str = str.substring(0, str.length() - 1);

            if ( str.trim().length() == 0 ) return;

            Chat chat;
            try {
                chat = getCurrentChat();
            } catch ( Exception e ) {
                Debug.log(e);
                return;
            }

            if ( chat == null ) {
                try {
                    HaberService.haberChat.sendMessage(editText.getText().toString());
                } catch (Exception e) {
                    Debug.log(e);
                }
            } else {
                try {
                    String body = editText.getText().toString();
                    chat.sendMessage(body);

                    Message msg = new Message();
                    msg.setBody(body);
                    msg.setFrom(Haber.getFullUsername(Haber.getUsername()));
                    msg.setTo(chat.getParticipant());

                    try {
                        msg.setPacketID(Util.makeSHA1Hash(msg.getFrom() + msg.getBody() + Util.getRandomInt(100)));
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
                    ChatSaver.OnMessageReceived(chat, msg);

                    chatThreads.get(chat).chatAdapter.addItem(msg);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatListView.post(new Runnable(){
                                public void run() {
                                    chatListView.smoothScrollToPosition(chatListView.getCount() - 1);
                                }});
                        }
                    });
                } catch (Exception e) {
                    Debug.log(e);
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    editText.setText("");
                }
            });
        }
    };

    void sortTabs() {
        final TreeMap<Integer, ArrayList<View>> tabs = new TreeMap<Integer, ArrayList<View>>(new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return lhs < rhs ? 1 : lhs > rhs ? -1 : 0;
            }
        });

        for (Object o : chatThreads.entrySet()) {
            Map.Entry pairs = (Map.Entry) o;
            ChatThread thread = (ChatThread)pairs.getValue();

            if ( tabs.containsKey(thread.getUnreadMessagesCount()))
                tabs.get(thread.getUnreadMessagesCount()).add(thread.tabView);
            else {
                ArrayList<View> views = new ArrayList<View>();
                views.add(thread.tabView);
                tabs.put(thread.getUnreadMessagesCount(), views);
            }
        }

        final LinearLayout scrollView = (LinearLayout)findViewById(R.id.tabCarry);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.removeAllViews();
                scrollView.addView(mainChatThread.tabView);

                for (Object o : tabs.entrySet()) {
                    Map.Entry pair = (Map.Entry) o;
                    for ( View v : (ArrayList<View>)pair.getValue() ) {
                        scrollView.addView(v);
                    }
                }
                tabs.clear();
            }
        });
    }

    public void openUrl(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.this);
                builder.setNegativeButton("Zatvori", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                View view = getLayoutInflater().inflate(R.layout.urlviewer, null);

                WebView webView = (WebView)view.findViewById(R.id.webView);
                webView.getSettings().setJavaScriptEnabled(true);
                if ( url.endsWith(".jpg") && url.startsWith("http://pokit.org/get/?")) {
                    String nUrl = url.replace("get/?", "get/img/");
                    webView.loadUrl(nUrl);
                } else
                    webView.loadUrl(url);

                builder.setView(view);
                AlertDialog dialog = builder.create();
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.show();
            }
        });
    }

    Chat getCurrentChat()  {
        if ( chatListView.getAdapter() == mainChatThread.chatAdapter )
            return null;

        for (Object o : chatThreads.entrySet()) {
            Map.Entry pairs = (Map.Entry) o;
            if (chatListView.getAdapter() == ((ChatThread)pairs.getValue()).chatAdapter)
                return (Chat) pairs.getKey();
        }

        Debug.log(new Exception("This isn't supposed to happen! EVERR"));
        return null;
    }


    public void onSmileyClick(View v) {
        try {
            SmileyChooser frag = (SmileyChooser) getSupportFragmentManager().findFragmentById(R.id.smileyFragment);
            frag.onSmileyClick(v);
            smileyDialog.dismiss();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(frag);
            transaction.commit();

        } catch ( Exception e ) {
            Debug.log(e);
        }
    }

    public void appendMessage(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText editText = (EditText)findViewById(R.id.editText);
                editText.append(str);
                editText.requestFocus();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, HaberService.class);
        startService(intent);
    }

    @Override
    public void onDestroy() {
        HaberService.removeHaberListener(this);
        super.onDestroy();
    }

    public static boolean InstanceExists() {
        return instance != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        setContentView(R.layout.activity_haber);
        Updater.CheckForUpdates(this);

        chatListView = (ListView)findViewById(R.id.chatListView);
        setListenerToRootView();

        //glavni Haber chat
        mainChatThread = new ChatThread("haber");
        mainChatThread.chatAdapter = new ChatAdapter(this, new ArrayList<ListChatItem>(), cmdListener, false);
        //load old messages
        mainChatThread.chatAdapter.putDivider("Stare poruke");
        for ( Message msg : ChatSaver.getSavedLobbyMessages() ) {
            if ( msg.getPacketID().equals("divider") )
                mainChatThread.chatAdapter.putDivider(msg.getBody());
            else
                mainChatThread.chatAdapter.addItem(msg, false);
        }
        mainChatThread.chatAdapter.putDivider("Ova sesija");

        for ( Message msg : Haber.getCachedLobbyMessages() )
            mainChatThread.chatAdapter.addItem(msg, false);
        mainChatThread.chatAdapter.notifyDataSetChanged();

        //ostali chatovi
        for ( Chat chats : HaberService.chatRooms ) {
            ChatThread thread = new ChatThread(chats.getParticipant());
            thread.chatAdapter = new ChatAdapter(this, new ArrayList<ListChatItem>(), cmdListener, true);


            for ( Message msg : ChatSaver.getSavedMessages() ) {
                if ( msg.getFrom().equals(chats.getParticipant()) || msg.getTo().equals(chats.getParticipant())) {
                    thread.chatAdapter.addItem(msg, false);
                }
            }

            thread.chatAdapter.notifyDataSetChanged();
            thread.setState(State.Normal);
            chatThreads.put(chats, thread);
        }

        HaberService.addHaberListener(this);

        chatListView.setAdapter(mainChatThread.chatAdapter);


        findViewById(R.id.btSmile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.this);
                        View view = getLayoutInflater().inflate(R.layout.smiley_chooser, null);
                        final SmileyChooser frag = (SmileyChooser)getSupportFragmentManager().findFragmentById(R.id.smileyFragment);

                        builder.setNeutralButton("Zatvori", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.remove(frag);
                                transaction.commit();
                            }
                        });

                        builder.setCancelable(false);
                        builder.setView(view);
                        smileyDialog = builder.create();
                        smileyDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                        smileyDialog.show();
                    }
                });

            }
        });

        //todo shift enter fix
        (findViewById(R.id.editText)).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ( keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN ) {
                    if ( event.isShiftPressed() ) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //appendMessage("\n");
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sendMessage.run();
                            }
                        });

                        return true;
                    }

                }
                return false;
            }
        });

        findViewById(R.id.btSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage.run();
            }
        });


        vibrationLock = false;


        //left drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        rlContent = (RelativeLayout) findViewById(R.id.rlContent);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_launcher, R.string.acc_drawer_open, R.string.acc_drawer_close)
        {
            @SuppressLint("NewApi")
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                float moveFactor = (Util.DpiToPixel(HaberActivity.this, 240) * slideOffset);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                {
                    rlContent.setTranslationX(moveFactor);
                }
                else
                {
                    TranslateAnimation anim = new TranslateAnimation(lastTranslate, moveFactor, 0.0f, 0.0f);
                    anim.setDuration(0);
                    anim.setFillAfter(true);
                    rlContent.startAnimation(anim);

                    lastTranslate = moveFactor;
                }
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
            if ( !drawer.isDrawerOpen(Gravity.LEFT) ) {
                openLeftDrawer();
            } else {
                closeLeftDrawer();
            }
        } else if ( keyCode == KeyEvent.KEYCODE_ENTER && event.getRepeatCount() == 0 ) {
            findViewById(R.id.btSend).performClick();
        } else {
            return super.onKeyDown(keyCode, event);
        }

        return false;
    }

    @Override
    public void onStatusChanged(String status) {
        Debug.log(status);
    }


    @Override
    public void onLoggedIn(MultiUserChat haberChat) {

    }


    @Override
    public void onMessageReceived(final Chat chat, final Message message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean vibratedYet = false;

                if ( chat != null ) {
                    if  ( AdvancedPreferences.ShouldVibrate(HaberActivity.this) && !vibrationLock ) {
                        if ( (getCurrentChat() != chat) || (getCurrentChat() == chat && AdvancedPreferences.ShouldVibrateOnActive(HaberActivity.this)) ) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            vibratedYet = true;
                        }
                    }


                    if ( chatThreads.containsKey(chat) ) {
                        chatThreads.get(chat).chatAdapter.addItem(message);
                        try {
                            Chat cchat = getCurrentChat();
                            if (cchat != chat)
                                chatThreads.get(chat).markTab();

                        } catch ( Exception er ) {
                            Debug.log(er);
                        }
                    } else {
                        ChatThread thread = new ChatThread(chat.getParticipant());
                        thread.chatAdapter = new ChatAdapter(HaberActivity.this, new ArrayList<ListChatItem>(), cmdListener, true);
                        for ( Message msg : ChatSaver.getSavedMessages() ) {
                            if ( msg.getFrom().equals(chat.getParticipant()) || msg.getTo().equals(chat.getParticipant())) {
                                if ( msg.getPacketID().equals("divider") )
                                    thread.chatAdapter.putDivider(msg.getBody());
                                else
                                    thread.chatAdapter.addItem(msg);
                            }
                        }

                        thread.chatAdapter.addItem(message);
                        chatThreads.put(chat, thread);

                        try {
                            Chat cchat = getCurrentChat();
                            if (cchat != chat)
                                thread.markTab();

                        } catch ( Exception er ) {
                            Debug.log(er);
                        }
                    }
                }
                else {
                    if  ( AdvancedPreferences.ShouldVibrate(HaberActivity.this) && !vibrationLock  ) {
                        if ( (getCurrentChat() != null && AdvancedPreferences.ShouldVibrateOnPublic(HaberActivity.this)) || (getCurrentChat() == null && AdvancedPreferences.ShouldVibrateOnActive(HaberActivity.this)) ) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            vibratedYet = true;
                        }
                    }

                    mainChatThread.chatAdapter.addItem(message);
                    try {
                        if ( getCurrentChat() != null )
                            mainChatThread.markTab();
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
                }

                chatListView.post(new Runnable(){
                    public void run() {
                        chatListView.smoothScrollToPosition(chatListView.getCount() - 1);
                    }});


                sortTabs();


                //check for @Reply and vibrates
                if ( !vibrationLock && !vibratedYet && AdvancedPreferences.ShouldVibrateOnReply(HaberActivity.this) && (message.getBody() != null) ) {
                    String mark = "@" + Haber.getUsername();
                    if ( message.getBody().toUpperCase().contains(mark.toUpperCase()) ) {
                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                        vibratedYet = true;
                    } else if (Haber.IsGuest() ) {
                        mark = "@" + Haber.getUsername().substring(1);
                        if ( message.getBody().toUpperCase().contains(mark.toUpperCase()) ) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            vibratedYet = true;
                        }
                    }
                }
            }
        });

    }

    private boolean isSoftKeyboardOpened = false;

    void setListenerToRootView(){
        final View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 100 ) { // 99% of the time the height diff will be due to a keyboard.

                    if(!isSoftKeyboardOpened){
                        scrollToBottom(false);
                    }
                    isSoftKeyboardOpened = true;
                }else if(isSoftKeyboardOpened){
                    isSoftKeyboardOpened = false;
                    scrollToBottom(false);
                }
            }
        });
    }

    void scrollToBottom(final boolean smooth) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatListView.post(new Runnable(){
                    public void run() {
                        if ( smooth )
                            chatListView.smoothScrollToPosition(chatListView.getCount() - 1);
                        else
                            chatListView.setSelection(chatListView.getCount() - 1);
                    }});
            }
        });
    }
    public void closeLeftDrawer() {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if ( drawer.isDrawerOpen(Gravity.LEFT) ) {
            drawer.closeDrawer(Gravity.LEFT);
        }
    }

    public void openLeftDrawer() {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if ( !drawer.isDrawerOpen(Gravity.LEFT) ) {
            drawer.openDrawer(Gravity.LEFT);
        }
    }

    @Override
    public void onRoomJoined(final Chat chat) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if ( !chatThreads.containsKey(chat) ) {
                    ChatThread thread = new ChatThread(chat.getParticipant());
                    thread.chatAdapter = new ChatAdapter(HaberActivity.this, new ArrayList<ListChatItem>(), cmdListener, true);
                    for ( Message msg : ChatSaver.getSavedMessages() ) {
                        if ( msg.getFrom().equals(chat.getParticipant()) || msg.getTo().equals(chat.getParticipant())) {
                            thread.chatAdapter.addItem(msg);
                        }
                    }

                    if ( AdvancedPreferences.ShouldSwitchToNewTab(HaberActivity.this))
                        thread.tabView.performClick();

                    chatThreads.put(chat, thread);
                } else {
                    if ( AdvancedPreferences.ShouldSwitchToNewTab(HaberActivity.this))
                        chatThreads.get(chat).tabView.performClick();

                }
            }
        });
    }

    @Override
    public void onChatEvent(final Haber.ChatEvent event, final String... params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String message = "UNPARSED: " + event.toString();
                if ( event == Haber.ChatEvent.Kicked ) {
                    if ( params.length == 2 ) {
                        //the user got kicked.

                        message = params[0] + " vas kickuje, razlog: " + params[1];
                        for (Object o : chatThreads.entrySet()) {
                            final Map.Entry pairs = (Map.Entry) o;
                            final ListChatItem item = ((ChatThread)pairs.getValue()).chatAdapter.putDivider(message);
                            if ( AdvancedPreferences.ShouldClearNotifications(HaberActivity.this)) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(AdvancedPreferences.GetStatusChangeTimeout(HaberActivity.this));
                                        } catch (Exception er) {
                                            Debug.log(er);
                                        }

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((ChatThread) pairs.getValue()).chatAdapter.remove(item);
                                            }
                                        });
                                    }
                                }.start();
                            }
                        }

                    } else {
                        //someone else got kicked

                        message = params[1] + " kickuje " + params[0] + ", razlog: " + params[2];
                    }
                } else if ( event == Haber.ChatEvent.Banned ) {
                    if ( params.length == 2 ) {
                        //the user got kicked.

                        message = params[0] + " vas banuje, razlog: " + params[1];
                        for (Object o : chatThreads.entrySet()) {
                            Map.Entry pairs = (Map.Entry) o;
                            ((ChatThread)pairs.getValue()).chatAdapter.putDivider(message);
                        }

                    } else {
                        //someone else got kicked

                        message = params[1] + " banuje " + params[0] + ", razlog: " + params[2];

                    }
                }
               final ListChatItem item = mainChatThread.chatAdapter.putDivider(message);

                if ( AdvancedPreferences.ShouldClearNotifications(HaberActivity.this)) {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(AdvancedPreferences.GetStatusChangeTimeout(HaberActivity.this));
                            } catch (Exception er) {
                                Debug.log(er);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainChatThread.chatAdapter.remove(item);
                                }
                            });
                        }
                    }.start();
                }

                scrollToBottom(true);
            }
        });
    }

    @Override
    public void onSoftDisconnect() {
        for (Object o : chatThreads.entrySet()) {
            Map.Entry pairs = (Map.Entry) o;
            ((ChatThread)pairs.getValue()).closeChat();
        }

        chatThreads.clear();

        Debug.log("finishing due to onSoftDisconnect...");
        finish();
    }


    enum State {
        Marked,
        Active,
        Normal
    }

    public class ChatThread {
        private ChatAdapter chatAdapter;
        private View tabView;
        private String user;

        private View tabBackground;
        private View rlMsgCounter;
        private TextView tvMsgCounter;

        State currentState;

        public void setState(final State state) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ( state == State.Active ) {
                        tabBackground.setBackgroundResource(R.drawable.tab_background_active);

                        rlMsgCounter.setVisibility(View.INVISIBLE);
                        tvMsgCounter.setText("0");
                    } else if ( state == State.Normal ) {
                        tabBackground.setBackgroundResource(R.drawable.tab_background);

                        rlMsgCounter.setVisibility(View.INVISIBLE);
                        tvMsgCounter.setText("0");
                    } else if ( state == State.Marked ) {
                        tabBackground.setBackgroundResource(R.drawable.tab_background_selected);

                        rlMsgCounter.setVisibility(View.VISIBLE);
                    }

                }
            });
            currentState = state;
        }

        View btClose;
        ChatThread thisThread;

        public String getUser() {
            return user;
        }


        public void closeChat() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btClose.performClick();
                }
            });

        }

        public int getUnreadMessagesCount() {
            return Integer.parseInt(tvMsgCounter.getText().toString());
        }

        public void markTab() {
            setState(State.Marked);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMsgCounter.setText((Integer.parseInt(tvMsgCounter.getText().toString()) + 1) + "");
                }
            });

        }

        public ChatThread(String other) {
            thisThread = this;
            user = other;

            other = Haber.getFullUsername(other);

            final String participant = other;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final LinearLayout scrollView = (LinearLayout)findViewById(R.id.tabCarry);

                    tabView = getLayoutInflater().inflate(R.layout.single_tab, null);

                    rlMsgCounter = tabView.findViewById(R.id.rlMessageCounter);

                    tvMsgCounter = (TextView)tabView.findViewById(R.id.tvMessageCounter);

                    tabBackground = tabView.findViewById(R.id.rlTabBackground);
                    ((TextView)tabView.findViewById(R.id.tvUser)).setText(Haber.getShortUsername(participant));

                    tabView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (Object o : chatThreads.entrySet()) {
                                        Map.Entry pairs = (Map.Entry) o;
                                        ((ChatThread)pairs.getValue()).setState(State.Normal);
                                    }

                                    mainChatThread.setState(State.Normal);
                                    setState(State.Active);

                                    chatListView.setAdapter(chatAdapter);
                                    chatListView.post(new Runnable(){
                                        public void run() {
                                            chatListView.smoothScrollToPosition(chatListView.getCount() - 1);
                                        }});

                                    closeLeftDrawer();
                                }
                            });
                        }
                    });

                    btClose = tabView.findViewById(R.id.tvClose);
                    if ( participant.equals("haber") )
                        btClose.setVisibility(View.INVISIBLE);

                    setState(State.Active);

                    btClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Animation anim = AnimationUtils.loadAnimation(HaberActivity.this, R.anim.tab_anim);
                            anim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) { }
                                @Override
                                public void onAnimationRepeat(Animation animation) { }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    for (Object o : chatThreads.entrySet()) {
                                        Map.Entry pairs = (Map.Entry) o;
                                        if (thisThread == pairs.getValue()) {
                                            chatThreads.remove(pairs.getKey());
                                            HaberService.chatRooms.remove(pairs.getKey());
                                            break;
                                        }
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            scrollView.removeView(tabView);
                                            mainChatThread.tabView.performClick();
                                        }
                                    });
                                }

                            });

                            tabView.startAnimation(anim);
                        }
                    });
                    scrollView.addView(tabView);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if ( drawer.isDrawerOpen(Gravity.LEFT) ) {
            closeLeftDrawer();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Imaš pametnija posla !?");
        builder.setPositiveButton("Gasi haber", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread() {
                    @Override
                    public void run() {

                        try {
                            Intent intent = new Intent(HaberActivity.this, HaberService.class);
                            stopService(intent);
                        } catch ( Exception e ) {
                            Debug.log(e);
                        }

                        Debug.log("Finishing because of user interaction (haber shutting down)");
                        finish();
                    }
                }.start();
            }
        });
        builder.setNeutralButton("Sakrij se u pozadini", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Debug.log("Finishg because of user interaction (move to back)");
                finish();
            }
        });
        builder.setNegativeButton("Vrati se na haber", null);
        builder.create().show();
    }
}
