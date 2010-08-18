package com.cloud.api;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cloud.api.BaseCmd.Manager;

@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
public @interface Implementation {
    String createMethod() default "";
    String method() default "";
    Manager manager() default Manager.ManagementServer;
}
