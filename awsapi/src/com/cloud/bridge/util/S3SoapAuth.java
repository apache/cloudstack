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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.axis2.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;


/**
 * Both the SOAP code and the DIME implementation in the REST servlet need
 * this authentication functionality.
 */
public class S3SoapAuth {
    protected final static Logger logger = Logger.getLogger(S3SoapAuth.class);

	public S3SoapAuth() {
	}

	public static void verifySignature( String givenSignature, String operation, String timestamp, String accessKey, String secretKey ) 
	    throws AxisFault {
        // -> calculate RFC 2104 HMAC-SHA1 digest over the constructed string
        String signString = "AmazonS3" + operation + timestamp;
        String calSig     = calculateRFC2104HMAC( signString, secretKey );
               
        if ( null == calSig || !givenSignature.equals( calSig ))
        {
     	     logger.error( "Signature mismatch, [" + givenSignature + "] [" + calSig + "] over [" + signString + "]" );
    	 	 throw new AxisFault( "Authentication signature mismatch on AccessKey: [" + accessKey + "] [" + operation + "]",
                                  "Client.SignatureDoesNotMatch" );
        } 
	}
	
    /**
     * Create a signature by the following method:
     *     new String( Base64( SHA1( key, byte array )))
     * 
     * @param signIt    - the data to generate a keyed HMAC over
     * @param secretKey - the user's unique key for the HMAC operation
     * @return String   - the recalculated string
     */
    private static String calculateRFC2104HMAC( String signIt, String secretKey ) {
   	    String result = null;
   	    try
   	    { 	 SecretKeySpec key = new SecretKeySpec( secretKey.getBytes(), "HmacSHA1" );
   	         Mac hmacSha1 = Mac.getInstance( "HmacSHA1" );
   	         hmacSha1.init( key ); 
             byte [] rawHmac = hmacSha1.doFinal( signIt.getBytes());
             result = new String( Base64.encodeBase64( rawHmac ));
   	 
   	    } catch( Exception e ) {
    	     logger.error( "Failed to generate keyed HMAC on soap request: " + e.getMessage());
             return null;
   	    }
   	    return result.trim();
    }
}
