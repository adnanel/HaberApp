package adnan.haber.util;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.Date;

import adnan.haber.types.FailedToSyncTimeException;

/**
 * Created by Adnan on 16.3.2015.
 */
public class ServerTimeSync {
    private static Date phoneDate;
    private static Date serverDate;

    public static void refreshTime() throws FailedToSyncTimeException {
        phoneDate = new Date();
        boolean success = false;

        for ( int i = 0; i < 5; i ++ ) {
            try {
                serverDate = getServerDate();
                success = true;
                break;
            } catch (Exception er) {
                Debug.log(er);
            }
        }

        if ( !success ) throw new FailedToSyncTimeException();
    }

    public static Date getTime() {
        if ( serverDate == null || phoneDate == null ) return null;

        return new Date((new Date()).getTime() + (serverDate.getTime() - phoneDate.getTime()) + 2000); //+ 2000 jer etf.ba openfire server zuri 2 sekunde
    }

    private static Date getServerDate() throws Exception {
        String TIME_SERVER = "ba.pool.ntp.org";
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
        TimeInfo timeInfo = timeClient.getTime(inetAddress);
        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
        return new Date(returnTime);
    }
}
