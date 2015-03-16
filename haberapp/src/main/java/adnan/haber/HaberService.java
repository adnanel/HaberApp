package adnan.haber;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;
import android.widget.Toast;

import org.apache.http.auth.InvalidCredentialsException;
import org.jivesoftware.smack.Chat;
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
import adnan.haber.util.ChatSaver;
import adnan.haber.util.Debug;
import adnan.haber.util.Util;

public class HaberService extends Service implements Haber.HaberListener, Haber.RoleChangeListener {
    private static List<Runnable> runnables = new ArrayList<>();
    private static List<Haber.HaberListener> haberListeners = new ArrayList<>();
    private static List<Haber.RoleChangeListener> roleChangeListeners = new ArrayList<>();

    static List<Chat> chatRooms = new ArrayList<>();
    private static MultiUserChat haberChat;

    public static synchronized void runOnHaberThread(Runnable runnable) {
        runnables.add(runnable);
    }

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
        haberCounter.resetCounters();

        if ( instance != null )
            instance.refreshNotification();
    }

    synchronized Runnable popRunnable() {
        Runnable run = runnables.get(runnables.size() - 1);
        runnables.remove(runnables.size() - 1);
        return run;
    }

    public static synchronized void addRoleListener(Haber.RoleChangeListener listener) {
        if ( roleChangeListeners.contains(listener) )
            roleChangeListeners.add(listener);
    }


    public static synchronized void addHaberListener(Haber.HaberListener listener) {
        if ( !haberListeners.contains(listener) )
            haberListeners.add(listener);
    }

    public static void StopService(Context context) throws Exception {
        if ( instance == null ) throw new Exception("No service to stop!");
        Haber.Disconnect();
        instance.stopSelf();
    }
    public static void RestartService(Context context) {
        try {
            Intent intent = new Intent(context, HaberService.class);
            context.stopService(intent);
            context.startService(intent);
        } catch ( Exception e ) {
            Debug.log(e);
        }
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

    private static synchronized ArrayList<Haber.HaberListener> getHaberListeners() {
        ArrayList<Haber.HaberListener> result = new ArrayList<>();
        for (Haber.HaberListener listener : haberListeners )
            result.add(listener);
        return result;
    }

    public static synchronized void removeHaberListener(Haber.HaberListener listener) {
        haberListeners.remove(listener);
    }

    @Override
    public void onDestroy(){
        notificationManager.cancel(NOTIF_ID);
        chatRooms.clear();
        haberChat = null;
        Haber.QuickDisconnect();

        runnables.clear();
        haberListeners.clear();
        instance = null;

        ChatSaver.Finalize();

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
        ChatSaver.Initialize(this);


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
                } catch ( InvalidCredentialsException er ) {
                    Debug.log("Invalid user/pass");
                    Debug.log(er);

                    Intent intent = new Intent(HaberService.this, WrongCredentials.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    for (Haber.HaberListener listener : getHaberListeners() )
                        listener.onSoftDisconnect();

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
                    int i;
                    int size = runnables.size();
                    for ( i = 0; i < size; i ++ )
                        popRunnable().run();

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

        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onStatusChanged(status);
        }

        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);
    }

    @Override
    public void onLoggedIn(MultiUserChat haberChat) {
        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onLoggedIn(haberChat);
        }
        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

        HaberService.haberChat = haberChat;
    }

    @Override
    public void onMessageReceived(Chat chat, Message message) {
        Date date = Util.getCurrentDate();
        message.setSubject(MessageDirection.INCOMING);

        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else if ( listener != this )
                listener.onMessageReceived(chat, message);
        }

        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

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
            Debug.log(String.format("Counter: %d\n chat is null: %d\ncorpses.length(): %d\nMessage: %s", counter, (chat == null) ? 1 : 0, corpses.size(), message.toXML().toString()));
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
                                } else
                                    HaberActivity.getInstance().sendMessage(msg.getFrom(), AutoReply.getMessage(HaberService.this));
                            }
                        });
                    } else {
                        Message msg = new Message();
                        msg.setSubject(MessageDirection.OUTGOING);
                        msg.setBody(AutoReply.getMessage(this));
                        msg.setTo(message.getFrom());
                        msg.setPacketID("autoReply");
                        msg.setPacketID(Util.GeneratePacketId(msg));


                        chat.sendMessage(msg.getBody());
                        ChatSaver.OnMessageReceived(chat, msg);
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
        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onRoomJoined(chat, selfStarted);
        }
        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

        if ( !chatRooms.contains( chat ) && chat != null )
            chatRooms.add(chat);
    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, String... params) {
        if ( event == Haber.ChatEvent.Hellbanned ) {
            counter++;
        }

        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onChatEvent(event, params);
        }
        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

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

        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onSoftDisconnect();
        }
        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

        this.stopSelf();
    }

    @Override
    public void onDeleteRequested(String user) {
        counter++;

        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onDeleteRequested(user);
        }
        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);
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
