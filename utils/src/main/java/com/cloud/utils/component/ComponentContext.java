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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;

import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.mgmt.ManagementBean;

/**
 *
 * ComponentContext.setApplication() and ComponentContext.getApplication()
 * are not recommended to be used outside, they exist to help wire Spring Framework
 *
 */
@SuppressWarnings("unchecked")
public class ComponentContext implements ApplicationContextAware {
    private static final Logger s_logger = Logger.getLogger(ComponentContext.class);

    private static ApplicationContext s_appContext;
    private static Map<Class<?>, ApplicationContext> s_appContextDelegates;
    private static boolean s_initializeBeans = true;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        s_logger.info("Setup Spring Application context");
        s_appContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return s_appContext;
    }

    public static void initComponentsLifeCycle() {
        if (!s_initializeBeans)
            return;

        AutowireCapableBeanFactory beanFactory = s_appContext.getAutowireCapableBeanFactory();

        Map<String, ComponentMethodInterceptable> interceptableComponents = getApplicationContext().getBeansOfType(ComponentMethodInterceptable.class);
        for (Map.Entry<String, ComponentMethodInterceptable> entry : interceptableComponents.entrySet()) {
            Object bean = getTargetObject(entry.getValue());
            beanFactory.configureBean(bean, entry.getKey());
        }

        Map<String, ComponentLifecycle> lifecycleComponents = getApplicationContext().getBeansOfType(ComponentLifecycle.class);

        Map<String, ComponentLifecycle>[] classifiedComponents = new Map[ComponentLifecycle.MAX_RUN_LEVELS];
        for (int i = 0; i < ComponentLifecycle.MAX_RUN_LEVELS; i++) {
            classifiedComponents[i] = new HashMap<String, ComponentLifecycle>();
        }

        for (Map.Entry<String, ComponentLifecycle> entry : lifecycleComponents.entrySet()) {
            classifiedComponents[entry.getValue().getRunLevel()].put(entry.getKey(), entry.getValue());
        }

        // Run the SystemIntegrityCheckers first
        Map<String, SystemIntegrityChecker> integrityCheckers = getApplicationContext().getBeansOfType(SystemIntegrityChecker.class);
        for (Entry<String, SystemIntegrityChecker> entry : integrityCheckers.entrySet()) {
            s_logger.info("Running SystemIntegrityChecker " + entry.getKey());
            try {
                entry.getValue().check();
            } catch (Throwable e) {
                s_logger.error("System integrity check failed. Refuse to startup", e);
                System.exit(1);
            }
        }

        // configuration phase
        Map<String, String> avoidMap = new HashMap<String, String>();
        for (int i = 0; i < ComponentLifecycle.MAX_RUN_LEVELS; i++) {
            for (Map.Entry<String, ComponentLifecycle> entry : classifiedComponents[i].entrySet()) {
                ComponentLifecycle component = entry.getValue();
                String implClassName = ComponentContext.getTargetClass(component).getName();
                s_logger.info("Configuring " + implClassName);

                if (avoidMap.containsKey(implClassName)) {
                    s_logger.info("Skip configuration of " + implClassName + " as it is already configured");
                    continue;
                }

                try {
                    component.configure(component.getName(), component.getConfigParams());
                } catch (ConfigurationException e) {
                    s_logger.error("Unhandled exception", e);
                    throw new RuntimeException("Unable to configure " + implClassName, e);
                }

                avoidMap.put(implClassName, implClassName);
            }
        }

        // starting phase
        avoidMap.clear();
        for (int i = 0; i < ComponentLifecycle.MAX_RUN_LEVELS; i++) {
            for (Map.Entry<String, ComponentLifecycle> entry : classifiedComponents[i].entrySet()) {
                ComponentLifecycle component = entry.getValue();
                String implClassName = ComponentContext.getTargetClass(component).getName();
                s_logger.info("Starting " + implClassName);

                if (avoidMap.containsKey(implClassName)) {
                    s_logger.info("Skip configuration of " + implClassName + " as it is already configured");
                    continue;
                }

                try {
                    component.start();

                    if (getTargetObject(component) instanceof ManagementBean)
                        registerMBean((ManagementBean)getTargetObject(component));
                } catch (Exception e) {
                    s_logger.error("Unhandled exception", e);
                    throw new RuntimeException("Unable to start " + implClassName, e);
                }

                avoidMap.put(implClassName, implClassName);
            }
        }
    }

    static void registerMBean(ManagementBean mbean) {
        try {
            JmxUtil.registerMBean(mbean);
        } catch (MalformedObjectNameException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        } catch (InstanceAlreadyExistsException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        } catch (MBeanRegistrationException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        } catch (NotCompliantMBeanException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        }
        s_logger.info("Registered MBean: " + mbean.getName());
    }

    public static <T> T getComponent(String name) {
        assert (s_appContext != null);
        return (T)s_appContext.getBean(name);
    }

    public static <T> T getComponent(Class<T> beanType) {
        assert (s_appContext != null);
        Map<String, T> matchedTypes = getComponentsOfType(beanType);
        if (matchedTypes.size() > 0) {
            for (Map.Entry<String, T> entry : matchedTypes.entrySet()) {
                Primary primary = getTargetClass(entry.getValue()).getAnnotation(Primary.class);
                if (primary != null)
                    return entry.getValue();
            }

            if (matchedTypes.size() > 1) {
                s_logger.warn("Unable to uniquely locate bean type " + beanType.getName());
                for (Map.Entry<String, T> entry : matchedTypes.entrySet()) {
                    s_logger.warn("Candidate " + getTargetClass(entry.getValue()).getName());
                }
            }

            return (T)matchedTypes.values().toArray()[0];
        }

        throw new NoSuchBeanDefinitionException(beanType.getName());
    }

    public static <T> Map<String, T> getComponentsOfType(Class<T> beanType) {
        return s_appContext.getBeansOfType(beanType);
    }

    public static Class<?> getTargetClass(Object instance) {
        while (instance instanceof Advised) {
            try {
                instance = ((Advised)instance).getTargetSource().getTarget();
            } catch (Exception e) {
                return instance.getClass();
            }
        }
        return instance.getClass();
    }

    public static <T> T getTargetObject(Object instance) {
        while (instance instanceof Advised) {
            try {
                instance = ((Advised)instance).getTargetSource().getTarget();
            } catch (Exception e) {
                return (T)instance;
            }
        }

        return (T)instance;
    }

    public static <T> T inject(Class<T> clz) {
        T instance;
        try {
            instance = clz.newInstance();
            return inject(instance);
        } catch (InstantiationException e) {
            s_logger.error("Unhandled InstantiationException", e);
            throw new RuntimeException("Unable to instantiate object of class " + clz.getName() + ", make sure it has public constructor");
        } catch (IllegalAccessException e) {
            s_logger.error("Unhandled IllegalAccessException", e);
            throw new RuntimeException("Unable to instantiate object of class " + clz.getName() + ", make sure it has public constructor");
        }
    }

    public static <T> T inject(Object instance) {
        // autowire dynamically loaded object
        AutowireCapableBeanFactory beanFactory = getApplicationContext(instance).getAutowireCapableBeanFactory();
        beanFactory.autowireBean(instance);
        return (T)instance;
    }

    private static ApplicationContext getApplicationContext(Object instance) {
        ApplicationContext result = null;

        synchronized (s_appContextDelegates) {
            if (instance != null && s_appContextDelegates != null) {
                result = s_appContextDelegates.get(instance.getClass());
            }
        }

        return result == null ? s_appContext : result;
    }

    public static synchronized void addDelegateContext(Class<?> clazz, ApplicationContext context) {
        if (s_appContextDelegates == null) {
            s_appContextDelegates = new HashMap<Class<?>, ApplicationContext>();
        }

        s_appContextDelegates.put(clazz, context);
    }

    public static synchronized void removeDelegateContext(Class<?> clazz) {
        if (s_appContextDelegates != null) {
            s_appContextDelegates.remove(clazz);
        }
    }

    public boolean isInitializeBeans() {
        return s_initializeBeans;
    }

    public void setInitializeBeans(boolean initializeBeans) {
        initInitializeBeans(initializeBeans);
    }

    private static synchronized void initInitializeBeans(boolean initializeBeans) {
        s_initializeBeans = initializeBeans;
    }
}
