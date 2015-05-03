package com.example.austin.clipboardsync;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by austin on 5/2/15.
 */

public class ClipManager {
    String server_url = null;

    ArrayList<Clip> get_clips(int start, int end) {
        NetTaskDescription task = new NetTaskDescription(server_url, true, start, end);
        return handle_network(task);
    }

    ClipManager(String url) {
        server_url = url;
    }

    ArrayList<Clip> paste(String contents) {
        NetTaskDescription task = new NetTaskDescription(server_url, false, new Clip(contents));
        return handle_network(task);
    }

    public class NetTaskDescription {
        public String url;
        public boolean is_copy;
        public int start;
        public int amount;
        public Clip clip;

        NetTaskDescription(String url, boolean is_copy, int start, int amount) {
            this.url = url;
            this.is_copy = is_copy;
            this.start = start;
            this.amount = amount;
            this.clip = null;
        }

        NetTaskDescription(String url, boolean is_copy, Clip clip) {
            this.url = url;
            this.is_copy = is_copy;
            this.start = -1;
            this.amount = -1;
            this.clip = clip;
        }
    }

    ArrayList<Clip> do_post(HttpURLConnection urlConnection, String contents) throws IOException {
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("charset", "utf-8");

        JSONObject response = new JSONObject();
        String json_string = "";
        try {
            response.put("contents", contents);
            response.put("owner_id", 1);
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

        InputStream in = new BufferedInputStream(urlConnection.getInputStream());

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readJson(reader);
        } finally {
            reader.close();
        }
    }

    ArrayList do_get(HttpURLConnection urlConnection) throws IOException {
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());

        if (urlConnection.getResponseCode() != 200) {
            throw new IOException("Response code was not 200.");
        }

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readJson(reader);
        } finally {
            reader.close();
        }
    }

    ArrayList<Clip> handle_network(NetTaskDescription task) {
        HttpURLConnection urlConnection = null;
        ArrayList<Clip> result = null;
        try {
            String adapted_url = "" + task.url;
            adapted_url += "?";
            if (task.start != -1) {
                adapted_url += "&start=" + Integer.toString(task.start);
            }
            if (task.amount != -1) {
                adapted_url += "&amount=" + Integer.toString(task.amount);
            }
            URL url = new URL(adapted_url);
            urlConnection = (HttpURLConnection) url.openConnection();

            if (task.is_copy) {
                result = do_get(urlConnection);
            } else {
                result = do_post(urlConnection, task.clip.text);
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

    public ArrayList readJson(JsonReader reader) throws IOException {
        ArrayList<Clip> messages = new ArrayList();

        reader.beginArray();
        while (reader.hasNext()) {
            messages.add(readMessage(reader));
        }
        reader.endArray();

        return messages;
    }

    public Clip readMessage(JsonReader reader) throws IOException {
        Clip clip = new Clip();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("timestamp")) {
                clip.timestamp = reader.nextString();
            } else if (name.equals("contents")) {
                clip.text = reader.nextString();
            } else if (name.equals("owner_id")) {
                clip.owner_id = reader.nextLong();
            } else if (name.equals("item_pk")) {
                clip.item_pk = reader.nextLong();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return clip;
    }
}
