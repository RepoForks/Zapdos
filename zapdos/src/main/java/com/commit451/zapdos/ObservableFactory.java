package com.commit451.zapdos;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataBuffer;

import java.io.IOException;

import rx.Observable;
import rx.functions.Func0;

/**
 * Creates {@link rx.Observable}s from {@link ServiceMethod}s
 */
public class ObservableFactory {

    public static Observable<DriveId> create(final Driver driver, final Request request) {
        return Observable.defer(new Func0<Observable<DriveId>>() {
            @Override
            public Observable<DriveId> call() {
                try {
                    return Observable.just(driver.write(request));
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public static <T> Observable<T> read(final Driver driver, final Request request, final ServiceMethod<T> serviceMethod) {
        return Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                try {
                    return Observable.just(blah(driver, serviceMethod, request));
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    private static <T> T blah(final Driver driver, ServiceMethod<T> serviceMethod, final Request request) throws IOException {
        MetadataBuffer buffer = driver.read(request);
        if (buffer == null) {
            throw new IOException("Buffer was null");
        }
        T response = serviceMethod.toResponse(buffer);
        buffer.release();
        return response;
    }
}
