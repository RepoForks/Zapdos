package com.commit451.zapdos;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Actually carries out the drive operations
 */
class Driver {

    public static final String METHOD_CREATE = "CREATE";
    public static final String METHOD_READ = "READ";
    public static final String METHOD_UPDATE = "UPDATE";
    public static final String METHOD_DELETE = "DELETE";

    private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    private GoogleApiClient mGoogleApiClient;

    Driver(GoogleApiClient googleApiClient) {
        mGoogleApiClient = googleApiClient;
    }

    public DriveId write(Request request) throws IOException {
        DriveFolder folder = getDriveFolder(request.uri, true);
        if (folder == null) {
            throw new IOException("Failed to create folder");
        }
        String fileName = request.uri.getLastPathSegment();
        DriveId fileId = findTitledFileInFolder(fileName, request.mimeType, folder);
        DriveContents contents;
        if (fileId == null) {
            contents = Drive.DriveApi.newDriveContents(mGoogleApiClient).await().getDriveContents();
        } else {
            contents = fileId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null)
                    .await()
                    .getDriveContents();
        }
        OutputStream stream = contents.getOutputStream();
        stream.write(request.requestBody.bytes);
        stream.close();
        if (fileId != null) {
            //overwriting existing
            contents.commit(mGoogleApiClient, request.requestBody.metadataChangeSet).await();
        } else {
            //creating new
            fileId = folder.createFile(mGoogleApiClient, request.requestBody.metadataChangeSet, contents)
                    .await()
                    .getDriveFile()
                    .getDriveId();
        }
        return fileId;
    }

    /**
     * Performs a search and gets resources at the targetted uri
     * @param uri the uri
     * @param mimeType the mime type
     * @return the buffer, which you are responsible for closing
     * @throws IOException
     */
    @Nullable
    public MetadataBuffer read(Request request) throws IOException {
        DriveFolder folder = getDriveFolder(request.uri, true);
        if (folder == null) {
            throw new IOException("Could not fetch folder");
        }

        String fileName = request.uri.getLastPathSegment();
        Query driveQuery = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, request.mimeType))
                .addFilter(Filters.contains(SearchableField.TITLE, fileName))
                .build();

        return folder.queryChildren(mGoogleApiClient, driveQuery)
                .await()
                .getMetadataBuffer();
    }

    /**
     * Get the folder
     *
     * @param uri                 a path to the folder you want, such as app://journals/j365
     * @param createIfNotExistent if you want to create all the subfolders if they do not exist along the way
     * @return the folder
     */
    @Nullable
    private DriveFolder getDriveFolder(Uri uri, boolean createIfNotExistent) {
        DriveFolder startFolder;
        if (Request.SCHEME_FILE.equals(uri.getAuthority())) {
            startFolder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        } else if (Request.SCHEME_APP.equals(uri.getAuthority())) {
            startFolder = Drive.DriveApi.getAppFolder(mGoogleApiClient);
        } else {
            //TODO this check should be elsewhere
            throw new IllegalArgumentException("The scheme must be one of `root` or `app`");
        }
        DriveFolder runnerFolder = startFolder;
        for (int i = 0; i < uri.getPathSegments().size(); i++) {
            String path = uri.getPathSegments().get(i);
            Query folderQuery = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, MIME_TYPE_FOLDER))
                    .addFilter(Filters.eq(SearchableField.TITLE, path))
                    .build();
            MetadataBuffer buffer = runnerFolder.queryChildren(mGoogleApiClient, folderQuery)
                    .await()
                    .getMetadataBuffer();
            if (buffer != null && buffer.getCount() > 0) {
                runnerFolder = buffer.get(0).getDriveId().asDriveFolder();
                buffer.release();
            } else {
                if (createIfNotExistent) {
                    for (int j = i; j < uri.getPathSegments().size(); j++) {
                        String title = uri.getPathSegments().get(j);
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(title)
                                .setMimeType(MIME_TYPE_FOLDER)
                                .build();
                        runnerFolder = runnerFolder.createFolder(mGoogleApiClient, changeSet)
                                .await()
                                .getDriveFolder();
                    }
                } else {
                    return null;
                }
                break;
            }
        }
        return runnerFolder;
    }

    @Nullable
    private DriveId findTitledFileInFolder(String title, String mimeType, DriveFolder folder) {
        Query driveQuery = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, title))
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, mimeType))
                .build();

        MetadataBuffer buffer = folder.queryChildren(mGoogleApiClient, driveQuery)
                .await()
                .getMetadataBuffer();
        if (buffer != null && buffer.getCount() > 0) {
            DriveId driveId = buffer.get(0).getDriveId();
            buffer.release();
            return driveId;
        } else {
            if (buffer != null) {
                buffer.release();
            }
        }
        return null;
    }
}
