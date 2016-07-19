package com.commit451.zapdos.sample;

import com.commit451.zapdos.Converter;
import com.commit451.zapdos.RequestBody;
import com.commit451.zapdos.Zapdos;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.MetadataBuffer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Converts messages to and from Google Drive
 */
public class CustomConverterFactory extends Converter.Factory{

    public static CustomConverterFactory create(GoogleApiClient googleApiClient) {
        CustomConverterFactory customConverterFactory = new CustomConverterFactory();
        customConverterFactory.mGoogleApiClient = googleApiClient;
        return customConverterFactory;
    }

    private GoogleApiClient mGoogleApiClient;

    @Override
    public Converter<MetadataBuffer, ?> responseBodyConverter(Type type, Annotation[] annotations, Zapdos retrofit) {
        if (type == Message.class) {
            return new MessageReadConverter(mGoogleApiClient);
        }
        return super.responseBodyConverter(type, annotations, retrofit);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Zapdos zapdos) {
        if (type == Message.class) {
            return new MessageWriteConverter(mGoogleApiClient);
        }
        return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, zapdos);
    }
}
