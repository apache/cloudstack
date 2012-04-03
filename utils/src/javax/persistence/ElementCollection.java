package javax.persistence;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { METHOD, FIELD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ElementCollection {
    FetchType fetch() default FetchType.LAZY;

    Class<?> targetClass() default void.class;
}
