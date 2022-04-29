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

    Class<?> attach() default Void.class;

    int[] position() default {};

    String[] param() default {};

    String[] paramKey() default {};

    Class<?>[] paramTyped() default {};

    boolean root() default false;

}
