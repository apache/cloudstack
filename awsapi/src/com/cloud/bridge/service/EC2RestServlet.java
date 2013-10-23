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
package com.cloud.bridge.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.databinding.ADBBean;
import org.apache.axis2.databinding.ADBException;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLSerializer;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.amazon.ec2.AllocateAddressResponse;
import com.amazon.ec2.AssociateAddressResponse;
import com.amazon.ec2.AttachVolumeResponse;
import com.amazon.ec2.AuthorizeSecurityGroupIngressResponse;
import com.amazon.ec2.CreateImageResponse;
import com.amazon.ec2.CreateKeyPairResponse;
import com.amazon.ec2.CreateSecurityGroupResponse;
import com.amazon.ec2.CreateSnapshotResponse;
import com.amazon.ec2.CreateTagsResponse;
import com.amazon.ec2.CreateVolumeResponse;
import com.amazon.ec2.DeleteKeyPairResponse;
import com.amazon.ec2.DeleteSecurityGroupResponse;
import com.amazon.ec2.DeleteSnapshotResponse;
import com.amazon.ec2.DeleteTagsResponse;
import com.amazon.ec2.DeleteVolumeResponse;
import com.amazon.ec2.DeregisterImageResponse;
import com.amazon.ec2.DescribeAvailabilityZonesResponse;
import com.amazon.ec2.DescribeImageAttributeResponse;
import com.amazon.ec2.DescribeImagesResponse;
import com.amazon.ec2.DescribeInstanceAttributeResponse;
import com.amazon.ec2.DescribeInstancesResponse;
import com.amazon.ec2.DescribeKeyPairsResponse;
import com.amazon.ec2.DescribeSecurityGroupsResponse;
import com.amazon.ec2.DescribeSnapshotsResponse;
import com.amazon.ec2.DescribeTagsResponse;
import com.amazon.ec2.DescribeVolumesResponse;
import com.amazon.ec2.DetachVolumeResponse;
import com.amazon.ec2.DisassociateAddressResponse;
import com.amazon.ec2.GetPasswordDataResponse;
import com.amazon.ec2.ImportKeyPairResponse;
import com.amazon.ec2.ModifyImageAttributeResponse;
import com.amazon.ec2.ModifyInstanceAttributeResponse;
import com.amazon.ec2.RebootInstancesResponse;
import com.amazon.ec2.RegisterImageResponse;
import com.amazon.ec2.ReleaseAddressResponse;
import com.amazon.ec2.ResetImageAttributeResponse;
import com.amazon.ec2.RevokeSecurityGroupIngressResponse;
import com.amazon.ec2.RunInstancesResponse;
import com.amazon.ec2.StartInstancesResponse;
import com.amazon.ec2.StopInstancesResponse;
import com.amazon.ec2.TerminateInstancesResponse;
import com.cloud.bridge.model.UserCredentialsVO;
import com.cloud.bridge.persist.dao.CloudStackUserDaoImpl;
import com.cloud.bridge.persist.dao.OfferingDaoImpl;
import com.cloud.bridge.persist.dao.UserCredentialsDaoImpl;
import com.cloud.bridge.service.controller.s3.ServiceProvider;
import com.cloud.bridge.service.core.ec2.EC2AddressFilterSet;
import com.cloud.bridge.service.core.ec2.EC2AssociateAddress;
import com.cloud.bridge.service.core.ec2.EC2AuthorizeRevokeSecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2AvailabilityZonesFilterSet;
import com.cloud.bridge.service.core.ec2.EC2CreateImage;
import com.cloud.bridge.service.core.ec2.EC2CreateKeyPair;
import com.cloud.bridge.service.core.ec2.EC2CreateVolume;
import com.cloud.bridge.service.core.ec2.EC2DeleteKeyPair;
import com.cloud.bridge.service.core.ec2.EC2DescribeAddresses;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZones;
import com.cloud.bridge.service.core.ec2.EC2DescribeImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2DescribeImages;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstances;
import com.cloud.bridge.service.core.ec2.EC2DescribeKeyPairs;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroups;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshots;
import com.cloud.bridge.service.core.ec2.EC2DescribeTags;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumes;
import com.cloud.bridge.service.core.ec2.EC2DisassociateAddress;
import com.cloud.bridge.service.core.ec2.EC2Engine;
import com.cloud.bridge.service.core.ec2.EC2Filter;
import com.cloud.bridge.service.core.ec2.EC2GroupFilterSet;
import com.cloud.bridge.service.core.ec2.EC2Image;
import com.cloud.bridge.service.core.ec2.EC2ImageFilterSet;
import com.cloud.bridge.service.core.ec2.EC2ImageAttributes.ImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2ImageLaunchPermission;
import com.cloud.bridge.service.core.ec2.EC2ImportKeyPair;
import com.cloud.bridge.service.core.ec2.EC2InstanceFilterSet;
import com.cloud.bridge.service.core.ec2.EC2IpPermission;
import com.cloud.bridge.service.core.ec2.EC2KeyPairFilterSet;
import com.cloud.bridge.service.core.ec2.EC2ModifyImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2ModifyInstanceAttribute;
import com.cloud.bridge.service.core.ec2.EC2RebootInstances;
import com.cloud.bridge.service.core.ec2.EC2RegisterImage;
import com.cloud.bridge.service.core.ec2.EC2ReleaseAddress;
import com.cloud.bridge.service.core.ec2.EC2RunInstances;
import com.cloud.bridge.service.core.ec2.EC2SecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2SnapshotFilterSet;
import com.cloud.bridge.service.core.ec2.EC2StartInstances;
import com.cloud.bridge.service.core.ec2.EC2StopInstances;
import com.cloud.bridge.service.core.ec2.EC2Tags;
import com.cloud.bridge.service.core.ec2.EC2TagsFilterSet;
import com.cloud.bridge.service.core.ec2.EC2Volume;
import com.cloud.bridge.service.core.ec2.EC2VolumeFilterSet;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.service.exception.NoSuchObjectException;
import com.cloud.bridge.service.exception.PermissionDeniedException;
import com.cloud.bridge.util.AuthenticationUtils;
import com.cloud.bridge.util.ConfigurationHelper;
import com.cloud.bridge.util.EC2RestAuth;
import com.cloud.stack.models.CloudStackAccount;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

@Component("EC2RestServlet")
public class EC2RestServlet extends HttpServlet {

    private static final long serialVersionUID = -6168996266762804888L;
    @Inject UserCredentialsDaoImpl ucDao;
    @Inject OfferingDaoImpl ofDao;
    @Inject CloudStackUserDaoImpl userDao;

    public static final Logger logger = Logger.getLogger(EC2RestServlet.class);

    private final OMFactory factory = OMAbstractFactory.getOMFactory();
    private final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();

    private String pathToKeystore   = null;
    private String keystorePassword = null;
    private String wsdlVersion      = null;
    private String version          = null;

    boolean debug=true;

    public EC2RestServlet() {
    }

    /**
     * We build the path to where the keystore holding the WS-Security X509 certificates
     * are stored.
     */
    @Override
    public void init( ServletConfig config ) throws ServletException {
    	SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());       	

    	File propertiesFile = ConfigurationHelper.findConfigurationFile("ec2-service.properties");
        Properties EC2Prop = null;

        if (null != propertiesFile) {
            logger.info("Use EC2 properties file: " + propertiesFile.getAbsolutePath());
            EC2Prop = new Properties();
            try {
                EC2Prop.load( new FileInputStream( propertiesFile ));
            } catch (FileNotFoundException e) {
                logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
            } catch (IOException e) {
                logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
            }
            String keystore  = EC2Prop.getProperty( "keystore" );
            keystorePassword = EC2Prop.getProperty( "keystorePass" );
            wsdlVersion      = EC2Prop.getProperty( "WSDLVersion", "2012-08-15" );
            version = EC2Prop.getProperty( "cloudbridgeVersion", "UNKNOWN VERSION" );

            String installedPath = System.getenv("CATALINA_HOME");
            if (installedPath == null) installedPath = System.getenv("CATALINA_BASE");
            if (installedPath == null) installedPath = System.getProperty("catalina.home");
            String webappPath = config.getServletContext().getRealPath("/");
            //pathToKeystore = new String( installedPath + File.separator + "webapps" + File.separator + webappName + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + keystore );
            pathToKeystore = new String( webappPath +  File.separator +  "WEB-INF" + File.separator + "classes" + File.separator + keystore );
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        doGetOrPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doGetOrPost(req, resp);
    }

    protected void doGetOrPost(HttpServletRequest request, HttpServletResponse response) {

        if(debug){
            System.out.println("EC2RestServlet.doGetOrPost: javax.servlet.forward.request_uri: "+request.getAttribute("javax.servlet.forward.request_uri"));
            System.out.println("EC2RestServlet.doGetOrPost: javax.servlet.forward.context_path: "+request.getAttribute("javax.servlet.forward.context_path"));
            System.out.println("EC2RestServlet.doGetOrPost: javax.servlet.forward.servlet_path: "+request.getAttribute("javax.servlet.forward.servlet_path"));
            System.out.println("EC2RestServlet.doGetOrPost: javax.servlet.forward.path_info: "+request.getAttribute("javax.servlet.forward.path_info"));
            System.out.println("EC2RestServlet.doGetOrPost: javax.servlet.forward.query_string: "+request.getAttribute("javax.servlet.forward.query_string"));

        }


        String action = request.getParameter( "Action" );
        logRequest(request);

        // -> unauthenticated calls, should still be done over HTTPS
        if (action.equalsIgnoreCase( "SetUserKeys" )) {
            setUserKeys(request, response);
            return;
        }

        if (action.equalsIgnoreCase( "CloudEC2Version" )) {
            cloudEC2Version(request, response);
            return;
        }

        // -> authenticated calls
        try {
            if (!authenticateRequest( request, response )) return;

            if (action.equalsIgnoreCase( "AllocateAddress"           )) allocateAddress(request, response);
            else if (action.equalsIgnoreCase( "AssociateAddress"          )) associateAddress(request, response);
            else if (action.equalsIgnoreCase( "AttachVolume"              )) attachVolume(request, response );
            else if (action.equalsIgnoreCase( "AuthorizeSecurityGroupIngress" )) authorizeSecurityGroupIngress(request, response);  
            else if (action.equalsIgnoreCase( "CreateImage"               )) createImage(request, response);
            else if (action.equalsIgnoreCase( "CreateSecurityGroup"       )) createSecurityGroup(request, response);
            else if (action.equalsIgnoreCase( "CreateSnapshot"            )) createSnapshot(request, response); 
            else if (action.equalsIgnoreCase( "CreateVolume"              )) createVolume(request, response);  
            else if (action.equalsIgnoreCase( "DeleteSecurityGroup"       )) deleteSecurityGroup(request, response);  
            else if (action.equalsIgnoreCase( "DeleteSnapshot"            )) deleteSnapshot(request, response); 
            else if (action.equalsIgnoreCase( "DeleteVolume"              )) deleteVolume(request, response);   
            else if (action.equalsIgnoreCase( "DeregisterImage"           )) deregisterImage(request, response);    
            else if (action.equalsIgnoreCase( "DescribeAddresses"         )) describeAddresses(request, response);
            else if (action.equalsIgnoreCase( "DescribeAvailabilityZones" )) describeAvailabilityZones(request, response); 
            else if (action.equalsIgnoreCase( "DescribeImageAttribute"    )) describeImageAttribute(request, response);  
            else if (action.equalsIgnoreCase( "DescribeImages"            )) describeImages(request, response);  
            else if (action.equalsIgnoreCase( "DescribeInstanceAttribute" )) describeInstanceAttribute(request, response);  
            else if (action.equalsIgnoreCase( "DescribeInstances"         )) describeInstances(request, response);  
            else if (action.equalsIgnoreCase( "DescribeSecurityGroups"    )) describeSecurityGroups(request, response);  
            else if (action.equalsIgnoreCase( "DescribeSnapshots"         )) describeSnapshots(request, response);  
            else if (action.equalsIgnoreCase( "DescribeVolumes"           )) describeVolumes(request, response); 
            else if (action.equalsIgnoreCase( "DetachVolume"              )) detachVolume(request, response);  
            else if (action.equalsIgnoreCase( "DisassociateAddress"       )) disassociateAddress(request, response);
            else if (action.equalsIgnoreCase( "ModifyImageAttribute"      )) modifyImageAttribute(request, response);  
            else if (action.equalsIgnoreCase( "ModifyInstanceAttribute"   )) modifyInstanceAttribute(request, response);
            else if (action.equalsIgnoreCase( "RebootInstances"           )) rebootInstances(request, response);  
            else if (action.equalsIgnoreCase( "RegisterImage"             )) registerImage(request, response);  
            else if (action.equalsIgnoreCase( "ReleaseAddress"            )) releaseAddress(request, response);
            else if (action.equalsIgnoreCase( "ResetImageAttribute"       )) resetImageAttribute(request, response);  
            else if (action.equalsIgnoreCase( "RevokeSecurityGroupIngress")) revokeSecurityGroupIngress(request, response);
            else if (action.equalsIgnoreCase( "RunInstances"              )) runInstances(request, response);   
            else if (action.equalsIgnoreCase( "StartInstances"            )) startInstances(request, response);  
            else if (action.equalsIgnoreCase( "StopInstances"             )) stopInstances(request, response); 
            else if (action.equalsIgnoreCase( "TerminateInstances"        )) terminateInstances(request, response); 
            else if (action.equalsIgnoreCase( "SetCertificate"            )) setCertificate(request, response);
            else if (action.equalsIgnoreCase( "DeleteCertificate"         )) deleteCertificate(request, response);
            else if (action.equalsIgnoreCase( "SetOfferMapping"           )) setOfferMapping(request, response);
            else if (action.equalsIgnoreCase( "DeleteOfferMapping"        )) deleteOfferMapping(request, response);      	    
            else if (action.equalsIgnoreCase( "CreateKeyPair"             )) createKeyPair(request, response);
            else if (action.equalsIgnoreCase( "ImportKeyPair"             )) importKeyPair(request, response);
            else if (action.equalsIgnoreCase( "DeleteKeyPair"             )) deleteKeyPair(request, response);
            else if (action.equalsIgnoreCase( "DescribeKeyPairs"          )) describeKeyPairs(request, response);
            else if (action.equalsIgnoreCase( "CreateTags"                )) createTags(request, response);
            else if (action.equalsIgnoreCase( "DeleteTags"                )) deleteTags(request, response);
            else if (action.equalsIgnoreCase( "DescribeTags"              )) describeTags(request, response);
            else if (action.equalsIgnoreCase( "GetPasswordData"           )) getPasswordData(request, response);
            else {
                logger.error("Unsupported action " + action);
                throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
            }

        } catch( EC2ServiceException e ) {
            response.setStatus(e.getErrorCode());

            if (e.getCause() != null && e.getCause() instanceof AxisFault) {
                String errorCode = ((AxisFault)e.getCause()).getFaultCode().getLocalPart();
                if (errorCode.startsWith("Client.")) // only in a SOAP API client error code is prefixed with Client.
                    errorCode = errorCode.split("Client.")[1];
                else if (errorCode.startsWith("Server.")) // only in a SOAP API server error code is prefixed with Server.
                    errorCode = errorCode.split("Server.")[1];
                faultResponse(response, errorCode, e.getMessage());
            }
            else {
                logger.error("EC2ServiceException: " + e.getMessage(), e);
                endResponse(response, e.toString());
            }
        } catch( PermissionDeniedException e ) {
            logger.error("Unexpected exception: " + e.getMessage(), e);
            response.setStatus(403);
            endResponse(response, "Access denied");

        } catch( Exception e ) {
            logger.error("Unexpected exception: " + e.getMessage(), e);
            response.setStatus(500);
            endResponse(response, e.toString());

        } finally {
            try {
                response.flushBuffer();
            } catch (IOException e) {
                logger.error("Unexpected exception " + e.getMessage(), e);
            }
        }       
    }

    /**
     * Provide an easy way to determine the version of the implementation running.
     * 
     * This is an unauthenticated REST call.
     */
    private void cloudEC2Version( HttpServletRequest request, HttpServletResponse response ) {
        String version_response = new String( "<?xml version=\"1.0\" encoding=\"utf-8\"?><CloudEC2Version>" + version + "</CloudEC2Version>" );
        response.setStatus(200);
        endResponse(response, version_response);
    }

    /**
     * This request registers the Cloud.com account holder to the EC2 service.   The Cloud.com
     * account holder saves his API access and secret keys with the EC2 service so that 
     * the EC2 service can make Cloud.com API calls on his behalf.   The given API access
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
        TransactionLegacy txn = null;
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
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            txn.start();
            // -> use the keys to see if the account actually exists
            ServiceProvider.getInstance().getEC2Engine().validateAccount( accessKey[0], secretKey[0] );
            UserCredentialsVO user = new UserCredentialsVO(accessKey[0], secretKey[0]);
            ucDao.persist(user);
            txn.commit();
        } catch( Exception e ) {
            logger.error("SetUserKeys " + e.getMessage(), e);
            response.setStatus(401);
            endResponse(response, e.toString());
            txn.close();
            return;
        }
        response.setStatus(200);	
        endResponse(response, "User keys set successfully");
    }

    /**
     * The SOAP API for EC2 uses WS-Security to sign all client requests.  This requires that 
     * the client have a public/private key pair and the public key defined by a X509 certificate.
     * Thus in order for a Cloud.com account holder to use the EC2's SOAP API he must register
     * his X509 certificate with the EC2 service.   This function allows the Cloud.com account
     * holder to "load" his X509 certificate into the service.   Note, that the SetUserKeys REST
     * function must be called before this call.
     * 
     * This is an authenticated REST call and as such must contain all the required REST parameters
     * including: Signature, Timestamp, Expires, etc.   The signature is calculated using the
     * Cloud.com account holder's API access and secret keys and the Amazon defined EC2 signature
     * algorithm.
     * 
     * A user can call this REST function any number of times, on each call the X509 certificate
     * simply over writes any previously stored value.
     */
    private void setCertificate( HttpServletRequest request, HttpServletResponse response ) 
            throws Exception { 
        TransactionLegacy txn = null;
        try {
            // [A] Pull the cert and cloud AccessKey from the request
            String[] certificate = request.getParameterValues( "cert" );
            if (null == certificate || 0 == certificate.length) {
                response.sendError(530, "Missing cert parameter" );
                return;
            }
//    	    logger.debug( "SetCertificate cert: [" + certificate[0] + "]" );

            String [] accessKey = request.getParameterValues( "AWSAccessKeyId" );
            if ( null == accessKey || 0 == accessKey.length ) { 
                response.sendError(530, "Missing AWSAccessKeyId parameter" ); 
                return; 
            }

            // [B] Open our keystore
            FileInputStream fsIn = new FileInputStream( pathToKeystore );
            KeyStore certStore = KeyStore.getInstance( "JKS" );
            certStore.load( fsIn, keystorePassword.toCharArray());

            // -> use the Cloud API key to save the cert in the keystore
            // -> write the cert into the keystore on disk
            Certificate userCert = null;
            CertificateFactory cf = CertificateFactory.getInstance( "X.509" );

            ByteArrayInputStream bs = new ByteArrayInputStream( certificate[0].getBytes());
            while (bs.available() > 0) userCert = cf.generateCertificate(bs);
            certStore.setCertificateEntry( accessKey[0], userCert );

            FileOutputStream fsOut = new FileOutputStream( pathToKeystore );
            certStore.store( fsOut, keystorePassword.toCharArray());

            // [C] Associate the cert's uniqueId with the Cloud API keys
            String uniqueId = AuthenticationUtils.X509CertUniqueId( userCert );
            logger.debug( "SetCertificate, uniqueId: " + uniqueId );
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            txn.start();
            UserCredentialsVO user = ucDao.getByAccessKey(accessKey[0]);
            user.setCertUniqueId(uniqueId);
            ucDao.update(user.getId(), user);
            response.setStatus(200);
            endResponse(response, "User certificate set successfully");
            txn.commit();

        } catch( NoSuchObjectException e ) {
            logger.error("SetCertificate exception " + e.getMessage(), e);
            response.sendError(404, "SetCertificate exception " + e.getMessage());

        } catch( Exception e ) {
            logger.error("SetCertificate exception " + e.getMessage(), e);
            response.sendError(500, "SetCertificate exception " + e.getMessage());
        } finally {
            txn.close();
        }

    }

    /**
     * The SOAP API for EC2 uses WS-Security to sign all client requests.  This requires that 
     * the client have a public/private key pair and the public key defined by a X509 certificate.
     * This REST call allows a Cloud.com account holder to remove a previouly "loaded" X509
     * certificate out of the EC2 service.
     * 
     * This is an unauthenticated REST call and as such must contain all the required REST parameters
     * including: Signature, Timestamp, Expires, etc.   The signature is calculated using the
     * Cloud.com account holder's API access and secret keys and the Amazon defined EC2 signature
     * algorithm.
     */
    private void deleteCertificate( HttpServletRequest request, HttpServletResponse response ) 
            throws Exception {
        TransactionLegacy txn = null;
        try {
            String [] accessKey = request.getParameterValues( "AWSAccessKeyId" );
            if ( null == accessKey || 0 == accessKey.length ) { 
                response.sendError(530, "Missing AWSAccessKeyId parameter" ); 
                return; 
            }

            // -> delete the specified entry and save back to disk
            FileInputStream fsIn = new FileInputStream( pathToKeystore );
            KeyStore certStore = KeyStore.getInstance( "JKS" );
            certStore.load( fsIn, keystorePassword.toCharArray());

            if ( certStore.containsAlias( accessKey[0] )) {
                certStore.deleteEntry( accessKey[0] );
                FileOutputStream fsOut = new FileOutputStream( pathToKeystore );
                certStore.store( fsOut, keystorePassword.toCharArray());

                // -> dis-associate the cert's uniqueId with the Cloud API keys
                /*	     	     UserCredentialsDao credentialDao = new UserCredentialsDao();
	     	     credentialDao.setCertificateId( accessKey[0], null );

                 */	     	     txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
                 UserCredentialsVO user = ucDao.getByAccessKey(accessKey[0]);
                 user.setCertUniqueId(null);
                 ucDao.update(user.getId(), user);
                 response.setStatus(200);
                 endResponse(response, "User certificate deleted successfully");
                 txn.commit();
            }
            else response.setStatus(404);

        } catch( NoSuchObjectException e ) {
            logger.error("SetCertificate exception " + e.getMessage(), e);
            response.sendError(404, "SetCertificate exception " + e.getMessage());

        } catch( Exception e ) {
            logger.error("DeleteCertificate exception " + e.getMessage(), e);
            response.sendError(500, "DeleteCertificate exception " + e.getMessage());
        } finally {
            txn.close();
        }
    }

    /**
     * Allow the caller to define the mapping between the Amazon instance type strings
     * (e.g., m1.small, cc1.4xlarge) and the cloudstack service offering ids.  Setting
     * an existing mapping just over writes the prevous values.
     */
    private void setOfferMapping( HttpServletRequest request, HttpServletResponse response ) {
        String amazonOffer = null;
        String cloudOffer = null;

        try {
            // -> all these parameters are required
            amazonOffer = request.getParameter( "amazonoffer" );
            if ( null == amazonOffer ) { 
                response.sendError(530, "Missing amazonoffer parameter" ); 
                return; 
            }

            cloudOffer = request.getParameter( "cloudoffer" );
            if ( null == cloudOffer ) {
                response.sendError(530, "Missing cloudoffer parameter" ); 
                return; 
            }
        } catch( Exception e ) {
            logger.error("SetOfferMapping exception " + e.getMessage(), e);
            response.setStatus(500);
            endResponse(response, "SetOfferMapping exception " + e.getMessage());
            return;
        }

        // validate account is admin level
        try {
            CloudStackAccount currentAccount = ServiceProvider.getInstance().getEC2Engine().getCurrentAccount();

            if (currentAccount.getAccountType() != 1) {
                logger.debug("SetOfferMapping called by non-admin user!");
                response.setStatus(500);
                endResponse(response, "Permission denied for non-admin user to setOfferMapping!");
                return;
            }
        } catch (Exception e) {
            logger.error("SetOfferMapping " + e.getMessage(), e);
            response.setStatus(401);
            endResponse(response, e.toString());
            return;
        }

        try {

            ofDao.setOfferMapping( amazonOffer, cloudOffer ); 

        } catch( Exception e ) {
            logger.error("SetOfferMapping " + e.getMessage(), e);
            response.setStatus(401);
            endResponse(response, e.toString());
            return;
        }
        response.setStatus(200);	
        endResponse(response, "offering mapping set successfully");
    }

    private void deleteOfferMapping( HttpServletRequest request, HttpServletResponse response ) {
        String amazonOffer = null;

        try {
            // -> all these parameters are required
            amazonOffer = request.getParameter( "amazonoffer" );
            if ( null == amazonOffer ) { 
                response.sendError(530, "Missing amazonoffer parameter" ); 
                return; 
            }

        } catch( Exception e ) {
            logger.error("DeleteOfferMapping exception " + e.getMessage(), e);
            response.setStatus(500);
            endResponse(response, "DeleteOfferMapping exception " + e.getMessage());
            return;
        }

        // validate account is admin level
        try {
            CloudStackAccount currentAccount = ServiceProvider.getInstance().getEC2Engine().getCurrentAccount();

            if (currentAccount.getAccountType() != 1) {
                logger.debug("deleteOfferMapping called by non-admin user!");
                response.setStatus(500);
                endResponse(response, "Permission denied for non-admin user to deleteOfferMapping!");
                return;
            }
        } catch (Exception e) {
            logger.error("deleteOfferMapping " + e.getMessage(), e);
            response.setStatus(401);
            endResponse(response, e.toString());
            return;
        }

        try {
            ofDao.deleteOfferMapping( amazonOffer ); 
        } catch( Exception e ) {
            logger.error("DeleteOfferMapping " + e.getMessage(), e);
            response.setStatus(401);
            endResponse(response, e.toString());
            return;
        }
        response.setStatus(200);	
        endResponse(response, "offering mapping deleted successfully");
    }

    /**
     * The approach taken here is to map these REST calls into the same objects used 
     * to implement the matching SOAP requests (e.g., AttachVolume).   This is done by parsing
     * out the URL parameters and loading them into the relevant EC2XXX object(s).   Once
     * the parameters are loaded the appropriate EC2Engine function is called to perform
     * the requested action.   The result of the EC2Engine function is a standard 
     * Amazon WSDL defined object (e.g., AttachVolumeResponse Java object).   Finally the
     * serialize method is called on the returned response object to obtain the extected
     * response XML.
     */
    private void attachVolume( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2Volume EC2request = new EC2Volume();

        // -> all these parameters are required
        String[] volumeId = request.getParameterValues( "VolumeId" );
        if ( null != volumeId && 0 < volumeId.length ) 
            EC2request.setId( volumeId[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - VolumeId");
        }

        String[] instanceId = request.getParameterValues( "InstanceId" );
        if ( null != instanceId && 0 < instanceId.length ) 
            EC2request.setInstanceId( instanceId[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        String[] device = request.getParameterValues( "Device" );
        if ( null != device && 0 < device.length ) 
            EC2request.setDevice( device[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Device");
        }

        // -> execute the request
        AttachVolumeResponse EC2response = EC2SoapServiceImpl.toAttachVolumeResponse( ServiceProvider.getInstance().getEC2Engine().attachVolume( EC2request ));
        serializeResponse(response, EC2response);
    }

    /**
     * The SOAP equivalent of this function appears to allow multiple permissions per request, yet
     * in the REST API documentation only one permission is allowed.
     */
    private void revokeSecurityGroupIngress( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2AuthorizeRevokeSecurityGroup EC2request = new EC2AuthorizeRevokeSecurityGroup();

        String[] groupName = request.getParameterValues( "GroupName" );
        if ( null != groupName && 0 < groupName.length ) 
            EC2request.setName( groupName[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - GroupName");
        }

        // -> not clear how many parameters there are until we fail to get IpPermissions.n.IpProtocol
        int nCount = 1, mCount;
        do  {
        EC2IpPermission perm = new EC2IpPermission();       	

            String[] protocol = request.getParameterValues( "IpPermissions." + nCount + ".IpProtocol" );
        if ( null != protocol && 0 < protocol.length ) 
                perm.setProtocol( protocol[0]);
            else break;

            String[] fromPort = request.getParameterValues( "IpPermissions." + nCount + ".FromPort" );
            if ( null != fromPort && 0 < fromPort.length ) {
                if ( protocol[0].equalsIgnoreCase("icmp") )
                    perm.setIcmpType( fromPort[0] ) ;
                else
                    perm.setFromPort( Integer.parseInt( fromPort[0]) );
            }

            String[] toPort = request.getParameterValues( "IpPermissions." + nCount + ".ToPort" );
            if ( null != toPort && 0 < toPort.length ) {
                if ( protocol[0].equalsIgnoreCase("icmp") )
                    perm.setIcmpCode( toPort[0] );
                else
                    perm.setToPort( Integer.parseInt( toPort[0]) );
            }

            // -> list: IpPermissions.n.IpRanges.m.CidrIp
            mCount = 1;
            do {
                String[] ranges = request.getParameterValues( "IpPermissions." + nCount + ".IpRanges." + mCount + ".CidrIp" );
        if ( null != ranges && 0 < ranges.length) 
                    perm.addIpRange( ranges[0]);
                else break;
                mCount++;
            } while( true );

            // -> list: IpPermissions.n.Groups.m.UserId and IpPermissions.n.Groups.m.GroupName
            mCount = 1;
            do {
                EC2SecurityGroup group = new EC2SecurityGroup();

                String[] user = request.getParameterValues( "IpPermissions." + nCount + ".Groups." + mCount + ".UserId" );
                if ( null != user && 0 < user.length)
                    group.setAccount( user[0]);
                else break;

                String[] name = request.getParameterValues( "IpPermissions." + nCount + ".Groups." + mCount + ".GroupName" );
                if ( null != name && 0 < name.length)
                    group.setName( name[0]);
                else break;

                perm.addUser( group);
                mCount++;
            } while( true );

            // -> multiple IP permissions can be specified per group name
            EC2request.addIpPermission( perm);
            nCount++;
        } while( true );

        if (1 == nCount) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - IpPermissions");
        }

        // -> execute the request
        RevokeSecurityGroupIngressResponse EC2response = EC2SoapServiceImpl.toRevokeSecurityGroupIngressResponse( 
                ServiceProvider.getInstance().getEC2Engine().revokeSecurityGroup( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void authorizeSecurityGroupIngress( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        // -> parse the complicated paramters into our standard object
        EC2AuthorizeRevokeSecurityGroup EC2request = new EC2AuthorizeRevokeSecurityGroup();

        String[] groupName = request.getParameterValues( "GroupName" );
        if ( null != groupName && 0 < groupName.length ) 
            EC2request.setName( groupName[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter 'Groupname'");
        }

        // -> not clear how many parameters there are until we fail to get IpPermissions.n.IpProtocol
        int nCount = 1;
        do 
        {  	EC2IpPermission perm = new EC2IpPermission();       	

        String[] protocol = request.getParameterValues( "IpPermissions." + nCount + ".IpProtocol" );
        if ( null != protocol && 0 < protocol.length ) 
            perm.setProtocol( protocol[0] );
        else break;

        String[] fromPort = request.getParameterValues( "IpPermissions." + nCount + ".FromPort" );
            if ( null != fromPort && 0 < fromPort.length ) {
                if ( protocol[0].equalsIgnoreCase("icmp") )
                    perm.setIcmpType( fromPort[0] ) ;
                else
                    perm.setFromPort( Integer.parseInt( fromPort[0]) );
            }

        String[] toPort = request.getParameterValues( "IpPermissions." + nCount + ".ToPort" );
            if ( null != toPort && 0 < toPort.length ) {
                if ( protocol[0].equalsIgnoreCase("icmp") )
                    perm.setIcmpCode( toPort[0] );
                else
                    perm.setToPort( Integer.parseInt( toPort[0]) );
            }

        // -> list: IpPermissions.n.IpRanges.m.CidrIp
        int mCount = 1;
        do 
        {  String[] ranges = request.getParameterValues( "IpPermissions." + nCount + ".IpRanges." + mCount + ".CidrIp" );
        if ( null != ranges && 0 < ranges.length) 
            perm.addIpRange( ranges[0] );
        else break;
        mCount++;

        } while( true );

        // -> list: IpPermissions.n.Groups.m.UserId and IpPermissions.n.Groups.m.GroupName 
        mCount = 1;
        do 
        {  String[] user = request.getParameterValues( "IpPermissions." + nCount + ".Groups." + mCount + ".UserId" );
        if ( null == user || 0 == user.length) break;

        String[] name = request.getParameterValues( "IpPermissions." + nCount + ".Groups." + mCount + ".GroupName" );
        if ( null == name || 0 == name.length) break;

        EC2SecurityGroup group = new EC2SecurityGroup();
        group.setAccount( user[0] );
        group.setName( name[0] );
        perm.addUser( group );
        mCount++;

        } while( true );

        // -> multiple IP permissions can be specified per group name
        EC2request.addIpPermission( perm );	
        nCount++;

        } while( true );

        if (1 == nCount) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - IpPermissions");
        }

        // -> execute the request
        AuthorizeSecurityGroupIngressResponse EC2response = EC2SoapServiceImpl.toAuthorizeSecurityGroupIngressResponse( 
                ServiceProvider.getInstance().getEC2Engine().authorizeSecurityGroup( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void detachVolume( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2Volume EC2request = new EC2Volume();

        String[] volumeId = request.getParameterValues( "VolumeId" );
        if ( null != volumeId && 0 < volumeId.length ) 
            EC2request.setId(volumeId[0]);
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter 'VolumeId'");
        }

        String[] instanceId = request.getParameterValues( "InstanceId" );
        if ( null != instanceId && 0 < instanceId.length ) 
            EC2request.setInstanceId(instanceId[0]);

        String[] device = request.getParameterValues( "Device" );
        if ( null != device && 0 < device.length ) 
            EC2request.setDevice( device[0] );

        // -> execute the request
        DetachVolumeResponse EC2response = EC2SoapServiceImpl.toDetachVolumeResponse( ServiceProvider.getInstance().getEC2Engine().detachVolume( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void deleteVolume( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2Volume EC2request = new EC2Volume();

        String[] volumeId = request.getParameterValues( "VolumeId" );
        if ( null != volumeId && 0 < volumeId.length ) 
            EC2request.setId(volumeId[0]);
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - VolumeId");
        }

        // -> execute the request
        DeleteVolumeResponse EC2response = EC2SoapServiceImpl.toDeleteVolumeResponse( ServiceProvider.getInstance().getEC2Engine().deleteVolume( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void createVolume( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2CreateVolume EC2request = new EC2CreateVolume();

        String[] zoneName = request.getParameterValues( "AvailabilityZone" );
        if ( null != zoneName && 0 < zoneName.length ) 
            EC2request.setZoneName( zoneName[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing parameter - AvailabilityZone");
        }

        String[] size = request.getParameterValues( "Size" );
        String[] snapshotId = request.getParameterValues("SnapshotId");
        boolean useSnapshot = false;
        boolean useSize = false;

        if (null != size && 0 < size.length)
            useSize = true;

        if (snapshotId != null && snapshotId.length != 0)
            useSnapshot = true;

        if (useSize && !useSnapshot) {
            EC2request.setSize( size[0] );
        } else if (useSnapshot && !useSize) {
            EC2request.setSnapshotId(snapshotId[0]);
        } else if (useSize && useSnapshot) {
            throw new EC2ServiceException( ClientError.InvalidParameterCombination, "Parameters 'Size' and 'SnapshotId' are mutually exclusive");
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Parameter 'Size' or 'SnapshotId' has to be specified");
        }


        // -> execute the request
        CreateVolumeResponse EC2response = EC2SoapServiceImpl.toCreateVolumeResponse( ServiceProvider.getInstance().getEC2Engine().createVolume( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void createSecurityGroup( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {

        String groupName, groupDescription = null;

        String[] name = request.getParameterValues( "GroupName" );
        if ( null != name && 0 < name.length ) 
            groupName = name[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - GroupName");
        }
        String[] desc = request.getParameterValues( "GroupDescription" );
        if ( null != desc && 0 < desc.length ) 
            groupDescription = desc[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - GroupDescription");
        }

        // -> execute the request
        CreateSecurityGroupResponse EC2response = EC2SoapServiceImpl.toCreateSecurityGroupResponse( ServiceProvider.getInstance().getEC2Engine().createSecurityGroup( groupName, groupDescription ));
        serializeResponse(response, EC2response);
    }

    private void deleteSecurityGroup( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        String groupName = null;

        String[] name = request.getParameterValues( "GroupName" );
        if ( null != name && 0 < name.length ) 
            groupName = name[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - GroupName");
        }

        // -> execute the request
        DeleteSecurityGroupResponse EC2response = EC2SoapServiceImpl.toDeleteSecurityGroupResponse( ServiceProvider.getInstance().getEC2Engine().deleteSecurityGroup( groupName ));
        serializeResponse(response, EC2response);
    }

    private void deleteSnapshot( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        String snapshotId = null;

        String[] snapSet = request.getParameterValues( "SnapshotId" );
        if ( null != snapSet && 0 < snapSet.length ) 
            snapshotId = snapSet[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - SnapshotId");
        }

        // -> execute the request
        DeleteSnapshotResponse EC2response = EC2SoapServiceImpl.toDeleteSnapshotResponse( ServiceProvider.getInstance().getEC2Engine().deleteSnapshot( snapshotId ));
        serializeResponse(response, EC2response);
    }

    private void createSnapshot( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        String volumeId = null;

        String[] volSet = request.getParameterValues( "VolumeId" );
        if ( null != volSet && 0 < volSet.length ) 
            volumeId = volSet[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - VolumeId");
        }

        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        CreateSnapshotResponse EC2response = EC2SoapServiceImpl.toCreateSnapshotResponse( engine.createSnapshot( volumeId ), engine);
        serializeResponse(response, EC2response);
    }

    private void deregisterImage( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2Image image = new EC2Image();

        String[] imageId = request.getParameterValues( "ImageId" );
        if ( null != imageId && 0 < imageId.length ) 
            image.setId( imageId[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ImageId");
        }

        // -> execute the request
        DeregisterImageResponse EC2response = EC2SoapServiceImpl.toDeregisterImageResponse( ServiceProvider.getInstance().getEC2Engine().deregisterImage( image ));
        serializeResponse(response, EC2response);
    }

    private void createImage( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2CreateImage EC2request = new EC2CreateImage();

        String[] instanceId = request.getParameterValues( "InstanceId" );
        if ( null != instanceId && 0 < instanceId.length ) 
            EC2request.setInstanceId( instanceId[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        String[] name = request.getParameterValues( "Name" );
        if ( null != name && 0 < name.length ) 
            EC2request.setName( name[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Name");
        }

        String[] description = request.getParameterValues( "Description" );
        if ( null != description && 0 < description.length ) {
            if (description[0].length() > 255)
                throw new EC2ServiceException( ClientError.InvalidParameterValue,
                        "Length of the value of parameter Description should be less than 255");
            EC2request.setDescription( description[0] );
        }


        // -> execute the request
        CreateImageResponse EC2response = EC2SoapServiceImpl.toCreateImageResponse( ServiceProvider.getInstance().getEC2Engine().createImage( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void registerImage( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2RegisterImage EC2request = new EC2RegisterImage();

        String[] location = request.getParameterValues( "ImageLocation" );
        if ( null != location && 0 < location.length ) 
            EC2request.setLocation( location[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing parameter - ImageLocation");
        }

        String[] cloudRedfined = request.getParameterValues( "Architecture" );
        if ( null != cloudRedfined && 0 < cloudRedfined.length ) 
            EC2request.setArchitecture( cloudRedfined[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Architecture");
        }

        String[] name = request.getParameterValues( "Name" );
        if ( null != name && 0 < name.length ) 
            EC2request.setName( name[0] );
        else {
               throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Name");
        }

        String[] description = request.getParameterValues( "Description" );
        if ( null != description && 0 < description.length ) 
            EC2request.setDescription( description[0] );

        // -> execute the request
        RegisterImageResponse EC2response = EC2SoapServiceImpl.toRegisterImageResponse( ServiceProvider.getInstance().getEC2Engine().registerImage( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void modifyImageAttribute( HttpServletRequest request, HttpServletResponse response )
            throws ADBException, XMLStreamException, IOException {
        EC2ModifyImageAttribute ec2request = new EC2ModifyImageAttribute();

       String[] imageId = request.getParameterValues( "ImageId" );
       if ( imageId != null && imageId.length > 0 )
            ec2request.setImageId( imageId[0]);
       else {
           throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ImageId");
       }

        String[] description = request.getParameterValues( "Description.Value" );
        if ( description != null && description.length > 0 ) {
            ec2request.setAttribute(ImageAttribute.description);
            ec2request.setDescription(description[0]);
        } else {
            //add all launch permissions to ec2request
            ec2request = addLaunchPermImageAttribute(request, ec2request);
            if (ec2request.getLaunchPermissionSet().length > 0)
                ec2request.setAttribute(ImageAttribute.launchPermission);
            else {
                throw new EC2ServiceException( ClientError.MissingParamter,
                        "Missing required parameter - Description/LaunchPermission should be provided");
            }
        }

        // -> execute the request
        ModifyImageAttributeResponse EC2response = EC2SoapServiceImpl.toModifyImageAttributeResponse(
                ServiceProvider.getInstance().getEC2Engine().modifyImageAttribute( ec2request ));
        serializeResponse(response, EC2response);
    }

    private EC2ModifyImageAttribute addLaunchPermImageAttribute(HttpServletRequest request, EC2ModifyImageAttribute ec2request) {
        String[] users = {".UserId", ".Group"};
        String[] operations = {"LaunchPermission.Add.", "LaunchPermission.Remove."};
        int nCount = 1;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                List<String> launchPermissionList = new ArrayList<String>();
                do {
                    String[] launchPermissionAddGroup = request.getParameterValues( operations[j] + nCount + users[i] );
                    if (launchPermissionAddGroup != null && launchPermissionAddGroup.length > 0)
                        launchPermissionList.add(launchPermissionAddGroup[0]);
                    else
                        break;
                    nCount++;
                } while (true);
                if (nCount != 1) {
                    EC2ImageLaunchPermission ec2LaunchPermission = new EC2ImageLaunchPermission();
                    if (operations[j].contains("Add"))
                        ec2LaunchPermission.setLaunchPermOp(EC2ImageLaunchPermission.Operation.add);
                    else
                        ec2LaunchPermission.setLaunchPermOp(EC2ImageLaunchPermission.Operation.remove);
                    for (String launchPerm : launchPermissionList) {
                        ec2LaunchPermission.addLaunchPermission(launchPerm);
                    }
                    ec2request.addLaunchPermission(ec2LaunchPermission);
                    nCount = 1;
                }
            }
        }

        return ec2request;
    }

    private void resetImageAttribute( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2ModifyImageAttribute ec2request = new EC2ModifyImageAttribute();

        String[] imageId = request.getParameterValues( "ImageId" );
        if ( imageId != null && imageId.length > 0)
            ec2request.setImageId(imageId[0]);
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ImageId");
        }

        String[] attribute = request.getParameterValues( "Attribute" );
        if ( attribute != null && attribute.length > 0 ) {
            if (attribute[0].equalsIgnoreCase("launchPermission"))
                ec2request.setAttribute(ImageAttribute.launchPermission);
            else {
                throw new EC2ServiceException( ClientError.MissingParamter,
                        "Missing required parameter - Description/LaunchPermission should be provided");
            }
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Attribute");
        }

        EC2ImageLaunchPermission launchPermission = new EC2ImageLaunchPermission();
        launchPermission.setLaunchPermOp(EC2ImageLaunchPermission.Operation.reset);
        ec2request.addLaunchPermission(launchPermission);

        // -> execute the request
        ResetImageAttributeResponse EC2response = EC2SoapServiceImpl.toResetImageAttributeResponse(
                ServiceProvider.getInstance().getEC2Engine().modifyImageAttribute( ec2request ));
        serializeResponse(response, EC2response);
    }

    private void runInstances( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2RunInstances EC2request = new EC2RunInstances();

        // -> so in the Amazon docs for this REST call there is no userData even though there is in the SOAP docs
        String[] imageId = request.getParameterValues( "ImageId" );
        if ( null != imageId && 0 < imageId.length ) 
            EC2request.setTemplateId( imageId[0] );
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ImageId");
        }

        String[] minCount = request.getParameterValues( "MinCount" );
        if ( minCount == null || minCount.length < 1) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - MinCount");
        } else if ( Integer.parseInt(minCount[0]) < 1) {
            throw new EC2ServiceException(ClientError.InvalidParameterValue,
                    "Value of parameter MinCount should be greater than 0");
        } else {
            EC2request.setMinCount( Integer.parseInt( minCount[0]) );
        }

        String[] maxCount = request.getParameterValues( "MaxCount" );
        if ( maxCount == null || maxCount.length < 1) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - MaxCount");
        } else if ( Integer.parseInt(maxCount[0]) < 1) {
            throw new EC2ServiceException(ClientError.InvalidParameterValue,
                    "Value of parameter MaxCount should be greater than 0");
        } else {
            EC2request.setMaxCount( Integer.parseInt( maxCount[0]) );
        }

        String[] instanceType = request.getParameterValues( "InstanceType" );
        if ( null != instanceType && 0 < instanceType.length ) 
            EC2request.setInstanceType( instanceType[0] );

        String[] zoneName = request.getParameterValues( "Placement.AvailabilityZone" );
        if ( null != zoneName && 0 < zoneName.length ) 
            EC2request.setZoneName( zoneName[0] );

        String[] size = request.getParameterValues("size");
        if (size != null) {
            EC2request.setSize(Integer.valueOf(size[0]));
        }

        String[] keyName = request.getParameterValues("KeyName");
        if (keyName != null) {
            EC2request.setKeyName(keyName[0]);
        }

        String[] userData = request.getParameterValues("UserData");
        if ( userData != null) {
            EC2request.setUserData( userData[0]);
        }

        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if ( key.startsWith("SecurityGroup")) {
                String[] value = request.getParameterValues(key);
                if (null != value && 0 < value.length) {
                    if ( key.startsWith("SecurityGroupId"))
                        EC2request.addSecuritGroupId( value[0]);
                    else
                        EC2request.addSecuritGroupName( value[0]);
                }
            }
        }

        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        RunInstancesResponse EC2response = EC2SoapServiceImpl.toRunInstancesResponse( engine.runInstances( EC2request ), engine);
        serializeResponse(response, EC2response);
    }

    private void rebootInstances( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2RebootInstances EC2request = new EC2RebootInstances();
        int count = 0;

        // -> load in all the "InstanceId.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("InstanceId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) {
                    EC2request.addInstanceId( value[0] );
                    count++;
                }
            }
        }	
        if (0 == count) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        // -> execute the request
        RebootInstancesResponse EC2response = EC2SoapServiceImpl.toRebootInstancesResponse( ServiceProvider.getInstance().getEC2Engine().rebootInstances(EC2request));
        serializeResponse(response, EC2response);
    }

    private void startInstances( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2StartInstances EC2request = new EC2StartInstances();
        int count = 0;

        // -> load in all the "InstanceId.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("InstanceId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) {
                    EC2request.addInstanceId( value[0] );
                    count++;
                }
            }
        }	
        if (0 == count) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        // -> execute the request
        StartInstancesResponse EC2response = EC2SoapServiceImpl.toStartInstancesResponse( ServiceProvider.getInstance().getEC2Engine().startInstances(EC2request));
        serializeResponse(response, EC2response);
    }

    private void stopInstances( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2StopInstances EC2request = new EC2StopInstances();
        int count = 0;

        // -> load in all the "InstanceId.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("InstanceId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) {
                    EC2request.addInstanceId( value[0] );
                    count++;
                }
            }
        }	
        if (0 == count) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        String[] force = request.getParameterValues("Force");
        if ( force != null) {
            EC2request.setForce( Boolean.parseBoolean(force[0]));
        }

        // -> execute the request
        StopInstancesResponse EC2response = EC2SoapServiceImpl.toStopInstancesResponse( ServiceProvider.getInstance().getEC2Engine().stopInstances( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void terminateInstances( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2StopInstances EC2request = new EC2StopInstances();
        int count = 0;

        // -> load in all the "InstanceId.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("InstanceId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) {
                    EC2request.addInstanceId( value[0] );
                    count++;
                }
            }
        }		
        if (0 == count) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        // -> execute the request
        EC2request.setDestroyInstances( true );
        TerminateInstancesResponse EC2response = EC2SoapServiceImpl.toTermInstancesResponse( ServiceProvider.getInstance().getEC2Engine().stopInstances( EC2request ));
        serializeResponse(response, EC2response);
    }

    /**
     * We are reusing the SOAP code to process this request.   We then use Axiom to serialize the
     * resulting EC2 Amazon object into XML to return to the client.
     */
    private void describeAvailabilityZones( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeAvailabilityZones EC2request = new EC2DescribeAvailabilityZones();

        // -> load in all the "ZoneName.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("ZoneName")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) EC2request.addZone( value[0] );
            }
        }		

        // add filters
        EC2Filter[] filterSet = extractFilters( request );
        if ( filterSet != null ) {
            EC2AvailabilityZonesFilterSet afs = new EC2AvailabilityZonesFilterSet();
            for( int i=0; i < filterSet.length; i++ ) {
                afs.addFilter(filterSet[i]);
            }
            EC2request.setFilterSet( afs );
        }

        // -> execute the request
        DescribeAvailabilityZonesResponse EC2response = EC2SoapServiceImpl.toDescribeAvailabilityZonesResponse(
                ServiceProvider.getInstance().getEC2Engine().describeAvailabilityZones( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void describeImages( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeImages EC2request = new EC2DescribeImages();

        // -> load in all the "ImageId.n" parameters if any, and ignore all other parameters
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("ImageId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) EC2request.addImageSet( value[0] );
            }
        }		
        // add filters
        EC2Filter[] filterSet = extractFilters( request );
        if ( filterSet != null ) {
            EC2ImageFilterSet ifs = new EC2ImageFilterSet();
            for( int i=0; i < filterSet.length; i++ ) {
                ifs.addFilter(filterSet[i]);
            }
            EC2request.setFilterSet( ifs );
        }
        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        DescribeImagesResponse EC2response = EC2SoapServiceImpl.toDescribeImagesResponse( engine.describeImages( EC2request ));
        serializeResponse(response, EC2response);
    }

    private void describeImageAttribute( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeImageAttribute ec2request = new EC2DescribeImageAttribute();

        String[] imageId = request.getParameterValues( "ImageId" );
        if (imageId != null && imageId.length > 0)
            ec2request.setImageId(imageId[0]);
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ImageId");
        }

        String[] attribute = request.getParameterValues( "Attribute" );
        if (attribute != null && attribute.length > 0) {
            if (attribute[0].equalsIgnoreCase("description"))
                ec2request.setAttribute(ImageAttribute.description);
            else if (attribute[0].equalsIgnoreCase("launchPermission"))
                ec2request.setAttribute(ImageAttribute.launchPermission);
            else {
            	throw new EC2ServiceException( ClientError.InvalidParameterValue,
                        "Only values supported for paramter Attribute are - Description/LaunchPermission");
            }
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Attribute");
        }

        DescribeImageAttributeResponse EC2response = EC2SoapServiceImpl.toDescribeImageAttributeResponse( ServiceProvider.getInstance().getEC2Engine().describeImageAttribute( ec2request ));
        serializeResponse(response, EC2response);
    }

    private void describeInstances( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException 
            {
        EC2DescribeInstances EC2request = new EC2DescribeInstances();

        // -> load in all the "InstanceId.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) 
        {
            String key = (String)names.nextElement();
            if (key.startsWith("InstanceId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) EC2request.addInstanceId( value[0] );
            }
        }		

        // -> are there any filters with this request?
        EC2Filter[] filterSet = extractFilters( request );
        if (null != filterSet)
        {
            EC2InstanceFilterSet ifs = new EC2InstanceFilterSet();
            for( int i=0; i < filterSet.length; i++ ) ifs.addFilter( filterSet[i] );
            EC2request.setFilterSet( ifs );
        }

        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        DescribeInstancesResponse EC2response = EC2SoapServiceImpl.toDescribeInstancesResponse( engine.describeInstances( EC2request ), engine);
        serializeResponse(response, EC2response);
            }

    private void describeAddresses( HttpServletRequest request, HttpServletResponse response )
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeAddresses ec2Request = new EC2DescribeAddresses();

        // -> load in all the "PublicIp.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("PublicIp")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) ec2Request.addPublicIp( value[0] );
            }
        }

        // add filters
        EC2Filter[] filterSet = extractFilters( request );
        if ( filterSet != null ) {
            EC2AddressFilterSet afs = new EC2AddressFilterSet();
            for ( int i=0; i < filterSet.length; i++ )
                afs.addFilter( filterSet[i] );
            ec2Request.setFilterSet( afs );
        }
        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        serializeResponse(response, EC2SoapServiceImpl.toDescribeAddressesResponse( engine.describeAddresses( ec2Request)));
    }

    private void allocateAddress( HttpServletRequest request, HttpServletResponse response )
            throws ADBException, XMLStreamException, IOException {

        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();

        AllocateAddressResponse ec2Response = EC2SoapServiceImpl.toAllocateAddressResponse( engine.allocateAddress());

        serializeResponse(response, ec2Response);
    }

    private void releaseAddress( HttpServletRequest request, HttpServletResponse response )
            throws ADBException, XMLStreamException, IOException {

        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();

        String publicIp = request.getParameter( "PublicIp" );
        if (publicIp == null) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - PublicIp");
        }

        EC2ReleaseAddress ec2Request = new EC2ReleaseAddress();
        if (ec2Request != null) {
            ec2Request.setPublicIp(publicIp);
        }

        ReleaseAddressResponse EC2Response = EC2SoapServiceImpl.toReleaseAddressResponse( engine.releaseAddress( ec2Request ));

        serializeResponse(response, EC2Response);
    }

    private void associateAddress( HttpServletRequest request, HttpServletResponse response )
            throws ADBException, XMLStreamException, IOException {
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();

        String publicIp = request.getParameter( "PublicIp" );
        if (null == publicIp) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - PublicIp");
        }
        String instanceId = request.getParameter( "InstanceId" );
        if (null == instanceId) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        EC2AssociateAddress ec2Request = new EC2AssociateAddress();
        if (ec2Request != null) {
            ec2Request.setInstanceId(instanceId);
            ec2Request.setPublicIp(publicIp);
        }

        AssociateAddressResponse ec2Response = EC2SoapServiceImpl.toAssociateAddressResponse( engine.associateAddress( ec2Request ));

        serializeResponse(response, ec2Response);
    }

    private void disassociateAddress( HttpServletRequest request, HttpServletResponse response )
            throws ADBException, XMLStreamException, IOException {    	
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();

        String publicIp = request.getParameter( "PublicIp" );
        if (null == publicIp) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - PublicIp");
        }

        EC2DisassociateAddress ec2Request = new EC2DisassociateAddress();
        if (ec2Request != null) {
            ec2Request.setPublicIp(publicIp);
        }

        DisassociateAddressResponse ec2Response = EC2SoapServiceImpl.toDisassociateAddressResponse( engine.disassociateAddress( ec2Request ) ); 

        serializeResponse(response, ec2Response);
    }


    private void describeSecurityGroups( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException 
            {
        EC2DescribeSecurityGroups EC2request = new EC2DescribeSecurityGroups();

        // -> load in all the "GroupName.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (key.startsWith("GroupName")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) EC2request.addGroupName( value[0] );
            }
        }	

        // -> are there any filters with this request?
        EC2Filter[] filterSet = extractFilters( request );
        if (null != filterSet) {
            EC2GroupFilterSet gfs = new EC2GroupFilterSet();
            for (EC2Filter filter : filterSet) gfs.addFilter( filter );
            EC2request.setFilterSet( gfs );
        }

        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();

        DescribeSecurityGroupsResponse EC2response = EC2SoapServiceImpl.toDescribeSecurityGroupsResponse( engine.describeSecurityGroups( EC2request ));
        serializeResponse(response, EC2response);
            }


    private void describeInstanceAttribute( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeInstances EC2request = new EC2DescribeInstances();
        String[] instanceId = request.getParameterValues( "InstanceId" );
        if ( instanceId != null && instanceId.length > 0)
            EC2request.addInstanceId( instanceId[0] );
        else
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");

        String[] attribute = request.getParameterValues( "Attribute" );
        if (attribute != null && attribute.length > 0) {
               if ( !attribute[0].equalsIgnoreCase("instanceType") ) {
                throw new EC2ServiceException( ClientError.InvalidParameterValue,
                        "Only value supported for paramter Attribute is 'instanceType'");
            }
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Attribute");
        }

        // -> execute the request
        DescribeInstanceAttributeResponse EC2response = EC2SoapServiceImpl.toDescribeInstanceAttributeResponse( ServiceProvider.getInstance().getEC2Engine().describeInstances(EC2request));
        serializeResponse(response, EC2response);
    }

    private void modifyInstanceAttribute(HttpServletRequest request, HttpServletResponse response)
            throws ADBException, XMLStreamException, IOException {
        EC2ModifyInstanceAttribute ec2Request = new EC2ModifyInstanceAttribute();

        String[] instanceId = request.getParameterValues( "InstanceId" );
        if ( instanceId != null && instanceId.length > 0 )
             ec2Request.setInstanceId(instanceId[0]);
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        String[] instanceType = request.getParameterValues( "InstanceType.Value" );
        String[] userData  = request.getParameterValues( "UserData.Value" );

        if ( instanceType != null && userData != null ) {
            throw new EC2ServiceException( ClientError.InvalidParameterCombination, "Only one attribute can be" +
                    " specified at a time");
        }
        if ( instanceType != null && instanceType.length > 0 ) {
            ec2Request.setInstanceType(instanceType[0]);
        } else if ( userData != null && userData.length > 0 ) {
            ec2Request.setUserData(userData[0]);
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter,
                    "Missing parameter - InstanceType/UserData should be provided");
        }

        // -> execute the request
        ModifyInstanceAttributeResponse EC2response = EC2SoapServiceImpl.toModifyInstanceAttributeResponse(
                ServiceProvider.getInstance().getEC2Engine().modifyInstanceAttribute( ec2Request ));
        serializeResponse(response, EC2response);
    }

    private void describeSnapshots( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException 
            {
        EC2DescribeSnapshots EC2request = new EC2DescribeSnapshots();

        // -> load in all the "SnapshotId.n" parameters if any, and ignore any other parameters
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) 
        {
            String key = (String)names.nextElement();
            if (key.startsWith("SnapshotId")) {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) EC2request.addSnapshotId( value[0] );
            }
        }

        // -> are there any filters with this request?
        EC2Filter[] filterSet = extractFilters( request );
        if (null != filterSet)
        {
            EC2SnapshotFilterSet sfs = new EC2SnapshotFilterSet();
            for( int i=0; i < filterSet.length; i++ ) sfs.addFilter( filterSet[i] );
            EC2request.setFilterSet( sfs );
        }

        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        DescribeSnapshotsResponse EC2response = EC2SoapServiceImpl.toDescribeSnapshotsResponse(
                engine.describeSnapshots( EC2request ));
        serializeResponse(response, EC2response);
            }


    private void describeVolumes( HttpServletRequest request, HttpServletResponse response ) 
            throws ADBException, XMLStreamException, IOException 
            {
        EC2DescribeVolumes EC2request = new EC2DescribeVolumes();

        // -> load in all the "VolumeId.n" parameters if any
        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) 
        {
            String key = (String)names.nextElement();
            if (key.startsWith("VolumeId")) 
            {
                String[] value = request.getParameterValues( key );
                if (null != value && 0 < value.length) EC2request.addVolumeId( value[0] );
            }
        }		

        // -> are there any filters with this request?
        EC2Filter[] filterSet = extractFilters( request );
        if (null != filterSet)
        {
            EC2VolumeFilterSet vfs = new EC2VolumeFilterSet();
            for( int i=0; i < filterSet.length; i++ ) vfs.addFilter( filterSet[i] );
            EC2request.setFilterSet( vfs );
        }

        // -> execute the request
        EC2Engine engine = ServiceProvider.getInstance().getEC2Engine();
        DescribeVolumesResponse EC2response = EC2SoapServiceImpl.toDescribeVolumesResponse(
                ServiceProvider.getInstance().getEC2Engine().describeVolumes( EC2request ), engine);
        serializeResponse(response, EC2response);
            }


    /**
     * Example of how the filters are defined in a REST request:
     * https://<server>/?Action=DescribeVolumes
     * &Filter.1.Name=attachment.instance-id
     * &Filter.1.Value.1=i-1a2b3c4d
     * &Filter.2.Name=attachment.delete-on-termination
     * &Filter.2.Value.1=true
     * 
     * @param request
     * @return List<EC2Filter>
     */
    private EC2Filter[] extractFilters( HttpServletRequest request )
    {
        String filterName    = null;
        String value         = null;
        EC2Filter nextFilter = null;
        boolean timeFilter   = false;
        int filterCount      = 1;
        int valueCount       = 1;

        List<EC2Filter> filterSet = new ArrayList<EC2Filter>();   

        do 
        {   filterName = request.getParameter( "Filter." + filterCount + ".Name" );
        if (null != filterName)
        {
            nextFilter = new EC2Filter();
            nextFilter.setName( filterName );
            timeFilter = (filterName.equalsIgnoreCase( "attachment.attach-time" ) || filterName.equalsIgnoreCase( "create-time" ));
            valueCount = 1;
            do
            {
                value = request.getParameter( "Filter." + filterCount + ".Value." + valueCount );
                if (null != value) 
                {
                    // -> time values are not encoded as regexes
                    if ( timeFilter )
                        nextFilter.addValue( value );
                    else nextFilter.addValueEncoded( value );

                    valueCount++;
                }
            }
            while( null != value );

            filterSet.add( nextFilter );
            filterCount++;
        }
        }
        while( null != filterName );

        if ( 1 == filterCount )
            return null;
        else return filterSet.toArray(new EC2Filter[0]);
    }


    private void describeKeyPairs(HttpServletRequest request, HttpServletResponse response) 
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeKeyPairs ec2Request = new EC2DescribeKeyPairs();

        Enumeration<?> names = request.getParameterNames();
        while( names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if ( key.startsWith("KeyName")) {
                String[] value = request.getParameterValues( key);
                if (null != value && 0 < value.length)
                    ec2Request.addKeyName( value[0]);
            }
        }

        EC2Filter[] filterSet = extractFilters( request );
        if (null != filterSet){
            EC2KeyPairFilterSet vfs = new EC2KeyPairFilterSet();
            for (EC2Filter filter : filterSet) {
                vfs.addFilter(filter);
            }
            ec2Request.setKeyFilterSet(vfs);
        }

        DescribeKeyPairsResponse EC2Response = EC2SoapServiceImpl.toDescribeKeyPairs(
                ServiceProvider.getInstance().getEC2Engine().describeKeyPairs( ec2Request ));
        serializeResponse(response, EC2Response);
    }

    private void importKeyPair(HttpServletRequest request, HttpServletResponse response) 
            throws ADBException, XMLStreamException, IOException {

        String keyName = request.getParameter("KeyName");
        if ( keyName == null ) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - KeyName");
        }

        String publicKeyMaterial = request.getParameter("PublicKeyMaterial");
        if ( publicKeyMaterial == null ) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - PublicKeyMaterial");
        }

        if (!publicKeyMaterial.contains(" "))
            publicKeyMaterial = new String(Base64.decodeBase64(publicKeyMaterial.getBytes())); 



        EC2ImportKeyPair ec2Request = new EC2ImportKeyPair();
        if (ec2Request != null) {
            ec2Request.setKeyName(request.getParameter("KeyName"));
            ec2Request.setPublicKeyMaterial(request.getParameter("PublicKeyMaterial"));
        }

        ImportKeyPairResponse EC2Response = EC2SoapServiceImpl.toImportKeyPair(
                ServiceProvider.getInstance().getEC2Engine().importKeyPair( ec2Request ));
        serializeResponse(response, EC2Response);
    }

    private void createKeyPair(HttpServletRequest request, HttpServletResponse response)
            throws ADBException, XMLStreamException, IOException { 
        String keyName = request.getParameter("KeyName");
        if (keyName==null) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - KeyName");
        }

        EC2CreateKeyPair ec2Request = new EC2CreateKeyPair();
        if (ec2Request != null) {
            ec2Request.setKeyName(keyName);
        }

        CreateKeyPairResponse EC2Response = EC2SoapServiceImpl.toCreateKeyPair(
                ServiceProvider.getInstance().getEC2Engine().createKeyPair(ec2Request));
        serializeResponse(response, EC2Response);	
    }

    private void deleteKeyPair(HttpServletRequest request, HttpServletResponse response)
            throws ADBException, XMLStreamException, IOException {
        String keyName = request.getParameter("KeyName");
        if (keyName==null) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - KeyName");
        }

        EC2DeleteKeyPair ec2Request = new EC2DeleteKeyPair();
        ec2Request.setKeyName(keyName);

        DeleteKeyPairResponse EC2Response = EC2SoapServiceImpl.toDeleteKeyPair(
                ServiceProvider.getInstance().getEC2Engine().deleteKeyPair(ec2Request));
        serializeResponse(response, EC2Response);
    }

    private void getPasswordData(HttpServletRequest request, HttpServletResponse response) 
            throws ADBException, XMLStreamException, IOException {
        String instanceId = request.getParameter("InstanceId");
        if (instanceId==null) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - InstanceId");
        }

        GetPasswordDataResponse EC2Response = EC2SoapServiceImpl.toGetPasswordData(
                ServiceProvider.getInstance().getEC2Engine().getPasswordData(instanceId));
        serializeResponse(response, EC2Response);
    }

    private void createTags(HttpServletRequest request, HttpServletResponse response)
            throws ADBException, XMLStreamException, IOException {
        EC2Tags ec2Request = createTagsRequest(request, response);
        if (ec2Request == null) return;
        CreateTagsResponse EC2Response = EC2SoapServiceImpl.toCreateTagsResponse(
                ServiceProvider.getInstance().getEC2Engine().modifyTags( ec2Request, "create"));
        serializeResponse(response, EC2Response);
    }

    private void deleteTags(HttpServletRequest request, HttpServletResponse response)
            throws ADBException, XMLStreamException, IOException {
        EC2Tags ec2Request = createTagsRequest(request, response);
        if (ec2Request == null) return;
        DeleteTagsResponse EC2Response = EC2SoapServiceImpl.toDeleteTagsResponse(
                ServiceProvider.getInstance().getEC2Engine().modifyTags( ec2Request, "delete"));
        serializeResponse(response, EC2Response);
    }

    private EC2Tags createTagsRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        EC2Tags ec2Request = new EC2Tags();
        ArrayList<String> resourceIdList = new ArrayList<String>();
        Map<String, String> resourceTagList = new HashMap<String, String>();

        int nCount = 1;
        do {
            String[] resourceIds = request.getParameterValues( "ResourceId." + nCount );
            if (resourceIds != null && resourceIds.length > 0)
                resourceIdList.add(resourceIds[0]);
            else break;
            nCount++;
        } while (true);
        if ( resourceIdList.isEmpty() ) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ResourceId");
        }
        ec2Request = EC2SoapServiceImpl.toResourceTypeAndIds(ec2Request, resourceIdList);

        nCount = 1;
        do {
            String[] tagKey = request.getParameterValues( "Tag." + nCount + ".Key" );
            if ( tagKey != null && tagKey.length > 0 ) {
               String[] tagValue = request.getParameterValues( "Tag." + nCount + ".Value" );
            if ( tagValue != null && tagValue.length > 0 ) {
                resourceTagList.put(tagKey[0], tagValue[0]);
            } else
                resourceTagList.put(tagKey[0], null);
            } else break;
            nCount++;
        } while (true);
        if ( resourceTagList.isEmpty() ) {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - ResourceTag");
        }
        ec2Request = EC2SoapServiceImpl.toResourceTag(ec2Request, resourceTagList);

        return ec2Request;
    }

    private void describeTags(HttpServletRequest request, HttpServletResponse response)
            throws ADBException, XMLStreamException, IOException {
        EC2DescribeTags ec2Request = new EC2DescribeTags();

        EC2Filter[] filterSet = extractFilters( request );
        if (null != filterSet) {
            EC2TagsFilterSet tfs = new EC2TagsFilterSet();
            for( int i=0; i < filterSet.length; i++ )
                tfs.addFilter( filterSet[i] );
            ec2Request.setFilterSet( tfs );
        }
        DescribeTagsResponse EC2Response = EC2SoapServiceImpl.toDescribeTagsResponse(
                ServiceProvider.getInstance().getEC2Engine().describeTags( ec2Request));
        serializeResponse(response, EC2Response);
    }

    

    /**
     * This function implements the EC2 REST authentication algorithm.   It uses the given
     * "AWSAccessKeyId" parameter to look up the Cloud.com account holder's secret key which is
     * used as input to the signature calculation.  In addition, it tests the given "Expires"
     * parameter to see if the signature has expired and if so the request fails.
     */
    private boolean authenticateRequest( HttpServletRequest request, HttpServletResponse response ) 
            throws SignatureException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, ParseException 
            {
        String cloudSecretKey = null;    
        String cloudAccessKey = null;
        String signature      = null;
        String sigMethod      = null;           

        // [A] Basic parameters required for an authenticated rest request
        //  -> note that the Servlet engine will un-URL encode all parameters we extract via "getParameterValues()" calls
        String[] awsAccess = request.getParameterValues( "AWSAccessKeyId" );
        if ( null != awsAccess && 0 < awsAccess.length ) 
            cloudAccessKey = awsAccess[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - AWSAccessKeyId");
        }

        String[] clientSig = request.getParameterValues( "Signature" );
        if ( null != clientSig && 0 < clientSig.length ) 
            signature = clientSig[0];
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Signature");
        }

        String[] method = request.getParameterValues( "SignatureMethod" );
        if ( null != method && 0 < method.length ) 
        {
            sigMethod = method[0];
            if (!sigMethod.equals( "HmacSHA256" ) && !sigMethod.equals( "HmacSHA1" )) {
            	throw new EC2ServiceException( ClientError.InvalidParameterValue,
                         "Unsupported SignatureMethod value: " + sigMethod + " expecting: HmacSHA256 or HmacSHA1");
            }
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - SignatureMethod");
        }

        String[] version = request.getParameterValues( "Version" );
        if ( null != version && 0 < version.length ) 
        {
            if (!version[0].equals( wsdlVersion )) {
                throw new EC2ServiceException( ClientError.InvalidParameterValue,
                        "Unsupported Version value: " + version[0] + " expecting: " + wsdlVersion);
            }
        } else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - Version");
        }

        String[] sigVersion = request.getParameterValues( "SignatureVersion" );
        if ( null != sigVersion && 0 < sigVersion.length ) 
        {
            if (!sigVersion[0].equals( "2" )) {
                throw new EC2ServiceException( ClientError.InvalidParameterValue,
                     "Unsupported SignatureVersion value: " + sigVersion[0] + " expecting: 2");
            }
        }
        else {
            throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter - SignatureVersion");
        }

        // -> can have only one but not both { Expires | Timestamp } headers
        String[] expires = request.getParameterValues( "Expires" );
        if ( null != expires && 0 < expires.length ) 
        {
            // -> contains the date and time at which the signature included in the request EXPIRES
            if (hasSignatureExpired( expires[0] )) { //InvalidSecurity.RequestHasExpired
                 throw new EC2ServiceException( ClientError.InvalidSecurity_RequestHasExpired,
                        "Expires parameter indicates signature has expired: " + expires[0]);
            }
        }
        else 
        {    // -> contains the date and time at which the request is SIGNED
            String[] time = request.getParameterValues( "Timestamp" );
            if ( null == time || 0 == time.length ) {
                throw new EC2ServiceException( ClientError.MissingParamter, "Missing required parameter -" +
                          " Timestamp/Expires");
            }
        } 

        // [B] Use the access key to get the users secret key from the cloud DB
        cloudSecretKey = userDao.getSecretKeyByAccessKey( cloudAccessKey );
        if ( cloudSecretKey == null ) {
            logger.debug( "Access key '" + cloudAccessKey + "' not found in the the EC2 service ");
            throw new EC2ServiceException( ClientError.AuthFailure, "Access key '" + cloudAccessKey + "' not found in the the EC2 service ");
        }

        // [C] Verify the signature
        //  -> getting the query-string in this way maintains its URL encoding
        EC2RestAuth restAuth = new EC2RestAuth();
        restAuth.setHostHeader( request.getHeader( "Host" ));
        String requestUri = request.getRequestURI();

        // If forwarded from another basepath:
        String forwardedPath = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if(forwardedPath!=null){
            requestUri=forwardedPath;
        }
        restAuth.setHTTPRequestURI( requestUri);

        String queryString = request.getQueryString();
        // getQueryString returns null (does it ever NOT return null for these), 
        // we need to construct queryString to avoid changing the auth code...
        if (queryString == null) {
            // construct our idea of a queryString with parameters!
            Enumeration<?> params = request.getParameterNames();
            if (params != null) {
                while(params.hasMoreElements()) {
                    String paramName = (String) params.nextElement();
                    // exclude the signature string obviously. ;)
                    if (paramName.equalsIgnoreCase("Signature")) continue;
                    // URLEncoder performs application/x-www-form-urlencoded-type encoding and not Percent encoding
                    // according to RFC 3986 as required by Amazon, we need to Percent-encode (URL Encode)
                    String encodedValue = URLEncoder.encode(request.getParameter(paramName), "UTF-8")
                            .replace("+", "%20").replace("*", "%2A");
                    if (queryString == null) 
                        queryString = paramName + "=" + encodedValue;
                    else 
                        queryString = queryString + "&" + paramName + "=" + encodedValue;
                }
            }
        }
        restAuth.setQueryString(queryString);

        if ( restAuth.verifySignature( request.getMethod(), cloudSecretKey, signature, sigMethod )) {
            UserContext.current().initContext( cloudAccessKey, cloudSecretKey, cloudAccessKey, "REST request", null );
            return true;
        }
        else throw new EC2ServiceException( ClientError.SignatureDoesNotMatch,
                "The request signature calculated does not match the signature provided by the user.");
    }

    /**
     * We check this to reduce replay attacks.
     * 
     * @param timeStamp
     * @return true - if the request is not longer valid, false otherwise
     * @throws ParseException
     */
    private boolean hasSignatureExpired( String timeStamp ) {
        Calendar cal = EC2RestAuth.parseDateString( timeStamp );
        if (null == cal) return false; 

        Date expiredTime = cal.getTime();          
        Date today       = new Date();   // -> gets set to time of creation
        if ( 0 >= expiredTime.compareTo( today )) { 
            logger.debug( "timestamp given: [" + timeStamp + "], now: [" + today.toString() + "]" );
            return true;
        }
        else return false;
    }

    private static void endResponse(HttpServletResponse response, String content) {
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

    private void logRequest(HttpServletRequest request) {
        if(logger.isInfoEnabled()) {
            logger.info("EC2 Request method: " + request.getMethod());
            logger.info("Request contextPath: " + request.getContextPath());
            logger.info("Request pathInfo: " + request.getPathInfo());
            logger.info("Request pathTranslated: " + request.getPathTranslated());
            logger.info("Request queryString: " + request.getQueryString());
            logger.info("Request requestURI: " + request.getRequestURI());
            logger.info("Request requestURL: " + request.getRequestURL());
            logger.info("Request servletPath: " + request.getServletPath());
            Enumeration<?> headers = request.getHeaderNames();
            if(headers != null) {
                while(headers.hasMoreElements()) {
                    Object headerName = headers.nextElement();
                    logger.info("Request header " + headerName + ":" + request.getHeader((String)headerName));
                }
            }

            Enumeration<?> params = request.getParameterNames();
            if(params != null) {
                while(params.hasMoreElements()) {
                    Object paramName = params.nextElement();
                    logger.info("Request parameter " + paramName + ":" + 
                            request.getParameter((String)paramName));
                }
            }
        }
    }

    /**
     * Send out an error response according to Amazon convention.
     */
    private void faultResponse(HttpServletResponse response, String errorCode, String errorMessage) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream());
            response.setContentType("text/xml; charset=UTF-8");
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.write("<Response><Errors><Error><Code>");
            out.write(errorCode);
            out.write("</Code><Message>");
            out.write(errorMessage);
            out.write("</Message></Error></Errors><RequestID>");
            out.write(UUID.randomUUID().toString());
            out.write("</RequestID></Response>");
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.error("Unexpected exception " + e.getMessage(), e);
        }
    }

    /**
     * Serialize Axis beans to XML output. 
     */
    private void serializeResponse(HttpServletResponse response, ADBBean EC2Response) 
            throws ADBException, XMLStreamException, IOException {
        OutputStream os = response.getOutputStream();
        response.setStatus(200);	
        response.setContentType("text/xml");
        XMLStreamWriter xmlWriter = xmlOutFactory.createXMLStreamWriter( os );
        xmlWriter.writeStartDocument("UTF-8","1.0");
        MTOMAwareXMLSerializer MTOMWriter = new MTOMAwareXMLSerializer( xmlWriter );
        MTOMWriter.setDefaultNamespace("http://ec2.amazonaws.com/doc/" + wsdlVersion + "/");
        EC2Response.serialize( null, factory, MTOMWriter );
        xmlWriter.flush();
        xmlWriter.close();
        os.close();
    }
}
