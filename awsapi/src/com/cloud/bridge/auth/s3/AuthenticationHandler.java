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
package com.cloud.bridge.auth.s3;

import java.sql.SQLException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Logger;

import com.cloud.bridge.model.UserCredentialsVO;
import com.cloud.bridge.persist.dao.UserCredentialsDaoImpl;
import com.cloud.bridge.service.UserContext;
import com.cloud.bridge.util.S3SoapAuth;

/*
 *  For SOAP compatibility.
 */

public class AuthenticationHandler implements Handler {
    protected final static Logger logger = Logger.getLogger(AuthenticationHandler.class);
    @Inject UserCredentialsDaoImpl ucDao;
    protected HandlerDescription handlerDesc = new HandlerDescription( "default handler" );
    private String name = "S3AuthenticationHandler";

    @Override
    public void init( HandlerDescription handlerdesc ) 
    {
        this.handlerDesc = handlerdesc;
    }

    @Override
    public String getName() 
    {
        //logger.debug( "getName entry S3AuthenticationHandler" + name );
        return name;
    }

    @Override
    public String toString() 
    {
        return (name != null) ? name.toString() : null;
    }

    @Override
    public HandlerDescription getHandlerDesc() 
    {
        return handlerDesc;
    }

    @Override
    public Parameter getParameter( String name ) 
    {
        return handlerDesc.getParameter( name );
    } 


    /**
     * Verify the request's authentication signature by extracting all the 
     * necessary parts of the request, obtaining the requestor's secret key, and
     * recalculating the signature.
     * 
     * On Signature mismatch raise an AxisFault (i.e., a SoapFault) with what Amazon S3 
     * defines as a "Client.SignatureMismatch" error.
     * 
     * Special case: need to deal with anonymous requests where no AWSAccessKeyId is
     * given.   In this case just pass the request on.
     */
    @Override
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault  
    {
        String accessKey  = null;
        String operation  = null;
        String msgSig     = null;
        String timestamp  = null;
        String secretKey  = null;
        String temp       = null;

        // [A] Obtain the HttpServletRequest object 
        HttpServletRequest httpObj =(HttpServletRequest)msgContext.getProperty("transport.http.servletRequest");
        if (null != httpObj) System.out.println("S3 SOAP auth test header access - acceptable Encoding type: "+ httpObj.getHeader("Accept-Encoding"));

        // [A] Try to recalculate the signature for non-anonymous requests
        try
        {  SOAPEnvelope soapEnvelope = msgContext.getEnvelope();
        SOAPBody     soapBody     = soapEnvelope.getBody();
        String       xmlBody      = soapBody.toString();
        //logger.debug( "xmlrequest: " + xmlBody );

        // -> did we get here yet its an EC2 request?
        int offset = xmlBody.indexOf( "http://ec2.amazonaws.com" );
        if (-1 != offset) return InvocationResponse.CONTINUE;


        // -> if it is anonymous request, then no access key should exist
        int start = xmlBody.indexOf( "AWSAccessKeyId>" );
        if (-1 == start) {
            UserContext.current().initContext();
            return InvocationResponse.CONTINUE;
        }           
        temp = xmlBody.substring( start+15 );
        int end   = temp.indexOf( "</" );
        accessKey = temp.substring( 0, end );
        //logger.debug( "accesskey " + accessKey );


        // -> what if we cannot find the user's key?
        if (null != (secretKey = lookupSecretKey( accessKey )))
        {
            // -> if any other field is missing, then the signature will not match
            if ( null != (operation = soapBody.getFirstElementLocalName()))
                operation = operation.trim();
            else operation = "";
            //logger.debug( "operation " + operation );

            start = xmlBody.indexOf( "Timestamp>" );
            if ( -1 < start )
            {
                temp = xmlBody.substring( start+10 );
                end  = temp.indexOf( "</" );
                timestamp = temp.substring( 0, end );
                //logger.debug( "timestamp " + timestamp );
            }
            else timestamp = "";

            start  = xmlBody.indexOf( "Signature>" );
            if ( -1 < start )
            {
                temp = xmlBody.substring( start+10 );
                end  = temp.indexOf( "</" );
                msgSig = temp.substring( 0, end );
                //logger.debug( "signature " + msgSig );
            }
            else msgSig = "";
        }
        }
        catch( Exception e )
        {
            logger.error("Signature calculation failed due to: ", e);
            throw new AxisFault( e.toString(), "Server.InternalError" );
        }


        // [B] Verify that the given signature matches what we calculated here
        if (null == secretKey)
        {
            logger.error( "Unknown AWSAccessKeyId: [" + accessKey + "]" );
            throw new AxisFault( "Unknown AWSAccessKeyId: [" + accessKey + "]", "Client.InvalidAccessKeyId" );
        }

        // -> for SOAP requests the Cloud API keys are sent here and only here
        S3SoapAuth.verifySignature( msgSig, operation, timestamp, accessKey, secretKey );   	
        UserContext.current().initContext( accessKey, secretKey, accessKey, "S3 SOAP request", httpObj );
        return InvocationResponse.CONTINUE;
    }


    public void revoke(MessageContext msgContext) 
    {
        logger.info(msgContext.getEnvelope().toString());
    }

    public void setName(String name) 
    {
        //logger.debug( "setName entry S3AuthenticationHandler " + name );
        this.name = name;
    }

    /**
     * Given the user's access key, then obtain his secret key in the user database.
     * 
     * @param accessKey - a unique string allocated for each registered user
     * @return the secret key or null of no matching user found
     */
    private String lookupSecretKey( String accessKey )
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
            {
        UserCredentialsVO cloudKeys = ucDao.getByAccessKey( accessKey );
        if ( null == cloudKeys ) {
            logger.debug( accessKey + " is not defined in the S3 service - call SetUserKeys" );
            return null; 
        }
        else return cloudKeys.getSecretKey(); 
            }

    @Override
    public void cleanup() 
    {
        //logger.debug( "cleanup entry S3AuthenticationHandler " );
    }

    @Override
    public void flowComplete( MessageContext arg0 ) 
    {
        //logger.debug( "flowComplete entry S3AuthenticationHandler " );
    }
}

