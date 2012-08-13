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
package com.cloud.bridge.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.core.s3.S3BucketAdapter;
import com.cloud.bridge.service.core.s3.S3MultipartPart;
import com.cloud.bridge.service.exception.FileNotExistException;
import com.cloud.bridge.service.exception.InternalErrorException;
import com.cloud.bridge.service.exception.OutOfStorageException;
import com.cloud.bridge.util.StringHelper;
import com.cloud.bridge.util.OrderedPair;

public class S3FileSystemBucketAdapter implements S3BucketAdapter {
    protected final static Logger logger = Logger.getLogger(S3FileSystemBucketAdapter.class);
	
	public S3FileSystemBucketAdapter() {
	}
	
	@Override
	public void createContainer(String mountedRoot, String bucket) {
		
		String dir = getBucketFolderDir(mountedRoot, bucket);
		File container = new File(dir);
		
		if (!container.exists()) {
		   if (!container.mkdirs())
			   throw new OutOfStorageException("Unable to create " + dir + " for bucket " + bucket); 
		}
	}
	
	@Override
	public void deleteContainer(String mountedRoot, String bucket) {
		String dir = getBucketFolderDir(mountedRoot, bucket);
		File path = new File(dir);
		if(!deleteDirectory(path))
			throw new OutOfStorageException("Unable to delete " + dir + " for bucket " + bucket); 
	}
	
	@Override
	public String getBucketFolderDir(String mountedRoot, String bucket) {
		String bucketFolder = getBucketFolderName(bucket);
		String dir;
		String separator = ""+File.separatorChar;
		if(!mountedRoot.endsWith(separator))
			dir = mountedRoot + separator + bucketFolder;
		else
			dir = mountedRoot + bucketFolder;
		
		return dir;
	}
	
	@Override
	public String saveObject(InputStream is, String mountedRoot, String bucket, String fileName) 
	{
		FileOutputStream fos = null;
		MessageDigest md5 = null;
		
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			logger.error("Unexpected exception " + e.getMessage(), e);
			throw new InternalErrorException("Unable to get MD5 MessageDigest", e);
		}
		
		File file = new File(getBucketFolderDir(mountedRoot, bucket) + File.separatorChar + fileName);
		try {
			// -> when versioning is off we need to rewrite the file contents
			file.delete();
			file.createNewFile();
			
	        fos = new FileOutputStream(file);
	        byte[] buffer = new byte[4096];
	        int len = 0;
	        while( (len = is.read(buffer)) > 0) {
	        	fos.write(buffer, 0, len);
	        	md5.update(buffer, 0, len);
	        	
	        }       
	        //Convert MD4 digest to (lowercase) hex String
	        return StringHelper.toHexString(md5.digest());
	        
		} 
		catch(IOException e) {
			logger.error("Unexpected exception " + e.getMessage(), e);
			throw new OutOfStorageException(e);
		}
		finally {
			try {
			    if (null != fos) fos.close();
			}
			catch( Exception e ) {
				logger.error("Can't close FileOutputStream " + e.getMessage(), e);			
			}
		}
	}
	
	/**
	 * From a list of files (each being one part of the multipart upload), concatentate all files into a single
	 * object that can be accessed by normal S3 calls.  This function could take a long time since a multipart is
	 * allowed to have upto 10,000 parts (each 5 gib long).   Amazon defines that while this operation is in progress
	 * whitespace is sent back to the client inorder to keep the HTTP connection alive.
	 * 
	 * @param mountedRoot - where both the source and dest buckets are located
	 * @param destBucket - resulting location of the concatenated objects
	 * @param fileName - resulting file name of the concatenated objects
	 * @param sourceBucket - special bucket used to save uploaded file parts
	 * @param parts - an array of file names in the sourceBucket
	 * @param client - if not null, then keep the servlet connection alive while this potentially long concatentation takes place
	 * @return OrderedPair with the first value the MD5 of the final object, and the second value the length of the final object
	 */
	@Override
	public OrderedPair<String,Long> concatentateObjects(String mountedRoot, String destBucket, String fileName, String sourceBucket, S3MultipartPart[] parts, OutputStream client) 
	{
		MessageDigest md5;
		long totalLength = 0;
		
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			logger.error("Unexpected exception " + e.getMessage(), e);
			throw new InternalErrorException("Unable to get MD5 MessageDigest", e);
		}
		
		File file = new File(getBucketFolderDir(mountedRoot, destBucket) + File.separatorChar + fileName);
		try {
			// -> when versioning is off we need to rewrite the file contents
			file.delete();
			file.createNewFile();
			
	        final FileOutputStream fos = new FileOutputStream(file);
	        byte[] buffer = new byte[4096];
	        
	        // -> get the input stream for the next file part
	        for( int i=0; i < parts.length; i++ )
	        {
	           DataHandler nextPart = loadObject( mountedRoot, sourceBucket, parts[i].getPath());
	           InputStream is = nextPart.getInputStream();
	           
	           int len = 0;
	           while( (len = is.read(buffer)) > 0) {
	        	   fos.write(buffer, 0, len);
	        	   md5.update(buffer, 0, len);
	        	   totalLength += len;
	           }
	           is.close();
	           
	           // -> after each file write tell the client we are still here to keep connection alive
	           if (null != client) {
	        	   client.write( new String(" ").getBytes());
	        	   client.flush();
	           }
	        }        
	        fos.close();	
	        return new OrderedPair<String, Long>(StringHelper.toHexString(md5.digest()), new Long(totalLength));
	        //Create an ordered pair whose first element is the MD4 digest as a (lowercase) hex String
		} 
		catch(IOException e) {
			logger.error("concatentateObjects unexpected exception " + e.getMessage(), e);
			throw new OutOfStorageException(e);
		}	
	}
	
	@Override
	public DataHandler loadObject(String mountedRoot, String bucket, String fileName) {
		File file = new File(getBucketFolderDir(mountedRoot, bucket) + File.separatorChar + fileName);
		try {
			return new DataHandler(file.toURL());
		} catch (MalformedURLException e) {
			throw new FileNotExistException("Unable to open underlying object file");
		}
	}
	
	@Override
	public void deleteObject(String mountedRoot, String bucket, String fileName) {
		String filePath = new String( getBucketFolderDir(mountedRoot, bucket) + File.separatorChar + fileName );
		File file = new File( filePath );	
		if (!file.delete()) {
			logger.error("file: " + filePath + ", f=" + file.isFile() + ", h=" + file.isHidden() + ", e=" + file.exists() + ", w=" + file.canWrite());
			throw new OutOfStorageException( "Unable to delete " + filePath + " for object deletion" ); 
		}
	}

	@Override
	public DataHandler loadObjectRange(String mountedRoot, String bucket, String fileName, long startPos, long endPos) {
		File file = new File(getBucketFolderDir(mountedRoot, bucket) + File.separatorChar + fileName);
		try {
			DataSource ds = new FileRangeDataSource(file, startPos, endPos);
			return new DataHandler(ds);
		} catch (MalformedURLException e) {
			throw new FileNotExistException("Unable to open underlying object file");
		} catch(IOException e) {
			throw new FileNotExistException("Unable to open underlying object file");
		}
	}
	
	public static boolean deleteDirectory(File path) {
		 if( path.exists() ) {
			 File[] files = path.listFiles();
			 for(int i = 0; i < files.length; i++) {
				 if(files[i].isDirectory()) {
					 deleteDirectory(files[i]);
				 } else {
					 files[i].delete();
				 }
			 }
		 }
		 return path.delete();
	}
	
	private String getBucketFolderName(String bucket) {
		// temporary 
		String name = bucket.replace(' ', '_');
		name = bucket.replace('\\', '-');
		name = bucket.replace('/', '-');
		
		return name;
	}
}
