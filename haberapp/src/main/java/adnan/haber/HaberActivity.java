package adnan.haber;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Pair;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import adnan.haber.adapters.ChatAdapter;
import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.fragments.SmileyChooser;
import adnan.haber.packets.PacketTimeStamp;
import adnan.haber.types.ListChatItem;
import adnan.haber.types.MessageDirection;
import adnan.haber.util.Debug;
import adnan.haber.util.HaberSSLSocketFactory;
import adnan.haber.util.Updater;
import adnan.haber.util.Util;
import adnan.haber.views.TabView;


public class HaberActivity extends ActionBarActivity implements Haber.HaberListener, Haber.RoleChangeListener {
    static HashMap<Chat, ChatThread> chatThreads = new HashMap<>();
    static private ChatThread mainChatThread;
    private ListView chatListView;
    private AlertDialog smileyDialog;
    private LeftDrawer mLeftDrawer;


    private boolean vibrationLock = true;
    private static HaberActivity instance = null;
    private float lastTranslate = 0.0f;

    public HashMap<Chat, ArrayList<Message>> messageStack = new HashMap<Chat, ArrayList<Message>>();
    public boolean initialized = false;

    private RelativeLayout rlContent;
    private TextView mStatusLabel;

    public static HaberActivity getInstance() {
        return instance;
    }

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

    public Chat findChatForUser(String user) {
        String other  = Haber.getShortUsername(user);

        for (Object o : chatThreads.entrySet()) {
            Map.Entry pairs = (Map.Entry) o;
            ChatThread thread = (ChatThread)pairs.getValue();

            if ( Haber.getShortUsername(thread.fullUser).equalsIgnoreCase(other) ) return (Chat)pairs.getKey();
        }
        return null;
    }

    public void sendMessage(String to, String body) {
        Chat chat;
        try {
            chat = findChatForUser(to);
        } catch ( Exception e ) {
            Debug.log(e);
            return;
        }

        if ( chat == null ) {
            try {
                if ( !Haber.IsHellBanned() ) {
                    HaberService.getHaberChat().sendMessage(body);
                } else {
                    Message msg = new Message();
                    msg.setBody(body);
                    msg.setFrom(Haber.getFullUsername(Haber.getUsername()));
                    msg.setSubject(MessageDirection.OUTGOING);

                    try {
                        msg.addExtension(new PacketTimeStamp(msg));
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }

                    mainChatThread.chatAdapter.addItem(msg);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainChatThread.chatAdapter.notifyDataSetChanged();
                            scrollToBottom(true);
                        }
                    });
                }
            } catch (Exception e) {
                Debug.log(e);
            }
        } else {
            try {
                chat.sendMessage(body);

                Message msg = new Message();
                msg.setBody(body);
                msg.setFrom(Haber.getFullUsername(Haber.getUsername()));
                msg.setTo(chat.getParticipant());
                msg.setSubject(MessageDirection.OUTGOING);

                try {
                    msg.addExtension(new PacketTimeStamp(msg));
                } catch ( Exception er ) {
                    Debug.log(er);
                }

                chatThreads.get(chat).chatAdapter.addItem(msg);

                scrollToBottom(true);
            } catch (Exception e) {
                Debug.log(e);
            }
        }
    }

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
            if ( chat != null )
                sendMessage(chat.getParticipant(), editText.getText().toString());
            else
                sendMessage("haber", editText.getText().toString());

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
                ArrayList<View> views = new ArrayList<>();
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
                EditText editText = (EditText) findViewById(R.id.editText);
                editText.append(str);
                editText.requestFocus();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        instance = this;

        Intent intent = new Intent(this, HaberService.class);
        startService(intent);

        HaberService.resetCounters();

    }


    @Override
    public void onPause() {
        instance = null;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static boolean InstanceExists() {
        return instance != null;
    }

    public void setListViewDivider(int height) {
        chatListView.setDividerHeight(height);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        (new Thread() {
            AlertDialog dialog;

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.this);
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        });
                        View view = getLayoutInflater().inflate(R.layout.startup_loader, null);
                        mStatusLabel = (TextView)view.findViewById(R.id.tvStatus);
                        builder.setView(view);
                        dialog = builder.create();
                        dialog.show();

                        HaberService.StartService(HaberActivity.this, new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                    }
                });
                int timer = 0;
                while ( !Haber.isConnected() ) {
                    try {
                        Thread.sleep(250);
                        timer += 250;
                        if ( timer >= 10 * 1000 ) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.this);
                                    builder.setTitle("Neuspješno!");
                                    builder.setMessage("Nisam se uspio konektovati na haber, pokušati ponovo?");
                                    builder.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(HaberActivity.this, SplashScreen.class);
                                            finish();
                                            startActivity(intent);
                                        }
                                    });
                                    builder.setNegativeButton("Ne", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    });

                                    builder.setCancelable(false);

                                    builder.create().show();
                                }
                            });

                            return;
                        }
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialize();
                        initialized = true;

                        for ( Map.Entry<Chat, ArrayList<Message>> pair : messageStack.entrySet() ) {
                            for ( Message msg : pair.getValue() )
                                onMessageReceived(pair.getKey(), msg);
                        }
                        messageStack.clear();

                        dialog.dismiss();
                    }
                });
            }
        }).start();
    }

    private void initialize() {
        LeftDrawer leftDrawer = (LeftDrawer)getSupportFragmentManager().findFragmentByTag(LeftDrawer.TAG);
        if ( leftDrawer != null ) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(leftDrawer);
            transaction.commit();
        }

        setContentView(R.layout.activity_haber);

        //attach left drawer
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.rlLeftDrawer, mLeftDrawer = LeftDrawer.newInstance(this), LeftDrawer.TAG);
        transaction.commit();

        Updater.CheckForUpdates(this, false);

        chatListView = (ListView)findViewById(R.id.chatListView);

        if ( AdvancedPreferences.ShouldUseBalloons(this))
            setListViewDivider(0);
        else
            setListViewDivider(1);

        setListenerToRootView();

        //glavni Haber chat

        if ( mainChatThread == null )
            mainChatThread = new ChatThread("haber");
        else {
            if ( mainChatThread.tabView.getParent() != null )
                ((LinearLayout)mainChatThread.tabView.getParent()).removeView(mainChatThread.tabView);
        }
        if ( mainChatThread.chatAdapter == null )
            mainChatThread.chatAdapter = new ChatAdapter(this, new ArrayList<ListChatItem>(), cmdListener, false);
        else {
            mainChatThread.chatAdapter.refreshContext(this);
        }

        //ostali chatovi
        for ( Chat chats : HaberService.chatRooms ) {
            ChatThread thread = new ChatThread(chats.getParticipant());
            for ( ChatThread oThread : chatThreads.values() )
                if ( thread.getTitle().equals(chats.getParticipant()) ) {
                    thread.chatAdapter = oThread.chatAdapter;
                }
            if ( thread.chatAdapter == null )
                thread.chatAdapter = new ChatAdapter(this, new ArrayList<ListChatItem>(), cmdListener, true);


            thread.chatAdapter.notifyDataSetChanged();
            thread.setState(TabState.Normal);
            chatThreads.put(chats, thread);

            if ( HaberService.haberCounter.getPms().containsKey(chats.getParticipant()) ) {
                thread.setUnreadMessagesCount(HaberService.haberCounter.getPms().get(chats.getParticipant()));
            }
        }

        chatListView.setAdapter(mainChatThread.chatAdapter);


        findViewById(R.id.btSmile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.this);
                        View view = getLayoutInflater().inflate(R.layout.smiley_chooser, null);
                        final SmileyChooser frag = (SmileyChooser) getSupportFragmentManager().findFragmentById(R.id.smileyFragment);

                        builder.setNeutralButton("Zatvori", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.remove(frag);
                                transaction.commit();
                            }
                        });

                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.remove(frag);
                                transaction.commit();
                            }
                        });

                        builder.setView(view);
                        smileyDialog = builder.create();
                        smileyDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                        smileyDialog.show();
                    }
                });

            }
        });

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


        //left drawer
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        rlContent = (RelativeLayout) findViewById(R.id.rlContent);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_launcher, R.string.acc_drawer_open, R.string.acc_drawer_close) {
            @SuppressLint("NewApi")
            public void onDrawerSlide(View drawerView, float slideOffset) {
                float moveFactor = (Util.DpiToPixel(HaberActivity.this, 240) * slideOffset);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    rlContent.setTranslationX(moveFactor);
                } else {
                    TranslateAnimation anim = new TranslateAnimation(lastTranslate, moveFactor, 0.0f, 0.0f);
                    anim.setDuration(0);
                    anim.setFillAfter(true);
                    rlContent.startAnimation(anim);

                    lastTranslate = moveFactor;
                }
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        chatListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                View nameCarry = view.findViewById(R.id.nameCarry);
                if (nameCarry != null) {
                    nameCarry.performClick();
                }
                return false;
            }
        });

        chatListView.post(new Runnable() {
            @Override
            public void run() {
                scrollToBottom(false); vibrationLock = false;
            }
        });

        sortTabs();
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
    public void onStatusChanged(final String status) {
        Debug.log(status);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( mStatusLabel != null )
                    mStatusLabel.setText(status);
            }
        });
    }


    @Override
    public void onLoggedIn(MultiUserChat haberChat) {

    }


    @Override
    public void onMessageReceived(final Chat chat, final Message message) {
        if ( !initialized ) {
            Debug.log("Putting message on stack!");
            if ( messageStack.containsKey(chat) ) {
                messageStack.get(chat).add(message);
            } else {
                ArrayList<Message> msgs = new ArrayList<>();
                msgs.add(message);
                messageStack.put(chat, msgs);
            }
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean vibratedYet = message.getSubject().equals(MessageDirection.OUTGOING);

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
                        thread.chatAdapter = new ChatAdapter(HaberActivity.this, new ArrayList<ListChatItem>(), cmdListener, true);

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

                    if  ( AdvancedPreferences.ShouldVibrate(HaberActivity.this) && !vibrationLock ) {
                        if ( (getCurrentChat() != chat) || (getCurrentChat() == chat && AdvancedPreferences.ShouldVibrateOnActive(HaberActivity.this)) ) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            vibratedYet = true;
                        }
                    }
                }
                else {
                    mainChatThread.chatAdapter.addItem(message);

                    if  ( AdvancedPreferences.ShouldVibrate(HaberActivity.this) && !vibrationLock  ) {
                        if ( (getCurrentChat() != null && AdvancedPreferences.ShouldVibrateOnPublic(HaberActivity.this)) || (getCurrentChat() == null && AdvancedPreferences.ShouldVibrateOnActive(HaberActivity.this)) ) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            vibratedYet = true;
                        }
                    }

                    try {
                        if ( getCurrentChat() != null )
                            mainChatThread.markTab();
                    } catch ( Exception er ) {
                        Debug.log(er);
                    }
                }

                scrollToBottom(true);


                sortTabs();


                //check for @Reply and vibrates
                if ( AdvancedPreferences.ShouldVibrate(HaberActivity.this)) {
                    if (!vibrationLock && !vibratedYet && AdvancedPreferences.ShouldVibrateOnReply(HaberActivity.this) && (message.getBody() != null)) {
                        String mark = "@" + Haber.getUsername();
                        if (message.getBody().toUpperCase().contains(mark.toUpperCase())) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            vibratedYet = true;
                        } else if (Haber.IsGuest()) {
                            mark = "@" + Haber.getUsername().substring(1);
                            if (message.getBody().toUpperCase().contains(mark.toUpperCase())) {
                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                                vibratedYet = true;
                            }
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
        if ( chatListView == null || !initialized ) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatListView.post(new Runnable(){
                    public void run() {
                        if ( chatListView.getChildCount() < 1 ) return;

                        if ( smooth ) {
                            if (chatListView.getLastVisiblePosition() >= chatListView.getAdapter().getCount() - 4) {
                                //It is scrolled all the way down here
                                chatListView.smoothScrollToPosition(chatListView.getCount() - 1);

                            }
                        } else
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
    public void onRoomJoined(final Chat chat, final boolean selfStarted) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if ( !chatThreads.containsKey(chat) ) {
                    ChatThread thread = new ChatThread(chat.getParticipant());
                    thread.chatAdapter = new ChatAdapter(HaberActivity.this, new ArrayList<ListChatItem>(), cmdListener, true);

                    if ( AdvancedPreferences.ShouldSwitchToNewTab(HaberActivity.this) || selfStarted)
                        thread.tabView.performClick();
                    else {
                        thread.setState(TabState.Normal);
                    }

                    chatThreads.put(chat, thread);
                } else {
                    if ( AdvancedPreferences.ShouldSwitchToNewTab(HaberActivity.this) || selfStarted)
                        chatThreads.get(chat).tabView.performClick();
                    else {
                        chatThreads.get(chat).setState(TabState.Normal);
                    }
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

                        message = params[0] + " je kickovan!";
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

                        message = params[0] + " je banovan!";

                    }
                } else if ( event == Haber.ChatEvent.Joined ) {
                    for (Map.Entry<Chat, ChatThread> pair : chatThreads.entrySet()) {
                        if ( pair.getValue().getUser().equals(params[0]) ) {
                            pair.getValue().chatAdapter.putDivider(Haber.getShortUsername(params[0]) + " je online!");
                            pair.getValue().setOnline(true);
                        }
                    }

                    if ( AdvancedPreferences.ShowJoinedLeftNotifications(HaberActivity.this) )
                        message = Haber.getShortUsername(params[0]) + " je zapoceo haber";
                    else
                        return;
                } else if ( event == Haber.ChatEvent.Left ) {
                    for (Map.Entry<Chat, ChatThread> pair : chatThreads.entrySet()) {
                        if ( pair.getValue().getUser().equals(params[0]) ) {
                            pair.getValue().chatAdapter.putDivider(Haber.getShortUsername(params[0]) + " je offline!");
                            pair.getValue().setOnline(false);
                        }
                    }

                    if ( AdvancedPreferences.ShowJoinedLeftNotifications(HaberActivity.this) )
                        message = Haber.getShortUsername(params[0]) + " je napustio haber";
                    else
                        return;
                }

                addNotification(message);

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

    @Override
    public void onDeleteRequested(final String user) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainChatThread.chatAdapter.removeMessagesFromUser(user);
                mainChatThread.chatAdapter.removeMessagesFromUser("ǂ" + user);
            }
        });
    }

    public void addNotification(String message) {
        if ( !initialized ) {
            Debug.log("Not showing notification, not yet intialized. " + message);
            return;
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
    }

    @Override
    public void onModeratorGranted(String username) {
        addNotification(String.format("%s je dobio status moderatora!", username));
    }

    @Override
    public void onModeratorGranted() {
        addNotification(String.format("Dobio si status moderatora!"));
    }

    @Override
    public void onModeratorRevoked(String username) {
        addNotification(String.format("%s je izgubio status moderatora!", username));
    }

    @Override
    public void onModeratorRevoked() {
        addNotification(String.format("Izgubio si status moderatora!"));
    }

    @Override
    public void onAdminGranted(String username) {
        addNotification(String.format("%s si status admina!", username));
    }

    @Override
    public void onAdminGranted() {
        addNotification(String.format("Dobio si status admina!"));
    }

    @Override
    public void onAdminRevoked(String username) {
        addNotification(String.format("%s je izgubio status admina!", username));
    }

    @Override
    public void onAdminRevoked() {
        addNotification(String.format("Izgubio si status admina!"));
    }


    public static enum TabState {
        Marked,
        Active,
        Normal
    }

    public class ChatThread {
        private ChatAdapter chatAdapter;
        private TabView tabView;
        public String fullUser;
        private boolean isOnline = true;
        private String title;

        public String getTitle() { return title; }

        TabState currentState;

        public void setOnline(boolean isOnline) {
            this.isOnline = isOnline;

            if ( currentState == TabState.Active ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        (findViewById(R.id.editText)).setEnabled(ChatThread.this.isOnline);
                    }
                });
            }
        }


        private void setState(final TabState state) {
            tabView.setState(state);
            currentState = state;
        }

        View btClose;
        ChatThread thisThread;

        public String getUser() {
            return fullUser;
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
            return tabView.getUnreadMessagesCount();
        }

        public void markTab() {
            setState(TabState.Marked);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tabView.setUnreadMessagesCount((tabView.getUnreadMessagesCount() + 1));
                }
            });
        }

        public void setUnreadMessagesCount(final int counter) {
            if ( counter == 0 ) return;

            setState(TabState.Marked);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tabView.setUnreadMessagesCount(counter);
                }
            });
        }

        public ChatThread(String other) {
            title = other;
            thisThread = this;
            other = Haber.getFullUsername(other);
            fullUser = other;

            final String participant = other;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final LinearLayout scrollView = (LinearLayout)findViewById(R.id.tabCarry);

                    tabView = new TabView(HaberActivity.this);

                    tabView.setTitle(Haber.getShortUsername(participant));
                    tabView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (Object o : chatThreads.entrySet()) {
                                        Map.Entry pairs = (Map.Entry) o;
                                        int counter = ((ChatThread)pairs.getValue()).getUnreadMessagesCount();
                                        ((ChatThread)pairs.getValue()).setState(TabState.Normal);
                                        ((ChatThread)pairs.getValue()).setUnreadMessagesCount(counter);
                                    }

                                    int counter = mainChatThread.getUnreadMessagesCount();
                                    mainChatThread.setState(TabState.Normal);
                                    mainChatThread.setUnreadMessagesCount(counter);

                                    setState(TabState.Active);

                                    chatListView.setAdapter(chatAdapter);
                                    (findViewById(R.id.editText)).setEnabled(isOnline);

                                    scrollToBottom(true);

                                    closeLeftDrawer();
                                }
                            });
                        }
                    });

                    btClose = tabView.findViewById(R.id.tvClose);
                    if ( participant.equals("haber") )
                        btClose.setVisibility(View.INVISIBLE);

                    setState(TabState.Active);

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
                                            scrollView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            scrollView.removeView(tabView);
                                                        }
                                                    });
                                                }
                                            });
                                            if ( mainChatThread != null )
                                                if ( mainChatThread.tabView != null )
                                                    mainChatThread.tabView.performClick();
                                        }
                                    });
                                }

                            });

                            tabView.startAnimation(anim);
                        }
                    });
                    tabView.startAnimation(AnimationUtils.loadAnimation(HaberActivity.this, R.anim.abc_slide_in_top));
                    scrollView.addView(tabView);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if ( drawer != null )
            if (drawer.isDrawerOpen(Gravity.LEFT)) {
                closeLeftDrawer();
                return;
            }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Imaš pametnija posla!?");
        builder.setPositiveButton("Gasi haber", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread() {
                    @Override
                    public void run() {

                        try {
                            HaberService.StopService();
                        } catch ( Exception e ) {
                            Debug.log(e);
                        }

                        Debug.log("Finishing because of user interaction (haber shutting down)");
                        chatThreads.clear();
                        mainChatThread = null;

                        finish();
                    }
                }.start();
            }
        });
        /*builder.setNeutralButton("Sakrij se u pozadini", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Debug.log("Finishg because of user interaction (move to back)");
                finish();
            }
        });*/
        builder.setNegativeButton("Vrati se na haber", null);
        builder.create().show();
    }
}
