package adnan.haber.types;

import java.util.Date;

/**
 * Created by Adnan on 23.1.2015..
 */
public class ArchiveItem {
    public String username;
    public int messageCount;

    public ArchiveItem(String user, int count) {
        this.username = user;
        this.messageCount = count;
    }

}
