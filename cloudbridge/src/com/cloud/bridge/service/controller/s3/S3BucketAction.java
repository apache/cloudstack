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
package com.cloud.bridge.service.controller.s3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLSerializer;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazon.s3.GetBucketAccessControlPolicyResponse;
import com.amazon.s3.ListAllMyBucketsResponse;
import com.amazon.s3.ListBucketResponse;
import com.cloud.bridge.io.MTOMAwareResultStreamWriter;
import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.persist.dao.BucketPolicyDao;
import com.cloud.bridge.persist.dao.MultipartLoadDao;
import com.cloud.bridge.persist.dao.SBucketDao;
import com.cloud.bridge.service.S3Constants;
import com.cloud.bridge.service.S3RestServlet;
import com.cloud.bridge.service.S3SoapServiceImpl;
import com.cloud.bridge.service.ServiceProvider;
import com.cloud.bridge.service.ServletAction;
import com.cloud.bridge.service.UserContext;
import com.cloud.bridge.service.core.s3.S3AccessControlPolicy;
import com.cloud.bridge.service.core.s3.S3BucketPolicy;
import com.cloud.bridge.service.core.s3.S3CanonicalUser;
import com.cloud.bridge.service.core.s3.S3CreateBucketConfiguration;
import com.cloud.bridge.service.core.s3.S3CreateBucketRequest;
import com.cloud.bridge.service.core.s3.S3CreateBucketResponse;
import com.cloud.bridge.service.core.s3.S3DeleteBucketRequest;
import com.cloud.bridge.service.core.s3.S3Engine;
import com.cloud.bridge.service.core.s3.S3GetBucketAccessControlPolicyRequest;
import com.cloud.bridge.service.core.s3.S3ListAllMyBucketsRequest;
import com.cloud.bridge.service.core.s3.S3ListAllMyBucketsResponse;
import com.cloud.bridge.service.core.s3.S3ListBucketObjectEntry;
import com.cloud.bridge.service.core.s3.S3ListBucketRequest;
import com.cloud.bridge.service.core.s3.S3ListBucketResponse;
import com.cloud.bridge.service.core.s3.S3MultipartUpload;
import com.cloud.bridge.service.core.s3.S3PolicyContext;
import com.cloud.bridge.service.core.s3.S3PutObjectRequest;
import com.cloud.bridge.service.core.s3.S3Response;
import com.cloud.bridge.service.core.s3.S3SetBucketAccessControlPolicyRequest;
import com.cloud.bridge.service.core.s3.S3BucketPolicy.PolicyAccess;
import com.cloud.bridge.service.core.s3.S3PolicyAction.PolicyActions;
import com.cloud.bridge.service.core.s3.S3PolicyCondition.ConditionKeys;
import com.cloud.bridge.service.exception.InternalErrorException;
import com.cloud.bridge.service.exception.InvalidRequestContentException;
import com.cloud.bridge.service.exception.NetworkIOException;
import com.cloud.bridge.service.exception.PermissionDeniedException;
import com.cloud.bridge.util.Converter;
import com.cloud.bridge.util.PolicyParser;
import com.cloud.bridge.util.StringHelper;
import com.cloud.bridge.util.Tuple;
import com.cloud.bridge.util.XSerializer;
import com.cloud.bridge.util.XSerializerXmlAdapter;


/**
 * @author Kelven Yang, John Zucker
 */
public class S3BucketAction implements ServletAction {
    protected final static Logger logger = Logger.getLogger(S3BucketAction.class);
    
    private DocumentBuilderFactory dbf = null;
	private OMFactory factory = OMAbstractFactory.getOMFactory();
	private XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();
    
	public S3BucketAction() {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware( true );

	}
	
	public void execute(HttpServletRequest request, HttpServletResponse response) 
	    throws IOException, XMLStreamException 
	{
		String method = request.getMethod(); 
		String queryString = request.getQueryString();
		
		if ( method.equalsIgnoreCase("PUT")) 
		{
			 if ( queryString != null && queryString.length() > 0 ) 
			 {
			 	  if ( queryString.startsWith("acl")) {
				 	   executePutBucketAcl(request, response);
				 	   return;
				  } 
				  else if (queryString.startsWith("versioning")) {
					   executePutBucketVersioning(request, response);
					   return;
				  } 
				  else if (queryString.startsWith("policy")) {
					   executePutBucketPolicy(request, response);
					   return;
				  }
				  else if (queryString.startsWith("logging")) {
					   executePutBucketLogging(request, response);
					   return;
				  }
				  else if (queryString.startsWith("website")) {
					   executePutBucketWebsite(request, response);
					   return;
				  }
			 }
			 executePutBucket(request, response);
		} 
		else if(method.equalsIgnoreCase("GET")) 
		{
			 if (queryString != null && queryString.length() > 0) 
			 {
				 if ( queryString.startsWith("acl")) {
					  executeGetBucketAcl(request, response);
					  return;
				 } 
				 else if (queryString.startsWith("versioning")) {
					  executeGetBucketVersioning(request, response);
					  return;
				 } 
				 else if (queryString.contains("versions")) {
					  executeGetBucketObjectVersions(request, response);
					  return;
				 } 
				 else if (queryString.startsWith("location")) {
					  executeGetBucketLocation(request, response);
					  return;
				 }
				 else if (queryString.startsWith("uploads")) {
					  executeListMultipartUploads(request, response);
					  return;
				 }
				 else if (queryString.startsWith("policy")) {
					  executeGetBucketPolicy(request, response);
					  return;
				 }
				 else if (queryString.startsWith("logging")) {
					  executeGetBucketLogging(request, response);
					  return;
				 } 
				 else if (queryString.startsWith("website")) {
					  executeGetBucketWebsite(request, response);
					  return;
				 } 
			 }
			
			 String bucketAtr = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
             if ( bucketAtr.equals( "/" ))
            	  executeGetAllBuckets(request, response);
             else executeGetBucket(request, response);
		} 
		else if (method.equalsIgnoreCase("DELETE")) 
		{
			 if (queryString != null && queryString.length() > 0) 
			 {
				 if ( queryString.startsWith("policy")) {
					  executeDeleteBucketPolicy(request, response);
					  return;
				 }
				 else if (queryString.startsWith("website")) {
					  executeDeleteBucketWebsite(request, response);
					  return;
				 }

			 }
			 executeDeleteBucket(request, response);
		}
		else if ( (method.equalsIgnoreCase("POST")) && (queryString.equalsIgnoreCase("delete")) )
		{
			// TODO - Hi Pri - Implement multi-object delete in a single command
			throw new InternalErrorException("Multi-object delete in a single command not yet implemented");
		}
		else throw new IllegalArgumentException("Unsupported method in REST request");
	}
	
	/** 
	 * In order to support a policy on the "s3:CreateBucket" action we must be able to set and get
	 * policies before a bucket is actually created.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void executePutBucketPolicy(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String bucketName = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
		String policy = streamToString( request.getInputStream());
		        
		// [A] Is there an owner of an existing policy or bucket?
        BucketPolicyDao policyDao = new BucketPolicyDao();
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName( bucketName );
        String owner = null;
        
        if ( null != bucket ) 
        {
        	 owner = bucket.getOwnerCanonicalId();
        }
        else 
        {    try {
        	     owner = policyDao.getPolicyOwner( bucketName );
             }
             catch( Exception e ) {}
        }

        
		// [B] "The bucket owner by default has permissions to attach bucket policies to their buckets using PUT Bucket policy." 
		//  -> the bucket owner may want to restrict the IP address from where this can be executed
	    String client = UserContext.current().getCanonicalUserId();
		S3PolicyContext context = new S3PolicyContext( PolicyActions.PutBucketPolicy, bucketName );
	    switch( S3Engine.verifyPolicy( context )) {
	    case ALLOW:
             break;
             
		case DEFAULT_DENY:
		     if (null != owner && !client.equals( owner )) {
		    	 response.setStatus(405);
		    	 return;
		     }
		     break;
		    	 
		case DENY:
             response.setStatus(403);
             return;
		}
			
	    
	    // [B] Place the policy into the database over writting an existing policy
    	try {
    		// -> first make sure that the policy is valid by parsing it
       		PolicyParser parser = new PolicyParser();
    		S3BucketPolicy sbp = parser.parse( policy, bucketName );

	        policyDao.deletePolicy( bucketName );
	        if (null != policy && !policy.isEmpty()) policyDao.addPolicy( bucketName, client, policy );
	                
    		if (null != sbp) ServiceProvider.getInstance().setBucketPolicy( bucketName, sbp );
    		response.setStatus(200);  		
    	}
    	catch( PermissionDeniedException e ) {
			logger.error("Put Bucket Policy failed due to " + e.getMessage(), e);	
			throw e; 		
    	}
    	catch( ParseException e ) {
			logger.error("Put Bucket Policy failed due to " + e.getMessage(), e);	
			throw new PermissionDeniedException( e.toString());		   		
    	}
		catch( Exception e ) {
			logger.error("Put Bucket Policy failed due to " + e.getMessage(), e);	
			response.setStatus(500);
		}
	}
	
	private void executeGetBucketPolicy(HttpServletRequest request, HttpServletResponse response) 
	{
		String bucketName = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);

		// [A] Is there an owner of an existing policy or bucket?
        BucketPolicyDao policyDao = new BucketPolicyDao();
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName( bucketName );
        String owner = null;
        
        if ( null != bucket ) 
        {
        	 owner = bucket.getOwnerCanonicalId();
        }
        else 
        {    try {
        	     owner = policyDao.getPolicyOwner( bucketName );
             }
             catch( Exception e ) {}
        }

        
		// [B] "The bucket owner by default has permissions to retrieve bucket policies using GET Bucket policy."
		//  -> the bucket owner may want to restrict the IP address from where this can be executed
		String client = UserContext.current().getCanonicalUserId();
		S3PolicyContext context = new S3PolicyContext( PolicyActions.GetBucketPolicy, bucketName );
		switch( S3Engine.verifyPolicy( context )) {
		case ALLOW:
             break;
             
		case DEFAULT_DENY:
		  	 if (null != owner && !client.equals( owner )) {
		   		 response.setStatus(405);
		   		 return;
		   	 }
		   	 break;
		    	 
		case DENY:
             response.setStatus(403);
             return;
		}

	    
	    // [B] Pull the policy from the database if one exists
    	try {
	        String policy = policyDao.getPolicy( bucketName );
	        if ( null == policy ) {
	    		 response.setStatus(404);
	        }
	        else {
    		     response.setStatus(200);
    			 response.setContentType("application/json");
    			 S3RestServlet.endResponse(response, policy);
	        }
    	}
		catch( Exception e ) {
			logger.error("Get Bucket Policy failed due to " + e.getMessage(), e);	
			response.setStatus(500);
		}
	}

	private void executeDeleteBucketPolicy(HttpServletRequest request, HttpServletResponse response) 
	{
		String bucketName = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
		
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName( bucketName );
		if (bucket != null) 
		{
		    String client = UserContext.current().getCanonicalUserId();
		    if (!client.equals( bucket.getOwnerCanonicalId())) {
		        response.setStatus(405);
		        return;
		    }
		}

    	try {
	        BucketPolicyDao policyDao = new BucketPolicyDao();
	        String policy = policyDao.getPolicy( bucketName );
	        if ( null == policy ) {
	    		 response.setStatus(204);
	        }
	        else {
	   	         ServiceProvider.getInstance().deleteBucketPolicy( bucketName );
    	         policyDao.deletePolicy( bucketName );
    		     response.setStatus(200);
	        }
    	}
		catch( Exception e ) {
			logger.error("Delete Bucket Policy failed due to " + e.getMessage(), e);	
			response.setStatus(500);
		}
	}

	public void executeGetAllBuckets(HttpServletRequest request, HttpServletResponse response) 
	    throws IOException, XMLStreamException 
	{
		Calendar cal = Calendar.getInstance();
		cal.set( 1970, 1, 1 );    
		S3ListAllMyBucketsRequest engineRequest = new S3ListAllMyBucketsRequest();
		engineRequest.setAccessKey(UserContext.current().getAccessKey());
		engineRequest.setRequestTimestamp( cal );
		engineRequest.setSignature( "" );
		
		


		S3ListAllMyBucketsResponse engineResponse = ServiceProvider.getInstance().getS3Engine().handleRequest(engineRequest);
		
		// To allow the all buckets list to be serialized via Axiom classes
		ListAllMyBucketsResponse allBuckets = S3SoapServiceImpl.toListAllMyBucketsResponse( engineResponse );
		
		OutputStream outputStream = response.getOutputStream();
		response.setStatus(200);	
	    response.setContentType("application/xml");   
	         // The content-type literally should be "application/xml; charset=UTF-8" 
	         // but any compliant JVM supplies utf-8 by default
		
		MTOMAwareResultStreamWriter resultWriter = new MTOMAwareResultStreamWriter ("ListAllMyBucketsResult", outputStream );
		resultWriter.startWrite();
		resultWriter.writeout(allBuckets);
		resultWriter.stopWrite();
		
	}

	public void executeGetBucket(HttpServletRequest request, HttpServletResponse response) 
	    throws IOException, XMLStreamException 
	{
		S3ListBucketRequest engineRequest = new S3ListBucketRequest();
		engineRequest.setBucketName((String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY));
		engineRequest.setDelimiter(request.getParameter("delimiter"));
		engineRequest.setMarker(request.getParameter("marker"));
		engineRequest.setPrefix(request.getParameter("prefix"));
		
		int maxKeys = Converter.toInt(request.getParameter("max-keys"), 1000);
		engineRequest.setMaxKeys(maxKeys);
		S3ListBucketResponse engineResponse = ServiceProvider.getInstance().getS3Engine().listBucketContents( engineRequest, false );
		
		// To allow the all list buckets result to be serialized via Axiom classes
		ListBucketResponse oneBucket = S3SoapServiceImpl.toListBucketResponse( engineResponse );
	
		OutputStream outputStream = response.getOutputStream();
		response.setStatus(200);	
	    response.setContentType("application/xml");   
	         // The content-type literally should be "application/xml; charset=UTF-8" 
	         // but any compliant JVM supplies utf-8 by default;
	    
		MTOMAwareResultStreamWriter resultWriter = new MTOMAwareResultStreamWriter ("ListBucketResult", outputStream );
		resultWriter.startWrite();
		resultWriter.writeout(oneBucket);
		resultWriter.stopWrite();

	}
	
	public void executeGetBucketAcl(HttpServletRequest request, HttpServletResponse response) 
	    throws IOException, XMLStreamException 
	{
		S3GetBucketAccessControlPolicyRequest engineRequest = new S3GetBucketAccessControlPolicyRequest();
		Calendar cal = Calendar.getInstance();
		cal.set( 1970, 1, 1 ); 
		engineRequest.setAccessKey(UserContext.current().getAccessKey());
		engineRequest.setRequestTimestamp( cal );
		engineRequest.setSignature( "" );
		engineRequest.setBucketName((String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY));

		S3AccessControlPolicy engineResponse = ServiceProvider.getInstance().getS3Engine().handleRequest(engineRequest);
		
		// To allow the bucket acl policy result to be serialized via Axiom classes
		GetBucketAccessControlPolicyResponse onePolicy = S3SoapServiceImpl.toGetBucketAccessControlPolicyResponse( engineResponse );

		OutputStream outputStream = response.getOutputStream();
		response.setStatus(200);	
	    response.setContentType("application/xml");   
	         // The content-type literally should be "application/xml; charset=UTF-8" 
	         // but any compliant JVM supplies utf-8 by default;
	    
		MTOMAwareResultStreamWriter resultWriter = new MTOMAwareResultStreamWriter ("GetBucketAccessControlPolicyResult", outputStream );
		resultWriter.startWrite();
		resultWriter.writeout(onePolicy);
		resultWriter.stopWrite();

		
	}
	
	public void executeGetBucketVersioning(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		// [A] Does the bucket exist?
		String bucketName = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
		String versioningStatus = null;
		
		if (null == bucketName) {
			logger.error( "executeGetBucketVersioning - no bucket name given" );
			response.setStatus( 400 ); 
			return; 
		}
		
		SBucketDao bucketDao = new SBucketDao();
		SBucket sbucket = bucketDao.getByName( bucketName );
		if (sbucket == null) {
			response.setStatus( 404 );
			return;
		}
		
		// [B] The owner may want to restrict the IP address at which this can be performed
		String client = UserContext.current().getCanonicalUserId();
		if (!client.equals( sbucket.getOwnerCanonicalId()))
		    throw new PermissionDeniedException( "Access Denied - only the owner can read bucket versioning" );

		S3PolicyContext context = new S3PolicyContext( PolicyActions.GetBucketVersioning, bucketName );
	    if (PolicyAccess.DENY == S3Engine.verifyPolicy( context )) {
             response.setStatus(403);
             return;
	    }


	    // [C]
		switch( sbucket.getVersioningStatus()) {
		default:
		case 0: versioningStatus = "";           break;
		case 1: versioningStatus = "Enabled";    break;
		case 2: versioningStatus = "Suspended";  break;
		}

		StringBuffer xml = new StringBuffer();
        xml.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
        xml.append( "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" );
        if (0 < versioningStatus.length()) xml.append( "<Status>" ).append( versioningStatus ).append( "</Status>" );
        xml.append( "</VersioningConfiguration>" );
      
		response.setStatus(200);
	    response.setContentType("text/xml; charset=UTF-8");
    	S3RestServlet.endResponse(response, xml.toString());
	}
	
	public void executeGetBucketObjectVersions(HttpServletRequest request, HttpServletResponse response) throws IOException
	{   
		S3ListBucketRequest engineRequest = new S3ListBucketRequest();
		String keyMarker       = request.getParameter("key-marker");
		String versionIdMarker = request.getParameter("version-id-marker");
		
		engineRequest.setBucketName((String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY));
		engineRequest.setDelimiter(request.getParameter("delimiter"));
		engineRequest.setMarker( keyMarker );  
		engineRequest.setPrefix(request.getParameter("prefix"));
		engineRequest.setVersionIdMarker( versionIdMarker );
		
		int maxKeys = Converter.toInt(request.getParameter("max-keys"), 1000);
		engineRequest.setMaxKeys(maxKeys);
		S3ListBucketResponse engineResponse = ServiceProvider.getInstance().getS3Engine().listBucketContents( engineRequest, true );
		
		// -> the SOAP version produces different XML
		StringBuffer xml = new StringBuffer();
        xml.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
        xml.append( "<ListVersionsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" );
        xml.append( "<Name>" ).append( engineResponse.getBucketName()).append( "</Name>" );
        
        if ( null == keyMarker )
        	 xml.append( "<KeyMarker/>" );
        else xml.append( "<KeyMarker>" ).append( keyMarker ).append( "</KeyMarker" ); 
  
        if ( null == versionIdMarker )
       	     xml.append( "<VersionIdMarker/>" );
        else xml.append( "<VersionIdMarker>" ).append( keyMarker ).append( "</VersionIdMarker" ); 

        xml.append( "<MaxKeys>" ).append( engineResponse.getMaxKeys()).append( "</MaxKeys>" );
        xml.append( "<IsTruncated>" ).append( engineResponse.isTruncated()).append( "</IsTruncated>" );
        
        S3ListBucketObjectEntry[] versions = engineResponse.getContents();
        for( int i=0; null != versions && i < versions.length; i++ )
        {
        	 S3CanonicalUser owner    = versions[i].getOwner();
        	 boolean isDeletionMarker = versions[i].getIsDeletionMarker();
        	 String displayName       = owner.getDisplayName();
        	 String id                = owner.getID();
        	 
        	 if ( isDeletionMarker ) 
        	 { 	  
        		  xml.append( "<DeleteMarker>" );  	 
                  xml.append( "<Key>" ).append( versions[i].getKey()).append( "</Key>" );
                  xml.append( "<VersionId>" ).append( versions[i].getVersion()).append( "</VersionId>" );
                  xml.append( "<IsLatest>" ).append( versions[i].getIsLatest()).append( "</IsLatest>" );
                  xml.append( "<LastModified>" ).append( DatatypeConverter.printDateTime( versions[i].getLastModified())).append( "</LastModified>" );
        	 }
        	 else
        	 { 	  xml.append( "<Version>" );  	 
                  xml.append( "<Key>" ).append( versions[i].getKey()).append( "</Key>" );
                  xml.append( "<VersionId>" ).append( versions[i].getVersion()).append( "</VersionId>" );
                  xml.append( "<IsLatest>" ).append( versions[i].getIsLatest()).append( "</IsLatest>" );
                  xml.append( "<LastModified>" ).append( DatatypeConverter.printDateTime( versions[i].getLastModified())).append( "</LastModified>" );
                  xml.append( "<ETag>" ).append( versions[i].getETag()).append( "</ETag>" );
                  xml.append( "<Size>" ).append( versions[i].getSize()).append( "</Size>" );
                  xml.append( "<StorageClass>" ).append( versions[i].getStorageClass()).append( "</StorageClass>" );
        	 }
        	 
             xml.append( "<Owner>" );
             xml.append( "<ID>" ).append( id ).append( "</ID>" );
             if ( null == displayName )
              	  xml.append( "<DisplayName/>" );
             else xml.append( "<DisplayName>" ).append( owner.getDisplayName()).append( "</DisplayName>" );
             xml.append( "</Owner>" );
        	 
             if ( isDeletionMarker )
            	  xml.append( "</DeleteMarker>" ); 
             else xml.append( "</Version>" );
        }
        xml.append( "</ListVersionsResult>" );
      
		response.setStatus(200);
	    response.setContentType("text/xml; charset=UTF-8");
    	S3RestServlet.endResponse(response, xml.toString());
	}
	
	public void executeGetBucketLogging(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO -- this is a beta feature of S3
		response.setStatus(501);
	}
	
	public void executeGetBucketLocation(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(501);
	}

	public void executeGetBucketWebsite(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(501);
	}

	public void executeDeleteBucketWebsite(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(501);
	}

	public void executePutBucket(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		int contentLength = request.getContentLength();
		Object objectInContent = null;
		
		if(contentLength > 0) 
		{
			InputStream is = null;
			try {
				is = request.getInputStream();
				String xml = StringHelper.stringFromStream(is);
				XSerializer serializer = new XSerializer(new XSerializerXmlAdapter()); 
				objectInContent = serializer.serializeFrom(xml);
				if(objectInContent != null && !(objectInContent instanceof S3CreateBucketConfiguration)) {
					throw new InvalidRequestContentException("Invalid request content in create-bucket: " + xml);
				}
				is.close();
				
			} catch (IOException e) {
				logger.error("Unable to read request data due to " + e.getMessage(), e);
				throw new NetworkIOException(e);
				
			} finally {
				if(is != null) is.close();
			}
		}
		
		S3CreateBucketRequest engineRequest = new S3CreateBucketRequest();
		engineRequest.setBucketName((String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY));
		engineRequest.setConfig((S3CreateBucketConfiguration)objectInContent);
		
		S3CreateBucketResponse engineResponse = ServiceProvider.getInstance().getS3Engine().handleRequest(engineRequest);
		response.addHeader("Location", "/" + engineResponse.getBucketName());
		response.setContentLength(0);
		response.setStatus(200);
		response.flushBuffer();
	}
	
	public void executePutBucketAcl(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		S3PutObjectRequest putRequest = null;
		
		// -> reuse the Access Control List parsing code that was added to support DIME
		String bucketName = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
		try {
		    putRequest = S3RestServlet.toEnginePutObjectRequest( request.getInputStream());
		}
		catch( Exception e ) {
			throw new IOException( e.toString());
		}
		
		// -> reuse the SOAP code to save the passed in ACLs
		S3SetBucketAccessControlPolicyRequest engineRequest = new S3SetBucketAccessControlPolicyRequest();
		engineRequest.setBucketName( bucketName );
		engineRequest.setAcl( putRequest.getAcl());
		
	    S3Response engineResponse = ServiceProvider.getInstance().getS3Engine().handleRequest(engineRequest);	
	    response.setStatus( engineResponse.getResultCode());
	}
	
	public void executePutBucketVersioning(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		String bucketName       = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
		String versioningStatus = null;
		Node   item             = null;

		if (null == bucketName) {
			logger.error( "executePutBucketVersioning - no bucket name given" );
			response.setStatus( 400 ); 
			return; 
		}
		
		// -> is the XML as defined?
		try {
		    DocumentBuilder db = dbf.newDocumentBuilder();
		    Document restXML = db.parse( request.getInputStream());
		    NodeList match = S3RestServlet.getElement( restXML, "http://s3.amazonaws.com/doc/2006-03-01/", "Status" ); 
	        if ( 0 < match.getLength()) 
	        {
	    	     item = match.item(0);
	    	     versioningStatus = new String( item.getFirstChild().getNodeValue());
	        }
	        else
	        {    logger.error( "executePutBucketVersioning - cannot find Status tag in XML body" );
				 response.setStatus( 400 ); 
				 return; 
	        }
		}
		catch( Exception e ) {
			logger.error( "executePutBucketVersioning - failed to parse XML due to " + e.getMessage(), e);
			response.setStatus(400);
			return;
		}
	     
	    try {
			// -> does not matter what the ACLs say only the owner can turn on versioning on a bucket
	        // -> the bucket owner may want to restrict the IP address from which this can occur
			SBucketDao bucketDao = new SBucketDao();
			SBucket sbucket = bucketDao.getByName( bucketName );
		
			String client = UserContext.current().getCanonicalUserId();
			if (!client.equals( sbucket.getOwnerCanonicalId()))
			    throw new PermissionDeniedException( "Access Denied - only the owner can turn on versioing on a bucket" );
		
			S3PolicyContext context = new S3PolicyContext( PolicyActions.PutBucketVersioning, bucketName );
		    if (PolicyAccess.DENY == S3Engine.verifyPolicy( context )) {
	             response.setStatus(403);
	             return;
		    }

			
			     if (versioningStatus.equalsIgnoreCase( "Enabled"  )) sbucket.setVersioningStatus( 1 );
			else if (versioningStatus.equalsIgnoreCase( "Suspended")) sbucket.setVersioningStatus( 2 );
			else { 
				 logger.error( "executePutBucketVersioning - unknown state: [" + versioningStatus + "]" );
				 response.setStatus( 400 ); 
				 return; 
		    }
			bucketDao.update( sbucket );
			
		} catch( PermissionDeniedException e ) {
			logger.error( "executePutBucketVersioning - failed due to " + e.getMessage(), e);
			throw e;
			
		} catch( Exception e ) {
			logger.error( "executePutBucketVersioning - failed due to " + e.getMessage(), e);
			response.setStatus(500);
			return;
		}		
		response.setStatus(200);
	}
	
	public void executePutBucketLogging(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO -- this is a S3 beta feature
		response.setStatus(501);
	}
	
	public void executePutBucketWebsite(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(501);
	}

	public void executeDeleteBucket(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		S3DeleteBucketRequest engineRequest = new S3DeleteBucketRequest();
		engineRequest.setBucketName((String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY));
		S3Response engineResponse = ServiceProvider.getInstance().getS3Engine().handleRequest(engineRequest);  
		response.setStatus(engineResponse.getResultCode());
		response.flushBuffer();
	}
	
	/**
	 * This is a very complex function with all the options defined by Amazon.   Part of the functionality is 
	 * provided by the query done against the database.  The CommonPrefixes functionality is done the same way 
	 * as done in the listBucketContents function (i.e., by iterating though the list to decide which output 
	 * element each key is placed).
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void executeListMultipartUploads(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		// [A] Obtain parameters and do basic bucket verification
		String bucketName     = (String)request.getAttribute(S3Constants.BUCKET_ATTR_KEY);
		String delimiter      = request.getParameter("delimiter");
		String keyMarker      = request.getParameter("key-marker");
		String prefix         = request.getParameter("prefix");
		int maxUploads        = 1000;
		int nextUploadId      = 0;
		String nextKey        = null;
		boolean isTruncated   = false;
		S3MultipartUpload[] uploads = null;
		S3MultipartUpload onePart = null;
		
		String temp = request.getParameter("max-uploads");
    	if (null != temp) {
    		maxUploads = Integer.parseInt( temp );
    		if (maxUploads > 1000 || maxUploads < 0) maxUploads = 1000;
    	}
    	
    	// -> upload-id-marker is ignored unless key-marker is also specified
		String uploadIdMarker = request.getParameter("upload-id-marker");
        if (null == keyMarker) uploadIdMarker = null;
    	
		// -> does the bucket exist, we may need it to verify access permissions
		SBucketDao bucketDao = new SBucketDao();
		SBucket bucket = bucketDao.getByName(bucketName);
		if (bucket == null) {
			logger.error( "listMultipartUpload failed since " + bucketName + " does not exist" );
	    	response.setStatus(404);
	    	return;
		}
		
		S3PolicyContext context = new S3PolicyContext( PolicyActions.ListBucketMultipartUploads, bucketName );
		context.setEvalParam( ConditionKeys.Prefix, prefix );
		context.setEvalParam( ConditionKeys.Delimiter, delimiter );
		S3Engine.verifyAccess( context, "SBucket", bucket.getId(), SAcl.PERMISSION_READ );

  			
		// [B] Query the multipart table to get the list of current uploads
    	try {
	        MultipartLoadDao uploadDao = new MultipartLoadDao();
	        Tuple<S3MultipartUpload[],Boolean> result = uploadDao.getInitiatedUploads( bucketName, maxUploads, prefix, keyMarker, uploadIdMarker );
    	    uploads = result.getFirst();
    	    isTruncated = result.getSecond().booleanValue();
    	}
		catch( Exception e ) {
			logger.error("List Multipart Uploads failed due to " + e.getMessage(), e);	
			response.setStatus(500);
		}

		StringBuffer xml = new StringBuffer();
	    xml.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
	    xml.append( "<ListMultipartUploadsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" );
	    xml.append( "<Bucket>" ).append( bucketName ).append( "</Bucket>" );
	    xml.append( "<KeyMarker>").append((null == keyMarker ? "" : keyMarker)).append( "</KeyMarker>" );
	    xml.append( "<UploadIdMarker>").append((null == uploadIdMarker ? "" : uploadIdMarker)).append( "</UploadIdMarker>" );
	    
	    
	    // [C] Construct the contents of the <Upload> element
		StringBuffer partsList = new StringBuffer();
	    for( int i=0; i < uploads.length; i++ ) 
	    {
	        onePart = uploads[i];
	        if (null == onePart) break;
	        
			if (delimiter != null && !delimiter.isEmpty()) 
			{
				// -> is this available only in the CommonPrefixes element?
				if (StringHelper.substringInBetween(onePart.getKey(), prefix, delimiter) != null)
					continue;
			}
	        	
	        nextKey      = onePart.getKey();
	        nextUploadId = onePart.getId();
	        partsList.append( "<Upload>" );
	        partsList.append( "<Key>" ).append( nextKey ).append( "</Key>" );
	        partsList.append( "<UploadId>" ).append( nextUploadId ).append( "</UploadId>" );
	        partsList.append( "<Initiator>" );
	        partsList.append( "<ID>" ).append( onePart.getAccessKey()).append( "</ID>" );
	        partsList.append( "<DisplayName></DisplayName>" );
	        partsList.append( "</Initiator>" );
	        partsList.append( "<Owner>" );
	        partsList.append( "<ID>" ).append( onePart.getAccessKey()).append( "</ID>" );
	        partsList.append( "<DisplayName></DisplayName>" );
	        partsList.append( "</Owner>" );       
	        partsList.append( "<StorageClass>STANDARD</StorageClass>" );
	        partsList.append( "<Initiated>" ).append( DatatypeConverter.printDateTime( onePart.getLastModified())).append( "</Initiated>" );
	        partsList.append( "</Upload>" );        	
	    }  
	        
	    // [D] Construct the contents of the <CommonPrefixes> elements (if any)
	    for( int i=0; i < uploads.length; i++ ) 
	    {
	        onePart = uploads[i];
	        if (null == onePart) break;

			if (delimiter != null && !delimiter.isEmpty()) 
			{
				String subName = StringHelper.substringInBetween(onePart.getKey(), prefix, delimiter);
				if (subName != null) 
				{
			        partsList.append( "<CommonPrefixes>" );
			        partsList.append( "<Prefix>" );
					if ( prefix != null && prefix.length() > 0 )
						partsList.append( prefix + delimiter + subName );
					else partsList.append( subName );
			        partsList.append( "</Prefix>" );
			        partsList.append( "</CommonPrefixes>" );
				}
			}		
		}
	    
	    // [D] Finish off the response
	    xml.append( "<NextKeyMarker>" ).append((null == nextKey ? "" : nextKey)).append( "</NextKeyMarker>" );
	    xml.append( "<NextUploadIdMarker>" ).append((0 == nextUploadId ? "" : nextUploadId)).append( "</NextUploadIdMarker>" );
	    xml.append( "<MaxUploads>" ).append( maxUploads ).append( "</MaxUploads>" );   
	    xml.append( "<IsTruncated>" ).append( isTruncated ).append( "</IsTruncated>" );

	    xml.append( partsList.toString());
	    xml.append( "</ListMultipartUploadsResult>" );
	      
		response.setStatus(200);
		response.setContentType("text/xml; charset=UTF-8");
	    S3RestServlet.endResponse(response, xml.toString());
	}
	
	private String streamToString( InputStream is ) throws IOException 
	{
		int n = 0;
		
	    if ( null != is ) 
	    {
	         Writer writer = new StringWriter();
	         char[] buffer = new char[1024];
	         try {
	             Reader reader = new BufferedReader( new InputStreamReader(is, "UTF-8"));
	             while ((n = reader.read(buffer)) != -1) writer.write(buffer, 0, n);
             } 
	         finally {
	             is.close();
	         }
	         return writer.toString();	        
	    } 
	    else return null;       
    }
}
