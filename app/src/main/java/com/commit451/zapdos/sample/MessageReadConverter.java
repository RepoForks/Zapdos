package com.commit451.zapdos.sample;

import com.commit451.zapdos.Converter;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Converter
 */
public class MessageReadConverter implements Converter<MetadataBuffer, Message> {

    private GoogleApiClient mGoogleApiClient;

    public MessageReadConverter(GoogleApiClient googleApiClient) {
        mGoogleApiClient = googleApiClient;
    }

    @Override
    public Message convert(MetadataBuffer value) throws IOException {
        if (value.getCount() > 0) {
            Metadata metadata = value.get(0);
            DriveContents driveContents = metadata.getDriveId().asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                    .await()
                    .getDriveContents();
            if (driveContents == null) {
                return null;
            }
            InputStream inputStream = driveContents.getInputStream();
            String content = StringUtil.read(inputStream);
            return new Message(content);
        }
        return null;
    }
}
