package adnan.haber.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import adnan.haber.Haber;
import adnan.haber.HaberService;

/**
 * Created by Adnan on 24.1.2015..
 */
public class ChatSaver implements Haber.HaberListener {
    static Context context;
    static SharedPreferences sharedPreferences;

    //todo clear prefs, store packet ID as well.
    //todo also load cache when starting a new chat

    final static String PREFS = "chat_cache";
    final static String PREF_COUNT = "count";
    final static String PREF_BODY = "body";
    final static String PREF_FROM = "from";
    final static String PREF_TO   = "to";
    final static String PREF_ID   = "id";

    final static String PREF_LOBBY_COUNT = "lcount";
    final static String PREF_LOBBY_BODY  = "lbody";
    final static String PREF_LOBBY_FROM  = "lfrom";
    final static String PREF_LOBBY_ID    = "lid";

    static ChatSaver instance;
    private ChatSaver() {
        instance = this;
    }

    public static void Initialize(Context context) {
        CredentialManager.Initialize(context);

        ChatSaver.context = context;
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if ( instance == null ) {
            instance = new ChatSaver();

            HaberService.addHaberListener(instance);

            //divider na lobby-u
            Message msg = new Message();
            msg.setFrom("haber");
            msg.setTo("haber");
            msg.setBody(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(Calendar.getInstance().getTime()));
            msg.setPacketID("divider");
            instance.onMessageReceived(null, msg);
        }
    }

    public static int getSavedMessagesCount() {
        int count = sharedPreferences.getInt(PREF_COUNT, 0);
        return count;
    }

    public static ArrayList<Message> getSavedMessages() {
        ArrayList<Message> result = new ArrayList<>();

        int count = sharedPreferences.getInt(PREF_COUNT, 0);
        for ( int i = 0; i < count; i ++ ) {
            Message msg = new Message();
            msg.setBody(sharedPreferences.getString(PREF_BODY + i, ""));
            msg.setFrom(sharedPreferences.getString(PREF_FROM + i, ""));
            msg.setTo(sharedPreferences.getString(PREF_TO + i, ""));
            msg.setPacketID(sharedPreferences.getString(PREF_ID + i, "1"));

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
        int count = sharedPreferences.getInt(PREF_LOBBY_COUNT, 0);
        return count;
    }

    public static ArrayList<Message> getSavedLobbyMessages() {
        ArrayList<Message> result = new ArrayList<>();

        int count = sharedPreferences.getInt(PREF_LOBBY_COUNT, 0);
        for ( int i = 0; i < count; i ++ ) {
            Message msg = new Message();
            msg.setBody(sharedPreferences.getString(PREF_LOBBY_BODY + i, ""));
            msg.setFrom(sharedPreferences.getString(PREF_LOBBY_FROM + i, ""));
            msg.setPacketID(sharedPreferences.getString(PREF_LOBBY_ID + i, "1"));

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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    static void saveMessages(ArrayList<Message> messages) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_COUNT, messages.size());
        int i = 0;
        for ( Message message : messages ) {
            editor.putString(PREF_BODY + i, message.getBody());
            editor.putString(PREF_FROM + i, message.getFrom());
            editor.putString(PREF_TO + i, message.getTo());
            editor.putString(PREF_ID + i, message.getPacketID());
            i++;
        }

        editor.commit();
    }

    static void saveLobbyMessages(ArrayList<Message> messages) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_LOBBY_COUNT, messages.size());
        int i = 0;
        for ( Message message : messages ) {
            editor.putString(PREF_LOBBY_BODY + i, message.getBody());
            editor.putString(PREF_LOBBY_FROM + i, message.getFrom());
            editor.putString(PREF_LOBBY_ID + i, message.getPacketID());

            i++;
        }

        editor.commit();
    }


    static void SaveMessage(Message msg) {

        ArrayList<Message> messages = getSavedMessages();
        messages.add(msg);
        saveMessages(messages);
    }

    static void SaveLobbyMessage(Message msg) {
        ArrayList<Message> messages = getSavedLobbyMessages();
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
        //message.setPacketID(message.);
        if (chat == null) {
            SaveLobbyMessage(message);
        } else
            SaveMessage(message);
    }

    @Override
    public void onRoomJoined(Chat chat) {
        Message msg = new Message();
        msg.setFrom("haber");
        msg.setTo("haber");
        msg.setBody(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(Calendar.getInstance().getTime()));
        msg.setPacketID("divider");
        instance.onMessageReceived(chat, msg);
    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, String... params) {

    }

    @Override
    public void onSoftDisconnect() {

    }

}

