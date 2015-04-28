package com.example.austin.clipboardsync;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    ClipboardManager clipboard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }

    String read_clipboard() {
        String clipboard_contents = null;
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            clipboard_contents = item.getText().toString();
        }
        return clipboard_contents;
    }

    void write_clipboard(String text) {
        ClipData clip = ClipData.newPlainText("simple text", text);
        clipboard.setPrimaryClip(clip);
    }

    public void paste_button(View view) {
        String clipboard_contents = read_clipboard();

        // if information retrieved
        if (clipboard_contents != null) {
            Toast.makeText(this, clipboard_contents, Toast.LENGTH_LONG).show();
            NetTaskDescription task = new NetTaskDescription(getString(R.string.server_url), false);
            new NetworkTask().execute(task);
        } else {
            Toast.makeText(this, "No text on clipboard", Toast.LENGTH_LONG).show();
        }
    }

    public void copy_button(View view) {
        NetTaskDescription task = new NetTaskDescription(getString(R.string.server_url), true);
        new NetworkTask().execute(task);
    }

    private class NetTaskDescription {
        public String url;
        public boolean is_fetch;

        NetTaskDescription(String url, boolean is_fetch) {
            this.url = url;
            this.is_fetch = is_fetch;
        }
    }

    private class NetworkTask extends AsyncTask<NetTaskDescription, String, ArrayList> {
        protected ArrayList doInBackground(NetTaskDescription... task_list) {
            NetTaskDescription task = task_list[0];
            HttpURLConnection urlConnection = null;
            ArrayList result = null;
            try {
                URL url = new URL(task.url);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                if (urlConnection.getResponseCode() != 200) {
                    throw new IOException("Response code was not 200.");
                }

                JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
                try {
                    result = readJson(reader);
                } finally {
                    reader.close();
                }

            } catch (MalformedURLException e) {
                System.out.println("Bad url");
            } catch (IOException e) {
                System.out.println("IO exception: " + e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return result;
        }

        protected void onPostExecute(ArrayList arg) {
            ArrayList<ArrayList> result = arg;
            if (result != null) {
                write_clipboard((String) result.get(0).get(1));
            }
        }
    }

    public ArrayList readJson(JsonReader reader) throws IOException {
        ArrayList messages = new ArrayList();

        reader.beginArray();
        while (reader.hasNext()) {
            messages.add(readMessage(reader));
        }
        reader.endArray();

        return messages;
    }

    public ArrayList readMessage(JsonReader reader) throws IOException {
        ArrayList<Object> list = new ArrayList<>();
        long owner_id = -1;
        String timestamp = "";
        String text = "";

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("timestamp")) {
                timestamp = reader.nextString();
            } else if (name.equals("contents")) {
                text = reader.nextString();
            } else if (name.equals("owner_id")) {
                owner_id = reader.nextLong();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        list.add(owner_id);
        list.add(text);
        list.add(timestamp);
        return list;
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
}
