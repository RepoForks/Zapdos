package com.commit451.zapdos;

import com.google.android.gms.drive.MetadataChangeSet;

/**
 * The things you need to fill drive contents and create a file
 */
public class RequestBody {

    public byte[] bytes;
    public MetadataChangeSet metadataChangeSet;

    public static RequestBody create(MetadataChangeSet metadataChangeSet, byte[] bytes) {
        return new RequestBody(metadataChangeSet, bytes);
    }

    private RequestBody(MetadataChangeSet metadataChangeSet, byte[] bytes) {
        this.bytes = bytes;
        this.metadataChangeSet = metadataChangeSet;
    }
}
