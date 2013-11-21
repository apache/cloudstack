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
package org.apache.cloudstack.spring.lifecycle;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * This class provides a method to do basically the same as @Inject of a type, but
 * it will only find the types in the current context and not the parent.  This class
 * should only be used for very specific Spring bootstrap logic.  In general @Inject
 * is infinitely better.  Basically you need a very good reason to use this.
 *
 */
public abstract class AbstractBeanCollector extends AbstractSmartLifeCycle implements BeanPostProcessor {

    Class<?>[] typeClasses = new Class<?>[] {};
    Map<Class<?>, Set<Object>> beans = new HashMap<Class<?>, Set<Object>>();

    @Override
    public int getPhase() {
        return 2000;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Class<?> typeClass : typeClasses) {
            if (typeClass.isAssignableFrom(bean.getClass())) {
                doPostProcessBeforeInitialization(bean, beanName);
                break;
            }
        }

        return bean;
    }

    protected void doPostProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    }

    protected void doPostProcessAfterInitialization(Object bean, Class<?> typeClass, String beanName) throws BeansException {
        Set<Object> beansOfType = beans.get(typeClass);

        if (beansOfType == null) {
            beansOfType = new HashSet<Object>();
            beans.put(typeClass, beansOfType);
        }

        beansOfType.add(bean);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for (Class<?> typeClass : typeClasses) {
            if (typeClass.isAssignableFrom(bean.getClass())) {
                doPostProcessAfterInitialization(bean, typeClass, beanName);
            }
        }

        return bean;
    }

    protected <T> Set<T> getBeans(Class<T> typeClass) {
        @SuppressWarnings("unchecked")
        Set<T> result = (Set<T>)beans.get(typeClass);

        if (result == null)
            return Collections.emptySet();

        return result;
    }

    public Class<?> getTypeClass() {
        if (typeClasses == null || typeClasses.length == 0)
            return null;

        return typeClasses[0];
    }

    public void setTypeClass(Class<?> typeClass) {
        this.typeClasses = new Class<?>[] {typeClass};
    }

    public Class<?>[] getTypeClasses() {
        return typeClasses;
    }

    public void setTypeClasses(Class<?>[] typeClasses) {
        this.typeClasses = typeClasses;
    }

}
