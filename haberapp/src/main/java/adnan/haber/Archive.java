package adnan.haber;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.jivesoftware.smack.packet.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import adnan.haber.adapters.ArchiveAdapter;
import adnan.haber.adapters.ChatAdapter;
import adnan.haber.adapters.ReadOnlyChatAdapter;
import adnan.haber.types.ArchiveItem;
import adnan.haber.types.ListChatItem;
import adnan.haber.types.MessageDirection;
import adnan.haber.util.ChatSaver;
import adnan.haber.util.HaberSSLSocketFactory;


public class Archive extends ActionBarActivity {
    private final static String PUBLIC_CHAT = "Javni chat";

    ListView listView;

    ArchiveAdapter archiveAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        getSupportActionBar().hide();

        listView = (ListView)findViewById(R.id.lvArchive);

        new Thread() {
            @Override
            public void run() {
                HashMap<String, Integer> map = new HashMap<>();
                map.put(PUBLIC_CHAT, ChatSaver.getSavedLobbyMessagesCount());

                for ( Message msg : ChatSaver.getSavedMessages() ) {
                    if ( msg.getTo() == null || msg.getFrom() == null ) continue;
                    if ( msg.getSubject() == null ) continue;

                    if ( msg.getSubject().equals(MessageDirection.INCOMING) ) {
                        if ( map.containsKey(msg.getFrom()) )
                            map.put(msg.getFrom(), map.get(msg.getFrom()) + 1);
                        else
                            map.put(msg.getFrom(), 1);
                    } else if ( msg.getSubject().equals(MessageDirection.OUTGOING)) {
                        if ( map.containsKey(msg.getTo()))
                            map.put(msg.getTo(), map.get(msg.getTo()) + 1);
                        else
                            map.put(msg.getTo(), 1);
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
                        listView.setAdapter(archiveAdapter = adapter);
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                ArchiveItem item = (ArchiveItem)parent.getItemAtPosition(position);

                                if ( item.username.equals(PUBLIC_CHAT) ) {
                                    final ReadOnlyChatAdapter adapter = new ReadOnlyChatAdapter(Archive.this, new ArrayList<ListChatItem>());

                                    for ( Message msg : ChatSaver.getSavedLobbyMessages(ChatSaver.ALL)) {
                                        if (!msg.getPacketID().equals("divider") ) {
                                            adapter.addItem(msg);
                                        }
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            listView.setAdapter(adapter);
                                            getSupportActionBar().show();
                                        }
                                    });
                                } else {
                                    final ReadOnlyChatAdapter adapter = new ReadOnlyChatAdapter(Archive.this, new ArrayList<ListChatItem>());

                                    for (Message msg : ChatSaver.getSavedMessages(ChatSaver.ALL)) {
                                        if (msg.getTo() == null || msg.getFrom() == null) continue;
                                        if (msg.getSubject() == null) continue;
                                        if (msg.getPacketID().equals("divider") ) {
                                            adapter.putDivider(msg.getBody());
                                        } else if (msg.getSubject().equals(MessageDirection.INCOMING)) {
                                            if (msg.getFrom().equals(item.username))
                                                adapter.addItem(msg);
                                        } else if (msg.getSubject().equals(MessageDirection.OUTGOING)) {
                                            if (msg.getTo().equals(item.username))
                                                adapter.addItem(msg);
                                        }
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            listView.setAdapter(adapter);
                                            getSupportActionBar().show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }
        }.start();

    }


    @Override
    public void onBackPressed() {
        if ( listView.getAdapter() instanceof ArchiveAdapter ) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listView.setAdapter(archiveAdapter);
                    getSupportActionBar().hide();
                }
            });
        }
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Izaberi format");
            builder.setPositiveButton("Prekid", null);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] {
                "CSV"
            });
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if ( which == 0 ) {
                        //csv
                        if ( !(listView.getAdapter() instanceof ReadOnlyChatAdapter) ) {
                            Toast.makeText(Archive.this, "todo :|", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ReadOnlyChatAdapter adapter = (ReadOnlyChatAdapter)listView.getAdapter();
                    }
                }
            });
            builder.create().show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
