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
package com.cloud.bridge.service.core.s3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.json.simple.parser.ParseException;

<<<<<<< HEAD
=======
import com.cloud.bridge.io.S3FileSystemBucketAdapter;
>>>>>>> 6472e7b... Now really adding the renamed files!
import com.cloud.bridge.model.MHost;
import com.cloud.bridge.model.MHostMount;
import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.model.SHost;
import com.cloud.bridge.model.SMeta;
import com.cloud.bridge.model.SObject;
import com.cloud.bridge.model.SObjectItem;
import com.cloud.bridge.persist.PersistContext;
import com.cloud.bridge.persist.dao.BucketPolicyDao;
import com.cloud.bridge.persist.dao.MHostDao;
import com.cloud.bridge.persist.dao.MHostMountDao;
import com.cloud.bridge.persist.dao.MultipartLoadDao;
import com.cloud.bridge.persist.dao.SAclDao;
import com.cloud.bridge.persist.dao.SBucketDao;
import com.cloud.bridge.persist.dao.SHostDao;
import com.cloud.bridge.persist.dao.SMetaDao;
import com.cloud.bridge.persist.dao.SObjectDao;
import com.cloud.bridge.persist.dao.SObjectItemDao;
<<<<<<< HEAD
import com.cloud.bridge.service.S3BucketAdapter;
import com.cloud.bridge.service.S3FileSystemBucketAdapter;
import com.cloud.bridge.service.ServiceProvider;
import com.cloud.bridge.service.UserContext;
=======
import com.cloud.bridge.service.S3Constants;
import com.cloud.bridge.service.UserContext;
import com.cloud.bridge.service.controller.s3.ServiceProvider;
>>>>>>> 6472e7b... Now really adding the renamed files!
import com.cloud.bridge.service.core.s3.S3BucketPolicy.PolicyAccess;
import com.cloud.bridge.service.core.s3.S3CopyObjectRequest.MetadataDirective;
import com.cloud.bridge.service.core.s3.S3PolicyAction.PolicyActions;
import com.cloud.bridge.service.core.s3.S3PolicyCondition.ConditionKeys;
import com.cloud.bridge.service.exception.HostNotMountedException;
import com.cloud.bridge.service.exception.InternalErrorException;
import com.cloud.bridge.service.exception.InvalidBucketName;
import com.cloud.bridge.service.exception.NoSuchObjectException;
import com.cloud.bridge.service.exception.ObjectAlreadyExistsException;
import com.cloud.bridge.service.exception.OutOfServiceException;
import com.cloud.bridge.service.exception.OutOfStorageException;
import com.cloud.bridge.service.exception.PermissionDeniedException;
import com.cloud.bridge.service.exception.UnsupportedException;
import com.cloud.bridge.util.DateHelper;
import com.cloud.bridge.util.PolicyParser;
import com.cloud.bridge.util.StringHelper;
<<<<<<< HEAD
import com.cloud.bridge.util.Tuple;

/**
 * @author Kelven Yang
=======
import com.cloud.bridge.util.OrderedPair;
import com.cloud.bridge.util.Triple;

/**
 * @author Kelven Yang, John Zucker
 * The CRUD control actions to be invoked from S3BucketAction or S3ObjectAction.
>>>>>>> 6472e7b... Now really adding the renamed files!
 */
public class S3Engine {
    protected final static Logger logger = Logger.getLogger(S3Engine.class);
    
    private final int LOCK_ACQUIRING_TIMEOUT_SECONDS = 10;		// ten seconds

    private final Map<Integer, S3BucketAdapter> bucketAdapters = new HashMap<Integer, S3BucketAdapter>();
    
    public S3Engine() {
    	bucketAdapters.put(SHost.STORAGE_HOST_TYPE_LOCAL, new S3FileSystemBucketAdapter());
    }
    
<<<<<<< HEAD
    /**
     * We treat this simply as first a get and then a put of the object the user wants to copy.
     */
=======
    
    /**
     * Return a S3CopyObjectResponse which represents an object being copied from source
     * to destination bucket.    
     * Called from S3ObjectAction when copying an object.
     * This can be treated as first a GET followed by a PUT of the object the user wants to copy.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
	public S3CopyObjectResponse handleRequest(S3CopyObjectRequest request) 
	{
		S3CopyObjectResponse response = new S3CopyObjectResponse();
		
		// [A] Get the object we want to copy
		S3GetObjectRequest getRequest = new S3GetObjectRequest();
		getRequest.setBucketName(request.getSourceBucketName());
		getRequest.setKey(request.getSourceKey());
		getRequest.setVersion(request.getVersion());
		getRequest.setConditions( request.getConditions());

		getRequest.setInlineData( true );
		getRequest.setReturnData( true );
		if ( MetadataDirective.COPY == request.getDirective()) 
		     getRequest.setReturnMetadata( true );
		else getRequest.setReturnMetadata( false );		
			
		//-> before we do anything verify the permissions on a copy basis
		String  destinationBucketName = request.getDestinationBucketName();
		String  destinationKeyName = request.getDestinationKey();
		S3PolicyContext context = new S3PolicyContext( PolicyActions.PutObject, destinationBucketName );
		context.setKeyName( destinationKeyName );
		context.setEvalParam( ConditionKeys.MetaData, request.getDirective().toString());
		context.setEvalParam( ConditionKeys.CopySource, "/" + request.getSourceBucketName() + "/" + request.getSourceKey());
		if (PolicyAccess.DENY == verifyPolicy( context )) 
            throw new PermissionDeniedException( "Access Denied - bucket policy DENY result" );
			  		
	    S3GetObjectResponse originalObject = handleRequest(getRequest); 	
	    int resultCode = originalObject.getResultCode();
	    if (200 != resultCode) {
	    	response.setResultCode( resultCode );
	    	response.setResultDescription( originalObject.getResultDescription());
	    	return response;
	    }
	    	    
	    response.setCopyVersion( originalObject.getVersion());

	    
	    // [B] Put the object into the destination bucket
	    S3PutObjectInlineRequest putRequest = new S3PutObjectInlineRequest();
	    putRequest.setBucketName(request.getDestinationBucketName()) ;
	    putRequest.setKey(destinationKeyName);
		if ( MetadataDirective.COPY == request.getDirective()) 
			 putRequest.setMetaEntries(originalObject.getMetaEntries());
		else putRequest.setMetaEntries(request.getMetaEntries());	
	    putRequest.setAcl(request.getAcl());                    // -> if via a SOAP call
	    putRequest.setCannedAccess(request.getCannedAccess());  // -> if via a REST call 
	    putRequest.setContentLength(originalObject.getContentLength());
	    putRequest.setData(originalObject.getData());

	    S3PutObjectInlineResponse putResp = handleRequest(putRequest);  
	    response.setResultCode( putResp.resultCode );
	    response.setResultDescription( putResp.getResultDescription());
		response.setETag( putResp.getETag());
		response.setLastModified( putResp.getLastModified());
		response.setPutVersion( putResp.getVersion());
		return response;
	}

    public S3CreateBucketResponse handleRequest(S3CreateBucketRequest request) 
    {
    	S3CreateBucketResponse response = new S3CreateBucketResponse();
		String cannedAccessPolicy = request.getCannedAccess();
    	String bucketName = request.getBucketName();
    	response.setBucketName( bucketName );
    	
		verifyBucketName( bucketName, false );
 	
		S3PolicyContext context = new S3PolicyContext( PolicyActions.CreateBucket,  bucketName );
		context.setEvalParam( ConditionKeys.Acl, cannedAccessPolicy );
		if (PolicyAccess.DENY == verifyPolicy( context )) 
            throw new PermissionDeniedException( "Access Denied - bucket policy DENY result" );
   	
		if (PersistContext.acquireNamedLock("bucket.creation", LOCK_ACQUIRING_TIMEOUT_SECONDS)) 
		{
<<<<<<< HEAD
			Tuple<SHost, String> shostTuple = null;
=======
			OrderedPair<SHost, String> shost_storagelocation_pair = null;
>>>>>>> 6472e7b... Now really adding the renamed files!
			boolean success = false;
			try {
				SBucketDao bucketDao = new SBucketDao();
				SAclDao    aclDao    = new SAclDao();
				
				if (bucketDao.getByName(request.getBucketName()) != null)
					throw new ObjectAlreadyExistsException("Bucket already exists"); 
					
<<<<<<< HEAD
				shostTuple = allocBucketStorageHost(request.getBucketName(), null);
=======
				shost_storagelocation_pair = allocBucketStorageHost(request.getBucketName(), null);
>>>>>>> 6472e7b... Now really adding the renamed files!
				
				SBucket sbucket = new SBucket();
				sbucket.setName(request.getBucketName());
				sbucket.setCreateTime(DateHelper.currentGMTTime());
				sbucket.setOwnerCanonicalId( UserContext.current().getCanonicalUserId());
<<<<<<< HEAD
				sbucket.setShost(shostTuple.getFirst());
				shostTuple.getFirst().getBuckets().add(sbucket);
=======
				sbucket.setShost(shost_storagelocation_pair.getFirst());
				shost_storagelocation_pair.getFirst().getBuckets().add(sbucket);
>>>>>>> 6472e7b... Now really adding the renamed files!
				bucketDao.save(sbucket);

				S3AccessControlList acl = request.getAcl();
				
				if ( null != cannedAccessPolicy ) 
					 setCannedAccessControls( cannedAccessPolicy, "SBucket", sbucket.getId(), sbucket );
				else if (null != acl) 
					 aclDao.save(  "SBucket", sbucket.getId(), acl );
				else setSingleAcl( "SBucket", sbucket.getId(), SAcl.PERMISSION_FULL ); 
			
				// explicitly commit the transaction
				PersistContext.commitTransaction();
				success = true;				
			} 
			finally 
			{
<<<<<<< HEAD
				if(!success && shostTuple != null) {
					S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter(shostTuple.getFirst());
					bucketAdapter.deleteContainer(shostTuple.getSecond(), request.getBucketName());
=======
				if(!success && shost_storagelocation_pair != null) {
					S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter(shost_storagelocation_pair.getFirst());
					bucketAdapter.deleteContainer(shost_storagelocation_pair.getSecond(), request.getBucketName());
>>>>>>> 6472e7b... Now really adding the renamed files!
				}
				PersistContext.releaseNamedLock("bucket.creation");
			}
			
		} else {
			throw new OutOfServiceException("Unable to acquire synchronization lock");
		}
		
		return response;
    }
    
<<<<<<< HEAD
=======
    /**
     * Return a S3Response which represents the effect of an object being deleted from its bucket.    
     * Called from S3BucketAction when deleting an object.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3Response handleRequest( S3DeleteBucketRequest request ) 
    {
    	S3Response response  = new S3Response();   	
		SBucketDao bucketDao = new SBucketDao();
		String bucketName = request.getBucketName();
		SBucket sbucket   = bucketDao.getByName( bucketName );
		
		if ( sbucket != null ) 
		{			 
			 S3PolicyContext context = new S3PolicyContext( PolicyActions.DeleteBucket, bucketName );
<<<<<<< HEAD
			 switch( verifyPolicy( context )) {
			 case ALLOW:
				  // -> bucket policy can give users permission to delete a bucket while ACLs cannot
=======
			 switch( verifyPolicy( context ))
			 {
			 case ALLOW:
				  // The bucket policy can give users permission to delete a bucket whereas ACLs cannot
>>>>>>> 6472e7b... Now really adding the renamed files!
				  break;
				  
			 case DENY:
	              throw new PermissionDeniedException( "Access Denied - bucket policy DENY result" );
				  
			 case DEFAULT_DENY:
			 default:
<<<<<<< HEAD
				  // -> does not matter what the ACLs say only the owner can delete a bucket
=======
				  // Irrespective of what the ACLs say, only the owner can delete a bucket
>>>>>>> 6472e7b... Now really adding the renamed files!
				  String client = UserContext.current().getCanonicalUserId();
				  if (!client.equals( sbucket.getOwnerCanonicalId())) {
				      throw new PermissionDeniedException( "Access Denied - only the owner can delete a bucket" );
				  }
				  break;
			 }

			 
<<<<<<< HEAD
			 // -> delete the file
			 Tuple<SHost, String> tupleBucketHost = getBucketStorageHost(sbucket);
			 S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleBucketHost.getFirst());			
			 bucketAdapter.deleteContainer(tupleBucketHost.getSecond(), request.getBucketName());
			
			 // -> cascade-deleting can delete related SObject/SObjectItem objects, but not SAcl, SMeta and policy objects. We
			 //    need to perform deletion of these objects related to bucket manually.
			 //    Delete SMeta & SAcl objects: (1)Get all the objects in the bucket, (2)then all the items in each object, (3) then all meta & acl data for each item
			 Set<SObject> objectsInBucket = sbucket.getObjectsInBucket();
			 Iterator it = objectsInBucket.iterator();
=======
			 // Delete the file from its storage location
			 OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost(sbucket);
			 S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());			
			 bucketAdapter.deleteContainer(host_storagelocation_pair.getSecond(), request.getBucketName());
			
			 // Cascade-deleting can delete related SObject/SObjectItem objects, but not SAcl, SMeta and policy objects.
			 // To delete SMeta & SAcl objects: 
			 // (1)Get all the objects in the bucket, 
			 // (2)then all the items in each object, 
			 // (3) then all meta & acl data for each item
			 Set<SObject> objectsInBucket = sbucket.getObjectsInBucket();
			 Iterator<SObject> it = objectsInBucket.iterator();
>>>>>>> 6472e7b... Now really adding the renamed files!
			 while( it.hasNext()) 
			 {
			 	SObject oneObject = (SObject)it.next();
				Set<SObjectItem> itemsInObject = oneObject.getItems();
<<<<<<< HEAD
				Iterator is = itemsInObject.iterator();
=======
				Iterator<SObjectItem> is = itemsInObject.iterator();
>>>>>>> 6472e7b... Now really adding the renamed files!
				while( is.hasNext()) 
				{
					SObjectItem oneItem = (SObjectItem)is.next();
		    		deleteMetaData( oneItem.getId());
		    		deleteObjectAcls( "SObjectItem", oneItem.getId());
				}				
			 }
			 	
<<<<<<< HEAD
			 // -> delete all the policy state associated with the bucket
=======
			 // Delete all the policy state associated with the bucket
>>>>>>> 6472e7b... Now really adding the renamed files!
			 try {
	   	         ServiceProvider.getInstance().deleteBucketPolicy( bucketName );
		         BucketPolicyDao policyDao = new BucketPolicyDao();
		         policyDao.deletePolicy( bucketName );
			 }
			 catch( Exception e ) {
                logger.error("When deleting a bucket we must try to delete its policy: ", e);
			 }
			 
			 deleteBucketAcls( sbucket.getId());
			 bucketDao.delete( sbucket );		
			 response.setResultCode(204);
			 response.setResultDescription("OK");
		} 
		else 
		{    response.setResultCode(404);
			 response.setResultDescription("Bucket does not exist");
		}
    	return response;
    }
    
<<<<<<< HEAD
=======
    /**
     * Return a S3ListBucketResponse which represents a list of up to 1000 objects contained ins  the bucket.    
     * Called from S3BucketAction for GETting objects and for GETting object versions.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3ListBucketResponse listBucketContents(S3ListBucketRequest request, boolean includeVersions) 
    {
    	S3ListBucketResponse response = new S3ListBucketResponse();
		String bucketName = request.getBucketName();
		String prefix = request.getPrefix();
		if (prefix == null) prefix = StringHelper.EMPTY_STRING;
		String marker = request.getMarker();
		if (marker == null)	marker = StringHelper.EMPTY_STRING;
			
		String delimiter = request.getDelimiter();
		int maxKeys = request.getMaxKeys();
		if(maxKeys <= 0) maxKeys = 1000;
		
		SBucketDao bucketDao = new SBucketDao();
		SBucket sbucket = bucketDao.getByName(bucketName);
		if (sbucket == null) throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");
		
		PolicyActions action = (includeVersions ? PolicyActions.ListBucketVersions : PolicyActions.ListBucket);
		S3PolicyContext context = new S3PolicyContext( action, bucketName );
		context.setEvalParam( ConditionKeys.MaxKeys, new String( "" + maxKeys ));
		context.setEvalParam( ConditionKeys.Prefix, prefix );
		context.setEvalParam( ConditionKeys.Delimiter, delimiter );
		verifyAccess( context, "SBucket", sbucket.getId(), SAcl.PERMISSION_READ ); 

		
<<<<<<< HEAD
		// when we query, request one more item so that we know how to set isTruncated flag 
=======
		// Wen execting the query, request one more item so that we know how to set isTruncated flag 
>>>>>>> 6472e7b... Now really adding the renamed files!
		SObjectDao sobjectDao = new SObjectDao();
		List<SObject> l = null;
		
		if ( includeVersions )
             l = sobjectDao.listAllBucketObjects( sbucket, prefix, marker, maxKeys+1 );   
		else l = sobjectDao.listBucketObjects( sbucket, prefix, marker, maxKeys+1 );   
		
		response.setBucketName(bucketName);
		response.setMarker(marker);
		response.setMaxKeys(maxKeys);
		response.setPrefix(prefix);
		response.setDelimiter(delimiter);
		response.setTruncated(l.size() > maxKeys);
		if(l.size() > maxKeys) {
			response.setNextMarker(l.get(l.size() - 1).getNameKey());
		}

<<<<<<< HEAD
		// SOAP response does not support versioning
=======
		// If needed - SOAP response does not support versioning
>>>>>>> 6472e7b... Now really adding the renamed files!
		response.setContents( composeListBucketContentEntries(l, prefix, delimiter, maxKeys, includeVersions, request.getVersionIdMarker()));
		response.setCommonPrefixes( composeListBucketPrefixEntries(l, prefix, delimiter, maxKeys));
		return response;
    }
    
    /**
<<<<<<< HEAD
     * To check on bucket policies defined we have to (look for and) evaluate the policy on each
     * bucket the user owns.
     * 
     * @param request
     * @return
=======
     * Return a S3ListAllMyBucketResponse which represents a list of all buckets owned by the requester.    
     * Called from S3BucketAction for GETting all buckets.
     * To check on bucket policies defined we have to (look for and) evaluate the policy on each
     * bucket the user owns.
>>>>>>> 6472e7b... Now really adding the renamed files!
     */
    public S3ListAllMyBucketsResponse handleRequest(S3ListAllMyBucketsRequest request) 
    {
    	S3ListAllMyBucketsResponse response = new S3ListAllMyBucketsResponse();   	
    	SBucketDao bucketDao = new SBucketDao();
    	
<<<<<<< HEAD
    	// -> "...you can only list buckets for which you are the owner."
=======
    	// "...you can only list buckets for which you are the owner."
>>>>>>> 6472e7b... Now really adding the renamed files!
    	List<SBucket> buckets = bucketDao.listBuckets(UserContext.current().getCanonicalUserId());
    	S3CanonicalUser owner = new S3CanonicalUser();
    	owner.setID(UserContext.current().getCanonicalUserId());
    	owner.setDisplayName("");
    	response.setOwner(owner);
    	
    	if (buckets != null) 
    	{
    		S3ListAllMyBucketsEntry[] entries = new S3ListAllMyBucketsEntry[buckets.size()];
    		int i = 0;
    		for(SBucket bucket : buckets) 
    		{	
    			String bucketName = bucket.getName();
    			S3PolicyContext context = new S3PolicyContext( PolicyActions.ListAllMyBuckets, bucketName );
    			verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_PASS ); 
 			
    			entries[i] = new S3ListAllMyBucketsEntry();
    			entries[i].setName(bucketName);
    			entries[i].setCreationDate(DateHelper.toCalendar(bucket.getCreateTime()));
    			i++;
    		}   		
    		response.setBuckets(entries);
    	}   	
    	return response;
    }
    
<<<<<<< HEAD
    public S3Response handleRequest(S3SetBucketAccessControlPolicyRequest request) 
    {
=======
    /**
     * Return an S3Response representing the result of PUTTING the ACL of a given bucket.
     * Called from S3BucketAction to PUT its ACL.
     */
    
    public S3Response handleRequest(S3SetBucketAccessControlPolicyRequest request) 
    { 
>>>>>>> 6472e7b... Now really adding the renamed files!
    	S3Response response = new S3Response();	
    	SBucketDao bucketDao = new SBucketDao();
    	String bucketName = request.getBucketName();
    	SBucket sbucket = bucketDao.getByName(bucketName);
    	if(sbucket == null) {
    		response.setResultCode(404);
    		response.setResultDescription("Bucket does not exist");
    		return response;
    	}
<<<<<<< HEAD
 
=======
    	
>>>>>>> 6472e7b... Now really adding the renamed files!
	    S3PolicyContext context = new S3PolicyContext( PolicyActions.PutBucketAcl, bucketName );
    	verifyAccess( context, "SBucket", sbucket.getId(), SAcl.PERMISSION_WRITE_ACL ); 

    	SAclDao aclDao = new SAclDao();
    	aclDao.save("SBucket", sbucket.getId(), request.getAcl());
   	
    	response.setResultCode(200);
    	response.setResultDescription("OK");
    	return response;
    }
    
<<<<<<< HEAD
=======
    
    /**
     * Return a S3AccessControlPolicy representing the ACL of a given bucket.
     * Called from S3BucketAction to GET its ACL.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3AccessControlPolicy handleRequest(S3GetBucketAccessControlPolicyRequest request) 
    {
    	S3AccessControlPolicy policy = new S3AccessControlPolicy();   	
    	SBucketDao bucketDao = new SBucketDao();
    	String bucketName = request.getBucketName();
    	SBucket sbucket = bucketDao.getByName( bucketName );
    	if (sbucket == null)
    		throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");
    	
    	S3CanonicalUser owner = new S3CanonicalUser();
    	owner.setID(sbucket.getOwnerCanonicalId());
    	owner.setDisplayName("");
    	policy.setOwner(owner);
    	
	    S3PolicyContext context = new S3PolicyContext( PolicyActions.GetBucketAcl, bucketName );
    	verifyAccess( context, "SBucket", sbucket.getId(), SAcl.PERMISSION_READ_ACL ); 

    	SAclDao aclDao = new SAclDao(); 
    	List<SAcl> grants = aclDao.listGrants("SBucket", sbucket.getId());
    	policy.setGrants(S3Grant.toGrants(grants));  	
    	return policy;
    }
    
    /**
<<<<<<< HEAD
     * This function should be called if a multipart upload is aborted OR has completed successfully and
     * the individual parts have to be cleaned up.
     * 
     * @param bucketName
     * @param uploadId
     * @param verifyPermission - if false then don't check the user's permission to clean up the state
     * @return
=======
     * This method should be called if a multipart upload is aborted OR has completed successfully and
     * the individual parts have to be cleaned up.
     * Called from S3ObjectAction when executing at completion or when aborting multipart upload.
     * @param bucketName
     * @param uploadId
     * @param verifyPermission - If false then do not check the user's permission to clean up the state
>>>>>>> 6472e7b... Now really adding the renamed files!
     */
    public int freeUploadParts(String bucketName, int uploadId, boolean verifyPermission)
    {
		// -> we need to look up the final bucket to figure out which mount point to use to save the part in
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if (bucket == null) {
			logger.error( "initiateMultipartUpload failed since " + bucketName + " does not exist" );
			return 404;
		}
	
<<<<<<< HEAD
		Tuple<SHost, String> tupleBucketHost = getBucketStorageHost(bucket);		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleBucketHost.getFirst());

		try {
    	    MultipartLoadDao uploadDao = new MultipartLoadDao();
    	    Tuple<String,String> exists = uploadDao.multipartExits( uploadId );
=======
		OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost(bucket);		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());

		try {
    	    MultipartLoadDao uploadDao = new MultipartLoadDao();
    	    OrderedPair<String,String> exists = uploadDao.multipartExits( uploadId );
>>>>>>> 6472e7b... Now really adding the renamed files!
    	    if (null == exists) {
    			logger.error( "initiateMultipartUpload failed since multipart upload" + uploadId + " does not exist" );
    	    	return 404;
    	    }
    	    
    	    // -> the multipart initiator or bucket owner can do this action by default
    	    if (verifyPermission)
    	    {
    	        String initiator = uploadDao.getInitiator( uploadId );
    	        if (null == initiator || !initiator.equals( UserContext.current().getAccessKey())) 
    	        {
    	    	    // -> write permission on a bucket allows a PutObject / DeleteObject action on any object in the bucket
    			    S3PolicyContext context = new S3PolicyContext( PolicyActions.AbortMultipartUpload, bucketName );
    	    	    context.setKeyName( exists.getSecond());
    			    verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE );
    	        }
    	    }

    	    // -> first get a list of all the uploaded files and delete one by one
	        S3MultipartPart[] parts = uploadDao.getParts( uploadId, 10000, 0 );
	        for( int i=0; i < parts.length; i++ )
	        {    
<<<<<<< HEAD
    	        bucketAdapter.deleteObject( tupleBucketHost.getSecond(), ServiceProvider.getInstance().getMultipartDir(), parts[i].getPath());
=======
    	        bucketAdapter.deleteObject( host_storagelocation_pair.getSecond(), ServiceProvider.getInstance().getMultipartDir(), parts[i].getPath());
>>>>>>> 6472e7b... Now really adding the renamed files!
	        }
	        
	        uploadDao.deleteUpload( uploadId );
    	    return 204;

		}
		catch( PermissionDeniedException e ) {
			logger.error("freeUploadParts failed due to [" + e.getMessage() + "]", e);	
            throw e;		
		}
		catch (Exception e) {
			logger.error("freeUploadParts failed due to [" + e.getMessage() + "]", e);	
			return 500;
		}     
	}

    /**
     * The initiator must have permission to write to the bucket in question in order to initiate
     * a multipart upload.  Also check to make sure the special folder used to store parts of 
     * a multipart exists for this bucket.
<<<<<<< HEAD
     *  
     * @param request
=======
     * Called from S3ObjectAction during many stages of multipart upload.
>>>>>>> 6472e7b... Now really adding the renamed files!
     */
    public S3PutObjectInlineResponse initiateMultipartUpload(S3PutObjectInlineRequest request)
    {
    	S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();	
		String bucketName = request.getBucketName();
		String nameKey = request.getKey();

		// -> does the bucket exist and can we write to it?
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if (bucket == null) {
			logger.error( "initiateMultipartUpload failed since " + bucketName + " does not exist" );
			response.setResultCode(404);
		}
	    
		S3PolicyContext context = new S3PolicyContext( PolicyActions.PutObject, bucketName );
		context.setKeyName( nameKey );
		context.setEvalParam( ConditionKeys.Acl, request.getCannedAccess());
		verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE );

		createUploadFolder( bucketName ); 

        try {
    	    MultipartLoadDao uploadDao = new MultipartLoadDao();
    	    int uploadId = uploadDao.initiateUpload( UserContext.current().getAccessKey(), bucketName, nameKey, request.getCannedAccess(), request.getMetaEntries());
    	    response.setUploadId( uploadId );
    	    response.setResultCode(200);
    	    
        } catch( Exception e ) {
            logger.error("initiateMultipartUpload exception: ", e);
        	response.setResultCode(500);
        }

        return response;
    }

    /**
     * Save the object fragment in a special (i.e., hidden) directory inside the same mount point as 
     * the bucket location that the final object will be stored in.
<<<<<<< HEAD
     * 
=======
     * Called from S3ObjectAction during many stages of multipart upload.
>>>>>>> 6472e7b... Now really adding the renamed files!
     * @param request
     * @param uploadId
     * @param partNumber
     * @return S3PutObjectInlineResponse
     */
    public S3PutObjectInlineResponse saveUploadPart(S3PutObjectInlineRequest request, int uploadId, int partNumber) 
    {
    	S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();	
		String bucketName = request.getBucketName();

		// -> we need to look up the final bucket to figure out which mount point to use to save the part in
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if (bucket == null) {
			logger.error( "saveUploadedPart failed since " + bucketName + " does not exist" );
			response.setResultCode(404);
		}
		S3PolicyContext context = new S3PolicyContext( PolicyActions.PutObject, bucketName );
		context.setKeyName( request.getKey());
		verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE );
		
<<<<<<< HEAD
		Tuple<SHost, String> tupleBucketHost = getBucketStorageHost(bucket);		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleBucketHost.getFirst());
=======
		OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost(bucket);		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
>>>>>>> 6472e7b... Now really adding the renamed files!
		String itemFileName = new String( uploadId + "-" + partNumber );
		InputStream is = null;

		try {
			is = request.getDataInputStream();
<<<<<<< HEAD
			String md5Checksum = bucketAdapter.saveObject(is, tupleBucketHost.getSecond(), ServiceProvider.getInstance().getMultipartDir(), itemFileName);
=======
			String md5Checksum = bucketAdapter.saveObject(is, host_storagelocation_pair.getSecond(), ServiceProvider.getInstance().getMultipartDir(), itemFileName);
>>>>>>> 6472e7b... Now really adding the renamed files!
			response.setETag(md5Checksum);	
			
    	    MultipartLoadDao uploadDao = new MultipartLoadDao();
    	    uploadDao.savePart( uploadId, partNumber, md5Checksum, itemFileName, (int)request.getContentLength());
        	response.setResultCode(200);

		} catch (IOException e) {
			logger.error("UploadPart failed due to " + e.getMessage(), e);
			response.setResultCode(500);
		} catch (OutOfStorageException e) {
			logger.error("UploadPart failed due to " + e.getMessage(), e);
			response.setResultCode(500);
		} catch (Exception e) {
			logger.error("UploadPart failed due to " + e.getMessage(), e);	
			response.setResultCode(500);
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("UploadPart unable to close stream from data handler.", e);
				}
			}
		}
    	
		return response;
    }
    
    /**
     * Create the real object represented by all the parts of the multipart upload.       
<<<<<<< HEAD
     * 
     * @param httpResp - servelet response handle to return the headers of the response (including version header) 
     * @param request - normal parameters need to create a new object (e.g., meta data)
     * @param parts - list of files that make up the multipart
     * @param os - response outputstream, this function can take a long time and we are required to
     *        keep the connection alive by returning whitespace characters back periodically.
     * @return
     * @throws IOException 
     */
    public S3PutObjectInlineResponse concatentateMultipartUploads(HttpServletResponse httpResp, S3PutObjectInlineRequest request, S3MultipartPart[] parts, OutputStream os) throws IOException
=======
     * Called from S3ObjectAction at completion of multipart upload.
     * @param httpResp - Servlet response handle to return the headers of the response (including version header) 
     * @param request - Normal parameters needed to create a new object (including metadata)
     * @param parts - List of files that make up the multipart
     * @param outputStream - Response output stream
     * N.B. - This method can be long-lasting 
     * We are required to keep the connection alive by returning whitespace characters back periodically.
     */
    
    public S3PutObjectInlineResponse concatentateMultipartUploads(HttpServletResponse httpResp, S3PutObjectInlineRequest request, S3MultipartPart[] parts, OutputStream outputStream) throws IOException
>>>>>>> 6472e7b... Now really adding the renamed files!
    {
    	// [A] Set up and initial error checking
    	S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();	
		String bucketName = request.getBucketName();
		String key = request.getKey();
		S3MetaDataEntry[] meta = request.getMetaEntries();
		
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if (bucket == null) {
			logger.error( "completeMultipartUpload( failed since " + bucketName + " does not exist" );
			response.setResultCode(404);
		}		

		// [B] Now we need to create the final re-assembled object
		//  -> the allocObjectItem checks for the bucket policy PutObject permissions
<<<<<<< HEAD
		Tuple<SObject, SObjectItem> tupleObjectItem = allocObjectItem(bucket, key, meta, null, request.getCannedAccess());
		Tuple<SHost, String> tupleBucketHost = getBucketStorageHost(bucket);		
		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleBucketHost.getFirst());
		String itemFileName = tupleObjectItem.getSecond().getStoredPath();
=======
		OrderedPair<SObject, SObjectItem> object_objectitem_pair = allocObjectItem(bucket, key, meta, null, request.getCannedAccess());
		OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost(bucket);		
		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
		String itemFileName = object_objectitem_pair.getSecond().getStoredPath();
>>>>>>> 6472e7b... Now really adding the renamed files!
		
		// -> Amazon defines that we must return a 200 response immediately to the client, but
		// -> we don't know the version header until we hit here
		httpResp.setStatus(200);
	    httpResp.setContentType("text/xml; charset=UTF-8");
<<<<<<< HEAD
		String version = tupleObjectItem.getSecond().getVersion();
=======
		String version = object_objectitem_pair.getSecond().getVersion();
>>>>>>> 6472e7b... Now really adding the renamed files!
		if (null != version) httpResp.addHeader( "x-amz-version-id", version );	
        httpResp.flushBuffer();
		

        // [C] Re-assemble the object from its uploaded file parts
		try {
			// explicit transaction control to avoid holding transaction during long file concatenation process
			PersistContext.commitTransaction();
			
<<<<<<< HEAD
			Tuple<String, Long> result = bucketAdapter.concatentateObjects( tupleBucketHost.getSecond(), bucket.getName(), itemFileName, ServiceProvider.getInstance().getMultipartDir(), parts, os );
			response.setETag(result.getFirst());
			response.setLastModified(DateHelper.toCalendar( tupleObjectItem.getSecond().getLastModifiedTime()));
		
			SObjectItemDao itemDao = new SObjectItemDao();
			SObjectItem item = itemDao.get( tupleObjectItem.getSecond().getId());
=======
			OrderedPair<String, Long> result = bucketAdapter.
					concatentateObjects
					( host_storagelocation_pair.getSecond(), 
							bucket.getName(), 
							itemFileName, 
							ServiceProvider.getInstance().getMultipartDir(), 
							parts, 
							outputStream );
			response.setETag(result.getFirst());
			response.setLastModified(DateHelper.toCalendar( object_objectitem_pair.getSecond().getLastModifiedTime()));
		
			SObjectItemDao itemDao = new SObjectItemDao();
			SObjectItem item = itemDao.get( object_objectitem_pair.getSecond().getId());
>>>>>>> 6472e7b... Now really adding the renamed files!
			item.setMd5(result.getFirst());
			item.setStoredSize(result.getSecond().longValue());
			response.setResultCode(200);

			PersistContext.getSession().save(item);			
		} 
		catch (Exception e) {
			logger.error("completeMultipartUpload failed due to " + e.getMessage(), e);		
		}
    	return response;	
    }
    
<<<<<<< HEAD
=======
    /**
     * Return a S3PutObjectInlineResponse which represents an object being created into a bucket      
     * Called from S3ObjectAction when PUTting or POTing an object.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3PutObjectInlineResponse handleRequest(S3PutObjectInlineRequest request) 
    {
    	S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();	
		String bucketName = request.getBucketName();
		String key = request.getKey();
		long contentLength = request.getContentLength();
		S3MetaDataEntry[] meta = request.getMetaEntries();
		S3AccessControlList acl = request.getAcl();
		
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if (bucket == null) throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");
		

<<<<<<< HEAD
		// -> is the caller allowed to write the object?
		//  -> the allocObjectItem checks for the bucket policy PutObject permissions
		Tuple<SObject, SObjectItem> tupleObjectItem = allocObjectItem(bucket, key, meta, acl, request.getCannedAccess());
		Tuple<SHost, String> tupleBucketHost = getBucketStorageHost(bucket);		
		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleBucketHost.getFirst());
		String itemFileName = tupleObjectItem.getSecond().getStoredPath();
=======
		// Is the caller allowed to write the object?
		// The allocObjectItem checks for the bucket policy PutObject permissions
		OrderedPair<SObject, SObjectItem> object_objectitem_pair = allocObjectItem(bucket, key, meta, acl, request.getCannedAccess());
		OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost(bucket);		
		
		S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
		String itemFileName = object_objectitem_pair.getSecond().getStoredPath();
>>>>>>> 6472e7b... Now really adding the renamed files!
		InputStream is = null;

		try {
			// explicit transaction control to avoid holding transaction during file-copy process
			PersistContext.commitTransaction();
			
			is = request.getDataInputStream();
<<<<<<< HEAD
			String md5Checksum = bucketAdapter.saveObject(is, tupleBucketHost.getSecond(), bucket.getName(), itemFileName);
			response.setETag(md5Checksum);
			response.setLastModified(DateHelper.toCalendar( tupleObjectItem.getSecond().getLastModifiedTime()));
	        response.setVersion( tupleObjectItem.getSecond().getVersion());
		
			SObjectItemDao itemDao = new SObjectItemDao();
			SObjectItem item = itemDao.get( tupleObjectItem.getSecond().getId());
=======
			String md5Checksum = bucketAdapter.saveObject(is, host_storagelocation_pair.getSecond(), bucket.getName(), itemFileName);
			response.setETag(md5Checksum);
			response.setLastModified(DateHelper.toCalendar( object_objectitem_pair.getSecond().getLastModifiedTime()));
	        response.setVersion( object_objectitem_pair.getSecond().getVersion());
		
			SObjectItemDao itemDao = new SObjectItemDao();
			SObjectItem item = itemDao.get( object_objectitem_pair.getSecond().getId());
>>>>>>> 6472e7b... Now really adding the renamed files!
			item.setMd5(md5Checksum);
			item.setStoredSize(contentLength);
			PersistContext.getSession().save(item);
			
		} catch (IOException e) {
			logger.error("PutObjectInline failed due to " + e.getMessage(), e);
		} catch (OutOfStorageException e) {
			logger.error("PutObjectInline failed due to " + e.getMessage(), e);
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("PutObjectInline unable to close stream from data handler.", e);
				}
			}
		}
    	
    	return response;
    }
<<<<<<< HEAD
=======
    
    /**
     * Return a S3PutObjectResponse which represents an object being created into a bucket      
     * Called from S3RestServlet when processing a DIME request.
     */
>>>>>>> 6472e7b... Now really adding the renamed files!

    public S3PutObjectResponse handleRequest(S3PutObjectRequest request)  
    {
    	S3PutObjectResponse response = new S3PutObjectResponse();	
		String bucketName = request.getBucketName();
		String key = request.getKey();
		long contentLength = request.getContentLength();
		S3MetaDataEntry[] meta = request.getMetaEntries();
		S3AccessControlList acl = request.getAcl();
		
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if(bucket == null) throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");
		
<<<<<<< HEAD
		// -> is the caller allowed to write the object?	
		//  -> the allocObjectItem checks for the bucket policy PutObject permissions
		Tuple<SObject, SObjectItem> tupleObjectItem = allocObjectItem(bucket, key, meta, acl, null);
		Tuple<SHost, String> tupleBucketHost = getBucketStorageHost(bucket);
    	
		S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter(tupleBucketHost.getFirst());
		String itemFileName = tupleObjectItem.getSecond().getStoredPath();
=======
		// Is the caller allowed to write the object?	
		// The allocObjectItem checks for the bucket policy PutObject permissions
		OrderedPair<SObject, SObjectItem> object_objectitem_pair = allocObjectItem(bucket, key, meta, acl, null);
		OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost(bucket);
    	
		S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
		String itemFileName = object_objectitem_pair.getSecond().getStoredPath();
>>>>>>> 6472e7b... Now really adding the renamed files!
		InputStream is = null;
		try {
			// explicit transaction control to avoid holding transaction during file-copy process
			PersistContext.commitTransaction();
			
			is = request.getInputStream();
<<<<<<< HEAD
			String md5Checksum = bucketAdapter.saveObject(is, tupleBucketHost.getSecond(), bucket.getName(), itemFileName);
			response.setETag(md5Checksum);
			response.setLastModified(DateHelper.toCalendar( tupleObjectItem.getSecond().getLastModifiedTime()));
			
			SObjectItemDao itemDao = new SObjectItemDao();
			SObjectItem item = itemDao.get( tupleObjectItem.getSecond().getId());
=======
			String md5Checksum = bucketAdapter.saveObject(is, host_storagelocation_pair.getSecond(), bucket.getName(), itemFileName);
			response.setETag(md5Checksum);
			response.setLastModified(DateHelper.toCalendar( object_objectitem_pair.getSecond().getLastModifiedTime()));
			
			SObjectItemDao itemDao = new SObjectItemDao();
			SObjectItem item = itemDao.get( object_objectitem_pair.getSecond().getId());
>>>>>>> 6472e7b... Now really adding the renamed files!
			item.setMd5(md5Checksum);
			item.setStoredSize(contentLength);
			PersistContext.getSession().save(item);
			
		} catch (OutOfStorageException e) {
			logger.error("PutObject failed due to " + e.getMessage(), e);
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Unable to close stream from data handler.", e);
				}
			}
		}
    	
    	return response;
    }

    /**
     * The ACL of an object is set at the object version level. By default, PUT sets the ACL of the latest 
<<<<<<< HEAD
     * version of an object. To set the ACL of a different version, use the versionId subresource. 
     * 
     * @param request
     * @return
     */
=======
     * version of an object. To set the ACL of a different version, using the versionId subresource. 
     * Called from S3ObjectAction to PUT an object's ACL.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3Response handleRequest(S3SetObjectAccessControlPolicyRequest request) 
    {
    	S3PolicyContext context = null;
    	
    	// [A] First find the object in the bucket
    	S3Response response  = new S3Response(); 	
    	SBucketDao bucketDao = new SBucketDao();
    	String bucketName = request.getBucketName();
    	SBucket sbucket = bucketDao.getByName( bucketName );
    	if(sbucket == null) {
    		response.setResultCode(404);
    		response.setResultDescription("Bucket " + bucketName + "does not exist");
    		return response;
    	}
    	
    	SObjectDao sobjectDao = new SObjectDao();
    	String nameKey = request.getKey();
    	SObject sobject = sobjectDao.getByNameKey( sbucket, nameKey );   	
    	if(sobject == null) {
    		response.setResultCode(404);
    		response.setResultDescription("Object " + request.getKey() + " in bucket " + bucketName + " does not exist");
    		return response;
    	}
    	
		String deletionMark = sobject.getDeletionMark();
		if (null != deletionMark) {
			response.setResultCode(404);
			response.setResultDescription("Object " + request.getKey() + " has been deleted (1)");
			return response;
		}
		

		// [B] Versioning allow the client to ask for a specific version not just the latest
		SObjectItem item = null;
        int versioningStatus = sbucket.getVersioningStatus();
		String wantVersion = request.getVersion();
		if ( SBucket.VERSIONING_ENABLED == versioningStatus && null != wantVersion)
			 item = sobject.getVersion( wantVersion );
		else item = sobject.getLatestVersion(( SBucket.VERSIONING_ENABLED != versioningStatus ));    
    	
		if (item == null) {
    		response.setResultCode(404);
			response.setResultDescription("Object " + request.getKey() + " has been deleted (2)");
			return response;  		
    	}

		if ( SBucket.VERSIONING_ENABLED == versioningStatus ) {
			 context = new S3PolicyContext( PolicyActions.PutObjectAclVersion, bucketName );
			 context.setEvalParam( ConditionKeys.VersionId, wantVersion );
			 response.setVersion( item.getVersion());
		}
		else context = new S3PolicyContext( PolicyActions.PutObjectAcl, bucketName );		
		context.setKeyName( nameKey );
		verifyAccess( context, "SObjectItem", item.getId(), SAcl.PERMISSION_WRITE_ACL );		

		// -> the acl always goes on the instance of the object
    	SAclDao aclDao = new SAclDao();
    	aclDao.save("SObjectItem", item.getId(), request.getAcl());
    	
    	response.setResultCode(200);
    	response.setResultDescription("OK");
    	return response;
    }
    
    /**
     * By default, GET returns ACL information about the latest version of an object. To return ACL 
     * information about a different version, use the versionId subresource
<<<<<<< HEAD
     * 
     * @param request
     * @return
     */
=======
     * Called from S3ObjectAction to get an object's ACL.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3AccessControlPolicy handleRequest(S3GetObjectAccessControlPolicyRequest request) 
    {
    	S3PolicyContext context = null;

    	// [A] Does the object exist that holds the ACL we are looking for?
    	S3AccessControlPolicy policy = new S3AccessControlPolicy();	
    	SBucketDao bucketDao = new SBucketDao();
    	String bucketName = request.getBucketName();
    	SBucket sbucket = bucketDao.getByName( bucketName );
    	if (sbucket == null)
    		throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");
    	
    	SObjectDao sobjectDao = new SObjectDao();
    	String nameKey = request.getKey();
    	SObject sobject = sobjectDao.getByNameKey( sbucket, nameKey );
    	if (sobject == null)
    		throw new NoSuchObjectException("Object " + request.getKey() + " does not exist");
    		
		String deletionMark = sobject.getDeletionMark();
		if (null != deletionMark) {
			policy.setResultCode(404);
			policy.setResultDescription("Object " + request.getKey() + " has been deleted (1)");
			return policy;
		}
		

		// [B] Versioning allow the client to ask for a specific version not just the latest
		SObjectItem item = null;
        int versioningStatus = sbucket.getVersioningStatus();
		String wantVersion = request.getVersion();
		if ( SBucket.VERSIONING_ENABLED == versioningStatus && null != wantVersion)
			 item = sobject.getVersion( wantVersion );
		else item = sobject.getLatestVersion(( SBucket.VERSIONING_ENABLED != versioningStatus ));    
    	
		if (item == null) {
    		policy.setResultCode(404);
			policy.setResultDescription("Object " + request.getKey() + " has been deleted (2)");
			return policy;  		
    	}

		if ( SBucket.VERSIONING_ENABLED == versioningStatus ) {
			 context = new S3PolicyContext( PolicyActions.GetObjectVersionAcl, bucketName );
			 context.setEvalParam( ConditionKeys.VersionId, wantVersion );
			 policy.setVersion( item.getVersion());
		}
		else context = new S3PolicyContext( PolicyActions.GetObjectAcl, bucketName );		
		context.setKeyName( nameKey );
		verifyAccess( context, "SObjectItem", item.getId(), SAcl.PERMISSION_READ_ACL );		

    	
        // [C] ACLs are ALWAYS on an instance of the object
    	S3CanonicalUser owner = new S3CanonicalUser();
    	owner.setID(sobject.getOwnerCanonicalId());
    	owner.setDisplayName("");
    	policy.setOwner(owner);
		policy.setResultCode(200);
   	
    	SAclDao aclDao = new SAclDao();
    	List<SAcl> grants = aclDao.listGrants( "SObjectItem", item.getId());
    	policy.setGrants(S3Grant.toGrants(grants));    
    	return policy;
    }
    
    /**
<<<<<<< HEAD
     * Implements both GetObject and GetObjectExtended.
     * 
     * @param request
     * @return
     */
=======
     * Handle requests for GET object and HEAD "get object extended"
     * Called from S3ObjectAction for GET and HEAD of an object.
     */
    
>>>>>>> 6472e7b... Now really adding the renamed files!
    public S3GetObjectResponse handleRequest(S3GetObjectRequest request) 
    {
    	S3GetObjectResponse response = new S3GetObjectResponse();
    	S3PolicyContext context = null;
    	boolean ifRange = false;
		long bytesStart = request.getByteRangeStart();
		long bytesEnd   = request.getByteRangeEnd();
    	int resultCode  = 200;

    	// [A] Verify that the bucket and the object exist
		SBucketDao bucketDao = new SBucketDao();
		String bucketName = request.getBucketName();
		SBucket sbucket = bucketDao.getByName(bucketName);
		if (sbucket == null) {
			response.setResultCode(404);
			response.setResultDescription("Bucket " + request.getBucketName() + " does not exist");
			return response;
		}

		SObjectDao objectDao = new SObjectDao();
		String nameKey = request.getKey();
		SObject sobject = objectDao.getByNameKey( sbucket, nameKey );
		if (sobject == null) {
			response.setResultCode(404);
			response.setResultDescription("Object " + request.getKey() + " does not exist in bucket " + request.getBucketName());
			return response;
		}
		
		String deletionMark = sobject.getDeletionMark();
		if (null != deletionMark) {
		    response.setDeleteMarker( deletionMark );
			response.setResultCode(404);
			response.setResultDescription("Object " + request.getKey() + " has been deleted (1)");
			return response;
		}
		

		// [B] Versioning allow the client to ask for a specific version not just the latest
		SObjectItem item = null;
        int versioningStatus = sbucket.getVersioningStatus();
		String wantVersion = request.getVersion();
		if ( SBucket.VERSIONING_ENABLED == versioningStatus && null != wantVersion) 			
			 item = sobject.getVersion( wantVersion );
		else item = sobject.getLatestVersion(( SBucket.VERSIONING_ENABLED != versioningStatus ));    
    	
		if (item == null) {
    		response.setResultCode(404);
			response.setResultDescription("Object " + request.getKey() + " has been deleted (2)");
			return response;  		
    	}
		
		if ( SBucket.VERSIONING_ENABLED == versioningStatus ) {
			 context = new S3PolicyContext( PolicyActions.GetObjectVersion, bucketName );
			 context.setEvalParam( ConditionKeys.VersionId, wantVersion );
		}
		else context = new S3PolicyContext( PolicyActions.GetObject, bucketName );		
 		context.setKeyName( nameKey );
		verifyAccess( context, "SObjectItem", item.getId(), SAcl.PERMISSION_READ );		
		
			
	    // [C] Handle all the IFModifiedSince ... conditions, and access privileges
		// -> http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.27 (HTTP If-Range header)
		if (request.isReturnCompleteObjectOnConditionFailure() && (0 <= bytesStart && 0 <= bytesEnd)) ifRange = true;

		resultCode = conditionPassed( request.getConditions(), item.getLastModifiedTime(), item.getMd5(), ifRange );
	    if ( -1 == resultCode ) {
	    	// -> If-Range implementation, we have to return the entire object
	    	resultCode = 200;
	    	bytesStart = -1;
	    	bytesEnd = -1;
	    }
	    else if (200 != resultCode) {
			response.setResultCode( resultCode );
	    	response.setResultDescription( "Precondition Failed" );			
			return response;
		}


		// [D] Return the contents of the object inline	
		// -> extract the meta data that corresponds the specific versioned item 
		SMetaDao metaDao = new SMetaDao();
		List<SMeta> itemMetaData = metaDao.getByTarget( "SObjectItem", item.getId());
		if (null != itemMetaData) 
		{
			int i = 0;
			S3MetaDataEntry[] metaEntries = new S3MetaDataEntry[ itemMetaData.size() ];
<<<<<<< HEAD
		    ListIterator it = itemMetaData.listIterator();
=======
		    ListIterator<SMeta> it = itemMetaData.listIterator();
>>>>>>> 6472e7b... Now really adding the renamed files!
		    while( it.hasNext()) {
		    	SMeta oneTag = (SMeta)it.next();
		    	S3MetaDataEntry oneEntry = new S3MetaDataEntry();
		    	oneEntry.setName( oneTag.getName());
		    	oneEntry.setValue( oneTag.getValue());
		    	metaEntries[i++] = oneEntry;
		    }
		    response.setMetaEntries( metaEntries );
		}
		    
		//  -> support a single byte range
		if ( 0 <= bytesStart && 0 <= bytesEnd ) {
		     response.setContentLength( bytesEnd - bytesStart );	
		     resultCode = 206;
		}
		else response.setContentLength( item.getStoredSize());
 
    	if(request.isReturnData()) 
    	{
    		response.setETag(item.getMd5());
    		response.setLastModified(DateHelper.toCalendar( item.getLastModifiedTime()));
    		response.setVersion( item.getVersion());
    		if (request.isInlineData()) 
    		{
<<<<<<< HEAD
    			Tuple<SHost, String> tupleSHostInfo = getBucketStorageHost(sbucket);
=======
    			OrderedPair<SHost, String> tupleSHostInfo = getBucketStorageHost(sbucket);
>>>>>>> 6472e7b... Now really adding the renamed files!
				S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleSHostInfo.getFirst());
				
				if ( 0 <= bytesStart && 0 <= bytesEnd )
					 response.setData(bucketAdapter.loadObjectRange(tupleSHostInfo.getSecond(), 
						   request.getBucketName(), item.getStoredPath(), bytesStart, bytesEnd ));
				else response.setData(bucketAdapter.loadObject(tupleSHostInfo.getSecond(), request.getBucketName(), item.getStoredPath()));
    		} 
    	}
    	
    	response.setResultCode( resultCode );
    	response.setResultDescription("OK");
    	return response;
    }
    
    /**
<<<<<<< HEAD
     * In one place we handle both versioning and non-versioning delete requests.
     */
	public S3Response handleRequest(S3DeleteObjectRequest request) 
	{		
		// -> verify that the bucket and object exist
=======
     * Handle object deletion requests, both versioning and non-versioning requirements.
     * Called from S3ObjectAction for deletion.
     */
	public S3Response handleRequest(S3DeleteObjectRequest request) 
	{		
		// Verify that the bucket and object exist
>>>>>>> 6472e7b... Now really adding the renamed files!
		S3Response response  = new S3Response();
		SBucketDao bucketDao = new SBucketDao();
		String bucketName = request.getBucketName();
		SBucket sbucket = bucketDao.getByName( bucketName );
		if (sbucket == null) {
			response.setResultCode(404);
<<<<<<< HEAD
			response.setResultDescription("Bucket does not exist");
=======
			response.setResultDescription("Bucket " + bucketName + " does not exist");
>>>>>>> 6472e7b... Now really adding the renamed files!
			return response;
		}
		
		SObjectDao objectDao = new SObjectDao();
		String nameKey = request.getKey();
		SObject sobject = objectDao.getByNameKey( sbucket, nameKey );
		if (sobject == null) {
			response.setResultCode(404);
<<<<<<< HEAD
			response.setResultDescription("Bucket does not exist");
=======
			response.setResultDescription("No object with key " +  nameKey + " exists in bucket " + bucketName);
>>>>>>> 6472e7b... Now really adding the renamed files!
			return response;
		}
		
				
<<<<<<< HEAD
		// -> versioning controls what delete means
=======
		// Discover whether versioning is enabled.  If so versioning requires the setting of a deletion marker.
>>>>>>> 6472e7b... Now really adding the renamed files!
		String storedPath = null;
		SObjectItem item = null;
        int versioningStatus = sbucket.getVersioningStatus();
		if ( SBucket.VERSIONING_ENABLED == versioningStatus ) 
	    {
			 String wantVersion = request.getVersion();
			 S3PolicyContext context = new S3PolicyContext( PolicyActions.DeleteObjectVersion, bucketName );
			 context.setKeyName( nameKey );
			 context.setEvalParam( ConditionKeys.VersionId, wantVersion );
			 verifyAccess( context, "SBucket", sbucket.getId(), SAcl.PERMISSION_WRITE );

			 if (null == wantVersion) {
<<<<<<< HEAD
				 // -> if versioning is on and no versionId is given then we just write a deletion marker
=======
				 // If versioning is on and no versionId is given then we just write a deletion marker
>>>>>>> 6472e7b... Now really adding the renamed files!
				 sobject.setDeletionMark( UUID.randomUUID().toString());
				 objectDao.update( sobject );
			 }
			 else {	
<<<<<<< HEAD
				  // -> are we removing the delete marker?
=======
				  // Otherwise remove the deletion marker if this has been set
>>>>>>> 6472e7b... Now really adding the renamed files!
				  String deletionMarker = sobject.getDeletionMark();
				  if (null != deletionMarker && wantVersion.equalsIgnoreCase( deletionMarker )) {
					  sobject.setDeletionMark( null );  
			    	  objectDao.update( sobject );	
			  		  response.setResultCode(204);
					  return response;
	              }
				  
<<<<<<< HEAD
				  // -> if versioning is on and the versionId is given then we delete the object matching that version
=======
				  // If versioning is on and the versionId is given (non-null) then delete the object matching that version
>>>>>>> 6472e7b... Now really adding the renamed files!
			      if ( null == (item = sobject.getVersion( wantVersion ))) {
			    	   response.setResultCode(404);
			    	   return response;
			      }
			      else {
<<<<<<< HEAD
			    	   // -> just delete the one item that matches the versionId from the database
=======
			    	   // Providing versionId is non-null, then just delete the one item that matches the versionId from the database
>>>>>>> 6472e7b... Now really adding the renamed files!
			    	   storedPath = item.getStoredPath();
			    	   sobject.deleteItem( item.getId());
			    	   objectDao.update( sobject );	    	   
			      }
			 }
	    }
		else 
<<<<<<< HEAD
		{	 // -> if versioning is off then we do delete the null object
=======
		{	 // If versioning is off then we do delete the null object
>>>>>>> 6472e7b... Now really adding the renamed files!
			 S3PolicyContext context = new S3PolicyContext( PolicyActions.DeleteObject, bucketName );
			 context.setKeyName( nameKey );
			 verifyAccess( context, "SBucket", sbucket.getId(), SAcl.PERMISSION_WRITE );

			 if ( null == (item = sobject.getLatestVersion( true ))) {
		    	  response.setResultCode(404);
		    	  return response;
		     }
		     else {
<<<<<<< HEAD
		    	  // -> if no item with a null version then we are done
		    	  if (null == item.getVersion()) {
		    	      // -> remove the entire object 
		    	      // -> cascade-deleting can delete related SObject/SObjectItem objects, but not SAcl and SMeta objects.
=======
		    	  // If there is no item with a null version then we are done
		    	  if (null == item.getVersion()) {
		    	      // Otherwiswe remove the entire object 
		    	      // Cascade-deleting can delete related SObject/SObjectItem objects, but not SAcl and SMeta objects.
>>>>>>> 6472e7b... Now really adding the renamed files!
		    	      storedPath = item.getStoredPath();
		    		  deleteMetaData( item.getId());
		    		  deleteObjectAcls( "SObjectItem", item.getId());
		    	      objectDao.delete( sobject );
		    	  }
		     }
		}
		
<<<<<<< HEAD
		// -> delete the file holding the object
		if (null != storedPath) 
		{
			 Tuple<SHost, String> tupleBucketHost = getBucketStorageHost( sbucket );
			 S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter( tupleBucketHost.getFirst());		 
			 bucketAdapter.deleteObject( tupleBucketHost.getSecond(), bucketName, storedPath );		
=======
		// Delete the file holding the object
		if (null != storedPath) 
		{
			 OrderedPair<SHost, String> host_storagelocation_pair = getBucketStorageHost( sbucket );
			 S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter( host_storagelocation_pair.getFirst());		 
			 bucketAdapter.deleteObject( host_storagelocation_pair.getSecond(), bucketName, storedPath );		
>>>>>>> 6472e7b... Now really adding the renamed files!
		}
		
		response.setResultCode(204);
		return response;
    }
    

	private void deleteMetaData( long itemId ) {
	    SMetaDao metaDao = new SMetaDao();
	    List<SMeta> itemMetaData = metaDao.getByTarget( "SObjectItem", itemId );
	    if (null != itemMetaData) 
	    {
<<<<<<< HEAD
	        ListIterator it = itemMetaData.listIterator();
=======
	        ListIterator<SMeta> it = itemMetaData.listIterator();
>>>>>>> 6472e7b... Now really adding the renamed files!
		    while( it.hasNext()) {
		       SMeta oneTag = (SMeta)it.next();
		       metaDao.delete( oneTag );
		    }
		}
	}

	private void deleteObjectAcls( String target, long itemId ) {
	    SAclDao aclDao = new SAclDao();
	    List<SAcl> itemAclData = aclDao.listGrants( target, itemId );
	    if (null != itemAclData) 
	    {
<<<<<<< HEAD
	        ListIterator it = itemAclData.listIterator();
=======
	        ListIterator<SAcl> it = itemAclData.listIterator();
>>>>>>> 6472e7b... Now really adding the renamed files!
		    while( it.hasNext()) {
		       SAcl oneTag = (SAcl)it.next();
		       aclDao.delete( oneTag );
		    }
		}
	}

	private void deleteBucketAcls( long bucketId ) {
	    SAclDao aclDao = new SAclDao();
	    List<SAcl> bucketAclData = aclDao.listGrants( "SBucket", bucketId );
	    if (null != bucketAclData) 
	    {
<<<<<<< HEAD
	        ListIterator it = bucketAclData.listIterator();
=======
	        ListIterator<SAcl> it = bucketAclData.listIterator();
>>>>>>> 6472e7b... Now really adding the renamed files!
		    while( it.hasNext()) {
		       SAcl oneTag = (SAcl)it.next();
		       aclDao.delete( oneTag );
		    }
		}
	}
	
	private S3ListBucketPrefixEntry[] composeListBucketPrefixEntries(List<SObject> l, String prefix, String delimiter, int maxKeys) 
	{
		List<S3ListBucketPrefixEntry> entries = new ArrayList<S3ListBucketPrefixEntry>();		
		int count = 0;
		
		for(SObject sobject : l) 
		{
			if(delimiter != null && !delimiter.isEmpty()) 
			{
				String subName = StringHelper.substringInBetween(sobject.getNameKey(), prefix, delimiter);
				if(subName != null) 
				{
					S3ListBucketPrefixEntry entry = new S3ListBucketPrefixEntry();
					if ( prefix != null && prefix.length() > 0)
						 entry.setPrefix(prefix + delimiter + subName);
					else entry.setPrefix(subName);
				}
			}		
			count++;
			if(count >= maxKeys) break;
		}
		
		if(entries.size() > 0) return entries.toArray(new S3ListBucketPrefixEntry[0]);
		return null;
	}
	
	/**
	 * The 'versionIdMarker' parameter only makes sense if enableVersion is true.   
	 * versionIdMarker is the starting point to return information back.  So for example if an
	 * object has versions 1,2,3,4,5 and the versionIdMarker is '3', then 3,4,5 will be returned
	 * by this function.   If the versionIdMarker is null then all versions are returned.
	 * 
	 * TODO - how does the versionIdMarker work when there is a deletion marker in the object?
	 */
	private S3ListBucketObjectEntry[] composeListBucketContentEntries(List<SObject> l, String prefix, String delimiter, int maxKeys, boolean enableVersion, String versionIdMarker) 
	{
		List<S3ListBucketObjectEntry> entries = new ArrayList<S3ListBucketObjectEntry>();
		SObjectItem latest  = null;
		boolean hitIdMarker = false;
		int count = 0;
		
		for( SObject sobject : l ) 
		{
			if (delimiter != null && !delimiter.isEmpty()) 
			{
				if (StringHelper.substringInBetween(sobject.getNameKey(), prefix, delimiter) != null)
					continue;
			}
			
			if (enableVersion) 
			{
				hitIdMarker = (null == versionIdMarker ? true : false);

<<<<<<< HEAD
				// -> this supports the REST call GET /?versions
				String deletionMarker = sobject.getDeletionMark();
                if ( null != deletionMarker ) 
                {
                	 // -> TODO we don't save the timestamp when something is deleted
=======
				// This supports GET REST calls with /?versions
				String deletionMarker = sobject.getDeletionMark();
                if ( null != deletionMarker ) 
                {
                	 // TODO we should also save the timestamp when something is deleted
>>>>>>> 6472e7b... Now really adding the renamed files!
                	 S3ListBucketObjectEntry entry = new S3ListBucketObjectEntry();
            		 entry.setKey(sobject.getNameKey());
            		 entry.setVersion( deletionMarker );
            		 entry.setIsLatest( true );
            		 entry.setIsDeletionMarker( true );
            		 entry.setLastModified( Calendar.getInstance( TimeZone.getTimeZone("GMT") ));
            		 entry.setOwnerCanonicalId(sobject.getOwnerCanonicalId());
            		 entry.setOwnerDisplayName("");
            		 entries.add( entry );
            		 latest = null;
                }
                else latest = sobject.getLatestVersion( false );
				
				Iterator<SObjectItem> it = sobject.getItems().iterator();
				while( it.hasNext()) 
				{
					SObjectItem item = (SObjectItem)it.next();
					
					if ( !hitIdMarker ) 
					{
						 if (item.getVersion().equalsIgnoreCase( versionIdMarker )) {
							 hitIdMarker = true;
							 entries.add( toListEntry( sobject, item, latest ));
						 }
					}
					else entries.add( toListEntry( sobject, item, latest ));
				}
			} 
			else 
			{   // -> if there are multiple versions of an object then just return its last version
				Iterator<SObjectItem> it = sobject.getItems().iterator();
				SObjectItem lastestItem = null;
				int maxVersion = 0;
				int version = 0;
				while(it.hasNext()) 
				{
					SObjectItem item = (SObjectItem)it.next();
					String versionStr = item.getVersion();
					
					if ( null != versionStr ) 
						 version = Integer.parseInt(item.getVersion());
					else lastestItem = item;
					
					// -> if the bucket has versions turned on
					if (version > maxVersion) {
						maxVersion = version;
						lastestItem = item;
					}
				}
				if (lastestItem != null) {
					entries.add( toListEntry( sobject, lastestItem, null ));
				}
			}
			
			count++;
			if(count >= maxKeys) break;
		}
		
		if ( entries.size() > 0 ) 
			 return entries.toArray(new S3ListBucketObjectEntry[0]);
		else return null;
	}
    
	private static S3ListBucketObjectEntry toListEntry( SObject sobject, SObjectItem item, SObjectItem latest ) 
	{
		S3ListBucketObjectEntry entry = new S3ListBucketObjectEntry();
		entry.setKey(sobject.getNameKey());
		entry.setVersion( item.getVersion());
		entry.setETag( "\"" + item.getMd5() + "\"" );
		entry.setSize(item.getStoredSize());
		entry.setStorageClass( "STANDARD" );
		entry.setLastModified(DateHelper.toCalendar(item.getLastModifiedTime()));
		entry.setOwnerCanonicalId(sobject.getOwnerCanonicalId());
		entry.setOwnerDisplayName("");
		
		if (null != latest && item == latest) entry.setIsLatest( true );
		return entry;
	}
    
<<<<<<< HEAD
	public Tuple<SHost, String> getBucketStorageHost(SBucket bucket) 
=======
	private OrderedPair<SHost, String> getBucketStorageHost(SBucket bucket) 
>>>>>>> 6472e7b... Now really adding the renamed files!
	{
		MHostMountDao mountDao = new MHostMountDao();
		
		SHost shost = bucket.getShost();
		if(shost.getHostType() == SHost.STORAGE_HOST_TYPE_LOCAL) {
<<<<<<< HEAD
			return new Tuple<SHost, String>(shost, shost.getExportRoot());
=======
			return new OrderedPair<SHost, String>(shost, shost.getExportRoot());
>>>>>>> 6472e7b... Now really adding the renamed files!
		}
		
		MHostMount mount = mountDao.getHostMount(ServiceProvider.getInstance().getManagementHostId(), shost.getId());
		if(mount != null) {
<<<<<<< HEAD
			return new Tuple<SHost, String>(shost, mount.getMountPath());
=======
			return new OrderedPair<SHost, String>(shost, mount.getMountPath());
>>>>>>> 6472e7b... Now really adding the renamed files!
		}

		// need to redirect request to other node
		throw new HostNotMountedException("Storage host " + shost.getHost() + " is not locally mounted");
	}
    
	/**
	 * Locate the folder to hold upload parts at the same mount point as the upload's final bucket
	 * location.   Create the upload folder dynamically.
	 * 
	 * @param bucketName
	 */
	private void createUploadFolder(String bucketName) 
	{
		if (PersistContext.acquireNamedLock("bucket.creation", LOCK_ACQUIRING_TIMEOUT_SECONDS)) 
		{
			try {
			    allocBucketStorageHost(bucketName, ServiceProvider.getInstance().getMultipartDir());
            }
		    finally {
		    	PersistContext.releaseNamedLock("bucket.creation");
		    }
		}
	}
	
	/**
	 * The overrideName is used to create a hidden storage bucket (folder) in the same location
	 * as the given bucketName.   This can be used to create a folder for parts of a multipart
	 * upload for the associated bucket.
	 * 
	 * @param bucketName
	 * @param overrideName
	 * @return
	 */
<<<<<<< HEAD
	private Tuple<SHost, String> allocBucketStorageHost(String bucketName, String overrideName) 
=======
	private OrderedPair<SHost, String> allocBucketStorageHost(String bucketName, String overrideName) 
>>>>>>> 6472e7b... Now really adding the renamed files!
	{
		MHostDao mhostDao = new MHostDao();
		SHostDao shostDao = new SHostDao();
		
		MHost mhost = mhostDao.get(ServiceProvider.getInstance().getManagementHostId());
		if(mhost == null)
			throw new OutOfServiceException("Temporarily out of service");
			
		if(mhost.getMounts().size() > 0) {
			Random random = new Random();
			MHostMount[] mounts = (MHostMount[])mhost.getMounts().toArray();
			MHostMount mount = mounts[random.nextInt(mounts.length)];
			S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter(mount.getShost());
			bucketAdapter.createContainer(mount.getMountPath(), (null != overrideName ? overrideName : bucketName));
<<<<<<< HEAD
			return new Tuple<SHost, String>(mount.getShost(), mount.getMountPath());
		}
		
		// To make things simple, only allow one local mounted storage root
=======
			return new OrderedPair<SHost, String>(mount.getShost(), mount.getMountPath());
		}
		
		// To make things simple, only allow one local mounted storage root TODO - Change in the future
>>>>>>> 6472e7b... Now really adding the renamed files!
		String localStorageRoot = ServiceProvider.getInstance().getStartupProperties().getProperty("storage.root");
		if(localStorageRoot != null) {
			SHost localSHost = shostDao.getLocalStorageHost(mhost.getId(), localStorageRoot);
			if(localSHost == null)
				throw new InternalErrorException("storage.root is configured but not initialized");
			
			S3BucketAdapter bucketAdapter =  getStorageHostBucketAdapter(localSHost);
			bucketAdapter.createContainer(localSHost.getExportRoot(),(null != overrideName ? overrideName : bucketName));
<<<<<<< HEAD
			return new Tuple<SHost, String>(localSHost, localStorageRoot);
=======
			return new OrderedPair<SHost, String>(localSHost, localStorageRoot);
>>>>>>> 6472e7b... Now really adding the renamed files!
		}
		
		throw new OutOfStorageException("No storage host is available");
	}
	
	public S3BucketAdapter getStorageHostBucketAdapter(SHost shost) 
	{
		S3BucketAdapter adapter = bucketAdapters.get(shost.getHostType());
		if(adapter == null) 
			throw new InternalErrorException("Bucket adapter is not installed for host type: " + shost.getHostType());
		
		return adapter;
	}

	/**
	 * If acl is set then the cannedAccessPolicy parameter should be null and is ignored.   
	 * The cannedAccessPolicy parameter is for REST Put requests only where a simple set of ACLs can be
	 * created with a single header value.  Note that we do not currently support "anonymous" un-authenticated 
	 * access in our implementation.
	 * 
	 * @throws IOException 
	 */
	@SuppressWarnings("deprecation")
<<<<<<< HEAD
	public Tuple<SObject, SObjectItem> allocObjectItem(SBucket bucket, String nameKey, S3MetaDataEntry[] meta, S3AccessControlList acl, String cannedAccessPolicy) 
=======
	public OrderedPair<SObject, SObjectItem> allocObjectItem(SBucket bucket, String nameKey, S3MetaDataEntry[] meta, S3AccessControlList acl, String cannedAccessPolicy) 
>>>>>>> 6472e7b... Now really adding the renamed files!
	{
		SObjectDao     objectDao     = new SObjectDao();
		SObjectItemDao objectItemDao = new SObjectItemDao();
		SMetaDao       metaDao       = new SMetaDao();
		SAclDao        aclDao        = new SAclDao();
		SObjectItem    item          = null;
		int            versionSeq    = 1;
		int      versioningStatus    = bucket.getVersioningStatus();
		
		Session session = PersistContext.getSession();
			
		// [A] To write into a bucket the user must have write permission to that bucket
		S3PolicyContext context = new S3PolicyContext( PolicyActions.PutObject, bucket.getName());
		context.setKeyName( nameKey );
		context.setEvalParam( ConditionKeys.Acl, cannedAccessPolicy);
<<<<<<< HEAD
		verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE );

		// [A] If versioning is off them we over write a null object item
=======

		verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE );  // TODO - check this validates plain POSTs

		// [B] If versioning is off them we over write a null object item
>>>>>>> 6472e7b... Now really adding the renamed files!
		SObject object = objectDao.getByNameKey(bucket, nameKey);
		if ( object != null ) 
		{
			 // -> if versioning is on create new object items
			 if ( SBucket.VERSIONING_ENABLED == versioningStatus )
			 {
			      session.lock(object, LockMode.UPGRADE);
			      versionSeq = object.getNextSequence();
			      object.setNextSequence(versionSeq + 1);
		 	      session.save(object);
		 	     
			      item = new SObjectItem();
			      item.setTheObject(object);
			      object.getItems().add(item);
			      item.setVersion(String.valueOf(versionSeq));
			      Date ts = DateHelper.currentGMTTime();
			      item.setCreateTime(ts);
			      item.setLastAccessTime(ts);
			      item.setLastModifiedTime(ts);
			      session.save(item);
			 }
			 else
			 {    // -> find an object item with a null version, can be null
				  //    if bucket started out with versioning enabled and was then suspended
				  item = objectItemDao.getByObjectIdNullVersion( object.getId());
				  if (item == null)
				  {
				      item = new SObjectItem();
				      item.setTheObject(object);
				      object.getItems().add(item);
				      Date ts = DateHelper.currentGMTTime();
				      item.setCreateTime(ts);
				      item.setLastAccessTime(ts);
				      item.setLastModifiedTime(ts);
				      session.save(item);		  
				  }
			 }
		} 
		else 
		{    // -> there is no object nor an object item
			 object = new SObject();
			 object.setBucket(bucket);
			 object.setNameKey(nameKey);
			 object.setNextSequence(2);
			 object.setCreateTime(DateHelper.currentGMTTime());
			 object.setOwnerCanonicalId(UserContext.current().getCanonicalUserId());
			 session.save(object);
		
		     item = new SObjectItem();
		     item.setTheObject(object);
		     object.getItems().add(item);
		     if (SBucket.VERSIONING_ENABLED  == versioningStatus) item.setVersion(String.valueOf(versionSeq));
		     Date ts = DateHelper.currentGMTTime();
		     item.setCreateTime(ts);
		     item.setLastAccessTime(ts);
		     item.setLastModifiedTime(ts);
		     session.save(item);
		}
			
		
		// [C] We will use the item DB id as the file name, MD5/contentLength will be stored later
		String suffix = null;
		int dotPos = nameKey.lastIndexOf('.');
		if (dotPos >= 0) suffix = nameKey.substring(dotPos);
		if ( suffix != null )
			 item.setStoredPath(String.valueOf(item.getId()) + suffix);
		else item.setStoredPath(String.valueOf(item.getId()));
		
		metaDao.save("SObjectItem", item.getId(), meta);
		
		
		// [D] Are we setting an ACL along with the object
		//  -> the ACL is ALWAYS set on a particular instance of the object (i.e., a version)
		if ( null != cannedAccessPolicy )
		{
			 setCannedAccessControls( cannedAccessPolicy, "SObjectItem", item.getId(), bucket ); 
		}
		else if (null == acl || 0 == acl.size()) 
		{
			 // -> this is termed the "private" or default ACL, "Owner gets FULL_CONTROL"
			 setSingleAcl( "SObjectItem", item.getId(), SAcl.PERMISSION_FULL ); 
		}
		else if (null != acl) {
			 aclDao.save( "SObjectItem", item.getId(), acl );
		}
		
		session.update(item);		
<<<<<<< HEAD
		return new Tuple<SObject, SObjectItem>(object, item);
=======
		return new OrderedPair<SObject, SObjectItem>(object, item);
>>>>>>> 6472e7b... Now really adding the renamed files!
	}
	
	
	/**
	 * Access controls that are specified via the "x-amz-acl:" headers in REST requests.
	 * Note that canned policies can be set when the object's contents are set
	 */
<<<<<<< HEAD
	private void setCannedAccessControls( String cannedAccessPolicy, String target, long objectId, SBucket bucket )
	{
	    if ( cannedAccessPolicy.equalsIgnoreCase( "public-read" )) 
	    {
             // -> owner gets FULL_CONTROL  and the anonymous principal (the 'A' symbol here) is granted READ access.
	    	 setDefaultAcls( target, objectId, SAcl.PERMISSION_FULL, SAcl.PERMISSION_READ, "A" );
	    }
	    else if (cannedAccessPolicy.equalsIgnoreCase( "public-read-write" )) 
	    {
	    	 // -> owner gets FULL_CONTROL and the anonymous principal (the 'A' symbol here) is granted READ and WRITE access
	    	 setDefaultAcls( target, objectId, SAcl.PERMISSION_FULL, (SAcl.PERMISSION_READ | SAcl.PERMISSION_WRITE), "A" );
	    }
	    else if (cannedAccessPolicy.equalsIgnoreCase( "authenticated-read" )) 
	    {
		     // -> Owner gets FULL_CONTROL and ANY principal authenticated as a registered S3 user (the '*' symbol here) is granted READ access
	    	 setDefaultAcls( target, objectId, SAcl.PERMISSION_FULL, SAcl.PERMISSION_READ, "*" );
	    }
	    else if (cannedAccessPolicy.equalsIgnoreCase( "private" )) 
	    {
		     // -> this is termed the "private" or default ACL, "Owner gets FULL_CONTROL"
		     setSingleAcl( target, objectId, SAcl.PERMISSION_FULL ); 
	    }
	    else if (cannedAccessPolicy.equalsIgnoreCase( "bucket-owner-read" )) 
	    {
	    	 // -> Object Owner gets FULL_CONTROL, Bucket Owner gets READ
	    	 // -> is equivalent to private when used with PUT Bucket
	    	 if ( target.equalsIgnoreCase( "SBucket" ))  
			      setSingleAcl( target, objectId, SAcl.PERMISSION_FULL );     		 
	    	 else setDefaultAcls( target, objectId, SAcl.PERMISSION_FULL, SAcl.PERMISSION_READ, bucket.getOwnerCanonicalId());
	    }
	    else if (cannedAccessPolicy.equalsIgnoreCase( "bucket-owner-full-control" )) 
	    {
	    	 // -> Object Owner gets FULL_CONTROL, Bucket Owner gets FULL_CONTROL
	    	 // -> is equivalent to private when used with PUT Bucket
	    	 if ( target.equalsIgnoreCase( "SBucket" ))   
			      setSingleAcl( target, objectId, SAcl.PERMISSION_FULL );     		 
	    	 else setDefaultAcls( target, objectId, SAcl.PERMISSION_FULL, SAcl.PERMISSION_FULL, bucket.getOwnerCanonicalId());
	    }
	    else throw new UnsupportedException( "Unknown Canned Access Policy: " + cannedAccessPolicy + " is not supported" );
=======
	public void setCannedAccessControls( String cannedAccessPolicy, String target, long objectId, SBucket bucket )
	{
       // Find the permission and symbol for the principal corresponding to the requested cannedAccessPolicy	
	    Triple<Integer,Integer,String> permission_permission_symbol_triple = 
	    		  SAcl.getCannedAccessControls(cannedAccessPolicy, target, bucket.getOwnerCanonicalId());
	    if ( null == permission_permission_symbol_triple.getThird() )
	    	setSingleAcl(target, objectId, permission_permission_symbol_triple.getFirst());
	    else 
	    {    setDefaultAcls( target, 
	    		objectId, 
	    		permission_permission_symbol_triple.getFirst(),    // permission according to ownership of object
	    		permission_permission_symbol_triple.getSecond(),   // permission according to ownership of bucket
	    		permission_permission_symbol_triple.getThird() );  // "symbol" to indicate principal or otherwise name of owner
	    	  
	    }
>>>>>>> 6472e7b... Now really adding the renamed files!
	}

	
	private void setSingleAcl( String target, long targetId, int permission ) 
	{	
		SAclDao aclDao  = new SAclDao();
        S3AccessControlList defaultAcl = new S3AccessControlList();
        
		// -> if an annoymous request, then do not rewrite the ACL
		String userId = UserContext.current().getCanonicalUserId();
        if (0 < userId.length())
        {
            S3Grant defaultGrant = new S3Grant();
            defaultGrant.setGrantee(SAcl.GRANTEE_USER);
            defaultGrant.setCanonicalUserID( userId );
            defaultGrant.setPermission( permission );
            defaultAcl.addGrant( defaultGrant );       
            aclDao.save( target, targetId, defaultAcl );
        }
	}
<<<<<<< HEAD

	/**
	 * Note that we use the Cloud Stack API Access key for the Canonical User Id everywhere
	 * (i.e., for buckets, and objects).   
=======
	

	/**
	 * The Cloud Stack API Access key is used for for the Canonical User Id everywhere (buckets and objects).   
>>>>>>> 6472e7b... Now really adding the renamed files!
	 * 
	 * @param owner - this can be the Cloud Access Key for a bucket owner or one of the
	 *                following special symbols:
	 *                (a) '*' - any principal authenticated user (i.e., any user with a registered Cloud Access Key)
	 *                (b) 'A' - any anonymous principal (i.e., S3 request without an Authorization header)
	 */
	private void setDefaultAcls( String target, long objectId, int permission1, int permission2, String owner  ) 
	{
		SAclDao aclDao = new SAclDao();
		S3AccessControlList defaultAcl = new S3AccessControlList();	   
		
		// -> object owner
        S3Grant defaultGrant = new S3Grant();
        defaultGrant.setGrantee(SAcl.GRANTEE_USER);
        defaultGrant.setCanonicalUserID( UserContext.current().getCanonicalUserId());
        defaultGrant.setPermission( permission1 );
        defaultAcl.addGrant( defaultGrant );	
    
        // -> bucket owner 
        defaultGrant = new S3Grant();
        defaultGrant.setGrantee(SAcl.GRANTEE_USER);
        defaultGrant.setCanonicalUserID( owner );
        defaultGrant.setPermission( permission2 );
        defaultAcl.addGrant( defaultGrant );	     
        aclDao.save( target, objectId, defaultAcl );
	}

	public static PolicyAccess verifyPolicy( S3PolicyContext context )
	{
		S3BucketPolicy policy = null;
		
<<<<<<< HEAD
		// -> on error of getting a policy ignore it
=======
		// Ordinarily a REST request will pass in an S3PolicyContext for a given bucket by this stage.  The HttpServletRequest object
		// should be held in the UserContext ready for extraction of the S3BucketPolicy.
		// If there is an error in obtaining the request object or in loading the policy then log the failure and return a S3PolicyContext
		// which indicates DEFAULT_DENY.  Where there is no failure, the policy returned should be specific to the Canonical User ID of the requester.
		
>>>>>>> 6472e7b... Now really adding the renamed files!
		try {
			// -> in SOAP the HttpServletRequest object is hidden and not passed around
		    if (null != context) {
		    	context.setHttp( UserContext.current().getHttp());
		    	policy = loadPolicy( context ); 
		    }
		    
			if ( null != policy ) 
				 return policy.eval(context, UserContext.current().getCanonicalUserId());
			else return PolicyAccess.DEFAULT_DENY;
		}
		catch( Exception e ) {
            logger.error("verifyAccess - loadPolicy failed, bucket: " + context.getBucketName() + " policy ignored", e);
			return PolicyAccess.DEFAULT_DENY;
		}
	}
	
	/**
	 * To determine access to a bucket or an object in a bucket evaluate first a define
	 * bucket policy and then any defined ACLs.
	 * 
	 * @param context - all data needed for bucket policies
	 * @param target - used for ACL evaluation, object identifier
	 * @param targetId - used for ACL evaluation
	 * @param requestedPermission - ACL type access requested
	 * 
	 * @throws ParseException, SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException 
	 */
	public static void verifyAccess( S3PolicyContext context, String target, long targetId, int requestedPermission ) 
	{
		switch( verifyPolicy( context ) ) {
        case ALLOW:   // overrides ACLs (?)
             return;

        case DENY:
             throw new PermissionDeniedException( "Access Denied - bucket policy DENY result" );
             
        case DEFAULT_DENY:
        default:
             accessAllowed( target, targetId, requestedPermission );
             break;
        }
	}
	
	/**
<<<<<<< HEAD
	 * This function verifies that the accessing client has the requested
	 * permission on the object/bucket/Acl represented by the tuble: <target, targetId>
	 * 
	 * For cases where an ACL is meant for any authenticated user we place a "*" for the
	 * Canonical User Id ("*" is not a legal Cloud Stack Access key).   
	 * 
	 * For cases where an ACL is meant for any anonymous user (or 'AllUsers') we place a "A" for the 
	 * Canonical User Id ("A" is not a legal Cloud Stack Access key).
=======
	 * This method verifies that the accessing client has the requested
	 * permission on the object/bucket/Acl represented by the tuple: <target, targetId>
	 * 
	 * For cases where an ACL is meant for any authenticated user we place a "*" for the
	 * Canonical User Id.  N.B. - "*" is not a legal Cloud (Bridge) Access key.   
	 * 
	 * For cases where an ACL is meant for any anonymous user (or 'AllUsers') we place a "A" for the 
	 * Canonical User Id.  N.B. - "A" is not a legal Cloud (Bridge) Access key. 
>>>>>>> 6472e7b... Now really adding the renamed files!
	 */
	public static void accessAllowed( String target, long targetId, int requestedPermission ) 
	{
		if (SAcl.PERMISSION_PASS == requestedPermission) return;
			
		SAclDao aclDao = new SAclDao();
		
<<<<<<< HEAD
		// -> if an annoymous request, then canonicalUserId is an empty string
		String userId = UserContext.current().getCanonicalUserId();
        if ( 0 == userId.length())
        {
             // -> is an anonymous principal ACL set for this <target, targetId>?
		     if (hasPermission( aclDao.listGrants( target, targetId, "A" ), requestedPermission )) return;
        }
        else
        {    // -> no priviledges means no access allowed		
		     if (hasPermission( aclDao.listGrants( target, targetId, userId ), requestedPermission )) return;

		     // -> or maybe there is any principal authenticated ACL set for this <target, targetId>?
		     if (hasPermission( aclDao.listGrants( target, targetId, "*" ), requestedPermission )) return;
        }
=======
		// If an annoymous request, then canonicalUserId is an empty string
		String userId = UserContext.current().getCanonicalUserId();
        if ( 0 == userId.length())
        {
             // Is an anonymous principal ACL set for this <target, targetId>?
		     if (hasPermission( aclDao.listGrants( target, targetId, "A" ), requestedPermission )) return;
        }
        else
        {    	
		     if (hasPermission( aclDao.listGrants( target, targetId, userId ), requestedPermission )) return;
		     // Or alternatively is there is any principal authenticated ACL set for this <target, targetId>?
		     if (hasPermission( aclDao.listGrants( target, targetId, "*" ), requestedPermission )) return;
        }
        // No privileges implies that no access is allowed	in the case of an anonymous user
>>>>>>> 6472e7b... Now really adding the renamed files!
        throw new PermissionDeniedException( "Access Denied - ACLs do not give user the required permission" );
	}
	
	/**
<<<<<<< HEAD
	 * This function assumes that the bucket has been tested to make sure it exists before
=======
	 * This method assumes that the bucket has been tested to make sure it exists before
>>>>>>> 6472e7b... Now really adding the renamed files!
	 * it is called.
	 * 
	 * @param context 
	 * @return S3BucketPolicy
	 * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException, ParseException 
	 */
	public static S3BucketPolicy loadPolicy( S3PolicyContext context ) 
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, ParseException
	{
<<<<<<< HEAD
		Tuple<S3BucketPolicy,Integer> result = ServiceProvider.getInstance().getBucketPolicy( context.getBucketName());
=======
		OrderedPair<S3BucketPolicy,Integer> result = ServiceProvider.getInstance().getBucketPolicy( context.getBucketName());
>>>>>>> 6472e7b... Now really adding the renamed files!
		S3BucketPolicy policy = result.getFirst();
		if ( null == policy )
		{
			 // -> do we have to load it from the database (any other value means there is no policy)?
			 if (-1 == result.getSecond().intValue())
			 {
			    BucketPolicyDao policyDao = new BucketPolicyDao();
			    String policyInJson = policyDao.getPolicy( context.getBucketName());
			    // -> place in cache that no policy exists in the database
			    if (null == policyInJson) {
	    	        ServiceProvider.getInstance().setBucketPolicy(context.getBucketName(), null);
			    	return null;
			    }
			    
	       		PolicyParser parser = new PolicyParser();
	    		policy = parser.parse( policyInJson, context.getBucketName());
	    		if (null != policy) 
	    	        ServiceProvider.getInstance().setBucketPolicy(context.getBucketName(), policy);
			 }
		}
		return policy;
	}
	
	public static void verifyBucketName( String bucketName, boolean useDNSGuidelines ) throws InvalidBucketName
	{
		 // [A] To comply with Amazon S3 basic requirements, bucket names must meet the following conditions
		 // -> must be between 3 and 255 characters long
		 int size = bucketName.length();		 
		 if (3 > size || size > 255) 
			 throw new InvalidBucketName( bucketName + " is not between 3 and 255 characters long" );
		 
		 // -> must start with a number or letter
		 if (!Character.isLetterOrDigit( bucketName.charAt( 0 ))) 
			 throw new InvalidBucketName( bucketName + " does not start with a number or letter" );
		 
		 // -> can contain lowercase letters, numbers, periods (.), underscores (_), and dashes (-)
		 // -> the bucket name can also contain uppercase letters but it is not recommended
		 for( int i=0; i < bucketName.length(); i++ ) 
		 {
			 char next = bucketName.charAt(i);
			      if (Character.isLetter( next )) continue;
			 else if (Character.isDigit( next ))  continue;
			 else if ('.' == next)                continue;
			 else if ('_' == next)                continue;
			 else if ('-' == next)                continue;
			 else throw new InvalidBucketName( bucketName + " contains the invalid character: " + next );
		 }
		 
		 // -> must not be formatted as an IP address (e.g., 192.168.5.4)
		 String[] parts = bucketName.split( "\\." );
		 if (4 == parts.length)
		 {
			 try {
				 int first  = Integer.parseInt( parts[0] );
				 int second = Integer.parseInt( parts[1] );
				 int third  = Integer.parseInt( parts[2] );
				 int fourth = Integer.parseInt( parts[3] );
				 throw new InvalidBucketName( bucketName + " is formatted as an IP address" );
			 }
<<<<<<< HEAD
			 catch( NumberFormatException e ) {}
=======
			 catch( NumberFormatException e ) 
			 {throw new InvalidBucketName( bucketName);}
>>>>>>> 6472e7b... Now really adding the renamed files!
		 }
	
		 
		 // [B] To conform with DNS requirements, Amazon recommends following these additional guidelines when creating buckets
		 // -> bucket names should be between 3 and 63 characters long
		 if (useDNSGuidelines)
		 {
			 // -> bucket names should be between 3 and 63 characters long
			 if (3 > size || size > 63) 
				 throw new InvalidBucketName( "DNS requiremens, bucket name: " + bucketName + " is not between 3 and 63 characters long" );

			 // -> bucket names should not contain underscores (_)
			 int pos = bucketName.indexOf( '_' );
			 if (-1 != pos) 
				 throw new InvalidBucketName( "DNS requiremens, bucket name: " + bucketName + " should not contain underscores" );
			 
             // -> bucket names should not end with a dash
			 if (bucketName.endsWith( "-" )) 
				 throw new InvalidBucketName( "DNS requiremens, bucket name: " + bucketName + " should not end with a dash" );
			 
			 // -> bucket names cannot contain two, adjacent periods
			 pos = bucketName.indexOf( ".." );
			 if (-1 != pos) 
				 throw new InvalidBucketName( "DNS requiremens, bucket name: " + bucketName + " should not contain \"..\"" );
			 
			 // -> bucket names cannot contain dashes next to periods (e.g., "my-.bucket.com" and "my.-bucket" are invalid)
			 if (-1 != bucketName.indexOf( "-." ) || -1 != bucketName.indexOf( ".-" ))
			     throw new InvalidBucketName( "DNS requiremens, bucket name: " + bucketName + " should not contain \".-\" or \"-.\"" );
		 }
	}
	
<<<<<<< HEAD
	private static boolean hasPermission( List<SAcl> priviledges, int requestedPermission ) 
	{
        ListIterator it = priviledges.listIterator();
        while( it.hasNext()) 
        {
           // -> is the requested permission "contained" in one or the granted rights for this user
=======
	private static boolean hasPermission( List<SAcl> privileges, int requestedPermission ) 
	{
        ListIterator<SAcl> it = privileges.listIterator();
        while( it.hasNext()) 
        {
           // True providing the requested permission is contained in one or the granted rights for this user.  False otherwise.
>>>>>>> 6472e7b... Now really adding the renamed files!
           SAcl rights = (SAcl)it.next();
           int permission = rights.getPermission();
           if (requestedPermission == (permission & requestedPermission)) return true;
        }
        return false;
	}
	
	/**
<<<<<<< HEAD
	 * ifRange is true and IfUnmodifiedSince or IfMatch fails then we return the entire object (indicated by
=======
	 * ifRange is true and ifUnmodifiedSince or IfMatch fails then we return the entire object (indicated by
>>>>>>> 6472e7b... Now really adding the renamed files!
	 * returning a -1 as the function result.
	 * 
	 * @param ifCond - conditional get defined by these tests
	 * @param lastModified - value used on ifModifiedSince or ifUnmodifiedSince
	 * @param ETag - value used on ifMatch and ifNoneMatch
<<<<<<< HEAD
	 * @param ifRange - using an If-Range HTTP functionality 
=======
	 * @param ifRange - using an if-Range HTTP functionality 
>>>>>>> 6472e7b... Now really adding the renamed files!
	 * @return -1 means return the entire object with an HTTP 200 (not a subrange)
	 */
	private int conditionPassed( S3ConditionalHeaders ifCond, Date lastModified, String ETag, boolean ifRange ) 
	{	
		if (null == ifCond) return 200;
		
		if (0 > ifCond.ifModifiedSince( lastModified )) 
			return 304;
		
		if (0 > ifCond.ifUnmodifiedSince( lastModified )) 
			return (ifRange ? -1 : 412);
		
		if (0 > ifCond.ifMatchEtag( ETag ))  			  
			return (ifRange ? -1 : 412);
		
		if (0 > ifCond.ifNoneMatchEtag( ETag ))  		  
			return 412;
		
		return 200;
	}
}
