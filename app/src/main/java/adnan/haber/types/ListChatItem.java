package adnan.haber.types;

/**
 * Created by Adnan on 23.1.2015..
 */
public class ListChatItem {
    public String message;
    public String author;
    public String time;
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
