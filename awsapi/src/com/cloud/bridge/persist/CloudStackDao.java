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
package com.cloud.bridge.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.cloud.bridge.util.ConfigurationHelper;


public class CloudStackDao {
	public static final Logger logger = Logger.getLogger(CloudStackDao.class);

	private Connection conn       = null;
	private String     dbName     = null;
	private String     dbHost     = null;
	private String     dbUser     = null;
	private String     dbPassword = null;
	private String     dbPort     = null; 
	
	public CloudStackDao() {
	    File propertiesFile = ConfigurationHelper.findConfigurationFile("db.properties");
	    Properties EC2Prop = null;
	       
	    if (null != propertiesFile) {
	   	    EC2Prop = new Properties();
	    	try {
				EC2Prop.load( new FileInputStream( propertiesFile ));
			} catch (FileNotFoundException e) {
				logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
			} catch (IOException e) {
				logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
			}
            dbHost     = EC2Prop.getProperty( "db.cloud.host" );
		    dbName     = EC2Prop.getProperty( "db.cloud.name" );
		    dbUser     = EC2Prop.getProperty( "db.cloud.username" );
		    dbPassword = EC2Prop.getProperty( "db.cloud.password" );
		    dbPort     = EC2Prop.getProperty( "db.cloud.port" );
		}
	}


	public String getConfigValue( String configName ){
		String value = null;
		try {
	        openConnection();
		    PreparedStatement statement = conn.prepareStatement ( "SELECT value FROM `cloud`.`configuration` where name = ?" );
		    statement.setString( 1, configName );
		    statement.executeQuery();
		    ResultSet rs = statement.getResultSet ();
		    if (rs.next()) {
		        value = rs.getString(1);
		    }

		}catch (Exception e) {
            logger.warn("Failed to access CloudStack DB, got error: ", e);
        } finally {
            try{
                closeConnection();
            }catch(SQLException e){
                
            }
		}	
		return value;
	}

	private void openConnection() 
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        if (null == conn) {
		    Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
            conn = DriverManager.getConnection( "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName, dbUser, dbPassword );
        }
	}
	
	private void closeConnection() throws SQLException {
		if (null != conn) conn.close();
		conn = null;
	}

}
