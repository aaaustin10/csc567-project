package com.example.austin.clipboardsync;

/**
 * Created by austin on 5/3/15.
 */
public class Clip {
    long owner_id;
    String timestamp;
    String text;
    long item_pk;

    Clip() {
        long owner_id = -1;
        String timestamp = "";
        String text = "";
        long item_pk = -1;
    }

    Clip(String text) {
        this();
        this.text = text;
    }
}
