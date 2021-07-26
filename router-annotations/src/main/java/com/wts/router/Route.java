package com.wts.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Route {

    String[] scheme();

    String[] host();

    String attach();

    int[] position() default {};

    String[] param() default {};

    boolean root() default false;

}
