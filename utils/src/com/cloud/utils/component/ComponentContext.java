// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.utils.component;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.TransactionContextBuilder;

/**
 * 
 * ComponentContext.setApplication() and ComponentContext.getApplication()
 * are not recommended to be used outside, they exist to help wire Spring Framework
 *
 */
@Component
public class ComponentContext implements ApplicationContextAware {
    private static final Logger s_logger = Logger.getLogger(ComponentContext.class);

	private static ApplicationContext s_appContext;  

    public void setApplicationContext(ApplicationContext applicationContext) {  
        s_appContext = applicationContext;  
    }  
  
    public static ApplicationContext getApplicationContext() {  
        return s_appContext;  
    }  
    
    public static <T> T getComponent(String name) {
    	assert(s_appContext != null);
    	return (T)s_appContext.getBean(name);
    }
    
    public static <T> T getComponent(Class<T> beanType) {
    	assert(s_appContext != null);
    	try {
    		return (T)s_appContext.getBean(beanType);
    	} catch(NoSuchBeanDefinitionException e) {
    		Map<String, T> matchedTypes = getComponentsOfType(beanType);
    		if(matchedTypes.size() > 0) {
	    		for(Map.Entry<String, T> entry : matchedTypes.entrySet()) {
	    			Primary primary = getTargetClass(entry).getAnnotation(Primary.class);
	    			if(primary != null)
	    				return entry.getValue();
	    		}
	    		
	    		s_logger.warn("Unable to uniquely locate bean type " + beanType.getName());
	    		return (T)matchedTypes.values().toArray()[0];
	    	}
    	}
    	throw new NoSuchBeanDefinitionException("Unable to resolve bean type " + beanType.getName());
    }
    
    public static <T> Map<String, T> getComponentsOfType(Class<T> beanType) {
    	return s_appContext.getBeansOfType(beanType);
    }
    
    public static <T> boolean isPrimary(Object instance, Class<T> beanType) {
    	
    	// we assume single line of interface inheritance of beanType
    	Class<?> componentType = beanType;
    	Class<?> targetClass = getTargetClass(instance);
    	Class<?> interfaces[] = targetClass.getInterfaces();
    	for(Class<?> intf : interfaces)  {
    		if(beanType.isAssignableFrom(intf)) {
    			componentType = intf;
    			break;
    		}
    	}
    	
		Map<String, T> matchedTypes = (Map<String, T>)ComponentContext.getComponentsOfType(componentType);
		if(matchedTypes.size() > 1) {
			Primary primary = targetClass.getAnnotation(Primary.class);
			if(primary != null)
				return true;
			
			return false;
    	}
    	
    	return true;
    }
     
    public static Class<?> getTargetClass(Object instance) {
	    if(instance instanceof Advised) {
	    	try {
	    		return ((Advised)instance).getTargetSource().getTarget().getClass();
	    	} catch(Exception e) {
	    		return instance.getClass();
	    	}
	    }
	    return instance.getClass();
    }
    
    public static <T> T inject(Class<T> clz) {
    	T instance = s_appContext.getAutowireCapableBeanFactory().createBean(clz);
    	return inject(instance);
    }
    
    public static <T> T inject(Object instance) {
    	// autowire dynamically loaded object
    	AutowireCapableBeanFactory  beanFactory = s_appContext.getAutowireCapableBeanFactory();
    	beanFactory.autowireBean(instance);

    	Advisor advisor = new DefaultPointcutAdvisor(new MatchAnyMethodPointcut(),
    			new TransactionContextBuilder());
    
    	ProxyFactory pf = new ProxyFactory();
        pf.setTarget(instance);
        pf.addAdvisor(advisor);
        
        return (T)pf.getProxy();        
    }
}
