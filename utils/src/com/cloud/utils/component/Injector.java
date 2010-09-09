/**
 * 
 */
package com.cloud.utils.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 *  Injector implements customized Injectors for ComponentLocator.
 *
 */
public interface Injector {
    /**
     * Can this injector handle injecting into this type of class?
     */
    boolean canInject(AnnotatedElement element, Annotation ann);

}
