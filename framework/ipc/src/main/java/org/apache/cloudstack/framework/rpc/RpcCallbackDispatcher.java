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
package org.apache.cloudstack.framework.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class RpcCallbackDispatcher<T> {
    private Method _callbackMethod;
    private T _targetObject;

    private RpcCallbackDispatcher(T target) {
        _targetObject = target;
    }

    @SuppressWarnings("unchecked")
    public T getTarget() {
        return (T)Enhancer.create(_targetObject.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
                _callbackMethod = arg1;
                return null;
            }
        });
    }

    public RpcCallbackDispatcher<T> setCallback(Object useless) {
        return this;
    }

    public static <P> RpcCallbackDispatcher<P> create(P target) {
        return new RpcCallbackDispatcher<P>(target);
    }

    public boolean dispatch(RpcClientCall clientCall) {
        assert (clientCall != null);

        if (_callbackMethod == null)
            return false;

        try {
            _callbackMethod.invoke(_targetObject, clientCall, clientCall.getContext());
        } catch (IllegalArgumentException e) {
            throw new RpcException("IllegalArgumentException when invoking RPC callback for command: " + clientCall.getCommand());
        } catch (IllegalAccessException e) {
            throw new RpcException("IllegalAccessException when invoking RPC callback for command: " + clientCall.getCommand());
        } catch (InvocationTargetException e) {
            throw new RpcException("InvocationTargetException when invoking RPC callback for command: " + clientCall.getCommand());
        }

        return true;
    }
}
