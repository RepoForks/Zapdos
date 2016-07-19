package com.commit451.zapdos;

import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Google Drive request
 */
public class Request {

    public static String SCHEME_FILE = "file";
    public static String SCHEME_APP = "app";

    Uri uri;
    RequestBody requestBody;
    //TODO make this dynamic
    String mimeType = "text/plain";
    //CREATE, update, delete, etc
    String method;

    public Request(Uri uri, String method, @Nullable RequestBody requestBody) {
        this.uri = uri;
        this.requestBody = requestBody;
        this.method = method;
    }
}
