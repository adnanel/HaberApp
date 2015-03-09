package adnan.haber.packets;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.delay.packet.DelayInformation;

import java.util.Date;

import adnan.haber.util.Debug;
import adnan.haber.util.Util;

/**
 * Created by Adnan on 12.2.2015..
 */

public class PacketTimeStamp implements PacketExtension {
    String time;

    public String getTime() {
        return time;
    }

    public PacketTimeStamp(Message message) {

        try {
            for (PacketExtension ext : message.getExtensions()) {
                if (ext instanceof DelayInformation) {

                    time = Util.dateToFormat(((DelayInformation) ext).getStamp());
                    return;
                }
            }
        } catch ( Exception er ) {
            Debug.log(er);
        }

        time = Util.dateToFormat(new Date());
    }

    public PacketTimeStamp(String stamp) {
        time = stamp;
    }

    @Override
    public String getElementName() {
        return "PacketTimeStamp";
    }

    @Override
    public String getNamespace() {
        return "PacketTimeStamp";
    }

    @Override
    public CharSequence toXML() {
        return "<PacketTimeStamp> " + time + " <PacketTimeStamp/>";
    }
}