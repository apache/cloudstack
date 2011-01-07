/**
 * 
 */
package com.cloud.utils.component;

import java.lang.reflect.AnnotatedElement;

import net.sf.cglib.proxy.Callback;

/**
 *  AnnotationIntercepter says it can intercept an annotation.
 */
public interface AnnotationInterceptor<T> {
    boolean needToIntercept(AnnotatedElement element);
    
    T interceptStart(AnnotatedElement element);
    
    void interceptComplete(AnnotatedElement element, T attach);
    
    void interceptException(AnnotatedElement element, T attach);
    
    Callback getCallback();
}
