package com.example.austin.clipboardsync;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public String read_clipboard(View view) {
        String clipboard_contents = null;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            clipboard_contents = item.getText().toString();
        }

        // if information retrieved
        if (clipboard_contents != null) {
            Toast.makeText(this, clipboard_contents, Toast.LENGTH_LONG).show();
            new DownloadImageTask().execute("http://www.google.com/");
            return clipboard_contents;
        } else {
            Toast.makeText(this, "No text on clipboard", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private class DownloadImageTask extends AsyncTask<String, String, String> {
        protected String doInBackground(String... urls) {
            System.out.println((urls[0]));
            return urls[0];
        }

        protected void onPostExecute(String result) {
            System.out.println(result);
        }
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
