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
package com.cloud.bridge.persist.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

import com.cloud.bridge.util.ConfigurationHelper;
import com.cloud.bridge.util.EncryptionSecretKeyCheckerUtil;



public class BaseDao {
	public static final Logger logger = Logger.getLogger(BaseDao.class);

	protected static String     cloud_dbName     = null;
	protected static String     dbHost     = null;
	protected static String     dbUser     = null;
	protected static String     dbPassword = null;
	protected static String     dbPort     = null;
	protected static String     awsapi_dbName     = null;
	
	static{
	    logger.info("Initializing DB props");
        File propertiesFile = ConfigurationHelper.findConfigurationFile("db.properties");
        Properties EC2Prop = null;
           
        if (null != propertiesFile) {
            if(EncryptionSecretKeyCheckerUtil.useEncryption()){
                StandardPBEStringEncryptor encryptor = EncryptionSecretKeyCheckerUtil.getEncryptor();
                EC2Prop = new EncryptableProperties(encryptor);
            } else {
                EC2Prop = new Properties();
            }

            try {
                EC2Prop.load( new FileInputStream( propertiesFile ));
            } catch (FileNotFoundException e) {
                logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
            } catch (IOException e) {
                logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
            }
            dbHost     = EC2Prop.getProperty( "db.cloud.host" );
            awsapi_dbName     = EC2Prop.getProperty( "db.awsapi.name" );
            cloud_dbName     = EC2Prop.getProperty( "db.cloud.name" );
            dbUser     = EC2Prop.getProperty( "db.cloud.username" );
            dbPassword = EC2Prop.getProperty( "db.cloud.password" );
            dbPort     = EC2Prop.getProperty( "db.cloud.port" );
        }
	}
	
	public BaseDao() {
	}

}
