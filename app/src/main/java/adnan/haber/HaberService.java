package adnan.haber;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;

import java.util.ArrayList;
import java.util.List;

import adnan.haber.types.Rank;
import adnan.haber.util.Debug;

public class HaberService extends Service implements Haber.HaberListener {
    private static List<Runnable> runnables = new ArrayList<>();
    private static List<Haber.HaberListener> haberListeners = new ArrayList<>();

    static List<Chat> chatRooms = new ArrayList<>();
    static MultiUserChat haberChat;

    public static boolean isConnected = false;
    public static synchronized void runOnHaberThread(Runnable runnable) {
        runnables.add(runnable);
    }

    private NotificationManager notificationManager;
    private int NOTIF_ID = 125125;

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
        switch ( GetRankForUser(Haber.getUsername())) {
            case Moderator:
            case Admin:
            case Enil:
            case Berina:
                return true;
            default:
                return false;
        }
    }

    public static void KickUser(String user, String reason) {
        try {
            haberChat.kickParticipant(user, reason);
        } catch ( Exception er ) {
            Debug.log(er);
        }
    }

    public static Rank GetRankForUser(String user) {
        Occupant occupant = haberChat.getOccupant(user);
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

        if ( Haber.getShortUsername(user).charAt(0) == 'ǂ')
            return Rank.Guest;

        String role = occupant.getRole();
        return Rank.fromString(role);
    }

    public static synchronized ArrayList<Haber.HaberListener> getHaberListeners() {
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



    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Haber");
        builder.setContentText("Servis za konekciju pokrenut u pozadini...");
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        PendingIntent intent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, SplashScreen.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        builder.setContentIntent(intent);
        notificationManager.notify(NOTIF_ID, builder.build());

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                Haber.initialize(HaberService.this, HaberService.this);

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
    public void onChatStarted(Chat chat, boolean isLocal) {
        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onChatStarted(chat, isLocal);
        }

        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

        if ( !chatRooms.contains( chat ) && chat != null )
            chatRooms.add(chat);
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
        isConnected = true;
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
    }

    @Override
    public void onRoomJoined(Chat chat) {
        ArrayList<Haber.HaberListener> corpses = new ArrayList<>();

        for ( Haber.HaberListener listener : getHaberListeners() ) {
            if ( listener == null )
                corpses.add(null);
            else
                listener.onRoomJoined(chat);
        }
        for (Haber.HaberListener listener : corpses )
            removeHaberListener(listener);

        if ( !chatRooms.contains( chat ) && chat != null )
            chatRooms.add(chat);
    }

    @Override
    public void onChatEvent(Haber.ChatEvent event, String... params) {

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

    public HaberService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
