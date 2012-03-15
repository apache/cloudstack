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

public class OfferingDao {
	public static final Logger logger = Logger.getLogger(OfferingDao.class);

	private Connection conn       = null;
	private String     dbName     = null;
	private String     dbUser     = null;
	private String     dbPassword = null;
	
	public OfferingDao() 
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
	
	public int getOfferingCount()
		throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
        PreparedStatement statement = null;
        int result = 0;
	
        openConnection();	
        try {            
	        statement = conn.prepareStatement ( "SELECT count(*) FROM offering_bundle" );
            ResultSet rs = statement.executeQuery();
	        if (rs.next()) result = rs.getInt(1);
            statement.close();	
            return result;
        } finally {
            closeConnection();
        }
	}
	
	public String getCloudOffering( String amazonEC2Offering )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
        PreparedStatement statement = null;
        String result = null;
	
        openConnection();	
        try {            
	        statement = conn.prepareStatement ( "SELECT CloudStackOffering FROM offering_bundle WHERE AmazonEC2Offering=?" );
            statement.setString( 1, amazonEC2Offering );
            ResultSet rs = statement.executeQuery();
	        if (rs.next()) result = rs.getString( "CloudStackOffering" );
            statement.close();	
            return result;
    
        } finally {
            closeConnection();
        }
	}
	
	public String getAmazonOffering( String cloudStackOffering )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
        PreparedStatement statement = null;
        String result = null;
	
        openConnection();	
        try {            
	        statement = conn.prepareStatement ( "SELECT AmazonEC2Offering FROM offering_bundle WHERE CloudStackOffering=?" );
            statement.setString( 1, cloudStackOffering );
            ResultSet rs = statement.executeQuery();
	        if (rs.next()) result = rs.getString( "AmazonEC2Offering" );
            statement.close();	
            return result;
    
        } finally {
            closeConnection();
        }		
	}
		
	public void setOfferMapping( String amazonEC2Offering, String cloudStackOffering ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
   {
        PreparedStatement statement = null;
        int id = -1;
        int count = 0;

        openConnection();	
        try {    
            // -> are we doing an update or an insert?  (are we over writing an existing entry?)
		    statement = conn.prepareStatement ( "SELECT ID FROM offering_bundle WHERE AmazonEC2Offering=?" );
            statement.setString( 1, amazonEC2Offering  );
            ResultSet rs = statement.executeQuery();
		    if (rs.next()) id = rs.getInt( "ID" );
            statement.close();			    

            if ( -1 == id )
            {
                 statement = conn.prepareStatement ( "INSERT INTO offering_bundle (AmazonEC2Offering, CloudStackOffering) VALUES (?,?)" );
                 statement.setString( 1, amazonEC2Offering  );
                 statement.setString( 2, cloudStackOffering );
            }
            else
            {    statement = conn.prepareStatement ( "UPDATE offering_bundle SET CloudStackOffering=? WHERE AmazonEC2Offering=?" );
                 statement.setString( 1, cloudStackOffering );
                 statement.setString( 2, amazonEC2Offering  );
            }         
            count = statement.executeUpdate();
            statement.close();	

        } finally {
            closeConnection();
        }
    }
	
	public void deleteOfferMapping( String amazonEC2Offering )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;

        openConnection();	
        try {
            statement = conn.prepareStatement ( "DELETE FROM offering_bundle WHERE AmazonEC2Offering=?" );
            statement.setString( 1, amazonEC2Offering );
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

    private void closeConnection() throws SQLException 
    {
        if (null != conn) conn.close();
        conn = null;
    }
}

