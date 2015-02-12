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

import adnan.haber.Haber;
import adnan.haber.HaberService;
import adnan.haber.types.ListChatItem;

/**
 * Created by Adnan on 24.1.2015..
 */
public class ChatSaver implements Haber.HaberListener {
    private static Context context;

    private final static String PREFS = "chat_cache_v3";
    private final static String PREF_COUNT  = "count";
    private final static String PREF_BODY   = "body";
    private final static String PREF_FROM   = "from";
    private final static String PREF_TO     = "to";
    private final static String PREF_ID     = "id";
    private final static String PREF_TSTAMP = "tstamp";

    private final static String PREF_LOBBY_COUNT = "lcount";
    private final static String PREF_LOBBY_BODY  = "lbody";
    private final static String PREF_LOBBY_FROM  = "lfrom";
    private final static String PREF_LOBBY_ID    = "lid";
    private final static String PREF_LOBBY_TSTAMP = "ltstamp";

    private static ChatSaver instance;
    private ChatSaver() {
        instance = this;
    }

    private static SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void Initialize(Context context) {
        ChatSaver.context = context;

        if ( instance == null ) {
            instance = new ChatSaver();

            HaberService.addHaberListener(instance);
        }
    }

    public static int getSavedMessagesCount() {
        return getSharedPreferences().getInt(PREF_COUNT, 0);
    }


    public static ArrayList<Message> getSavedMessages() {
        return getSavedMessages(30);
    }

    public static ArrayList<Message> getSavedMessages(int limit) {
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
            msg.addExtension(new Haber.PacketTimeStamp(getSharedPreferences().getString(PREF_TSTAMP + i, "1")));

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
        ArrayList<Message> result = new ArrayList<>();

        int count = getSharedPreferences().getInt(PREF_LOBBY_COUNT, 0);
        int start = count - limit;
        if ( start < 0 ) start = 0;

        for ( int i = start; i < count; i ++ ) {
            Message msg = new Message();
            msg.setBody(getSharedPreferences().getString(PREF_LOBBY_BODY + i, ""));
            msg.setFrom(getSharedPreferences().getString(PREF_LOBBY_FROM + i, ""));
            msg.setPacketID(getSharedPreferences().getString(PREF_LOBBY_ID + i, "1"));
            msg.addExtension(new Haber.PacketTimeStamp(getSharedPreferences().getString(PREF_LOBBY_TSTAMP + i, "1")));

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
            i++;
        }

        editor.commit();
    }

    private static String getTimeStamp(Message message) {
        for (PacketExtension ext : message.getExtensions() ) {
            if ( ext instanceof Haber.PacketTimeStamp ) {
                return ((Haber.PacketTimeStamp) ext).getTime();
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
            i++;
        }

        editor.commit();
    }


    static void SaveMessage(Message msg) {

        ArrayList<Message> messages = getSavedMessages();
        for ( Message message : messages ) {
            if ( message.getPacketID().equals(msg.getPacketID())) return;
        }

        messages.add(msg);
        saveMessages(messages);
    }

    static void SaveLobbyMessage(Message msg) {
        ArrayList<Message> messages = getSavedLobbyMessages();
        for ( Message message : messages ) {
            if ( message.getPacketID().equals(msg.getPacketID())) return;
        }


        messages.add(msg);
        saveLobbyMessages(messages);
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

