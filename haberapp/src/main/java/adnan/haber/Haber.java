package adnan.haber;

import android.content.Context;

import org.apache.http.auth.InvalidCredentialsException;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.UserStatusListener;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xmlpull.v1.XmlPullParser;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import adnan.haber.packets.DeletePacket;
import adnan.haber.packets.DeletePacketProvider;
import adnan.haber.packets.HellbanPacket;
import adnan.haber.packets.HellbanPacketProvider;
import adnan.haber.packets.PacketTimeStamp;
import adnan.haber.util.CredentialManager;
import adnan.haber.util.Debug;
import adnan.haber.util.Util;


/**
 * Created by Adnan on 20.1.2015..
 */
public class Haber {
    static Haber instance = null;
    static String lastError = "";

    HaberListener statusListener;
    RoleChangeListener roleChangeListener;

    Context context;
    XMPPConnection connection;
    private static MultiUserChat haberChat;

    private static String  username = "ǂAndro" + getRandomInt();
    private static boolean isGuest = true;
    private static boolean hellBanned = false;
    private static String  password = "";
    private static ArrayList<Message> cachedLobbyMessages = new ArrayList<Message>();

    public static boolean IsHellBanned() {
        return hellBanned;
    }
    public static synchronized ArrayList<Message> getCachedLobbyMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        for ( Message msg : cachedLobbyMessages )
            messages.add(msg);

        return messages;
    }

    public static boolean IsOnline(String user) {
        for ( String str : getHaberChat().getOccupants() )
            if ( str.equals(user) ) return true;
        return false;
    }

    public static boolean IsGuest() { return isGuest; }
    static synchronized void addMessageToCache(Message msg) {
        if ( cachedLobbyMessages == null ) return;

        cachedLobbyMessages.add(msg);
    }

    public static void setPassword(String str) { password = Util.encryptPassword(str); }

    public static void setIsGuest(boolean guest) {
        isGuest = guest;
    }

    public static String getUsername() {
        return username;
    }

    public static void QuickDisconnect() {
        try {
            haberChat.leave();
        } catch ( Exception e ) {
            Debug.log(e);
        }
        instance = null;
    }

    public static void Disconnect() {
        Debug.log("Disconnecting...");

        try {
            if ( instance != null ) {
                if (instance.statusListener != null) {
                    instance.statusListener.onSoftDisconnect();
                    instance.statusListener = null;
                }

                haberChat.leave();
                instance.connection.disconnect();
            }

            instance = null;


        } catch ( Exception e ) {
            Debug.log(e);
        }
    }

    static int getRandomInt() {
        Random rand = new Random();
        return rand.nextInt(1000);
    }

    public static void setUser(String user) throws Exception {
        if ( isConnected() ) throw new Exception("Can't set username while connected!");
        username = user;
    }

    public static boolean isConnected() {
        if ( instance == null ) return false;
        try {
            return instance.connection.isConnected() && getHaberChat().isJoined();
        } catch (Exception e) {
            Debug.log(e);
            return false;
        }
    }

    private Haber(HaberListener listener, Context context) throws Exception {
        this.context = context;

        statusListener = listener;

        statusListener.onStatusChanged("Povezujem se...");

        if (connect()) {
            statusListener.onStatusChanged("Yay!");
        } else {
            statusListener.onStatusChanged(lastError);
            throw new Exception("Failed!");
        }

    }


    public Chat startChat(String user) {
        if ( statusListener == null ) {
            Debug.log("This isnt supposed to happen. EVER!");
            return null;
        }

        Chat chat = getHaberChat().createPrivateChat(user, new MessageListener() {
            int id = 0;
            String salt = Util.getRandomInt(1000) + "";

            @Override
            public void processMessage(Chat chat, Message message) {
                if ( message.getPacketID() == null ) {
                    message.setPacketID((id++) + salt);
                }

                try {
                    message.addExtension(new PacketTimeStamp(message));
                    message.setPacketID(Util.GeneratePacketId(message));
                } catch ( Exception er ) {
                    Debug.log(er);
                }

                if ( chat.getParticipant().equals(message.getFrom()) )
                    statusListener.onMessageReceived(chat, message);
                else {
                    /* A very strange bug, need to create a new private chat with message.getFrom() */
                    Debug.log("Applying workaround for bug lchat.participant != message.getFrom");
                    Chat nchat = startChat(message.getFrom());
                    statusListener.onMessageReceived(nchat, message);
                }
            }
        });


        statusListener.onRoomJoined(chat, true);
        return chat;
    }


    public static Chat StartChat(String user) {
        if ( instance == null ) {
            Debug.log("This isnt supposed to happen. EVER!");
            return null;
        }

        return instance.startChat(user);
    }

    public static String getFullUsername(String user) {
        for ( String str : getHaberChat().getOccupants() ) {
            if ( getShortUsername(str).equals(user) )
                return str;
        }
        return user;
    }

    public static String getShortUsername(String user) {
        if ( user.indexOf("/") != -1 ) {
            if ( user.toUpperCase().endsWith("HABER") )
                return user.substring(0, user.indexOf("@"));
            return user.substring(user.indexOf("/") + 1);
        } else if ( user.toUpperCase().contains("@etf.ba") ) {
            return user.substring(0, user.indexOf("@"));
        }
        return user;
    }

    public static MultiUserChat getHaberChat() {
        if ( haberChat == null ) {
            if ( HaberService.haberChatExists() )
                return haberChat = HaberService.getHaberChat();

            Debug.log("haberChat is null!! Probably not even connected..");
            if ( instance != null )
                instance.statusListener.onSoftDisconnect();
        }
        return haberChat;
    }

    public boolean connect() throws InvalidCredentialsException {
        try {

            // Create a connection to the igniterealtime.org XMPP server.
            String server = "etf.ba";
            int port = 5222;

            SmackAndroid.init(context);
            ConnectionConfiguration config = new ConnectionConfiguration(server, port);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

            connection = new XMPPTCPConnection(config);

            ProviderManager.addExtensionProvider("delete", "haber:delete", new DeletePacketProvider());
            ProviderManager.addExtensionProvider("hellban", "haber:hellban", new HellbanPacketProvider());

            hellBanned = false;

            try {
                connection.connect();

                if ( isGuest ) {
                    connection.loginAnonymously();
                    username = "ǂAndro" + getRandomInt();
                } else {
                    try {
                        SASLAuthentication.registerSASLMechanism("DIGEST-MD5", SASLDigestMD5Mechanism.class);
                        SASLAuthentication.supportSASLMechanism("DIGEST-MD5");

                        connection.login(username, password, "");
                    } catch ( Exception er ) {
                        if ( er.toString().endsWith("not-authorized") ) {
                            setUser("");
                            setPassword("");
                            setIsGuest(true);
                            
                            throw new InvalidCredentialsException(er.toString());
                        }
                        Debug.log(er);
                        setIsGuest(true);
                        return connect();
                    }
                }

                MultiUserChat chat = new MultiUserChat(connection, "haber@conference.etf.ba");
                chat.join(username);

                statusListener.onLoggedIn(chat);
                haberChat = chat;


                chat.addParticipantStatusListener(new ParticipantStatusListener() {
                    @Override
                    public void joined(String s) { statusListener.onChatEvent(ChatEvent.Joined, s); }

                    @Override
                    public void left(String s) {
                        statusListener.onChatEvent(ChatEvent.Left, s);
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
                        if ( roleChangeListener != null )
                            roleChangeListener.onModeratorGranted(s);
                    }

                    @Override
                    public void moderatorRevoked(String s) {
                        if ( roleChangeListener != null )
                            roleChangeListener.onModeratorRevoked(s);
                    }

                    @Override
                    public void ownershipGranted(String s) {

                    }

                    @Override
                    public void ownershipRevoked(String s) {

                    }

                    @Override
                    public void adminGranted(String s) {
                        if ( roleChangeListener != null )
                            roleChangeListener.onAdminGranted(s);
                    }

                    @Override
                    public void adminRevoked(String s) {
                        if ( roleChangeListener != null )
                            roleChangeListener.onAdminRevoked(s);
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
                        if ( roleChangeListener != null )
                            roleChangeListener.onModeratorGranted();
                    }

                    @Override
                    public void moderatorRevoked() {
                        if ( roleChangeListener != null )
                            roleChangeListener.onModeratorRevoked();
                    }

                    @Override
                    public void ownershipGranted() {

                    }

                    @Override
                    public void ownershipRevoked() {

                    }

                    @Override
                    public void adminGranted() {
                        if ( roleChangeListener != null )
                            roleChangeListener.onAdminGranted();
                    }

                    @Override
                    public void adminRevoked() {
                        if ( roleChangeListener != null )
                            roleChangeListener.onAdminRevoked();
                    }
                });

                chat.addMessageListener(new PacketListener() {
                    int id = 0;
                    String salt = Util.getRandomInt(1000) + "";

                    @Override
                    public void processPacket(Packet packet) throws SmackException.NotConnectedException {
                        Debug.log("processPacket() " + packet.toXML().toString());
                        if (!(packet instanceof Message)) return;

                        try {
                            Message message = (Message) packet;
                            if (message.getPacketID() == null) {
                                message.setPacketID((id++) + salt);
                            }

                            try {
                                if (HaberService.IsMod(message.getFrom())) {
                                    for (PacketExtension ex : packet.getExtensions()) {
                                        if (ex instanceof DeletePacket) {
                                            statusListener.onDeleteRequested(((DeletePacket) ex).target);
                                            return;
                                        } else if (ex instanceof HellbanPacket) {
                                            if (Haber.getFullUsername(Haber.getUsername()).toUpperCase().equals(Haber.getFullUsername(((HellbanPacket) ex).target).toUpperCase())) {
                                                hellBanned = true;
                                                statusListener.onChatEvent(ChatEvent.Hellbanned);
                                                return;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception er) {
                                Debug.log(er);
                            }


                            try {
                                message.addExtension(new PacketTimeStamp(message));
                                message.setPacketID(Util.GeneratePacketId(message));
                            } catch (Exception er) {
                                Debug.log(er);
                            }


                            statusListener.onMessageReceived(null, message);
                            addMessageToCache(message);
                        } catch (Exception e) {
                            Debug.log("This exception is expected! " + e.toString());
                            Debug.log(e);
                        }
                    }
                });
                connection.addPacketInterceptor(new PacketInterceptor() {
                    @Override
                    public void interceptPacket(Packet packet) {

                    }
                }, new PacketFilter() {
                    @Override
                    public boolean accept(Packet packet) {
                        Debug.log(packet.toXML().toString());
                        return true;
                    }
                });


                ChatManager.getInstanceFor(connection).addChatListener(new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean isLocal) {
                        Debug.log("chatCreated");

                        if ( isLocal ) return;

                        statusListener.onRoomJoined(chat, false);
                        chat.addMessageListener(new MessageListener() {
                            int id = 0;
                            String salt = Util.getRandomInt(1000) + "";

                            @Override
                            public void processMessage(Chat lchat, Message message) {
                                Debug.log("processMessage() " + message.toXML().toString());
                                if ( message.getBody() == null ) {
                                    Debug.log("Blocking message with null body!");
                                    return;
                                }

                                if ( message.getPacketID() == null ) {
                                    message.setPacketID((id++) + salt);
                                }

                                try {
                                    message.addExtension(new PacketTimeStamp(message));
                                    message.setPacketID(Util.GeneratePacketId(message));
                                } catch ( Exception er ) {
                                    Debug.log(er);
                                }

                                if ( lchat.getParticipant().equals(message.getFrom()) )
                                    statusListener.onMessageReceived(lchat, message);
                                else {
                                    /* A very strange bug, need to create a new private chat with message.getFrom() */
                                    Debug.log("Applying workaround for bug lchat.participant != message.getFrom");
                                    Chat chat = startChat(message.getFrom());
                                    statusListener.onMessageReceived(chat, message);
                                }
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
        catch (InvalidCredentialsException er) {
            throw er;
        }
        catch (Exception e) {
            lastError = e.toString();
            Debug.log(e);
            return false;
        }
    }


    public static boolean Initialize(HaberListener statusListener, Context context) throws InvalidCredentialsException {
        cachedLobbyMessages = new ArrayList<>();

        if ( instance != null ) {
            Disconnect();
        }

        if (CredentialManager.ShouldLoginOnStartup(context) && (CredentialManager.GetSavedPassword(context).length() > 0) && (CredentialManager.GetSavedUsername(context).length() > 0)) {
            setIsGuest(false);
            try {
                setUser(CredentialManager.GetSavedUsername(context));
                setPassword(CredentialManager.GetSavedPassword(context));
            } catch ( Exception er ) {
                Debug.log(er);
            }
        }

        try {
            instance = new Haber(statusListener, context);
        } catch ( InvalidCredentialsException er) {
            throw er;
        } catch ( Exception er ) {
            return false;
        }

        return true;
    }

    public interface RoleChangeListener {
        public abstract void onModeratorGranted(String username);
        public abstract void onModeratorGranted(); //self

        public abstract void onModeratorRevoked(String username);
        public abstract void onModeratorRevoked(); //self

        public abstract void onAdminGranted(String username);
        public abstract void onAdminGranted(); //self

        public abstract void onAdminRevoked(String username);
        public abstract void onAdminRevoked(); //self
    }

    public interface HaberListener {
        public abstract void onStatusChanged(String status);
        public abstract void onLoggedIn(MultiUserChat haberChat);
        public abstract void onMessageReceived(Chat chat, Message message);
        public abstract void onRoomJoined(Chat chat, boolean selfStarted);
        public abstract void onChatEvent(ChatEvent event, String... params);
        //soft disconnect - when the user disconnects intentionally
        public abstract void onSoftDisconnect();
        public abstract void onDeleteRequested(String user);
    }

    public enum ChatEvent {
        Banned,
        Kicked,
        Joined,
        Left,
        Hellbanned
    }


}
