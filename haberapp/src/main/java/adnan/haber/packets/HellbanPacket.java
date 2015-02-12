package adnan.haber.packets;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Created by Adnan on 12.2.2015..
 */


public class HellbanPacket implements PacketExtension {
    public String target;

    public HellbanPacket(String target) {
        this.target = target;
    }


    @Override
    public String getElementName() {
        return "HellbanPacket";
    }

    @Override
    public String getNamespace() {
        return "HellbanPacket";
    }

    @Override
    public CharSequence toXML() {
        return "<HellbanPacket> " + target + " <HellbanPacket/>";
    }
}
