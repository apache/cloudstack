/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.framework.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@SuppressWarnings("rawtypes")
public class AsyncCallbackDispatcher<T, R> implements AsyncCompletionCallback {
    protected Logger logger = LogManager.getLogger(getClass());

    private Method _callbackMethod;
    private final T _targetObject;
    private Object _contextObject;
    private Object _resultObject;
    private AsyncCallbackDriver _driver = new InplaceAsyncCallbackDriver();
    private static Map<Class, Enhancer> enMap = new HashMap<Class, Enhancer>();

    private AsyncCallbackDispatcher(T target) {
        assert (target != null);
        _targetObject = target;
    }

    public AsyncCallbackDispatcher<T, R> attachDriver(AsyncCallbackDriver driver) {
        assert (driver != null);
        _driver = driver;

        return this;
    }

    public Method getCallbackMethod() {
        return _callbackMethod;
    }

    @SuppressWarnings("unchecked")
    public T getTarget() {
        Class<?> clz = _targetObject.getClass();
        String clzName = clz.getName();
        if (clzName.contains("EnhancerByCloudStack"))
            clz = clz.getSuperclass();


        Enhancer en = null;
        synchronized (enMap) {
            en = enMap.get(clz);
            if (en == null) {
                en = new Enhancer();

                en.setSuperclass(clz);
                en.setCallback(new MethodInterceptor() {
                    @Override
                    public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
                        return null;
                        }
                    });
                enMap.put(clz, en);
            }
        }

        try {
            T t = (T)en.create();
            Factory factory = (Factory)t;
            factory.setCallback(0, new MethodInterceptor() {
                @Override
                public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
                    if (arg1.getParameterTypes().length == 0 && arg1.getName().equals("finalize")) {
                        return null;
                    } else {
                        _callbackMethod = arg1;
                        _callbackMethod.setAccessible(true);
                        return null;
                    }
                }
            });
            return t;
        } catch (Throwable e) {
            logger.error("Unexpected exception", e);
        }

        return null;
    }

    public AsyncCallbackDispatcher<T, R> setCallback(Object useless) {
        return this;
    }

    public AsyncCallbackDispatcher<T, R> setContext(Object context) {
        _contextObject = context;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <P> P getContext() {
        return (P)_contextObject;
    }

    @Override
    public void complete(Object resultObject) {
        _resultObject = resultObject;
        _driver.performCompletionCallback(this);
    }

    @SuppressWarnings("unchecked")
    public R getResult() {
        return (R)_resultObject;
    }

    // for internal use
    Object getTargetObject() {
        return _targetObject;
    }

    public static <P, R> AsyncCallbackDispatcher<P, R> create(P target) {
        return new AsyncCallbackDispatcher<P, R>(target);
    }

    public static boolean dispatch(Object target, AsyncCallbackDispatcher callback) {
        assert (callback != null);
        assert (target != null);

        try {
            callback.getCallbackMethod().invoke(target, callback, callback.getContext());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("IllegalArgumentException when invoking RPC callback for command: " + callback.getCallbackMethod().getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("IllegalAccessException when invoking RPC callback for command: " + callback.getCallbackMethod().getName());
        } catch (InvocationTargetException e) {
            throw new RuntimeException("InvocationTargetException when invoking RPC callback for command: " + callback.getCallbackMethod().getName(), e);
        }

        return true;
    }
}
