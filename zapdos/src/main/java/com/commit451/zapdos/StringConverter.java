package com.commit451.zapdos;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Easy implementation which turns a {@link MetadataBuffer} into a string
 */
public abstract class StringConverter<T> implements Converter<MetadataBuffer, T> {

    private GoogleApiClient mGoogleApiClient;

    public StringConverter(GoogleApiClient apiClient) {
        mGoogleApiClient = apiClient;
    }

    /**
     * Called to convert a String into an object of type T.
     *
     * @param string The String parsed from JSON.
     */
    public abstract T getFromString(String string);

    @Override
    public T convert(MetadataBuffer value) throws IOException {
        if (value.getCount() > 0) {
            Metadata metadata = value.get(0);

            DriveContents driveContents = metadata.getDriveId().asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                    .await()
                    .getDriveContents();
            if (driveContents == null) {
                return null;
            }
            InputStream inputStream = driveContents.getInputStream();
            String string = Utils.read(inputStream);
            return getFromString(string);
        }
        return null;
    }
}
