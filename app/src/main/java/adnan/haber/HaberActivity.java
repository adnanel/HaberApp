package adnan.haber;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    ChatThread mainChatThread;
    ListView chatListView;
    AlertDialog smileyDialog;

    ChatAdapter.CommandBarListener cmdListener = new ChatAdapter.CommandBarListener() {

        @Override
        public void onKick(String user) {
            HaberService.KickUser(user, "");
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


    public void sortTabs() {
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
                webView.loadUrl(url);   //todo currently just white

                builder.setView(view);
                AlertDialog dialog = builder.create();
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.show();
            }
        });
    }

    public Chat getCurrentChat() throws Exception {
        if ( chatListView.getAdapter() == mainChatThread.chatAdapter )
            return null;

        for (Object o : chatThreads.entrySet()) {
            Map.Entry pairs = (Map.Entry) o;
            if (chatListView.getAdapter() == ((ChatThread)pairs.getValue()).chatAdapter)
                return (Chat) pairs.getKey();
        }

        Debug.log("This isn't supposed to happen!");
        throw new Exception("Oh maw gaaaawd");
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
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_haber);
        Updater.CheckForUpdates(this);

        chatListView = (ListView)findViewById(R.id.chatListView);
        setListenerToRootView();

        //glavni Haber chat
        mainChatThread = new ChatThread("haber");
        mainChatThread.chatAdapter = new ChatAdapter(this, new ArrayList<ListChatItem>(), cmdListener);
        //load old messages
        mainChatThread.chatAdapter.putDivider("Stare poruke");
        for ( Message msg : ChatSaver.getSavedLobbyMessages() ) {
            if ( msg.getPacketID().equals("divider") )
                mainChatThread.chatAdapter.putDivider(msg.getBody());
            else
                mainChatThread.chatAdapter.addItem(msg);
        }
        mainChatThread.chatAdapter.putDivider("Ova sesija");

        for ( Message msg : Haber.getCachedLobbyMessages() )
            mainChatThread.chatAdapter.addItem(msg);


        //ostali chatovi
        for ( Chat chats : HaberService.chatRooms ) {
            ChatThread thread = new ChatThread(chats.getParticipant());
            thread.chatAdapter = new ChatAdapter(this, new ArrayList<ListChatItem>(), cmdListener);


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
        ((EditText)findViewById(R.id.editText)).setOnKeyListener(new View.OnKeyListener() {
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
                                findViewById(R.id.btSend).performClick();
                            }
                        });
                    }
                }
                return false;
            }
        });

        findViewById(R.id.btSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                            msg.setPacketID(Util.makeSHA1Hash(msg.getFrom() + msg.getBody()));
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
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
            if ( !drawer.isDrawerOpen(Gravity.LEFT) ) {
                drawer.openDrawer(Gravity.LEFT);
            } else {
                drawer.closeDrawer(Gravity.LEFT);
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


                if  ( AdvancedPreferences.ShouldVibrate(HaberActivity.this))
                    ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);

                if ( chat != null ) {
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
                        thread.chatAdapter = new ChatAdapter(HaberActivity.this, new ArrayList<ListChatItem>(), cmdListener);
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
            }
        });

    }

    boolean isSoftKeyboardOpened = false;

    public void setListenerToRootView(){
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

    public void scrollToBottom(final boolean smooth) {
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

    @Override
    public void onRoomJoined(final Chat chat) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if ( !chatThreads.containsKey(chat) ) {
                    ChatThread thread = new ChatThread(chat.getParticipant());
                    thread.chatAdapter = new ChatAdapter(HaberActivity.this, new ArrayList<ListChatItem>(), cmdListener);
                    for ( Message msg : ChatSaver.getSavedMessages() ) {
                        if ( msg.getFrom().equals(chat.getParticipant()) || msg.getTo().equals(chat.getParticipant())) {
                            thread.chatAdapter.addItem(msg);
                        }
                    }

                    thread.tabView.performClick();

                    chatThreads.put(chat, thread);
                } else {
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
                            Map.Entry pairs = (Map.Entry) o;
                            ((ChatThread)pairs.getValue()).chatAdapter.putDivider(message);
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
                mainChatThread.chatAdapter.putDivider(message);

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

                                    DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
                                    if ( drawer.isDrawerOpen(Gravity.LEFT) ) {
                                        drawer.closeDrawer(Gravity.LEFT);
                                    }
                                }
                            });
                        }
                    });

                    tabView.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            // todo
                            if ( event.getActionMasked() == MotionEvent.ACTION_DOWN ) {
                                Debug.log("here");
                            }

                            return false;
                        }
                    });
                    btClose = tabView.findViewById(R.id.tvClose);
                    if ( participant.equals("haber") )
                        btClose.setVisibility(View.INVISIBLE);

                    setState(State.Active);

                    btClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
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
                    scrollView.addView(tabView);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if ( drawer.isDrawerOpen(Gravity.LEFT) ) {
            drawer.closeDrawer(Gravity.LEFT);
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

                        finish();
                    }
                }.start();
            }
        });
        builder.setNeutralButton("Sakrij se u pozadini", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton("Vrati se na haber", null);
        builder.create().show();
    }
}
