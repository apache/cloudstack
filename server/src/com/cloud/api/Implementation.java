package com.cloud.api;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cloud.server.ManagementServer;

@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
public @interface Implementation {
//    String createMethod() default "";
//    String method() default "";
//    Class<?> manager() default ManagementServer.class;
    Class<?> responseObject();
    String description() default "";
}
