//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/*
 * This helper class provides a way to retrieve Method in a strong-type way. It takes advantage of power of
 * Intelligent IDE(Eclipse) in code-editing
 *
 * DummyImpl dummy = new DummyImpl();
 * MethodCapturer<DummyImpl> capturer = MethodCapturer.capture(dummy);
 * Method method = capturer.get(capturer.instance().foo2());
 *
 */
public class MethodCapturer<T> {

    private final static int CACHE_SIZE = 1024;

    private T _instance;
    private Method _method;

    private static WeakHashMap<Object, Object> s_cache = new WeakHashMap<Object, Object>();

    private MethodCapturer() {
    }

    @SuppressWarnings("unchecked")
    public static <T> MethodCapturer<T> capture(T obj) {
        synchronized (s_cache) {
            MethodCapturer<T> capturer = (MethodCapturer<T>)s_cache.get(obj);
            if (capturer != null) {
                return capturer;
            }

            final MethodCapturer<T> capturerNew = new MethodCapturer<T>();

            Enhancer en = new Enhancer();
            en.setSuperclass(obj.getClass());
            en.setCallbacks(new Callback[] {new MethodInterceptor() {
                @Override
                public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
                    capturerNew.setMethod(arg1);
                    return null;
                }
            }, new MethodInterceptor() {
                @Override
                public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
                    return null;
                }
            }});
            en.setCallbackFilter(new CallbackFilter() {
                @Override
                public int accept(Method method) {
                    if (method.getParameterTypes().length == 0 && method.getName().equals("finalize")) {
                        return 1;
                    }
                    return 0;
                }
            });

            capturerNew.setInstance((T)en.create());

            // We expect MethodCapturer is only used for singleton objects here, so we only maintain a limited cache
            // here
            if (s_cache.size() < CACHE_SIZE) {
                s_cache.put(obj, capturerNew);
            }

            return capturerNew;
        }
    }

    public T instance() {
        return _instance;
    }

    private void setInstance(T instance) {
        _instance = instance;
    }

    public Method get(Object... useless) {
        return _method;
    }

    private void setMethod(Method method) {
        _method = method;
    }
}
