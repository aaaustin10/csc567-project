package com.example.austin.clipboardsync;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class Preferences extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        ((TextView) findViewById(R.id.username_field)).setText(MainActivity.username);
        ((TextView) findViewById(R.id.password_field)).setText(MainActivity.password);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_preferences, menu);
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

    public void save_button(View view) {
        new NetTask().execute();
    }

    // courtesy of 'Pavel Repin' from stack overflow
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    int do_post(HttpURLConnection urlConnection, String username, String password) throws IOException {
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("charset", "utf-8");

        JSONObject response = new JSONObject();
        String json_string = "";
        try {
            response.put("username", username);
            ByteArrayOutputStream baos = new ByteArrayOutputStream( );
            baos.write(username.getBytes());
            baos.write(password.getBytes());
            response.put("passkey", Encrypter.hash(baos.toByteArray()));
            json_string = response.toString();
        } catch (JSONException e) {
            System.out.println(e);
        }

        byte[] buffer = json_string.getBytes();
        urlConnection.setFixedLengthStreamingMode(buffer.length);
        urlConnection.setRequestProperty("Content-Length", "" + Integer.toString(buffer.length));

        OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
        out.write(buffer);
        out.flush();

        return Integer.parseInt(convertStreamToString(urlConnection.getInputStream()));
    }

    Integer GetOrCreateLogin () {
        String username = ((TextView) findViewById(R.id.username_field)).getText().toString();
        String password = ((TextView) findViewById(R.id.password_field)).getText().toString();

        HttpURLConnection urlConnection = null;
        Integer result = 0;
        try {
            String adapted_url = "" + getString(R.string.login_url);
            if (true) {
                adapted_url += "?create=" + Boolean.toString(true);
            }
            URL url = new URL(adapted_url);
            urlConnection = (HttpURLConnection) url.openConnection();
            result = do_post(urlConnection, username, password);
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
    class NetTask extends AsyncTask<Boolean, String, Integer> {
        protected Integer doInBackground(Boolean... args) {
            return GetOrCreateLogin();
        }

        protected void onPostExecute(Integer result) {
            if (result == -1) {
                Toast.makeText(Preferences.this, "Wrong username or password", Toast.LENGTH_LONG).show();
            } else if (result == 0) {
                // doesn't exist create it?
                System.out.println("what the");
            } else {
                MainActivity.username = ((TextView) findViewById(R.id.username_field)).getText().toString();
                MainActivity.password = ((TextView) findViewById(R.id.password_field)).getText().toString();
                MainActivity.owner_id = result;
                finish();
            }
        }
    }
}
