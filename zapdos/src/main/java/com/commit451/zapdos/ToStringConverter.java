package com.commit451.zapdos;

public class ToStringConverter implements Converter<Object, String> {

    static final ToStringConverter INSTANCE = new ToStringConverter();

    @Override
    public String convert(Object value) {
        return value.toString();
    }
}
