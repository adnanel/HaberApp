package adnan.haber.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import adnan.haber.Haber;
import adnan.haber.HaberActivity;
import adnan.haber.HaberService;
import adnan.haber.packets.PacketTimeStamp;
import adnan.haber.types.ListChatItem;

/**
 * Created by Adnan on 24.1.2015..
 */
public class ChatSaver implements Haber.HaberListener {
    public static final int ALL = -1;

    private static Context context;

    private final static String PREFS = "chat_cache_v4_1";
    private final static String PREF_COUNT  = "count";
    private final static String PREF_BODY   = "body";
    private final static String PREF_FROM   = "from";
    private final static String PREF_TO     = "to";
    private final static String PREF_ID     = "id";
    private final static String PREF_TSTAMP = "tstamp";
    private final static String PREF_DIR    = "direction";

    private final static String PREF_LOBBY_COUNT  = "lcount";
    private final static String PREF_LOBBY_BODY   = "lbody";
    private final static String PREF_LOBBY_FROM   = "lfrom";
    private final static String PREF_LOBBY_ID     = "lid";
    private final static String PREF_LOBBY_TSTAMP = "ltstamp";
    private final static String PREF_LOBBY_DIR    = "ldirection";

    private static ChatSaver instance;
    private ChatSaver() {
        instance = this;
    }

    private static SharedPreferences getSharedPreferences() {
        if ( context == null ) {
            Debug.log("I have no context!!! Trying to pull one from the activity if its active to avoid crash...");
            if (HaberActivity.InstanceExists())
                context = HaberActivity.getInstance();
            else
                Debug.log("No luck, WE'RE GOING DOWN!!!");
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void Finalize() {
        Debug.log("Finalizing ChatSaver...");
        if ( instance != null ) {
            instance = null;
        } else {
            Debug.log(new Exception("Chat saver was not initialized!"));
        }
    }

    public static void Initialize(Context context) {
        ChatSaver.context = context;

        if ( instance != null ) {
            Finalize();
        }

        instance = new ChatSaver();

        Message message = new Message();
        message.setFrom("divider");
        message.setSubject("divider");
        message.setPacketID("divider");
        message.setBody(Util.dateToFormat("dd-MM-yyyy HH:mm", new Date()));
        instance.onMessageReceived(null, message);
        HaberService.addHaberListener(instance);
    }

    public static int getSavedMessagesCount() {
        return getSharedPreferences().getInt(PREF_COUNT, 0);
    }

    public static boolean MessageExists(String id) {
        return MessageExists(id, getSavedMessagesCount());
    }


    public static boolean MessageExists(String id, int count ) {
        int i = getSavedMessagesCount() - count;
        if ( i < 0 ) i = 0;
        SharedPreferences prefs = getSharedPreferences();
        for (; i < getSavedMessagesCount(); i ++ ) {
            if ( prefs.getString(PREF_ID + i, "").equals(id) ) return true;
        }
        return false;
    }

    public static boolean LobbyMessageExists(String id) {
        return LobbyMessageExists(id, getSavedLobbyMessagesCount());
    }


    public static boolean LobbyMessageExists(String id, int count ) {
        int i = getSavedMessagesCount() - count;
        if ( i < 0 ) i = 0;
        SharedPreferences prefs = getSharedPreferences();
        for (; i < getSavedLobbyMessagesCount(); i ++ ) {
            if ( prefs.getString(PREF_LOBBY_ID + i, "").equals(id) ) return true;
        }
        return false;
    }

    public static ArrayList<Message> getSavedMessages() {
        return getSavedMessages(30);
    }

    public static ArrayList<Message> getSavedMessages(int limit) {
        if ( limit == ALL ) limit = getSavedMessagesCount();

        ArrayList<Message> result = new ArrayList<>();

        int count = getSharedPreferences().getInt(PREF_COUNT, 0);
        int start = count - limit;
        if ( start < 0 ) start = 0;

        for ( int i = start; i < count; i ++ ) {
            Message msg = new Message();
            msg.setBody(getSharedPreferences().getString(PREF_BODY + i, ""));
            msg.setFrom(getSharedPreferences().getString(PREF_FROM + i, ""));
            msg.setTo(getSharedPreferences().getString(PREF_TO + i, ""));
            msg.setPacketID(getSharedPreferences().getString(PREF_ID + i, "1"));
            msg.addExtension(new PacketTimeStamp(getSharedPreferences().getString(PREF_TSTAMP + i, "1")));
            msg.setSubject(getSharedPreferences().getString(PREF_DIR + i, ""));

            boolean existing = false;
            for ( Message message : result )
                if ( message.getPacketID().equals(msg.getPacketID()) ) {
                    existing = true;
                    break;
                }

            if ( !existing )
                result.add(msg);
        }


        return result;
    }

    public static int getSavedLobbyMessagesCount() {
        return getSharedPreferences().getInt(PREF_LOBBY_COUNT, 0);
    }

    public static ArrayList<Message> getSavedLobbyMessages() {
        return getSavedLobbyMessages(30);
    }

    public static ArrayList<Message> getSavedLobbyMessages(int limit) {
        if ( limit == ALL ) limit = getSavedLobbyMessagesCount();

        ArrayList<Message> result = new ArrayList<>();

        int count = getSharedPreferences().getInt(PREF_LOBBY_COUNT, 0);
        int start = count - limit;
        if ( start < 0 ) start = 0;

        for ( int i = start; i < count; i ++ ) {
            Message msg = new Message();
            msg.setBody(getSharedPreferences().getString(PREF_LOBBY_BODY + i, ""));
            msg.setFrom(getSharedPreferences().getString(PREF_LOBBY_FROM + i, ""));
            msg.setPacketID(getSharedPreferences().getString(PREF_LOBBY_ID + i, "1"));
            msg.addExtension(new PacketTimeStamp(getSharedPreferences().getString(PREF_LOBBY_TSTAMP + i, "1")));
            msg.setSubject(getSharedPreferences().getString(PREF_LOBBY_DIR + i, ""));

            boolean existing = false;
            for ( Message message : result )
                if ( message.getPacketID().equals(msg.getPacketID()) ) {
                    existing = true;
                    break;
                }

            if ( !existing )
                result.add(msg);
        }

        return result;
    }

    public static void ClearCache() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.clear();
        editor.commit();
    }

    static void saveMessages(ArrayList<Message> messages) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(PREF_COUNT, messages.size());
        int i = 0;
        for ( Message message : messages ) {
            editor.putString(PREF_BODY + i, message.getBody());
            editor.putString(PREF_FROM + i, message.getFrom());
            editor.putString(PREF_TO + i, message.getTo());
            editor.putString(PREF_ID + i, message.getPacketID());
            editor.putString(PREF_TSTAMP + i, getTimeStamp(message));
            editor.putString(PREF_DIR + i, message.getSubject());

            i++;
        }

        editor.commit();
    }

    private static String getTimeStamp(Message message) {
        for (PacketExtension ext : message.getExtensions() ) {
            if ( ext instanceof PacketTimeStamp ) {
                return ((PacketTimeStamp) ext).getTime();
            }
        }
        return "1";
    }

    static void saveLobbyMessages(ArrayList<Message> messages) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(PREF_LOBBY_COUNT, messages.size());
        int i = 0;
        for ( Message message : messages ) {
            editor.putString(PREF_LOBBY_BODY + i, message.getBody());
            editor.putString(PREF_LOBBY_FROM + i, message.getFrom());
            editor.putString(PREF_LOBBY_ID + i, message.getPacketID());
            editor.putString(PREF_LOBBY_TSTAMP + i, getTimeStamp(message));
            editor.putString(PREF_LOBBY_DIR + i, message.getSubject());

            i++;
        }

        editor.commit();
    }


    static void SaveMessage(Message msg) {
        if ( MessageExists(msg.getPacketID(), 25)) return;

        SharedPreferences.Editor editor = getSharedPreferences().edit();

        int i = getSavedMessagesCount();
        editor.putInt(PREF_COUNT, i + 1);
        editor.putString(PREF_BODY + i, msg.getBody());
        editor.putString(PREF_FROM + i, msg.getFrom());
        editor.putString(PREF_TO + i, msg.getTo());
        editor.putString(PREF_ID + i, msg.getPacketID());
        editor.putString(PREF_TSTAMP + i, getTimeStamp(msg));
        editor.putString(PREF_DIR + i, msg.getSubject());


        editor.commit();
    }

    static void SaveLobbyMessage(Message msg) {
        if ( LobbyMessageExists(msg.getPacketID(), 25)) return;

        SharedPreferences.Editor editor = getSharedPreferences().edit();

        int i = getSavedLobbyMessagesCount();
        editor.putInt(PREF_LOBBY_COUNT, i + 1);
        editor.putString(PREF_LOBBY_BODY + i, msg.getBody());
        editor.putString(PREF_LOBBY_FROM + i, msg.getFrom());
        editor.putString(PREF_LOBBY_ID + i, msg.getPacketID());
        editor.putString(PREF_LOBBY_TSTAMP + i, getTimeStamp(msg));
        editor.putString(PREF_LOBBY_DIR + i, msg.getSubject());


        editor.commit();
    }

    @Override
    public void onStatusChanged(String status) {

    }

    @Override
    public void onLoggedIn(MultiUserChat haberChat) {

    }

    //Helper function to save outcoming private messages
    public static void OnMessageReceived(Chat chat, Message message) {
        if ( instance == null ) {
            Debug.log("ChatSaver instance is null!!");
            return;
        }
        instance.onMessageReceived(chat, message);
    }

    @Override
    public void onMessageReceived(Chat chat, Message message) {
        //DO NOT ASSIGN ID HERE! message is already prepared at this point.

        if (chat == null) {
            SaveLobbyMessage(message);
        } else
            SaveMessage(message);
    }

    @Override
    public void onRoomJoined(Chat chat, boolean selfStarted) {
        Message msg = new Message();
        msg.setFrom("haber");
        msg.setTo("haber");
        try {
            msg.setBody(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(Calendar.getInstance().getTime()));
        } catch ( Exception er ) {
            Debug.log(er);
            msg.setBody("You shouldn't see this.");
        }
        msg.setPacketID("divider");
        instance.onMessageReceived(chat, msg);
    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, String... params) {

    }

    @Override
    public void onSoftDisconnect() {

    }

    @Override
    public void onDeleteRequested(String user) {
        ArrayList<Message> messages = getSavedLobbyMessages();
        ArrayList<Message> queue = new ArrayList<>();
        for ( Message item : messages ) {
            if ( item.getFrom().equals(user) ) {
                queue.add(item);
            }
        }
        for (Message item : queue)
            messages.remove(item);

        saveLobbyMessages(messages);
    }

}

