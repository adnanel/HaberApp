package adnan.haber;

import android.content.Context;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.UserStatusListener;

import java.util.ArrayList;
import java.util.Random;

import adnan.haber.util.Debug;


/**
 * Created by Adnan on 20.1.2015..
 */
public class Haber {
    static Haber instance = null;

    HaberListener statusListener;
    Context context;
    XMPPConnection connection;

    private static String  username = "ǂAndro" + getRandomInt();
    private static boolean isGuest = true;
    private static String  password = "";
    private static ArrayList<Message> cachedLobbyMessages = new ArrayList<Message>();

    public static synchronized ArrayList<Message> getCachedLobbyMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        for ( Message msg : cachedLobbyMessages )
            messages.add(msg);
        return messages;
    }

    public static boolean IsGuest() { return isGuest; }
    static synchronized void addMessageToCache(Message msg) {
        cachedLobbyMessages.add(msg);
    }

    public static void setPassword(String str) {
        password = str;
    }

    public static void setIsGuest(boolean guest) {
        isGuest = guest;
    }

    public static String getUsername() {
        return username;
    }



    public static void Disconnect() {
        Debug.log("Disconnecting...");

        try {
            instance.statusListener.onSoftDisconnect();
            instance.statusListener = null;

            instance.connection.disconnect();
            cachedLobbyMessages.clear();

            instance = null;


        } catch ( Exception e ) {
            Debug.log(e);
        }
    }

    static int getRandomInt() {
        Random rand = new Random();
        return rand.nextInt(1000);
    }

    public static void setUser(String user) {
        username = user;
    }

    public static boolean isConnected() {
        if ( instance == null ) return false;
        try {
            return instance.connection.isConnected();
        } catch (Exception e) {
            Debug.log(e);
            return false;
        }
    }

    private Haber(HaberListener listener, Context context) {
        this.context = context;

        statusListener = listener;

        statusListener.onStatusChanged("Povezujem se...");
        if ( connect() ) {
            statusListener.onStatusChanged("Yay!");
        } else {
            statusListener.onStatusChanged("Failed!");
        }

    }


    public void startChat(String user) {
        if ( statusListener == null ) {
            Debug.log("This isnt supposed to happen. EVER!");
            return;
        }

        Chat chat = HaberService.haberChat.createPrivateChat(user, new MessageListener() {
            @Override
            public void processMessage(Chat chat, Message message) {
                // Commented out because double messages were received. The global messagelistener covers this one as well (apparently)
                // statusListener.onMessageReceived(chat, message);
            }
        });

        statusListener.onRoomJoined(chat);
    }


    public static void StartChat(String user) {
        if ( instance == null ) {
            Debug.log("This isnt supposed to happen. EVER!");
            return;
        }

        instance.startChat(user);
    }

    public static String getFullUsername(String user) {
        for ( String str : HaberService.haberChat.getOccupants() ) {
            if ( getShortUsername(str).equals(user) )
                return str;
        }
        return user;
    }

    public static String getShortUsername(String user) {
        if ( user.indexOf("/") != -1 )
            return user.substring(user.indexOf("/") + 1);
        return user;
    }

    public boolean connect() {
        try {

            // Create a connection to the igniterealtime.org XMPP server.
            String server = "etf.ba";
            int port = 5222;

            SmackAndroid.init(context);
            ConnectionConfiguration config = new ConnectionConfiguration(server, port);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

            config.setDebuggerEnabled(true);

            connection = new XMPPTCPConnection(config);

            try {
                connection.connect();
                if ( isGuest ) {
                    connection.loginAnonymously();
                } else {
                    try {
                        SASLAuthentication.registerSASLMechanism("DIGEST-MD5", SASLDigestMD5Mechanism.class);
                        SASLAuthentication.supportSASLMechanism("DIGEST-MD5");

                        connection.login(username, password);

                    } catch ( Exception er ) {
                        Debug.log(er);
                        isGuest = true;
                        return connect();
                    }
                }

                MultiUserChat chat = new MultiUserChat(connection, "haber@conference.etf.ba");
                chat.join(username);

                statusListener.onLoggedIn(chat);

                chat.addParticipantStatusListener(new ParticipantStatusListener() {
                    @Override
                    public void joined(String s) {

                    }

                    @Override
                    public void left(String s) {

                    }

                    @Override
                    public void kicked(String s, String s2, String s3) {
                        statusListener.onChatEvent(ChatEvent.Kicked, s, s2, s3);
                    }

                    @Override
                    public void voiceGranted(String s) {

                    }

                    @Override
                    public void voiceRevoked(String s) {

                    }

                    @Override
                    public void banned(String s, String s2, String s3) {
                        statusListener.onChatEvent(ChatEvent.Banned, s, s2, s3);
                    }

                    @Override
                    public void membershipGranted(String s) {

                    }

                    @Override
                    public void membershipRevoked(String s) {

                    }

                    @Override
                    public void moderatorGranted(String s) {

                    }

                    @Override
                    public void moderatorRevoked(String s) {

                    }

                    @Override
                    public void ownershipGranted(String s) {

                    }

                    @Override
                    public void ownershipRevoked(String s) {

                    }

                    @Override
                    public void adminGranted(String s) {

                    }

                    @Override
                    public void adminRevoked(String s) {

                    }

                    @Override
                    public void nicknameChanged(String s, String s2) {

                    }
                });
                chat.addUserStatusListener(new UserStatusListener() {
                    @Override
                    public void kicked(String s, String s2) {
                        statusListener.onChatEvent(ChatEvent.Kicked, s, s2);
                    }

                    @Override
                    public void voiceGranted() {

                    }

                    @Override
                    public void voiceRevoked() {

                    }

                    @Override
                    public void banned(String s, String s2) {
                        statusListener.onChatEvent(ChatEvent.Banned, s, s2);
                    }

                    @Override
                    public void membershipGranted() {

                    }

                    @Override
                    public void membershipRevoked() {

                    }

                    @Override
                    public void moderatorGranted() {

                    }

                    @Override
                    public void moderatorRevoked() {

                    }

                    @Override
                    public void ownershipGranted() {

                    }

                    @Override
                    public void ownershipRevoked() {

                    }

                    @Override
                    public void adminGranted() {

                    }

                    @Override
                    public void adminRevoked() {

                    }
                });

                chat.addMessageListener(new PacketListener() {
                    @Override
                    public void processPacket(Packet packet) throws SmackException.NotConnectedException {
                        try {
                            Message message = (Message)packet;
                            statusListener.onMessageReceived(null, message);
                            addMessageToCache(message);
                        } catch ( Exception e ) {
                            Debug.log("This exception is expected! " + e.toString());
                            Debug.log(e);
                        }
                    }
                });
                ChatManager.getInstanceFor(connection).addChatListener(new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean isLocal) {
                        statusListener.onChatStarted(chat, isLocal);
                        chat.addMessageListener(new MessageListener() {
                            @Override
                            public void processMessage(Chat chat, Message message) {
                                statusListener.onMessageReceived(chat, message);
                            }
                        });
                    }
                });
            } catch (SmackException.ConnectionException e ) {
                for (HostAddress addr : e.getFailedAddresses() ) {
                    Debug.log(addr.getErrorMessage());
                }

                Debug.log(e);

                return false;
            }

            return true;
        }
        catch (Exception e) {
            Debug.log(e);
            return false;
        }
    }


    public static void initialize( HaberListener statusListener, Context context ) {
        if ( instance != null ) {
            instance.Disconnect();
        }

        instance = new Haber(statusListener, context);
    }


    public interface HaberListener {
        public abstract void onStatusChanged(String status);
        public abstract void onChatStarted(Chat chat, boolean isLocal);
        public abstract void onLoggedIn(MultiUserChat haberChat);
        public abstract void onMessageReceived(Chat chat, Message message);
        public abstract void onRoomJoined(Chat chat);
        public abstract void onChatEvent(ChatEvent event, String... params);
        //soft disconnect - when the user disconnects intentionally
        public abstract void onSoftDisconnect();
    }

    public enum ChatEvent {
        Banned,
        Kicked
    }
}
