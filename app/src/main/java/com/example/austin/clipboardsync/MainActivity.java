package com.example.austin.clipboardsync;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.security.Key;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends ActionBarActivity {
    public static final String PREFS_FILE = "Preferences";
    public static String username;
    public static String password;
    public static int owner_id = -1;
    static ArrayList<Clip> clips;
    ClipboardManager clipboard = null;
    ClipManager clip_client = null;
    ClipAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // set up adapter
        adapter = new ClipAdapter();
        ListView list = (ListView) findViewById(R.id.list_view_main);
        clips = new ArrayList<>();
        list.setAdapter(adapter);
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clip_client = new ClipManager(getString(R.string.server_url), "somekey");
        adapter.fetch_clips();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Clip c = (Clip) adapter.getItem(position);
                write_clipboard(c.text);
            }
        });

        // load preferences
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        username = settings.getString("username", "");
        password = settings.getString("password", "");
        owner_id = settings.getInt("owner_id", -1);
        if (username.equals("")) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putInt("owner_id", owner_id);
        editor.apply();
    }

    String read_clipboard() {
        String clipboard_contents = null;
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            clipboard_contents = item.getText().toString();
        }
        return clipboard_contents;
    }

    int write_clipboard(String text) {
        ClipData clip = ClipData.newPlainText("simple text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        return 0;
    }

    public void paste_button(View view) {
        new NetSync().execute(true);
    }

    public void fetch_button(View view) {
        new NetSync().execute(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class ClipAdapter extends BaseAdapter {

        public void fetch_clips() {
            new NetSync().execute();
        }

        public int getCount() {
            return clips.size();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.clipboard_item, parent, false);
            }
            TextView name = (TextView) convertView.findViewById(R.id.nameView);

            Clip c = (Clip) getItem(position);
            name.setText(c.text);
            return convertView;
        }

        public long getItemId(int position) {
            return clips.get(position).item_pk;
        }

        public Object getItem(int position) {
            return clips.get(position);
        }
    }

    class NetSync extends AsyncTask<Boolean, String, ArrayList<Clip>> {
        boolean is_paste = false;
        boolean not_text = false;

        protected ArrayList<Clip> doInBackground(Boolean... args) {
            boolean is_paste = false;
            if (args.length == 1) {
                is_paste = args[0];
            }
            this.is_paste = is_paste;

            if (is_paste) {
                if (read_clipboard() == null) {
                    not_text = true;
                    return null;
                }
                return clip_client.paste(read_clipboard());
            } else {
                return clip_client.get_clips(-1, -1);
            }
        }

        protected void onPostExecute(ArrayList<Clip> result) {
            if (not_text) {
                Toast.makeText(MainActivity.this, "There is no text on the clipboard.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (result != null) {
                if (is_paste) {
                    clips.add(0, result.get(0));
                    adapter.notifyDataSetChanged();
                } else {
                    clips = result;
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_LONG).show();
            }
        }
    }
}
