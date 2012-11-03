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

import java.io.Serializable;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDao;

@Component
public class ComponentLocator {

    public static ComponentLocator getCurrentLocator() {
    	return ComponentContext.getCompanent(ComponentLocator.class);
    }
    
    public static ComponentLocator getLocator(String server) {
    	return ComponentContext.getCompanent(ComponentLocator.class);
    }
    
    public static Object getComponent(String componentName) {
    	return ComponentContext.getCompanent(componentName);
    }
    
    public <T extends GenericDao<?, ? extends Serializable>> T getDao(Class<T> clazz) {
        return ComponentContext.getCompanent(clazz);
    }
    
    public <T> T getManager(Class<T> clazz) {
        return ComponentContext.getCompanent(clazz);
    }
    
    public <T> T getPluggableService(Class<T> clazz) {
        return ComponentContext.getCompanent(clazz);
    }
    
    public static <T> T inject(Class<T> clazz) {
    	return ComponentContext.inject(clazz);
    }
}
