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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.apache.log4j.Logger;

/**
 * @author Kelven Yang
 */
public class CloudSessionFactory {
	private static CloudSessionFactory instance;
	public static final Logger logger = Logger.getLogger(CloudSessionFactory.class);
	
	private SessionFactory factory;
	
	private CloudSessionFactory() {
		Configuration cfg = new Configuration();
		File file = ConfigurationHelper.findConfigurationFile("hibernate.cfg.xml");

        File propertiesFile = ConfigurationHelper.findConfigurationFile("db.properties");
        Properties dbProp = null;
        String     dbName     = null;
        String     dbHost     = null;
        String     dbUser     = null;
        String     dbPassword = null;
        String     dbPort     = null; 
               
        if (null != propertiesFile) {
            dbProp = new Properties();
            try {
                dbProp.load( new FileInputStream( propertiesFile ));
            } catch (FileNotFoundException e) {
                logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
            } catch (IOException e) {
                logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
            }
        }
	        

	    //
		// we are packaging hibernate mapping files along with the class files, 
    	// make sure class loader use the same class path when initializing hibernate mapping.
		// This is important when we are deploying and testing at different environment (Tomcat/JUnit test runner)
		//
        if(file != null && dbProp != null){
        	Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        	cfg.configure(file);
        	
            dbHost     = dbProp.getProperty( "db.cloud.host" );
            dbName     = dbProp.getProperty( "db.awsapi.name" );
            dbUser     = dbProp.getProperty( "db.cloud.username" );
            dbPassword = dbProp.getProperty( "db.cloud.password" );
            dbPort     = dbProp.getProperty( "db.cloud.port" );

            cfg.setProperty("hibernate.connection.url", "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName);
        	cfg.setProperty("hibernate.connection.username", dbUser);
        	cfg.setProperty("hibernate.connection.password", dbPassword);
        	
        	
    		factory = cfg.buildSessionFactory();
        }else{
            logger.warn("Unable to open load db configuration");
            throw new RuntimeException("nable to open load db configuration");
        }
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
