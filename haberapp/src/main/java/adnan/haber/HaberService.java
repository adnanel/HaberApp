package adnan.haber;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.apache.http.auth.InvalidCredentialsException;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.types.MessageDirection;
import adnan.haber.types.Rank;
import adnan.haber.util.AutoReply;
import adnan.haber.util.Debug;
import adnan.haber.util.Util;

public class HaberService extends Service implements Haber.HaberListener,
        Haber.RoleChangeListener,
        ConnectionListener {

    static List<Chat> chatRooms = new ArrayList<>();
    private static MultiUserChat haberChat;

    private NotificationManager notificationManager;
    private int NOTIF_ID = 125125;
    private int NOTIF_EVENT_ID = NOTIF_ID + 1;

    public static HaberCounter haberCounter;

    private static HaberService instance = null;

    public static boolean haberChatExists() {
        return haberChat != null;
    }

    public static MultiUserChat getHaberChat() {
        if ( haberChat == null )
            haberChat = Haber.getHaberChat();
        return haberChat;
    }

    public static void resetCounters() {
        if (haberCounter != null )
            haberCounter.resetCounters();

        if ( instance != null )
            instance.refreshNotification();
    }

    public static void StopService() throws Exception {
        if ( instance == null ) throw new Exception("No service to stop!");
        Haber.Disconnect();
        instance.stopSelf();
    }

    private static Runnable onStartRunnable = null;


    public static boolean StartService(Context context, Runnable onStart) {
        try {
            if ( instance != null ) {
                onStart.run();
                return false;
            }
            onStartRunnable = onStart;
            Intent intent = new Intent(context, HaberService.class);
            context.startService(intent);
        } catch ( Exception e ) {
            Debug.log(e);
        }
        return true;
    }


    public static void RestartService(Context context, Runnable onStart) {
        try {
            onStartRunnable = onStart;
            Intent intent = new Intent(context, HaberService.class);
            context.stopService(intent);
            context.startService(intent);
        } catch ( Exception e ) {
            Debug.log(e);
        }
    }

    public static void RestartService(Context context) {
        RestartService(context, null);
    }

    public static boolean CanKick() {
        return IsMod(Haber.getUsername());
    }

    public static void KickUser(String user, String reason) {
        try {
            haberChat.kickParticipant(user, reason);
        } catch ( Exception er ) {
            Debug.log(er);
        }
    }

    private static HashMap<String, Boolean> modCache = new HashMap<>();
    public static boolean IsMod(String user) {
        if ( modCache.containsKey(user)) return modCache.get(user);
        boolean result = isMod(user);
        modCache.put(user, result);
        return result;
    }

    private static boolean isMod(String user) {
        Occupant occupant = haberChat.getOccupant(Haber.getFullUsername(user));
        if ( occupant == null ) return false;

        String role = occupant.getRole();
        return Rank.fromString(role) == Rank.Moderator;
    }

    private static HashMap<String, Rank> rankCache = new HashMap<>();

    public static Rank GetRankForUser(String user) {
        user = Haber.getShortUsername(user);
        if (rankCache.containsKey(user)) {
            return rankCache.get(user);
        }
        Rank rank;
        try {
            rankCache.put(user, rank = findRankForUser(user));
        } catch ( TemporaryUnknownException er ) {
            Debug.log(String.format("Rank for %s is temporary unknown, treating him like a guest.", user));
            return Rank.Guest;
        }
        return rank;
    }

    private static Rank findRankForUser(String user) throws TemporaryUnknownException {
        if ( haberChat == null )
            throw new TemporaryUnknownException();

        Occupant occupant = haberChat.getOccupant(Haber.getFullUsername(user));
        if ( occupant == null )
            throw new TemporaryUnknownException();

        if ( Haber.getShortUsername(user).toUpperCase().equals("ADNAN_E") )
            return Rank.Adnan;
        if ( Haber.getShortUsername(user).toUpperCase().equals("EON") )
            return Rank.Enil;
        if ( Haber.getShortUsername(user).toUpperCase().equals("BEE") || Haber.getShortUsername(user).toUpperCase().equals("BII") )
            return Rank.Berina;
        if ( Haber.getShortUsername(user).toUpperCase().equals("MATHILDA") )
            return Rank.Mathilda;
        if ( Haber.getShortUsername(user).toUpperCase().equals("SOUXIE") )
            return Rank.Alma;
        if ( Haber.getShortUsername(user).toUpperCase().equals("MEMI~") )
            return Rank.Memi;
        if ( Haber.getShortUsername(user).toUpperCase().equals("ZAPHOD") || Haber.getShortUsername(user).toUpperCase().equals("LUCY") )
            return Rank.Lamija;
        if ( Haber.getShortUsername(user).toUpperCase().equals("KOKI") )
            return Rank.Merima;
        if ( Haber.getShortUsername(user).toUpperCase().equals("VEDRAN") )
            return Rank.Vedran;

        if ( Haber.getShortUsername(user).charAt(0) == 'ǂ')
            return Rank.Guest;

        String role = occupant.getRole();
        return Rank.fromString(role);
    }


    @Override
    public void onDestroy(){
        notificationManager.cancel(NOTIF_ID);
        chatRooms.clear();
        haberChat = null;
        Haber.QuickDisconnect();

        instance = null;

        super.onDestroy();
    }

    public void refreshNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Haber Servis");
        builder.setContentText(getServiceStatus());
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ugasi haber", PendingIntent.getBroadcast(
                this,
                0,
                new Intent(this, StopServiceBroadcast.class),
                PendingIntent.FLAG_CANCEL_CURRENT
        ));


        PendingIntent intent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, SplashScreen.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        builder.setContentIntent(intent);
        notificationManager.notify(NOTIF_ID, builder.build());
    }



    String getServiceStatus() {
        if ( HaberActivity.getInstance() != null ) {
            return "Servis pokrenut u pozadini...";
        }

        if ( haberCounter.getPmCount() == 0 && haberCounter.getMentionCount() == 0 ) {
            return "Nema novih poruka/spominjanja";
        } else if ( haberCounter.getPmCount() != 0 && haberCounter.getMentionCount() == 0 ) {
            return String.format("Privatne poruke (%d)", haberCounter.getPmCount());
        } else if ( haberCounter.getPmCount() != 0 && haberCounter.getMentionCount() != 0 ) {
            return String.format("Privatne poruke (%d), Spominjanja (%d)", haberCounter.getPmCount(), haberCounter.getMentionCount());
        } else if ( haberCounter.getPmCount() == 0 && haberCounter.getMentionCount() != 0 ) {
            return String.format("Spominjanja (%d)", haberCounter.getMentionCount());
        }

        return "UNKNOWN STATE!";
    }

    @Override
    public void onCreate() {
        //all that's initialized here, should be disposed in HaberService's onDestroy();
        Debug.Initialize(this); // <- must be first
        if ( onStartRunnable != null )
            onStartRunnable.run();

        instance = this;
        haberCounter = new HaberCounter();

        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        refreshNotification();


        new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    if (!Haber.Initialize(HaberService.this, HaberService.this)) {
                        stopSelf();
                        return;
                    }
                    Haber.setConnectionListener(HaberService.this);
                } catch ( InvalidCredentialsException er ) {
                    Debug.log("Invalid user/pass");
                    Debug.log(er);

                    Intent intent = new Intent(HaberService.this, WrongCredentials.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    if ( HaberActivity.getInstance() != null )
                        HaberActivity.getInstance().onSoftDisconnect();

                    stopSelf();
                    return;
                }
                if ( (haberChat == null) || !haberChat.isJoined() ) {
                    if ( HaberActivity.InstanceExists() ) {
                        Intent kickOnStart = new Intent(HaberService.this, KickedOnStartActivity.class);
                        kickOnStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        HaberService.this.startActivity(kickOnStart);

                        stopSelf();
                    } else {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(HaberService.this);
                        builder.setContentTitle("Haber");
                        builder.setContentText("Izbačeni ste iz sobe!");
                        builder.setSmallIcon(R.drawable.ic_launcher);
                        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
                        builder.setAutoCancel(true);

                        Intent kickedIntent = new Intent(HaberService.this, KickedOnStartActivity.class);
                        PendingIntent intent = PendingIntent.getActivity(
                                HaberService.this,
                                1,
                                kickedIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );


                        builder.setContentIntent(intent);
                        notificationManager.notify(NOTIF_EVENT_ID, builder.build());

                        stopSelf();
                    }
                    return;
                }

                while ( !this.isInterrupted() ) {
                    try {
                        Thread.sleep(500);
                    } catch ( Exception e ) {
                        Debug.log(e);
                        break;
                    }

                    if ( !Haber.isConnected() ) {
                        stopSelf();
                    } else if ( haberChat != null ) {
                        if (!haberChat.isJoined()) {
                            Intent kickOnStart = new Intent(HaberService.this, KickedOnStartActivity.class);
                            kickOnStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            HaberService.this.startActivity(kickOnStart);

                            stopSelf();
                            return;
                        }
                    }
                }

                stopSelf();
            }
        }.start();
    }

    @Override
    public void onStatusChanged(final String status) {
        Toast.makeText(HaberService.this, status, Toast.LENGTH_LONG).show();

        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onStatusChanged(status);
        else
            stopSelf();
    }

    @Override
    public void onLoggedIn(MultiUserChat haberChat) {
        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onLoggedIn(haberChat);
        else
            stopSelf();

        HaberService.haberChat = haberChat;
    }

    @Override
    public void onMessageReceived(Chat chat, Message message) {
        Date date = Util.getCurrentDate();
        message.setSubject(MessageDirection.INCOMING);

        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onMessageReceived(chat, message);
        else
            stopSelf();

        if ( !chatRooms.contains( chat ) && chat != null )
            chatRooms.add(chat);

        //if activity isn't shown => haber is in background
        if ( HaberActivity.getInstance() == null ) {
            if ( chat != null ) {
                if ( message.getFrom() != null ) {
                    haberCounter.addPm(message.getFrom());
                    refreshNotification();
                }
            } else if ( message.getBody() != null ) {
                if ( !Haber.IsGuest() ) {
                    if (message.getBody().toUpperCase().contains(Haber.getUsername().toUpperCase())) {
                        haberCounter.mentionCounter++;
                        refreshNotification();
                    }
                } else {
                    if (message.getBody().toUpperCase().contains(Haber.getUsername().substring(1).toUpperCase())) {
                        haberCounter.mentionCounter++;
                        refreshNotification();
                    }
                }
            }
        }

        counter++;
        if ( counter >= 25 && !message.getSubject().equals(MessageDirection.OUTGOING)) {
            if (AdvancedPreferences.ShouldVibrate(this) && AdvancedPreferences.ShouldVibrateInService(this) && !HaberActivity.InstanceExists()) {
                boolean shouldVibrate = false;
                if ( chat == null ) {
                    if ( message.getBody() != null ) {
                        if (AdvancedPreferences.ShouldVibrateOnReply(this)) {
                            String mark = "@" + Haber.getUsername();
                            if (message.getBody().toUpperCase().contains(mark.toUpperCase())) {
                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                                shouldVibrate = true;
                            } else if (Haber.IsGuest()) {
                                mark = "@" + Haber.getUsername().substring(1);
                                if (message.getBody().toUpperCase().contains(mark.toUpperCase())) {
                                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                                    shouldVibrate = true;
                                }
                            }
                        }
                    }
                } else {
                    shouldVibrate = true;
                }

                if ( shouldVibrate )
                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
            }
        }

        long delay = ((Util.getCurrentDate()).getTime() - date.getTime());
        Debug.log("onMessageReceived - " + delay + " ms");
        if ( delay > 300 ) {
            Debug.log("Slow frame detected, data:");
            Debug.log(String.format("Counter: %d\n chat is null: %d\nMessage: %s", counter, (chat == null) ? 1 : 0, message.toXML().toString()));
        }


        //auto reply code
        if (AutoReply.getEnabled(this) && (chat != null)) {
            if ( AutoReply.getMode(this) == AutoReply.MODE_EVERY_MESSAGE) {
                try {
                    Debug.log("Sending AutoReply message to " + chat.getParticipant() + ", message: " + AutoReply.getMessage(this));

                    if ( HaberActivity.InstanceExists() ) {
                        final Message msg = message;
                        HaberActivity.getInstance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if ( HaberActivity.getInstance().findChatForUser(msg.getFrom()) == null ) {
                                    Debug.log("Couldn't find the chat for " + msg.getFrom() + "!!!");
                                } else {
                                    if ( HaberActivity.getInstance().getCurrentChat() != null ) {
                                        if (HaberActivity.getInstance().getCurrentChat().getParticipant().equals(msg.getFrom()))
                                            Debug.log("Not auto replying after all. (current chat participant == sender)");
                                        else
                                            HaberActivity.getInstance().sendMessage(msg.getFrom(), AutoReply.getMessage(HaberService.this));
                                    } else {
                                        HaberActivity.getInstance().sendMessage(msg.getFrom(), AutoReply.getMessage(HaberService.this));
                                    }
                                }
                            }
                        });
                    } else {
                        Message msg = new Message();
                        msg.setSubject(MessageDirection.OUTGOING);
                        msg.setBody(AutoReply.getMessage(this));
                        msg.setTo(message.getFrom());
                        msg.setPacketID("autoReply");


                        chat.sendMessage(msg.getBody());
                    }
                } catch ( Exception er ) {
                    Debug.log(er);
                }
            }
        }
        //end of auto reply code
    }
    int counter = 0;

    @Override
    public void onRoomJoined(Chat chat, boolean selfStarted) {
        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onRoomJoined(chat, selfStarted);
        else
            stopSelf();

        if ( !chatRooms.contains( chat ) && chat != null )
            chatRooms.add(chat);
    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, String... params) {
        if ( event == Haber.ChatEvent.Hellbanned ) {
            counter++;
        }

        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onChatEvent(event, params);
        else
            stopSelf();

        if ( params.length == 2 && (event == Haber.ChatEvent.Banned || event == Haber.ChatEvent.Kicked)) {
            if ( HaberActivity.InstanceExists() ) {
                Intent kickOnStart = new Intent(HaberService.this, KickedOnStartActivity.class);
                kickOnStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                kickOnStart.putExtra("reason", params[1]);
                kickOnStart.putExtra("user", params[0]);

                HaberService.this.startActivity(kickOnStart);

                stopSelf();
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(HaberService.this);
                builder.setContentTitle("Haber");
                builder.setContentText("Izbačeni ste iz sobe!");
                builder.setSmallIcon(R.drawable.ic_launcher);
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
                builder.setAutoCancel(true);

                Intent kickedIntent = new Intent(HaberService.this, KickedOnStartActivity.class);
                kickedIntent.putExtra("reason", params[1]);
                kickedIntent.putExtra("user", params[0]);

                PendingIntent intent = PendingIntent.getActivity(
                        HaberService.this,
                        1,
                        kickedIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

                builder.setContentIntent(intent);
                notificationManager.notify(NOTIF_EVENT_ID, builder.build());

                stopSelf();
            }
        }
    }

    @Override
    public void onSoftDisconnect() {
        chatRooms.clear();

        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onSoftDisconnect();

        this.stopSelf();
    }

    @Override
    public void onDeleteRequested(String user) {
        counter++;

        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        if ( HaberActivity.getInstance() != null )
            HaberActivity.getInstance().onDeleteRequested(user);
        else
            stopSelf();
    }

    public HaberService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onModeratorGranted(String username) {

    }

    @Override
    public void onModeratorGranted() {

    }

    @Override
    public void onModeratorRevoked(String username) {

    }

    @Override
    public void onModeratorRevoked() {

    }

    @Override
    public void onAdminGranted(String username) {

    }

    @Override
    public void onAdminGranted() {

    }

    @Override
    public void onAdminRevoked(String username) {

    }

    @Override
    public void onAdminRevoked() {

    }

    @Override
    public void connected(XMPPConnection xmppConnection) {

    }

    @Override
    public void authenticated(XMPPConnection xmppConnection) {

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Debug.log("Connection closing, error: ");
        Debug.log(e);

        if ( HaberActivity.getInstance() != null ) {
            if ( AdvancedPreferences.AutoReconnectEnabled(HaberService.this) ) {
                HaberService.this.sendBroadcast(new Intent(this, RestartServiceBroadcast.class));

                HaberActivity.getInstance().finish();
                stopSelf();
            } else {
                HaberActivity.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HaberActivity.getInstance());
                        builder.setTitle("Pukla konekcija!");
                        builder.setMessage("Izgubljena konekcija sa etf.ba!");
                        builder.setPositiveButton("Povezi se opet", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopSelf();
                                HaberActivity.getInstance().finish();

                                Intent intent = new Intent(HaberService.this, SplashScreen.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                startActivity(intent);
                            }
                        });
                        builder.setNegativeButton("Zatvori app", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopSelf();
                                HaberActivity.getInstance().finish();
                            }
                        });
                        builder.setCancelable(false);
                        builder.create().show();
                    }
                });
            }
        } else {
            if ( AdvancedPreferences.AutoReconnectEnabled(HaberService.this) ) {
                HaberService.this.sendBroadcast(new Intent(this, RestartServiceBroadcast.class));

                stopSelf();
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(HaberService.this);
                builder.setContentTitle("Haber");
                builder.setContentText("Izgubili ste konekciju sa haberom! Dodirni ovdje da se opet spojiš!");
                builder.setSmallIcon(R.drawable.ic_launcher);
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
                builder.setAutoCancel(true);

                PendingIntent intent = PendingIntent.getBroadcast(
                        this,
                        0,
                        new Intent(this, RestartServiceBroadcast.class),
                        PendingIntent.FLAG_CANCEL_CURRENT
                );


                builder.setContentIntent(intent);
                notificationManager.notify(NOTIF_EVENT_ID, builder.build());

                stopSelf();
            }
        }
    }

    @Override
    public void reconnectingIn(int i) {

    }

    @Override
    public void reconnectionSuccessful() {

    }

    @Override
    public void reconnectionFailed(Exception e) {

    }

    public class HaberCounter {
        private HashMap<String, Integer> pms = new HashMap<>();
        private int mentionCounter = 0;
        private int pmCount = 0;

        public void resetCounters() {
            pms.clear();
            mentionCounter = 0;
            pmCount = 0;
        }

        public int getPmCount() {
            return pmCount;
        }

        public int getMentionCount() {
            return mentionCounter;
        }


        public HashMap<String, Integer> getPms() { /* tehe */
            return pms;
        }

        private void addPm(String from) {
            if ( pms.containsKey(from))
                pms.put(from, pms.get(from) + 1);
            else
                pms.put(from, 1);

            pmCount ++;
        }
    }

    public static class TemporaryUnknownException extends Exception {
    }
}
