package com.cloud.gate.service;

import java.util.Calendar;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.amazon.s3.client.AmazonS3Stub;
import com.amazon.s3.client.AmazonS3Stub.CreateBucket;
import com.amazon.s3.client.AmazonS3Stub.CreateBucketResponse;
import com.amazon.s3.client.AmazonS3Stub.DeleteBucket;
import com.amazon.s3.client.AmazonS3Stub.DeleteBucketResponse;
import com.cloud.gate.testcase.BaseTestCase;

public class ServiceTestCase extends BaseTestCase {
    protected final static Logger logger = Logger.getLogger(ServiceTestCase.class);
    
    private AmazonS3Stub serviceStub;

	protected void setUp() {
		super.setUp();
		
		try {
			serviceStub = new AmazonS3Stub("http://localhost:8080/gate/services/AmazonS3");
		} catch (Exception e) {
			logger.error("Exception " + e.getMessage(), e);
		}
	}
    
    public void testCreateBucket() {
    	Assert.assertTrue(serviceStub != null);
    	
    	try {
    		CreateBucket bucket = new CreateBucket();
    		bucket.setBucket("Test bucket 3");
    		bucket.setSignature("My signature 3");
    		bucket.setTimestamp(Calendar.getInstance());
    		
    		CreateBucketResponse response = serviceStub.createBucket(bucket);
    	} catch(Exception e) {
    		logger.error("Exception " + e.getMessage(), e);
    		Assert.assertTrue(false);
    	}
    }
    
    public void testDeleteBucket() {
    	Assert.assertTrue(serviceStub != null);
    	
    	try {
    		DeleteBucket bucket = new DeleteBucket();
    		bucket.setBucket("Test bucket 3");
    		bucket.setSignature("My signature 3");
    		bucket.setTimestamp(Calendar.getInstance());
    		
    		DeleteBucketResponse response = serviceStub.deleteBucket(bucket);
    	} catch(Exception e) {
    		logger.error("Exception " + e.getMessage(), e);
    		Assert.assertTrue(false);
    	}
    }
}
