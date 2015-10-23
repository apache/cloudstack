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

package com.cloud.utils.component;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

import com.cloud.utils.Pair;

public class ComponentInstantiationPostProcessor implements InstantiationAwareBeanPostProcessor {
    private static final Logger s_logger = Logger.getLogger(ComponentInstantiationPostProcessor.class);

    private List<ComponentMethodInterceptor> _interceptors = new ArrayList<ComponentMethodInterceptor>();
    private Callback[] _callbacks;
    private CallbackFilter _callbackFilter;

    public ComponentInstantiationPostProcessor() {
        _callbacks = new Callback[2];
        _callbacks[0] = NoOp.INSTANCE;
        _callbacks[1] = new InterceptorDispatcher();

        _callbackFilter = new InterceptorFilter();
    }

    public List<ComponentMethodInterceptor> getInterceptors() {
        return _interceptors;
    }

    public void setInterceptors(List<ComponentMethodInterceptor> interceptors) {
        _interceptors = interceptors;
    }

    private Callback[] getCallbacks() {
        return _callbacks;
    }

    private CallbackFilter getCallbackFilter() {
        return _callbackFilter;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (_interceptors != null && _interceptors.size() > 0) {
            if (ComponentMethodInterceptable.class.isAssignableFrom(beanClass)) {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(beanClass);
                enhancer.setCallbacks(getCallbacks());
                enhancer.setCallbackFilter(getCallbackFilter());
                enhancer.setNamingPolicy(ComponentNamingPolicy.INSTANCE);

                Object bean = enhancer.create();
                return bean;
            }
        }
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        return pvs;
    }

    protected class InterceptorDispatcher implements MethodInterceptor {
        @Override
        public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            ArrayList<Pair<ComponentMethodInterceptor, Object>> interceptors = new ArrayList<Pair<ComponentMethodInterceptor, Object>>();

            for (ComponentMethodInterceptor interceptor : getInterceptors()) {
                if (interceptor.needToIntercept(method)) {
                    Object objReturnedInInterceptStart = interceptor.interceptStart(method, target);
                    interceptors.add(new Pair<ComponentMethodInterceptor, Object>(interceptor, objReturnedInInterceptStart));
                }
            }
            boolean success = false;
            try {
                Object obj = methodProxy.invokeSuper(target, args);
                success = true;
                return obj;
            } finally {
                for (Pair<ComponentMethodInterceptor, Object> interceptor : interceptors) {
                    if (success) {
                        interceptor.first().interceptComplete(method, target, interceptor.second());
                    } else {
                        interceptor.first().interceptException(method, target, interceptor.second());
                    }
                }
            }
        }
    }

    protected class InterceptorFilter implements CallbackFilter {
        @Override
        public int accept(Method method) {
            for (ComponentMethodInterceptor interceptor : getInterceptors()) {

                if (interceptor.needToIntercept(method)) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
