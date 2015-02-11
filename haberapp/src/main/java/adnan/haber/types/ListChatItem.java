package adnan.haber.types;

import java.util.Date;

/**
 * Created by Adnan on 23.1.2015..
 */
public class ListChatItem {
    public String message;
    public String author;
    public Date time;
    public String id;

    public boolean isSpacer = false;

    public Rank rank;

    public ListChatItem() {

    }

    public ListChatItem(String message) {
        this.message = message;
        this.isSpacer = true;
    }
}
