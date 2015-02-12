package adnan.haber.packets;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created by Adnan on 12.2.2015..
 */
public class HellbanPacketProvider implements PacketExtensionProvider {
    @Override
    public PacketExtension parseExtension(XmlPullParser xmlPullParser) throws Exception {
        return new HellbanPacket(xmlPullParser.nextText());
    }
}
