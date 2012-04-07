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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.core.s3.S3MetaDataEntry;
import com.cloud.bridge.service.core.s3.S3MultipartPart;
import com.cloud.bridge.service.core.s3.S3MultipartUpload;
import com.cloud.bridge.util.ConfigurationHelper;
import com.cloud.bridge.util.Tuple;

public class MultipartLoadDao {
	public static final Logger logger = Logger.getLogger(MultipartLoadDao.class);

	private Connection conn       = null;
	private String     dbName     = null;
	private String     dbUser     = null;
	private String     dbPassword = null;
	
	public MultipartLoadDao() {
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
	
	/**
	 * If a multipart upload exists with the uploadId value then return the non-null creators
	 * accessKey.
	 * 
	 * @param uploadId
	 * @return creator of the multipart upload, and NameKey of upload
	 * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException 
	 */
	public Tuple<String,String> multipartExits( int uploadId ) 
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
	    PreparedStatement statement = null;
	    String accessKey = null;
	    String nameKey = null;
		
        openConnection();	
        try {            
		    statement = conn.prepareStatement ( "SELECT AccessKey, NameKey FROM multipart_uploads WHERE ID=?" );
	        statement.setInt( 1, uploadId );
	        ResultSet rs = statement.executeQuery();
		    if ( rs.next()) {
		    	 accessKey = rs.getString( "AccessKey" );
		    	 nameKey = rs.getString( "NameKey" );
		    	 return new Tuple<String,String>( accessKey, nameKey );
		    }
		    else return null;
        
        } finally {
            closeConnection();
        }
	}
	
	/**
	 * The multipart upload was either successfully completed or was aborted.   In either case, we need
	 * to remove all of its state from the tables.   Note that we have cascade deletes so all tables with
	 * uploadId as a foreign key are automatically cleaned.
	 * 
	 * @param uploadId
	 * 
	 * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException 
	 */
	public void deleteUpload( int uploadId )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
	    PreparedStatement statement = null;
		
        openConnection();	
        try {
		    statement = conn.prepareStatement ( "DELETE FROM multipart_uploads WHERE ID=?" );
	        statement.setInt( 1, uploadId );
	        int count = statement.executeUpdate();
            statement.close();	
        
        } finally {
            closeConnection();
        }
	}
	
	/**
	 * The caller needs to know who initiated the multipart upload.
	 * 
	 * @param uploadId
	 * @return the access key value defining the initiator
	 * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	 */
	public String getInitiator( int uploadId ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
	    PreparedStatement statement = null;
	    String initiator = null;
		
        openConnection();	
        try {
		    statement = conn.prepareStatement ( "SELECT AccessKey FROM multipart_uploads WHERE ID=?" );
	        statement.setInt( 1, uploadId );
	        ResultSet rs = statement.executeQuery();
		    if (rs.next()) initiator = rs.getString( "AccessKey" );
            statement.close();			    
            return initiator;
        
        } finally {
            closeConnection();
        }
	}
	
	/**
	 * Create a new "in-process" multipart upload entry to keep track of its state.
	 * 
	 * @param accessKey
	 * @param bucketName
	 * @param key
	 * @param cannedAccess
	 * 
	 * @return if positive its the uploadId to be returned to the client
	 * 
	 * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException 
	 */
	public int initiateUpload( String accessKey, String bucketName, String key, String cannedAccess, S3MetaDataEntry[] meta ) 
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
	    PreparedStatement statement = null;
		int uploadId = -1;
		
        openConnection();	
        try {
	        Date tod = new Date();
	        java.sql.Timestamp dateTime = new Timestamp( tod.getTime());

		    statement = conn.prepareStatement ( "INSERT INTO multipart_uploads (AccessKey, BucketName, NameKey, x_amz_acl, CreateTime) VALUES (?,?,?,?,?)" );
	        statement.setString( 1, accessKey );
	        statement.setString( 2, bucketName );
	        statement.setString( 3, key );
	        statement.setString( 4, cannedAccess );      
	        statement.setTimestamp( 5, dateTime );
            int count = statement.executeUpdate();
            statement.close();	
            
            // -> we need the newly entered ID 
		    statement = conn.prepareStatement ( "SELECT ID FROM multipart_uploads WHERE AccessKey=? AND BucketName=? AND NameKey=? AND CreateTime=?" );
	        statement.setString( 1, accessKey );
	        statement.setString( 2, bucketName );
	        statement.setString( 3, key );
	        statement.setTimestamp( 4, dateTime );
	        ResultSet rs = statement.executeQuery();
		    if (rs.next()) {
		    	uploadId = rs.getInt( "ID" );
		        saveMultipartMeta( uploadId, meta );
		    }
            statement.close();			    
            return uploadId;
        
        } finally {
            closeConnection();
        }
	}
	
	/**
	 * Remember all the individual parts that make up the entire multipart upload so that once
	 * the upload is complete all the parts can be glued together into a single object.  Note, 
	 * the caller can over write an existing part.
	 * 
	 * @param uploadId
	 * @param partNumber
	 * @param md5
	 * @param storedPath
	 * @param size
	 * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	 */
	public void savePart( int uploadId, int partNumber, String md5, String storedPath, int size ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;
        int id = -1;
        int count = 0;
	
        openConnection();	
        try {
            Date tod = new Date();
            java.sql.Timestamp dateTime = new java.sql.Timestamp( tod.getTime());

            // -> are we doing an update or an insert?  (are we over writting an existing entry?)
		    statement = conn.prepareStatement ( "SELECT ID FROM multipart_parts WHERE UploadID=? AND partNumber=?" );
            statement.setInt( 1, uploadId );
            statement.setInt( 2, partNumber  );
            ResultSet rs = statement.executeQuery();
		    if (rs.next()) id = rs.getInt( "ID" );
            statement.close();			    

            if ( -1 == id )
            {
	             statement = conn.prepareStatement ( "INSERT INTO multipart_parts (UploadID, partNumber, MD5, StoredPath, StoredSize, CreateTime) VALUES (?,?,?,?,?,?)" );
                 statement.setInt(    1, uploadId );
                 statement.setInt(    2, partNumber );
                 statement.setString( 3, md5 );
                 statement.setString( 4, storedPath );   
                 statement.setInt(    5, size );
                 statement.setTimestamp( 6, dateTime );
            }
            else
            {    statement = conn.prepareStatement ( "UPDATE multipart_parts SET MD5=?, StoredSize=?, CreateTime=? WHERE UploadId=? AND partNumber=?" );
                 statement.setString( 1, md5 );
                 statement.setInt(    2, size );
                 statement.setTimestamp( 3, dateTime );
                 statement.setInt(    4, uploadId );
                 statement.setInt(    5, partNumber );
            }
            count = statement.executeUpdate();
            statement.close();	
            
        } finally {
            closeConnection();
        }
    }
	
	/**
	 * It is possible for there to be a null canned access policy defined.
	 * @param uploadId
	 * @return the value defined in the x-amz-acl header or null
	 */
	public String getCannedAccess( int uploadId )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
	    PreparedStatement statement = null;
	    String access = null;
		
        openConnection();	
        try {
		    statement = conn.prepareStatement ( "SELECT x_amz_acl FROM multipart_uploads WHERE ID=?" );
	        statement.setInt( 1, uploadId );
	        ResultSet rs = statement.executeQuery();
		    if (rs.next()) access = rs.getString( "x_amz_acl" );
            statement.close();			    
            return access;
        
        } finally {
            closeConnection();
        }
	}
	
	/**
	 * When the multipart are being composed into one object we need any meta data to be saved with
	 * the new re-constituted object.
	 * 
	 * @param uploadId
	 * @return an array of S3MetaDataEntry (will be null if no meta values exist)
	 * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	 */
	public S3MetaDataEntry[] getMeta( int uploadId )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		List<S3MetaDataEntry> metaList = new ArrayList<S3MetaDataEntry>();
	    PreparedStatement statement = null;
	    int count = 0;
		
        openConnection();	
        try {
		    statement = conn.prepareStatement ( "SELECT Name, Value FROM multipart_meta WHERE UploadID=?" );
	        statement.setInt( 1, uploadId );
		    ResultSet rs = statement.executeQuery();
		    
		    while (rs.next()) 
		    {
		    	S3MetaDataEntry oneMeta = new S3MetaDataEntry();
		    	oneMeta.setName(  rs.getString( "Name" ));
	            oneMeta.setValue( rs.getString( "Value" ));
	            metaList.add( oneMeta );
	            count++;
		    }
            statement.close();	
            
            if ( 0 == count )
            	 return null;
            else return metaList.toArray(new S3MetaDataEntry[0]);
        
        } finally {
            closeConnection();
        }
	}
	
	/** 
	 * The result has to be ordered by key and if there is more than one identical key then all the 
	 * identical keys are ordered by create time.
	 * 
	 * @param bucketName
	 * @param maxParts
	 * @param prefix - can be null
	 * @param keyMarker - can be null
	 * @param uploadIdMarker - can be null, should only be defined if keyMarker is not-null
	 * @return Tuple<S3MultipartUpload[], isTruncated>
	 * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	 */
	public Tuple<S3MultipartUpload[],Boolean> getInitiatedUploads( String bucketName, int maxParts, String prefix, String keyMarker, String uploadIdMarker )
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		S3MultipartUpload[] inProgress = new S3MultipartUpload[maxParts];
	    PreparedStatement statement = null;
	    boolean isTruncated = false;
	    int i = 0;
	    int pos = 1;
	    
	    // -> SQL like condition requires the '%' as a wildcard marker
	    if (null != prefix) prefix = prefix + "%";
	    
	    StringBuffer queryStr = new StringBuffer();
	    queryStr.append( "SELECT ID, AccessKey, NameKey, CreateTime FROM multipart_uploads WHERE BucketName=? " );   
	    if (null != prefix        ) queryStr.append( "AND NameKey like ? " );
	    if (null != keyMarker     ) queryStr.append( "AND NameKey > ? ");
        if (null != uploadIdMarker) queryStr.append( "AND ID > ? " );    
        queryStr.append( "ORDER BY NameKey, CreateTime" );
		
        openConnection();	
        try {
		    statement = conn.prepareStatement ( queryStr.toString());
		    statement.setString( pos++, bucketName );
		    if (null != prefix        ) statement.setString( pos++, prefix );
		    if (null != keyMarker     ) statement.setString( pos++, keyMarker );
		    if (null != uploadIdMarker) statement.setString( pos, uploadIdMarker );
		    ResultSet rs = statement.executeQuery();
		    
		    while (rs.next() && i < maxParts) 
		    {
		    	Calendar tod = Calendar.getInstance();
		    	tod.setTime( rs.getTimestamp( "CreateTime" ));

		    	inProgress[i] = new S3MultipartUpload();
		    	inProgress[i].setId( rs.getInt( "ID" )); 
		    	inProgress[i].setAccessKey( rs.getString( "AccessKey" ));
		    	inProgress[i].setLastModified( tod );
		    	inProgress[i].setBucketName( bucketName );
		    	inProgress[i].setKey( rs.getString( "NameKey" ));
		    	i++;
		    }
		    
		    if (rs.next()) isTruncated = true;
            statement.close();		
            
            if (i < maxParts) inProgress = (S3MultipartUpload[])resizeArray(inProgress,i);
            return new Tuple<S3MultipartUpload[], Boolean>(inProgress, isTruncated);
        
        } finally {
            closeConnection();
        }

	}
	
	/**
	 * Return info on a range of upload parts that have already been stored in disk.
	 * Note that parts can be uploaded in any order yet we must returned an ordered list
	 * of parts thus we use the "ORDERED BY" clause to sort the list.
	 * 
	 * @param uploadId
	 * @param maxParts
	 * @param startAt
	 * @return an array of S3MultipartPart objects
	 * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	 */
	public S3MultipartPart[] getParts( int uploadId, int maxParts, int startAt ) 
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		S3MultipartPart[] parts = new S3MultipartPart[maxParts];
	    PreparedStatement statement = null;
	    int i = 0;
		
        openConnection();	
        try {
		    statement = conn.prepareStatement ( "SELECT   partNumber, MD5, StoredSize, StoredPath, CreateTime " +
		    		                            "FROM     multipart_parts " +
		    		                            "WHERE    UploadID=? " +
		    		                            "AND      partNumber > ? AND partNumber < ? " +
		    		                            "ORDER BY partNumber" );
	        statement.setInt( 1, uploadId );
	        statement.setInt( 2, startAt  );
	        statement.setInt( 3, startAt + maxParts + 1 );
		    ResultSet rs = statement.executeQuery();
		    
		    while (rs.next() && i < maxParts) 
		    {
		    	Calendar tod = Calendar.getInstance();
		    	tod.setTime( rs.getTimestamp( "CreateTime" ));
		    	
		    	parts[i] = new S3MultipartPart();
		    	parts[i].setPartNumber( rs.getInt( "partNumber" )); 
		    	parts[i].setEtag( rs.getString( "MD5" ));
		    	parts[i].setLastModified( tod );
		    	parts[i].setSize( rs.getInt( "StoredSize" ));
		    	parts[i].setPath( rs.getString( "StoredPath" ));
		    	i++;
		    }
            statement.close();		
            
            if (i < maxParts) parts = (S3MultipartPart[])resizeArray(parts,i);
            return parts;
        
        } finally {
            closeConnection();
        }
	}
  
	/**
	 * How many parts exist after the endMarker part number?
	 * 
	 * @param uploadId
	 * @param endMarker - can be used to see if getUploadedParts was truncated
	 * @return number of parts with partNumber greater than endMarker
	 * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	 */
	public int numParts( int uploadId, int endMarker ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        PreparedStatement statement = null;
        int count = 0;
	
        openConnection();	
        try {
	        statement = conn.prepareStatement ( "SELECT count(*) FROM multipart_parts WHERE UploadID=? AND partNumber > ?" );
            statement.setInt( 1, uploadId );
            statement.setInt( 2, endMarker );
	        ResultSet rs = statement.executeQuery();	    
	        if (rs.next()) count = rs.getInt( 1 );
            statement.close();			    
            return count;
    
        } finally {
            closeConnection();
        }
    }

	/**
	 * A multipart upload request can have zero to many meta data entries to be applied to the
	 * final object.   We need to remember all of the objects meta data until the multipart is complete.
	 * 
	 * @param uploadId - defines an in-process multipart upload
	 * @param meta - an array of meta data to be assocated with the uploadId value
	 * 
	 * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException 
	 */
	private void saveMultipartMeta( int uploadId, S3MetaDataEntry[] meta ) 
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		if (null == meta) return;	
	    PreparedStatement statement = null;
		
        openConnection();	
        try {
            for( int i=0; i < meta.length; i++ ) 
            {
               S3MetaDataEntry entry = meta[i];
		       statement = conn.prepareStatement ( "INSERT INTO multipart_meta (UploadID, Name, Value) VALUES (?,?,?)" );
	           statement.setInt( 1, uploadId );
	           statement.setString( 2, entry.getName());
	           statement.setString( 3, entry.getValue());
               int count = statement.executeUpdate();
               statement.close();
            }
            
        } finally {
            closeConnection();
        }
	}
	
	private void openConnection() 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        if (null == conn) {
	        Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
            conn = DriverManager.getConnection( "jdbc:mysql://localhost:3306/"+dbName, dbUser, dbPassword );
        }
	}

    private void closeConnection() throws SQLException {
	    if (null != conn) conn.close();
	    conn = null;
    }
    
    /**
    * Reallocates an array with a new size, and copies the contents
    * of the old array to the new array.
    * 
    * @param oldArray  the old array, to be reallocated.
    * @param newSize   the new array size.
    * @return          A new array with the same contents.
    */
    private static Object resizeArray(Object oldArray, int newSize) 
    {
       int oldSize = java.lang.reflect.Array.getLength(oldArray);
       Class elementType = oldArray.getClass().getComponentType();
       Object newArray = java.lang.reflect.Array.newInstance(
             elementType,newSize);
       int preserveLength = Math.min(oldSize,newSize);
       if (preserveLength > 0)
          System.arraycopy (oldArray,0,newArray,0,preserveLength);
       return newArray; 
    }
}
