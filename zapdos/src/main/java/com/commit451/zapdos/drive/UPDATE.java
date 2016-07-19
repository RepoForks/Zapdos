package com.commit451.zapdos.drive;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Updates an item within Google Drive */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface UPDATE {

    /**
     * The path to write to within Google Drive
     */
    String value() default "";
}
