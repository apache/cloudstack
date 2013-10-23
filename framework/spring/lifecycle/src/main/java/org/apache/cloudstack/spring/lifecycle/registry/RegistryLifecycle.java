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
package org.apache.cloudstack.spring.lifecycle.registry;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

import com.cloud.utils.component.Registry;

public class RegistryLifecycle implements BeanPostProcessor, SmartLifecycle, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(RegistryLifecycle.class);
    
    Registry<Object> registry;
    
    /* The bean name works around circular dependency issues in Spring.  This shouldn't be
     * needed if your beans are already nicely organized.  If they look like spaghetti, then you
     * can use this.
     */
    String registryBeanName;
    Set<Object> beans = new HashSet<Object>();
    Class<?> typeClass;
    ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ( typeClass.isAssignableFrom(bean.getClass()) )
            beans.add(bean);
        
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void start() {
        Iterator<Object> iter = beans.iterator();
        Registry<Object> registry = lookupRegistry();
        
        while ( iter.hasNext() ) {
            Object next = iter.next();
            if ( registry.register(next) ) {
                log.debug("Registered {}", next);
            } else {
                iter.remove();
            }
        }
    }

    @Override
    public void stop() {
        Registry<Object> registry = lookupRegistry();
        
        for ( Object bean : beans ) {
            registry.unregister(bean);
        }
        
        beans.clear();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return 2000;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @SuppressWarnings("unchecked")
    protected Registry<Object> lookupRegistry() {
        return registry == null ? applicationContext.getBean(registryBeanName, Registry.class) : registry;
    }

    public Registry<Object> getRegistry() {
        return registry;
    }

    public void setRegistry(Registry<Object> registry) {
        this.registry = registry;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public String getRegistryBeanName() {
        return registryBeanName;
    }

    public void setRegistryBeanName(String registryBeanName) {
        this.registryBeanName = registryBeanName;
    }

}
