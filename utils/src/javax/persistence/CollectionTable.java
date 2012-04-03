package javax.persistence;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { METHOD, FIELD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface CollectionTable {
    String catalog() default "";

    JoinColumn[] joinColumns() default {};

    String name() default "";

    String schema() default "";

    UniqueConstraint[] uniqueConstraints() default {};
}
