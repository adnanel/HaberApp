package adnan.haber.packets;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Created by Adnan on 12.2.2015..
 */


public class DeletePacket implements PacketExtension {
    public String target;

    public DeletePacket(String target) {
        this.target = target;
    }


    @Override
    public String getElementName() {
        return "DeletePacket";
    }

    @Override
    public String getNamespace() {
        return "DeletePacket";
    }

    @Override
    public CharSequence toXML() {
        return "<DeletePacket> " + target + " <DeletePacket/>";
    }
}
