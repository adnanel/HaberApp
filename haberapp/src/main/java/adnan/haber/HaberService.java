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
import java.util.HashMap;
import java.util.List;

import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.types.Rank;
import adnan.haber.util.Debug;

public class HaberService extends Service implements Haber.HaberListener {
    private static List<Runnable> runnables = new ArrayList<>();
    private static List<Haber.HaberListener> haberListeners = new ArrayList<>();

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

    public static synchronized void addHaberListener(Haber.HaberListener listener) {
        if ( !haberListeners.contains(listener) )
            haberListeners.add(listener);
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

    public static boolean IsMod(String user) {
        Occupant occupant = haberChat.getOccupant(Haber.getFullUsername(user));
        if ( occupant == null ) return false;

        String role = occupant.getRole();
        return Rank.fromString(role) == Rank.Moderator;
    }

    public static Rank GetRankForUser(String user) {
        Occupant occupant = haberChat.getOccupant(Haber.getFullUsername(user));
        if ( occupant == null ) return Rank.Guest;  //todo return cached (and cache it first)

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
        if ( Haber.getShortUsername(user).toUpperCase().equals("ZAPHOD") )
            return Rank.Lamija;
        if ( Haber.getShortUsername(user).toUpperCase().equals("KOKI") )
            return Rank.Merima;

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

    private void destroyService() {
        super.onDestroy();
    }

    @Override
    public void onDestroy(){
        new Thread() {
            @Override
            public void run() {
                notificationManager.cancel(NOTIF_ID);
                for ( Chat chat : chatRooms )
                    chat.close();

                try {
                    haberChat.leave();
                } catch ( Exception er ) {
                    Debug.log(er);
                }

                Haber.Disconnect();

                destroyService();
            }
        }.start();

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
                if ( !haberChat.isJoined() ) {
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
                    } else if ( !haberChat.isJoined() ) {
                        Intent kickOnStart = new Intent(HaberService.this, KickedOnStartActivity.class);
                        kickOnStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        HaberService.this.startActivity(kickOnStart);

                        stopSelf();
                        return;
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

        if (AdvancedPreferences.ShouldVibrateInService(this)) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
        }
    }

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

        public void addPm(String from) {
            if ( pms.containsKey(from))
                pms.put(from, pms.get(from) + 1);
            else
                pms.put(from, 1);

            pmCount ++;
        }
    }
}
