package adnan.haber;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.jivesoftware.smack.packet.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import adnan.haber.adapters.ArchiveAdapter;
import adnan.haber.types.ArchiveItem;
import adnan.haber.types.MessageDirection;
import adnan.haber.util.ChatSaver;


public class Archive extends ActionBarActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        listView = (ListView)findViewById(R.id.lvArchive);
        new Thread() {
            @Override
            public void run() {
                HashMap<String, Integer> map = new HashMap<>();
                for ( Message msg : ChatSaver.getSavedMessages() ) {
                    if ( msg.getTo() == null || msg.getFrom() == null ) continue;
                    if ( msg.getSubject() == null ) continue;

                    if ( msg.getSubject().equals(MessageDirection.INCOMING) ) {
                        if ( map.containsKey(msg.getFrom()) )
                            map.put(msg.getFrom(), map.get(msg.getFrom()) + 1);
                        else
                            map.put(msg.getFrom(), 0);
                    } else if ( msg.getSubject().equals(MessageDirection.OUTGOING)) {
                        if ( map.containsKey(msg.getTo()))
                            map.put(msg.getTo(), map.get(msg.getTo()) + 1);
                        else
                            map.put(msg.getTo(), 0);
                    }
                }

                final ArrayList<ArchiveItem> list = new ArrayList<>();
                for (Map.Entry<String, Integer> pair : map.entrySet() ) {
                    list.add(new ArchiveItem(pair.getKey(), pair.getValue()));
                }
                final ArchiveAdapter adapter = new ArchiveAdapter(Archive.this, list);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                    }
                });
            }
        }.start();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_archive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_export) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
