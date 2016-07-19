package com.commit451.zapdos.drive;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Deletes from Google Drive */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface DELETE {

    /**
     * A path to what you want to get from Google Drive
     */
    String value() default "";
}
