/*
 * Copyright 2011 Cloud.com, Inc.
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
package com.cloud.bridge.persist.dao;

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

public class BucketPolicyDao {
	public static final Logger logger = Logger.getLogger(BucketPolicyDao.class);

	private Connection conn       = null;
	private String     dbName     = null;
	private String     dbUser     = null;
	private String     dbPassword = null;
	
	public BucketPolicyDao() 
	{
	    File propertiesFile = ConfigurationHelper.findConfigurationFile("ec2-service.properties");
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
		    dbName     = EC2Prop.getProperty( "dbName" );
		    dbUser     = EC2Prop.getProperty( "dbUser" );
		    dbPassword = EC2Prop.getProperty( "dbPassword" );
		}
	}

	public void addPolicy( String bucketName, String owner, String policy ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;

        openConnection();	
        try {            
            statement = conn.prepareStatement ( "INSERT INTO bucket_policies (BucketName, OwnerCanonicalID, Policy) VALUES (?,?,?)" );
            statement.setString( 1, bucketName );
            statement.setString( 2, owner  );
            statement.setString( 3, policy );
            int count = statement.executeUpdate();
            statement.close();	

        } finally {
            closeConnection();
        }
    }
	
	/**
	 * Since a bucket policy can exist before its bucket we also need to keep the policy's owner
	 * so we can restrict who modifies it (because of the "s3:CreateBucket" action).
	 */
	public String getPolicyOwner( String bucketName )
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;
        String owner = null;

        openConnection();	
        try {            
            statement = conn.prepareStatement ( "SELECT OwnerCanonicalID FROM bucket_policies WHERE BucketName=?" );
            statement.setString( 1, bucketName );
            ResultSet rs = statement.executeQuery();
	        if (rs.next()) owner = rs.getString( "OwnerCanonicalID" );
            statement.close();	
            return owner;

        } finally {
            closeConnection();
        }
    }

	public String getPolicy( String bucketName ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;
        String policy = null;
	
        openConnection();	
        try {            
	        statement = conn.prepareStatement ( "SELECT Policy FROM bucket_policies WHERE BucketName=?" );
            statement.setString( 1, bucketName );
            ResultSet rs = statement.executeQuery();
	        if (rs.next()) policy = rs.getString( "Policy" );
            statement.close();	
            return policy;
    
        } finally {
            closeConnection();
        }
    }

	public void deletePolicy( String bucketName )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;
	
        openConnection();	
        try {
	        statement = conn.prepareStatement ( "DELETE FROM bucket_policies WHERE BucketName=?" );
            statement.setString( 1, bucketName );
            int count = statement.executeUpdate();
            statement.close();	
    
        } finally {
            closeConnection();
        }
    }

	private void openConnection() 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException 
    {
        if (null == conn) {
            Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
            conn = DriverManager.getConnection( "jdbc:mysql://localhost:3306/"+dbName, dbUser, dbPassword );
        }
    }

    private void closeConnection() throws SQLException {
        if (null != conn) conn.close();
        conn = null;
    }
}
