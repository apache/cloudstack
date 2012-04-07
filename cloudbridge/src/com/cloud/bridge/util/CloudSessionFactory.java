/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.util;

import java.io.File;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * @author Kelven Yang
 */
public class CloudSessionFactory {
	private static CloudSessionFactory instance;
	
	private SessionFactory factory;
	
	private CloudSessionFactory() {
		Configuration cfg = new Configuration();
		File file = ConfigurationHelper.findConfigurationFile("hibernate.cfg.xml");
		
		//
		// we are packaging hibernate mapping files along with the class files, 
    	// make sure class loader use the same class path when initializing hibernate mapping.
		// This is important when we are deploying and testing at different environment (Tomcat/JUnit test runner)
		//
    	Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		factory = cfg.configure(file).buildSessionFactory();
	}
	
	public synchronized static CloudSessionFactory getInstance() {
		if(instance == null) {
			instance = new CloudSessionFactory();
		}
		return instance;
	}
	
	public Session openSession() {
		return factory.openSession();
	}
}
