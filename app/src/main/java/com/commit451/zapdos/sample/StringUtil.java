package com.commit451.zapdos.sample;

import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Stupid string utils that should be somewhere cool
 */
public class StringUtil {

    @Nullable
    public static String read(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        } catch (Exception e) {
            // :(
            return null;
        }
    }
}
