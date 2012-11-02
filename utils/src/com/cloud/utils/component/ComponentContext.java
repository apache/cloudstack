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

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.TransactionContextBuilder;

/**
 * 
 * ComponentContext.setApplication() and ComponentContext.getApplication()
 * are not recommended to be used outside, they exist to help wire Spring Framework
 *
 */
@Component
public class ComponentContext implements ApplicationContextAware {
	private static ApplicationContext s_appContext;  

    public void setApplicationContext(ApplicationContext applicationContext) {  
        s_appContext = applicationContext;  
    }  
  
    public static ApplicationContext getApplicationContext() {  
        return s_appContext;  
    }  
    
    public static  <T> T getCompanent(String name) {
    	assert(s_appContext != null);
    	return (T)s_appContext.getBean(name);
    }
    
    public static <T> T getCompanent(Class<T> beanType) {
    	assert(s_appContext != null);
    	return (T)s_appContext.getBean(beanType);
    }

    public static<T> T inject(Class<T> clz) {
    	T instance = s_appContext.getAutowireCapableBeanFactory().createBean(clz);
    	return inject(instance);
    }
    
    public static<T> T inject(Object instance) {
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
