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
package com.cloud.bridge.util;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;


/**
 * This class expects that the caller pulls the required headers from the standard
 * HTTPServeletRequest structure.   This class is responsible for providing the
 * RFC2104 calculation to ensure that the signature is valid for the signing string.
 * The signing string is a representation of the request.
 * Notes are given below on what values are expected.
 * This class is used for the Authentication check for REST requests and Query String
 * Authentication requests.
 *
 */

public class RestAuth {
    protected final static Logger logger = Logger.getLogger(RestAuth.class);

	// TreeMap: used when constructing the CanonicalizedAmzHeaders Element of the StringToSign
	protected TreeMap<String, String> AmazonHeaders = null;   // not always present
	protected String                  bucketName    = null;   // not always present
	protected String                  queryString   = null;   // for CanonicalizedResource - only interested in a string starting with particular values
	protected String                  uriPath       = null;   // only interested in the resource path
	protected String                  date          = null;   // only if x-amz-date is not set
	protected String                  contentType   = null;   // not always present
	protected String                  contentMD5    = null;   // not always present
	protected boolean                 amzDateSet    = false;
	protected boolean				  useSubDomain  = false;
	
	protected Set<String> allowedQueryParams;
	
	public RestAuth() {
		// these must be lexicographically sorted
		AmazonHeaders = new TreeMap<String, String>();
		allowedQueryParams = new HashSet<String>() {{  
			add("acl");
			add("lifecycle");
			add("location");
			add("logging");
			add("notification");
			add("partNumber");
			add("policy");
			add("requestPayment");
			add("torrent");
			add("uploadId");
			add("uploads");
			add("versionId");
			add("versioning");
			add("versions");
			add("website");
			add("delete");
			}}; 
	}

	public RestAuth(boolean useSubDomain) {
		//invoke the other constructor
		this();
		this.useSubDomain = useSubDomain; 
	}
	
	public void setUseSubDomain(boolean value) {
		useSubDomain = value;
	}
	
	public boolean getUseSubDomain() {
		return useSubDomain;
	}
	
	/**
	 * This header is used iff the "x-amz-date:" header is not defined.
	 * Value is used in constructing the StringToSign for signature verification.
	 * 
	 * @param date - the contents of the "Date:" header, skipping the 'Date:' preamble.
	 *        OR pass in the value of the "Expires=" query string parameter passed in
	 *        for "Query String Authentication".
	 */
	public void setDateHeader( String date ) {
		if (this.amzDateSet) return;
		if (null != date) date = date.trim();
		this.date = date;
	}
	
	/**
	 * Value is used in constructing the StringToSign for signature verification.
     *
	 * @param type - the contents of the "Content-Type:" header, skipping the 'Content-Type:' preamble.
	 */
	public void setContentTypeHeader( String type ) {
		if (null != type) type = type.trim();
		this.contentType = type;
	}

	
	/**
	 * Value is used in constructing the StringToSign for signature verification.
	 * @param type - the contents of the "Content-MD5:" header, skipping the 'Content-MD5:' preamble.
	 */
	public void setContentMD5Header( String md5 ) {
		if (null != md5) md5 = md5.trim();
		this.contentMD5 = md5;
	}
	
		
	/**
	 * The bucket name can be in the "Host:" header but it does not have to be.  It can 
	 * instead be in the uriPath as the first step in the path.
	 * 
	 * Used as part of the CanonalizedResource element of the StringToSign.
	 * If we get "Host: static.johnsmith.net:8080",  then the bucket name is "static.johnsmith.net"
	 * 
	 * @param header - contents of the "Host:" header, skipping the 'Host:' preamble. 
	 */
	public void setHostHeader( String header ) {
		if (null == header) {
			this.bucketName = null;
			return;
		}
		
		// -> is there a port on the name?
		header = header.trim();
        int offset = header.indexOf( ":" );
        if (-1 != offset) header = header.substring( 0, offset );
        this.bucketName = header;
	}
	
	
	/**
	 * Used as part of the CanonalizedResource element of the StringToSign.
	 * CanonicalizedResource = [ "/" + Bucket ] +
	 * <HTTP-Request-URI, from the protocol name up to the query string> + [sub-resource]
	 * The list of sub-resources that must be included when constructing the CanonicalizedResource Element are: acl, lifecycle, location, 
	 * logging, notification, partNumber, policy, requestPayment, torrent, uploadId, uploads, versionId, versioning, versions and website.
	 * (http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html)
	 * @param query - results from calling "HttpServletRequest req.getQueryString()"
	 */
	public void setQueryString( String query ) {
		if (null == query) {
			this.queryString = null;
			return;
		}
		
		// Sub-resources (i.e.: query params) must be lex sorted
		Set<String> subResources = new TreeSet<String>(); 
		
		String [] queryParams = query.split("&");
		StringBuffer builtQuery= new StringBuffer();
		for (String queryParam:queryParams) {
			// lookup parameter name
			String paramName = queryParam.split("=")[0];
			if (allowedQueryParams.contains(paramName)) {
				subResources.add(queryParam);
			}
		}
		for (String subResource:subResources) {
			builtQuery.append(subResource + "&");
		}
		// If anything inside the string buffer, add a "?" at the beginning, 
		// and then remove the last '&'
		if (builtQuery.length() > 0) {
			builtQuery.insert(0, "?");
			builtQuery.deleteCharAt(builtQuery.length()-1);
		}
		this.queryString = builtQuery.toString();
	}
	
	
	/**
	 * Used as part of the CanonalizedResource element of the StringToSign.
	 * Append the path part of the un-decoded HTTP Request-URI, up-to but not including the query string.
	 * 
	 * @param path - - results from calling "HttpServletRequest req.getPathInfo()"
	 */
	public void addUriPath( String path ) {
		if (null != path) path = path.trim();
        this.uriPath = path;
	}
	
	
	/**
	 * Pass in each complete Amazon header found in the HTTP request one at a time.
	 * Each Amazon header added will become part of the signature calculation.
	 * We are using a TreeMap here because of the S3 definition:
	 *      "Sort the collection of headers lexicographically by header name."
	 * 
	 * @param headerAndValue - needs to be the complete amazon header (i.e., starts with "x-amz").
	 */
	public void addAmazonHeader( String headerAndValue ) {
		if (null == headerAndValue) return;
		
		String canonicalized = null;
		
		// [A] First Canonicalize the header and its value
		//  -> we use the header 'name' as the key since we have to sort on that
		int    offset = headerAndValue.indexOf( ":" );
		String header = headerAndValue.substring( 0, offset+1 ).toLowerCase();
		String value  = headerAndValue.substring( offset+1 ).trim();
		
		// -> RFC 2616, Section 4.2: unfold the header's value by replacing linear white space with a single space character
		// -> does the HTTPServeletReq already do this for us?
		value = value.replaceAll( "  ", " " );             // -> multiple spaces to one space
		value = value.replaceAll( "(\r\n|\t|\n)", " " );   // -> CRLF, tab, and LF to one space

		
		// [B] Does this header already exist?
		if ( AmazonHeaders.containsKey( header )) {
			 // -> combine header fields with the same name into one "header-name:comma-separated-value-list" pair as prescribed by RFC 2616, section 4.2, without any white-space between values. 
			 canonicalized = AmazonHeaders.get( header );
             canonicalized = new String( canonicalized + "," + value + "\n" );	
             canonicalized = canonicalized.replaceAll( "\n,", "," );   // remove the '\n' from the first stored value
	    }
		else canonicalized = new String( header + value + "\n" );  // -> as per spec, no space between header and its value

		AmazonHeaders.put( header, canonicalized );
		
		// [C] "x-amz-date:" takes precedence over the "Date:" header
		if (header.equals( "x-amz-date:" )) {
			this.amzDateSet = true;
			if (null != this.date) this.date = null;
		}
	}
	
	
	/**
	 * The request is authenticated if we can regenerate the same signature given 
	 * on the request.  Before calling this function make sure to set the header values
	 * defined by the public values above.
	 * 
	 * @param httpVerb  - the type of HTTP request (e.g., GET, PUT)
	 * @param secretKey - value obtained from the AWSAccessKeyId
	 * @param signature - the signature we are trying to recreate, note can be URL-encoded
	 * 
	 * @throws SignatureException 
	 * 
	 * @return true if request has been authenticated, false otherwise
	 * @throws UnsupportedEncodingException 
	 */
	
	public boolean verifySignature( String httpVerb, String secretKey, String signature )
	    throws SignatureException, UnsupportedEncodingException {
	    	
		if (null == httpVerb || null == secretKey || null == signature) return false;
        
		httpVerb  = httpVerb.trim();
		secretKey = secretKey.trim();
		signature = signature.trim();
		
		// First calculate the StringToSign after the caller has initialized all the header values
		String StringToSign = genStringToSign( httpVerb );
		String calSig       = calculateRFC2104HMAC( StringToSign, secretKey );
		// Was the passed in signature URL encoded? (it must be base64 encoded)
		int offset = signature.indexOf( "%" );
		if (-1 != offset) signature = URLDecoder.decode( signature, "UTF-8" );
	
        boolean match = signature.equals( calSig );
        if (!match) 
        	logger.error( "Signature mismatch, [" + signature + "] [" + calSig + "] over [" + StringToSign + "]" );
        
        return match;
	}

	
	/**
	 * This function generates the single string that will be used to sign with a users
	 * secret key.
	 * 
	 * StringToSign = HTTP-Verb + "\n" +
	 * Content-MD5 + "\n" +
	 * Content-Type + "\n" +
	 * Date + "\n" +
	 * CanonicalizedAmzHeaders +
	 * CanonicalizedResource;
	 * 
	 * @return The single StringToSign or null.
     */
	private String genStringToSign( String httpVerb ) {
		StringBuffer canonicalized = new StringBuffer();
		String temp = null;
		String canonicalizedResourceElement = genCanonicalizedResourceElement();
		canonicalized.append( httpVerb ).append( "\n" );
		if ( (null != this.contentMD5) ) 
			canonicalized.append( this.contentMD5 );
		canonicalized.append( "\n" );
		
		if ( (null != this.contentType) )
			canonicalized.append( this.contentType );
		canonicalized.append( "\n" );

		if (null != this.date) 
			canonicalized.append( this.date );
		
		canonicalized.append( "\n" );
		
		if (null != (temp = genCanonicalizedAmzHeadersElement())) canonicalized.append( temp );
		if (null != canonicalizedResourceElement) canonicalized.append( canonicalizedResourceElement );
		
		if ( 0 == canonicalized.length())
			 return null;
		
		return canonicalized.toString();
	}
	
	
	/**
	 * CanonicalizedResource represents the Amazon S3 resource targeted by the request.
	 * CanonicalizedResource = [ "/" + Bucket ] +
	 *    <HTTP-Request-URI, from the protocol name up to the query string> +
	 *    [ sub-resource, if present. For example "?acl", "?location", "?logging", or "?torrent"];
     *
	 * @return A single string representing CanonicalizedResource or null.
	 */
	private String genCanonicalizedResourceElement() {
		StringBuffer canonicalized = new StringBuffer();

		if(this.useSubDomain && this.bucketName != null)
			canonicalized.append( "/" ).append( this.bucketName );
		
		if (null != this.uriPath    ) canonicalized.append( this.uriPath );
		if (null != this.queryString) canonicalized.append( this.queryString );
		
		if ( 0 == canonicalized.length())
			 return null;
		
		return canonicalized.toString();
	}
	
	
	/**
	 * Construct the Canonicalized Amazon headers element of the StringToSign by 
	 * concatenating all headers in the TreeMap into a single string.
	 * 
	 * @return A single string with all the Amazon headers glued together, or null
	 *         if no Amazon headers appeared in the request.
	 */
	private String genCanonicalizedAmzHeadersElement() {
		Collection<String> headers       = AmazonHeaders.values();
		Iterator<String>   itr           = headers.iterator();
		StringBuffer       canonicalized = new StringBuffer();
		
		while( itr.hasNext()) 
			canonicalized.append( itr.next());
		
		if ( 0 == canonicalized.length())
			 return null;
		
		return canonicalized.toString();
	}
	
	
    /**
     * Create a signature by the following method:
     *     new String( Base64( SHA1( key, byte array )))
     * 
     * @param signIt    - the data to generate a keyed HMAC over
     * @param secretKey - the user's unique key for the HMAC operation
     * @return String   - the recalculated string
     * @throws SignatureException
     */
    private String calculateRFC2104HMAC( String signIt, String secretKey )
        throws SignatureException {
   	    String result = null;
   	    try { 	
   	    	SecretKeySpec key = new SecretKeySpec( secretKey.getBytes(), "HmacSHA1" );
   	        Mac hmacSha1 = Mac.getInstance( "HmacSHA1" );
    	    hmacSha1.init( key ); 
            byte [] rawHmac = hmacSha1.doFinal( signIt.getBytes());
            result = new String( Base64.encodeBase64( rawHmac ));
   	        } 
   	    catch( InvalidKeyException e ) {
   		    throw new SignatureException( "Failed to generate keyed HMAC on REST request because key " + secretKey + " is invalid" + e.getMessage());
   	         }
   	    catch (Exception e) {
 		    throw new SignatureException( "Failed to generate keyed HMAC on REST request: " + e.getMessage());  
   	         }
   	    return result.trim();
    }
}
