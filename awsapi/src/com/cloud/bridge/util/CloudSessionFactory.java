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
<<<<<<< HEAD
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
=======
>>>>>>> 6472e7b... Now really adding the renamed files!

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
<<<<<<< HEAD
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.apache.log4j.Logger;

=======
>>>>>>> 6472e7b... Now really adding the renamed files!

/**
 * @author Kelven Yang
 */
public class CloudSessionFactory {
	private static CloudSessionFactory instance;
<<<<<<< HEAD
	public static final Logger logger = Logger.getLogger(CloudSessionFactory.class);
=======
>>>>>>> 6472e7b... Now really adding the renamed files!
	
	private SessionFactory factory;
	
	private CloudSessionFactory() {
		Configuration cfg = new Configuration();
		File file = ConfigurationHelper.findConfigurationFile("hibernate.cfg.xml");
<<<<<<< HEAD

        File propertiesFile = ConfigurationHelper.findConfigurationFile("db.properties");
        Properties dbProp = null;
        String     dbName     = null;
        String     dbHost     = null;
        String     dbUser     = null;
        String     dbPassword = null;
        String     dbPort     = null; 
               
        if (null != propertiesFile) {
            
            if(EncryptionSecretKeyCheckerUtil.useEncryption()){
                StandardPBEStringEncryptor encryptor = EncryptionSecretKeyCheckerUtil.getEncryptor();
                dbProp = new EncryptableProperties(encryptor);
            } else {
                dbProp = new Properties();
            }
            
            try {
                dbProp.load( new FileInputStream( propertiesFile ));
            } catch (FileNotFoundException e) {
                logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
            } catch (IOException e) {
                logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
            }
        }
	        

	    //
=======
		
		//
>>>>>>> 6472e7b... Now really adding the renamed files!
		// we are packaging hibernate mapping files along with the class files, 
    	// make sure class loader use the same class path when initializing hibernate mapping.
		// This is important when we are deploying and testing at different environment (Tomcat/JUnit test runner)
		//
<<<<<<< HEAD
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
=======
    	Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		factory = cfg.configure(file).buildSessionFactory();
>>>>>>> 6472e7b... Now really adding the renamed files!
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
