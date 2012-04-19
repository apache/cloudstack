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
package com.cloud.bridge.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.bind.*;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cloud.bridge.io.MultiPartDimeInputStream;
import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.persist.PersistContext;
import com.cloud.bridge.persist.dao.UserCredentialsDao;
import com.cloud.bridge.service.controller.s3.S3BucketAction;
import com.cloud.bridge.service.controller.s3.S3ObjectAction;
import com.cloud.bridge.service.core.s3.S3AccessControlList;
import com.cloud.bridge.service.core.s3.S3AuthParams;
import com.cloud.bridge.service.core.s3.S3Engine;
import com.cloud.bridge.service.core.s3.S3Grant;
import com.cloud.bridge.service.core.s3.S3MetaDataEntry;
import com.cloud.bridge.service.core.s3.S3PutObjectRequest;
import com.cloud.bridge.service.core.s3.S3PutObjectResponse;
import com.cloud.bridge.service.exception.InvalidBucketName;
import com.cloud.bridge.service.exception.NoSuchObjectException;
import com.cloud.bridge.service.exception.PermissionDeniedException;
import com.cloud.bridge.util.AuthenticationUtils;
import com.cloud.bridge.util.HeaderParam;
import com.cloud.bridge.util.RestAuth;
import com.cloud.bridge.util.S3SoapAuth;

/**
 * @author Kelven Yang, Mark Joseph, John Zucker
 */
public class S3RestServlet extends HttpServlet {
	private static final long serialVersionUID = -6168996266762804877L;
	
	public static final Logger logger = Logger.getLogger(S3RestServlet.class);
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
	    processRequest( req, resp, "GET" );
    }
	
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
    {
    	// -> DIME requests are authenticated via the SOAP auth mechanism
    	String type = req.getHeader( "Content-Type" );
    	if ( null != type && type.equalsIgnoreCase( "application/dime" )) 	
    	     processDimeRequest(req, resp);
    	else processRequest( req, resp, "POST" );
    }
    
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        processRequest( req, resp, "PUT" );
    }
    
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
        processRequest( req, resp, "HEAD" );
    }
    
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        processRequest( req, resp, "OPTIONS" );
    }
    
    protected void doDelete( HttpServletRequest req, HttpServletResponse resp ) {
        processRequest( req, resp, "DELETE" );
    }
    
	/**
	 * POST requests do not get authenticated on entry.   The associated
	 * access key and signature headers are embedded in the message not encoded
	 * as HTTP headers.
	 */
    private void processRequest( HttpServletRequest request, HttpServletResponse response, String method ) 
    {
        try {
        	logRequest(request);
        	
        	// Our extensions to the S3 REST API for simple management actions
        	// are conveyed with Request parameter CloudAction.
        	// The present extensions are either to set up the user credentials
        	// (see the cloud-bridge-register script for more detail) or
        	// to report our version of this capability.
        	// -> unauthenticated calls, should still be done over HTTPS
        	String cloudAction = request.getParameter( "CloudAction" );
            if (null != cloudAction) 
            {
    	        if (cloudAction.equalsIgnoreCase( "SetUserKeys" )) {
    	            setUserKeys(request, response);
    	            return;
    	        }

    	        if (cloudAction.equalsIgnoreCase( "CloudS3Version" )) {
    	            cloudS3Version(request, response);
    	            return;
    	        }
    	    }

            
    	    // -> authenticated calls
        	if (!method.equalsIgnoreCase( "POST" )) {
        	    S3AuthParams params = extractRequestHeaders( request );
        		authenticateRequest( request, params );
        	}
        	
        	ServletAction action = routeRequest(request);
        	if ( action != null ) {
        		 action.execute(request, response);
        	} 
        	else {
        		 response.setStatus(404);
            	 endResponse(response, "File not found");
        	}
        	
			PersistContext.commitTransaction();
			
        } 
        catch( InvalidBucketName e) {
    		logger.error("Unexpected exception " + e.getMessage(), e);
    		response.setStatus(400);
        	endResponse(response, "Invalid Bucket Name - " + e.toString());    	
        } 
        catch(PermissionDeniedException e) {
    		logger.error("Unexpected exception " + e.getMessage(), e);
    		response.setStatus(403);
        	endResponse(response, "Access denied - " + e.toString());
        } 
        catch(Throwable e) {
    		logger.error("Unexpected exception " + e.getMessage(), e);
    		response.setStatus(400);
        	endResponse(response, "Bad request");
        	
        } finally {
        	try {
				response.flushBuffer();
			} catch (IOException e) {
	    		logger.error("Unexpected exception " + e.getMessage(), e);
			}
			PersistContext.closeSession();
        }
    }
 
    /**
     * Provide an easy way to determine the version of the implementation running.
     * 
     * This is an unauthenticated REST call.
     */
    private void cloudS3Version( HttpServletRequest request, HttpServletResponse response ) {
        String version = new String( "<?xml version=\"1.0\" encoding=\"utf-8\"?><CloudS3Version>1.04</CloudS3Version>" );       		
		response.setStatus(200);
        endResponse(response, version);
    }

    /**
     * This request registers the user Cloud.com account holder to the S3 service.   The Cloud.com
     * account holder saves his API access and secret keys with the S3 service so that 
     * each rest call he makes can be verified was originated from him.   The given API access
     * and secret key are saved into the "usercredentials" database table.   
     * 
     * This is an unauthenticated REST call.   The only required parameters are 'accesskey' and
     * 'secretkey'. 
     * 
     * To verify that the given keys represent an existing account they are used to execute the
     * Cloud.com's listAccounts API function.   If the keys do not represent a valid account the
     * listAccounts function will fail.
     * 
     * A user can call this REST function any number of times, on each call the Cloud.com secret
     * key is simply over writes any previously stored value.
     * 
     * As with all REST calls HTTPS should be used to ensure their security.
     */
    private void setUserKeys( HttpServletRequest request, HttpServletResponse response ) {
    	String[] accessKey = null;
    	String[] secretKey = null;
    	
    	try {
		    // -> all these parameters are required
            accessKey = request.getParameterValues( "accesskey" );
		    if ( null == accessKey || 0 == accessKey.length ) { 
		         response.sendError(530, "Missing accesskey parameter" ); 
		         return; 
		    }

            secretKey = request.getParameterValues( "secretkey" );
            if ( null == secretKey || 0 == secretKey.length ) {
                 response.sendError(530, "Missing secretkey parameter" ); 
                 return; 
            }
        } catch( Exception e ) {
		    logger.error("SetUserKeys exception " + e.getMessage(), e);
    		response.setStatus(500);
        	endResponse(response, "SetUserKeys exception " + e.getMessage());
		    return;
        }

        try {
            // -> use the keys to see if the account actually exists
    	    //ServiceProvider.getInstance().getEC2Engine().validateAccount( accessKey[0], secretKey[0] );
    	    UserCredentialsDao credentialDao = new UserCredentialsDao();
    	    credentialDao.setUserKeys( accessKey[0], secretKey[0] ); 
    	    
        } catch( Exception e ) {
   		    logger.error("SetUserKeys " + e.getMessage(), e);
    		response.setStatus(401);
        	endResponse(response, e.toString());
        	return;
        }
    	response.setStatus(200);	
        endResponse(response, "User keys set successfully");
    }
    
    /**
     * We are using the S3AuthParams class to hide where the header values are coming
     * from so that the authenticateRequest call can be made from several places.
     */
    public static S3AuthParams extractRequestHeaders( HttpServletRequest request ) {
    	S3AuthParams params = new S3AuthParams();
    	   	
		Enumeration headers = request.getHeaderNames();
		if (null != headers) 
		{
			while( headers.hasMoreElements()) 
			{
	    		HeaderParam oneHeader = new HeaderParam();
	    		String headerName = (String)headers.nextElement();
	    		oneHeader.setName( headerName );
	    		oneHeader.setValue( request.getHeader( headerName ));
	    		params.addHeader( oneHeader );
			}
		}
    	return params;
    }
    
    public static void authenticateRequest( HttpServletRequest request, S3AuthParams params ) 
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException 
    {
    	RestAuth auth          = new RestAuth(ServiceProvider.getInstance().getUseSubDomain()); 	
    	String   AWSAccessKey  = null;
    	String   signature     = null;
    	String   authorization = null;
    	
    	// [A] Is it an annonymous request?
    	if (null == (authorization = params.getHeader( "Authorization" ))) {
            UserContext.current().initContext();
            return;
    	}
    	
    	// [B] Is it an authenticated request?
    	int offset = authorization.indexOf( "AWS" );
    	if (-1 != offset) {
    		String temp = authorization.substring( offset+3 ).trim();
    		offset = temp.indexOf( ":" );
    		AWSAccessKey = temp.substring( 0, offset );
    		signature    = temp.substring( offset+1 );
    	}
    
    	// [C] Calculate the signature from the request's headers
    	auth.setDateHeader( request.getHeader( "Date" ));
    	auth.setContentTypeHeader( request.getHeader( "Content-Type" ));
    	auth.setContentMD5Header( request.getHeader( "Content-MD5" ));
    	auth.setHostHeader( request.getHeader( "Host" ));
    	auth.setQueryString( request.getQueryString());
    	auth.addUriPath( request.getRequestURI());
    	
    	// -> are their any Amazon specific (i.e. 'x-amz-' ) headers?
    	HeaderParam[] headers = params.getHeaders();
    	for( int i=0; null != headers && i < headers.length; i++ )
		{
			 String headerName = headers[i].getName();
			 String ignoreCase = headerName.toLowerCase();
			 if (ignoreCase.startsWith( "x-amz-" ))
				 auth.addAmazonHeader( headerName + ":" + headers[i].getValue());
		}
		
		UserInfo info = ServiceProvider.getInstance().getUserInfo(AWSAccessKey);
		if (info == null) throw new PermissionDeniedException("Unable to authenticate access key: " + AWSAccessKey);
		
    	try {
			if (auth.verifySignature( request.getMethod(), info.getSecretKey(), signature )) {
				UserContext.current().initContext(AWSAccessKey, info.getSecretKey(), AWSAccessKey, info.getDescription(), request);
				return;
			}
		
			// -> turn off auth - just for testing
			//UserContext.current().initContext("Mark", "123", "Mark", "testing", request);
            //return;
       
		} catch (SignatureException e) {
			throw new PermissionDeniedException(e);
			
		} catch (UnsupportedEncodingException e) {
	    	throw new PermissionDeniedException(e);
		}
		throw new PermissionDeniedException("Invalid signature");
    }
    
    
    
    private ServletAction routeRequest(HttpServletRequest request) 
    {
    	// Simple URL routing for S3 REST calls.
    	String pathInfo = request.getPathInfo();
    	String bucketName = null;
    	String key = null;
    	
    	// Irrespective of whether the requester is using subdomain or full host naming of path expressions
    	// to buckets, wherever the request is made up of a service endpoint followed by a /, in AWS S3 this always
    	// conveys a ListAllMyBuckets command
    	
    	if ( ( pathInfo == null ) || ( pathInfo.indexOf('/') != 0 ) ) 
    	   {
			logger.warn("Invalid REST request URI " + pathInfo);
			return null;
		    }
    	
    	String serviceEndpoint = ServiceProvider.getInstance().getServiceEndpoint();
    	String host            = request.getHeader("Host");
    	
		if  ( (serviceEndpoint.equalsIgnoreCase( host )) && (pathInfo.equalsIgnoreCase("/")) ) {
			request.setAttribute(S3Constants.BUCKET_ATTR_KEY, "/");
			return new S3BucketAction();   // for ListAllMyBuckets
		}

		// Because there is a leading / at position 0 of pathInfo, now subtract this to process the remainder	
		pathInfo = pathInfo.substring(1); 
    			
    	if (ServiceProvider.getInstance().getUseSubDomain()) 
    		
    	    {   		
    		// -> verify the format of the bucket name
    		int endPos = host.indexOf( ServiceProvider.getInstance().getMasterDomain());
    		if ( endPos > 0 ) 
    		{
    			 bucketName = host.substring(0, endPos);
    			 S3Engine.verifyBucketName( bucketName, false );
    			 request.setAttribute(S3Constants.BUCKET_ATTR_KEY, bucketName);
    		}
    		else request.setAttribute(S3Constants.BUCKET_ATTR_KEY, "");
    		
    		if (pathInfo == null || pathInfo.equalsIgnoreCase("/")) 
    		{
    			return new S3BucketAction();
    		} 
    		else {
    			String objectKey = pathInfo.substring(1);
    			request.setAttribute(S3Constants.OBJECT_ATTR_KEY, objectKey);
    			return new S3ObjectAction();
    		}
    	}
    	
    	else 
    		
    	{
    		 		
    		int endPos = pathInfo.indexOf('/');  // Subsequent / character?
    		
    	    if (endPos < 1)
    	    {
    	    	 bucketName = pathInfo;
    	    	 S3Engine.verifyBucketName( bucketName, false );
   			     request.setAttribute(S3Constants.BUCKET_ATTR_KEY, bucketName);
   			     return new S3BucketAction();
   		    }
    	    else
    		{
    			 bucketName = pathInfo.substring(0, endPos);
    		     key        = pathInfo.substring(endPos + 1);			
   			     S3Engine.verifyBucketName( bucketName, false );
   			
    			 if (!key.isEmpty()) 
    			 {
	    		 	  request.setAttribute(S3Constants.BUCKET_ATTR_KEY, bucketName);
	    			  request.setAttribute(S3Constants.OBJECT_ATTR_KEY, pathInfo.substring(endPos + 1));
	    			  return new S3ObjectAction();
    			 } 
    			 else {
        			  request.setAttribute(S3Constants.BUCKET_ATTR_KEY, bucketName);
        			  return new S3BucketAction();
    			 }
    		}
    	}
    }
    
    
    public static void endResponse(HttpServletResponse response, String content) {
    	try {
            byte[] data = content.getBytes();
            response.setContentLength(data.length);
            OutputStream os = response.getOutputStream();
            os.write(data);
            os.close();
    	} catch(Throwable e) {
    		logger.error("Unexpected exception " + e.getMessage(), e);
    	}
    }

    public static void writeResponse(HttpServletResponse response, String content) throws IOException {
        byte[] data = content.getBytes();
        OutputStream os = response.getOutputStream();
        os.write(data);
    }
    
    public static void writeResponse(HttpServletResponse response, InputStream is) throws IOException {
    	byte[] data = new byte[4096];
    	int length = 0;
    	while((length = is.read(data)) > 0) {
    		response.getOutputStream().write(data, 0, length);
    	}
    }
    
    /**
     * A DIME request is really a SOAP request that we are dealing with, and so its
     * authentication is the SOAP authentication approach.   Since Axis2 does not handle
     * DIME messages we deal with them here.
     * 
     * @param request
     * @param response
     */
    private void processDimeRequest(HttpServletRequest request, HttpServletResponse response) {
    	S3PutObjectRequest  putRequest  = null;
    	S3PutObjectResponse putResponse = null;
    	int                 bytesRead   = 0;
    	
        S3Engine engine = new S3Engine();
    	
        try {   
        	logRequest(request);
        	
            MultiPartDimeInputStream ds = new MultiPartDimeInputStream( request.getInputStream());
        	
            // -> the first stream MUST be the SOAP party
            if (ds.nextInputStream())
            {
                //logger.debug( "DIME msg [" + ds.getStreamType() + "," + ds.getStreamTypeFormat() + "," + ds.getStreamId() + "]" );
                byte[] buffer = new byte[8192];
                bytesRead = ds.read( buffer, 0, 8192 ); 
                //logger.debug( "DIME SOAP Bytes read: " + bytesRead );
                ByteArrayInputStream bis = new ByteArrayInputStream( buffer, 0, bytesRead );
                putRequest = toEnginePutObjectRequest( bis ); 
            }
            
            // -> we only need to support a DIME message with two bodyparts
            if (null != putRequest && ds.nextInputStream())
            {
            	InputStream is = ds.getInputStream();
            	putRequest.setData( is );
            }

            // -> need to do SOAP level auth here, on failure return the SOAP fault
            StringBuffer xml = new StringBuffer();
            String AWSAccessKey = putRequest.getAccessKey();
    		UserInfo info = ServiceProvider.getInstance().getUserInfo(AWSAccessKey);
    		try 
    		{   S3SoapAuth.verifySignature( putRequest.getSignature(), "PutObject", putRequest.getRawTimestamp(), AWSAccessKey, info.getSecretKey());   	
    		
    		} catch( AxisFault e ) {
    			String reason = e.toString();
    			int start = reason.indexOf( ".AxisFault:" );
    			if (-1 != start) reason = reason.substring( start+11 );
    					
                xml.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
                xml.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" >\n" );
                xml.append( "<soap:Body>\n" );               
                xml.append( "<soap:Fault>\n" );
                xml.append( "<faultcode>" ).append( e.getFaultCode().toString()).append( "</faultcode>\n" );
                xml.append( "<faultstring>" ).append( reason ).append( "</faultstring>\n" );
                xml.append( "</soap:Fault>\n" );
                xml.append( "</soap:Body></soap:Envelope>" );
      		
          	    endResponse(response, xml.toString());
  			    PersistContext.commitTransaction();
  			    return;
    		}
    		   		
    		// -> PutObject S3 Bucket Policy would be done in the engine.handleRequest() call
    		UserContext.current().initContext( AWSAccessKey, info.getSecretKey(), AWSAccessKey, "S3 DIME request", request );
            putResponse = engine.handleRequest( putRequest );
            
            xml.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
            xml.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" );
            xml.append( "<soap:Body>" );
            xml.append( "<tns:PutObjectResponse>" );
            xml.append( "<tns:PutObjectResponse>" );
            xml.append( "<tns:ETag>\"").append( putResponse.getETag()).append( "\"</tns:ETag>" );
            xml.append( "<tns:LastModified>").append( DatatypeConverter.printDateTime(putResponse.getLastModified())).append( "</tns:LastModified>" );
            xml.append( "</tns:PutObjectResponse></tns:PutObjectResponse>" );
            xml.append( "</soap:Body></soap:Envelope>" );
            		
        	endResponse(response, xml.toString());
			PersistContext.commitTransaction();
        } 
        catch(PermissionDeniedException e) {
		    logger.error("Unexpected exception " + e.getMessage(), e);
		    response.setStatus(403);
    	    endResponse(response, "Access denied");   	
        }
        catch(Throwable e) 
        {
    		logger.error("Unexpected exception " + e.getMessage(), e);
        } 
        finally 
        {
			PersistContext.closeSession();
        }
    }

    
    /**
     * Convert the SOAP XML we extract from the DIME message into our local object.
     * Here Axis2 is not parsing the SOAP for us.   I tried to use the Amazon PutObject
     * parser but it keep throwing exceptions.
     * 
     * @param putObjectInline
     * @return 
     * @throws Exception 
     */
	public static S3PutObjectRequest toEnginePutObjectRequest( InputStream is ) throws Exception 
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware( true );
		
		DocumentBuilder db       = dbf.newDocumentBuilder();
		Document        doc      = db.parse( is );
		Node            parent   = null;
		Node            contents = null;
		NodeList        children = null;
		String          temp     = null;
		String          element  = null;
		int             count    = 0;

		S3PutObjectRequest request = new S3PutObjectRequest();

		// [A] Pull out the simple nodes first
		NodeList part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Bucket" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setBucketName( contents.getFirstChild().getNodeValue());
		}	
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Key" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setKey( contents.getFirstChild().getNodeValue());
		}		
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "ContentLength" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    {
		    	String length = contents.getFirstChild().getNodeValue();
		    	if (null != length) request.setContentLength( Long.decode( length ));
		    }
		}		
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "AWSAccessKeyId" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setAccessKey( contents.getFirstChild().getNodeValue());
		}		
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Signature" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setSignature( contents.getFirstChild().getNodeValue());
		}		
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Timestamp" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setRawTimestamp( contents.getFirstChild().getNodeValue());
		}	
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "StorageClass" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setStorageClass( contents.getFirstChild().getNodeValue());
		}		
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Credential" );
		if (null != part)
		{
		    if (null != (contents = part.item( 0 )))
		    	request.setCredential( contents.getFirstChild().getNodeValue());
		}
		
		
		// [B] Get a list of all 'Metadata' elements
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Metadata" );
		if (null != part)
		{
			count = part.getLength();
			S3MetaDataEntry[] metaEntry = new S3MetaDataEntry[ count ];
			
			for( int i=0; i < count; i++ )
			{
				 parent = part.item(i);
				 metaEntry[i] = new S3MetaDataEntry();

				 // -> get a list of all the children elements of the 'Metadata' parent element
				 if (null != (children = parent.getChildNodes()))
				 {
					 int numChildren = children.getLength();
					 for( int j=0; j < numChildren; j++ )
					 {
						  contents = children.item( j );
						  element = contents.getNodeName().trim();
						  if ( element.endsWith( "Name" ))
						  {
							   temp = contents.getFirstChild().getNodeValue();
							   if (null != temp) metaEntry[i].setName( temp );
						  }
						  else if (element.endsWith( "Value" ))
						  {
							   temp = contents.getFirstChild().getNodeValue();
							   if (null != temp) metaEntry[i].setValue( temp );
						  }
					 }
				 }
			}
			request.setMetaEntries( metaEntry );
		}

		// [C] Get a list of all Grant elements in an AccessControlList
		part = getElement( doc, "http://s3.amazonaws.com/doc/2006-03-01/", "Grant" );
		if (null != part)
		{
			S3AccessControlList engineAcl = new S3AccessControlList();

			count = part.getLength();
            for( int i=0; i < count; i++ )
            {
				 parent = part.item(i);
				 S3Grant engineGrant = new S3Grant();

				 // -> get a list of all the children elements of the 'Grant' parent element
				 if (null != (children = parent.getChildNodes()))
				 {
					 int numChildren = children.getLength();
					 for( int j=0; j < numChildren; j++ )
					 {
						contents = children.item( j );
						element  = contents.getNodeName().trim();
						if ( element.endsWith( "Grantee" ))
						{
		                     NamedNodeMap attbs = contents.getAttributes();
		                     if (null != attbs)
		                     {
		                    	 Node type = attbs.getNamedItemNS( "http://www.w3.org/2001/XMLSchema-instance", "type" );
		                    	 if ( null != type ) 
		                    		  temp = type.getFirstChild().getNodeValue().trim();
		                    	 else temp = null;
		                    	 
								 if ( null != temp && temp.equalsIgnoreCase( "CanonicalUser" ))
								 {
								      engineGrant.setGrantee(SAcl.GRANTEE_USER);
									  engineGrant.setCanonicalUserID( getChildNodeValue( contents, "ID" ));
								 } 
								 else throw new UnsupportedOperationException( "Missing http://www.w3.org/2001/XMLSchema-instance:type value" ); 
		                     }
						}
						else if (element.endsWith( "Permission" ))
						{
						     temp = contents.getFirstChild().getNodeValue().trim();
							      if (temp.equalsIgnoreCase("READ"        )) engineGrant.setPermission(SAcl.PERMISSION_READ);
							 else if (temp.equalsIgnoreCase("WRITE"       )) engineGrant.setPermission(SAcl.PERMISSION_WRITE);
							 else if (temp.equalsIgnoreCase("READ_ACP"    )) engineGrant.setPermission(SAcl.PERMISSION_READ_ACL);
							 else if (temp.equalsIgnoreCase("WRITE_ACP"   )) engineGrant.setPermission(SAcl.PERMISSION_WRITE_ACL);
							 else if (temp.equalsIgnoreCase("FULL_CONTROL")) engineGrant.setPermission(SAcl.PERMISSION_FULL);
							 else throw new UnsupportedOperationException( "Unsupported permission: " + temp ); 
						}
					 }
					 engineAcl.addGrant( engineGrant );
				 }
            }
    		request.setAcl( engineAcl );
		}
		return request;
	}
		
	/**
	 * Have to deal with XML with and without namespaces.
	 */
	public static NodeList getElement( Document doc, String namespace, String tagName ) 
	{
	    NodeList part = doc.getElementsByTagNameNS( namespace, tagName );
	    if (null == part || 0 == part.getLength()) part = doc.getElementsByTagName( tagName );
	   
	    return part;
	}

	/**
	 * Looking for the value of a specific child of the given parent node.
	 * 
	 * @param parent
	 * @param childName
	 * @return
	 */
	private static String getChildNodeValue( Node parent, String childName )
	{
		NodeList children = null;
		Node     element  = null;

		if (null != (children = parent.getChildNodes()))
		{
			 int numChildren = children.getLength();
			 for( int i=0; i < numChildren; i++ )
			 {
				if (null != (element = children.item( i )))
				{
				    // -> name may have a namespace on it
					String name = element.getNodeName().trim();
					if ( name.endsWith( childName )) 
					{
						 String value = element.getFirstChild().getNodeValue();
						 if (null != value) value = value.trim();
						 return value;
					}
				}
			 }
		}
		return null;
	}
	    
    private void logRequest(HttpServletRequest request) {
    	if(logger.isInfoEnabled()) {
    		logger.info("Request method: " + request.getMethod());
    		logger.info("Request contextPath: " + request.getContextPath());
    		logger.info("Request pathInfo: " + request.getPathInfo());
    		logger.info("Request pathTranslated: " + request.getPathTranslated());
    		logger.info("Request queryString: " + request.getQueryString());
    		logger.info("Request requestURI: " + request.getRequestURI());
    		logger.info("Request requestURL: " + request.getRequestURL());
    		logger.info("Request servletPath: " + request.getServletPath());
    		Enumeration headers = request.getHeaderNames();
    		if(headers != null) {
    			while(headers.hasMoreElements()) {
    				Object headerName = headers.nextElement();
    	    		logger.info("Request header " + headerName + ":" + request.getHeader((String)headerName));
    			}
    		}
    		
    		Enumeration params = request.getParameterNames();
    		if(params != null) {
    			while(params.hasMoreElements()) {
    				Object paramName = params.nextElement();
    	    		logger.info("Request parameter " + paramName + ":" + 
    	    			request.getParameter((String)paramName));
    			}
    		}
    		logger.info( "- End of request -" );
    	}
    }
}
