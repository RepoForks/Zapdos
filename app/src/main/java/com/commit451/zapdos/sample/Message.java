package com.commit451.zapdos.sample;

import com.google.gson.annotations.SerializedName;

/**
 * A model which can serialize and deserialize to and from json
 */
public class Message {

    @SerializedName("moment")
    long moment;
    @SerializedName("text")
    String text;

    public Message(String text) {
        this.text = text;
        this.moment = System.currentTimeMillis();
    }
}
