package com.commit451.zapdos.sample;

import com.commit451.zapdos.Converter;
import com.commit451.zapdos.RequestBody;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.gson.Gson;

import java.io.IOException;

/**
 * Converter
 */
public class MessageWriteConverter implements Converter<Message, RequestBody> {

    private GoogleApiClient mGoogleApiClient;

    public MessageWriteConverter(GoogleApiClient googleApiClient) {
        mGoogleApiClient = googleApiClient;
    }

    @Override
    public RequestBody convert(Message value) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(value);
        return RequestBody.create(createMetadataChangeSet(), json.getBytes());
    }

    private MetadataChangeSet createMetadataChangeSet() {
        return new MetadataChangeSet.Builder()
                //.setTitle(title)
                .build();
    }
}
