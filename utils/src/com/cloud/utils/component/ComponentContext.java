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

import org.springframework.context.ApplicationContext;

/**
 * 
 * ComponentContext.setApplication() and ComponentContext.getApplication()
 * are not recommended to be used outside, they exist to help wire Spring Framework
 *
 */
public class ComponentContext {
	private static ApplicationContext s_appContext;  

    public static void setApplicationContext(ApplicationContext applicationContext) {  
        s_appContext = applicationContext;  
    }  
  
    public static ApplicationContext getApplicationContext() {  
        return s_appContext;  
    }  
    
    public <T> T getCompanent(String name) {
    	assert(s_appContext != null);
    	return (T)s_appContext.getBean(name);
    }
}
