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
package com.cloud.bridge.service.core.ec2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.cloud.bridge.model.CloudStackServiceOfferingVO;
import com.cloud.bridge.persist.dao.CloudStackAccountDao;
import com.cloud.bridge.persist.dao.CloudStackSvcOfferingDao;
import com.cloud.bridge.persist.dao.OfferingDao;
import com.cloud.bridge.service.UserContext;
import com.cloud.bridge.service.core.ec2.EC2ImageAttributes.ImageAttribute;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.service.exception.EC2ServiceException.ServerError;
import com.cloud.bridge.util.ConfigurationHelper;
import com.cloud.stack.CloudStackApi;
import com.cloud.stack.models.CloudStackAccount;
import com.cloud.stack.models.CloudStackDiskOffering;
import com.cloud.stack.models.CloudStackInfoResponse;
import com.cloud.stack.models.CloudStackIngressRule;
import com.cloud.stack.models.CloudStackIpAddress;
import com.cloud.stack.models.CloudStackKeyPair;
import com.cloud.stack.models.CloudStackKeyValue;
import com.cloud.stack.models.CloudStackNetwork;
import com.cloud.stack.models.CloudStackNetworkOffering;
import com.cloud.stack.models.CloudStackNic;
import com.cloud.stack.models.CloudStackOsType;
import com.cloud.stack.models.CloudStackPasswordData;
import com.cloud.stack.models.CloudStackResourceLimit;
import com.cloud.stack.models.CloudStackResourceTag;
import com.cloud.stack.models.CloudStackSecurityGroup;
import com.cloud.stack.models.CloudStackSnapshot;
import com.cloud.stack.models.CloudStackTemplate;
import com.cloud.stack.models.CloudStackTemplatePermission;
import com.cloud.stack.models.CloudStackUser;
import com.cloud.stack.models.CloudStackUserVm;
import com.cloud.stack.models.CloudStackVolume;
import com.cloud.stack.models.CloudStackZone;
import com.cloud.utils.component.ManagerBase;

/**
 * EC2Engine processes the ec2 commands and calls their cloudstack analogs
 *
 */
@Component
public class EC2Engine extends ManagerBase {
    protected final static Logger logger = Logger.getLogger(EC2Engine.class);
    String managementServer = null;
    String cloudAPIPort = null;

    @Inject CloudStackSvcOfferingDao scvoDao;
    @Inject OfferingDao ofDao;
    @Inject CloudStackAccountDao accDao;
    
    private CloudStackApi _eng = null;

    private CloudStackAccount currentAccount = null;

    public EC2Engine() throws IOException {
    }
    
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		
		try {
			loadConfigValues();
		} catch(IOException e) {
			logger.error("EC2Engine Configuration failure", e);
			throw new ConfigurationException("EC2Engine configuration failure");
		}
        return true;
	}


    /**
     * Which management server to we talk to?
     * Load a mapping form Amazon values for 'instanceType' to cloud defined
     * diskOfferingId and serviceOfferingId.
     * 
     * @throws IOException
     */
    private void loadConfigValues() throws IOException {
        File propertiesFile = ConfigurationHelper.findConfigurationFile("ec2-service.properties");
        if (null != propertiesFile) {
            logger.info("Use EC2 properties file: " + propertiesFile.getAbsolutePath());
            Properties EC2Prop = new Properties();
            try {
                EC2Prop.load( new FileInputStream( propertiesFile ));
            } catch (FileNotFoundException e) {
                logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
            } catch (IOException e) {
                logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
            }
            managementServer = EC2Prop.getProperty( "managementServer" );
            cloudAPIPort = EC2Prop.getProperty( "cloudAPIPort", null );

            try {
                if(ofDao.getOfferingCount() == 0) {
                    String strValue = EC2Prop.getProperty("m1.small.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("m1.small", strValue);

                    strValue = EC2Prop.getProperty("m1.large.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("m1.large", strValue);

                    strValue = EC2Prop.getProperty("m1.xlarge.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("m1.xlarge", strValue);

                    strValue = EC2Prop.getProperty("c1.medium.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("c1.medium", strValue);

                    strValue = EC2Prop.getProperty("c1.xlarge.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("c1.xlarge", strValue);

                    strValue = EC2Prop.getProperty("m2.xlarge.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("m2.xlarge", strValue);

                    strValue = EC2Prop.getProperty("m2.2xlarge.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("m2.2xlarge", strValue);

                    strValue = EC2Prop.getProperty("m2.4xlarge.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("m2.4xlarge", strValue);

                    strValue = EC2Prop.getProperty("cc1.4xlarge.serviceId");
                    if(strValue != null) ofDao.setOfferMapping("cc1.4xlarge", strValue);
                }
            } catch(Exception e) {
                logger.error("Unexpected exception ", e);
            }
        } else logger.error( "ec2-service.properties not found" );
    }

    /**
     * Helper function to manage the api connection
     * 
     * @return
     */
    private CloudStackApi getApi() {
        if (_eng == null) {
            _eng = new CloudStackApi(managementServer, cloudAPIPort, false);
        }
        // regardless of whether _eng is initialized, we must make sure
        // access/secret keys are current with what's in the UserCredentials
        _eng.setApiKey(UserContext.current().getAccessKey());
        _eng.setSecretKey(UserContext.current().getSecretKey());
        return _eng;
    }


    /**
     * Verifies account can access CloudStack
     * 
     * @param accessKey
     * @param secretKey
     * @return
     * @throws EC2ServiceException
     */
    public boolean validateAccount( String accessKey, String secretKey ) throws EC2ServiceException {
        String oldApiKey = null;
        String oldSecretKey = null;

        if (accessKey == null || secretKey == null) {
            return false;
        }

        // okay, instead of using the getApi() nonsense for validate, we are going to manage _eng
        if (_eng == null) {
            _eng = new CloudStackApi(managementServer, cloudAPIPort, false);
        }

        try {
            oldApiKey = _eng.getApiKey();
            oldSecretKey = _eng.getSecretKey();
        } catch(Exception e) {
            // we really don't care, and expect this
        }
        try {
            _eng.setApiKey(accessKey);
            _eng.setSecretKey(secretKey);
            List<CloudStackAccount> accts = _eng.listAccounts(null, null, null, null, null, null, null, null);
            if (oldApiKey != null && oldSecretKey != null) {
                _eng.setApiKey(oldApiKey);
                _eng.setSecretKey(oldSecretKey);
            }
            if (accts == null) {
                return false;
            }
            return true;
        } catch(Exception e) {
            logger.error("Validate account failed!");
            throw new EC2ServiceException(ClientError.AuthFailure, "User not authorised");
        }
    }

    /**
     * Creates a security group
     * 
     * @param groupName
     * @param groupDesc
     * @return
     */
    public EC2SecurityGroup createSecurityGroup(String groupName, String groupDesc) {
        EC2SecurityGroup sg = new EC2SecurityGroup();
        try {
            CloudStackSecurityGroup grp = getApi().createSecurityGroup(groupName, null, groupDesc, null);
            if (grp != null && grp.getId() != null) {
            	sg.setId(grp.getId());
            }
        } catch( Exception e ) {
            logger.error( "EC2 CreateSecurityGroup - ", e);
            handleException(e);
        }
        return sg;
    }

    /**
     * Deletes a security group
     * 
     * @param groupName
     * @return
     */
    public boolean deleteSecurityGroup(String groupName) {
        try {
            CloudStackInfoResponse resp = getApi().deleteSecurityGroup(null, null, null, groupName);
            if (resp != null) {
                return resp.getSuccess();
            }
            return false;
        } catch( Exception e ) {
            logger.error( "EC2 DeleteSecurityGroup - ", e);
            handleException(e);
        }
        return false;
    }

    /**
     * returns a list of security groups
     * 
     * @param request
     * @return
     */
    public EC2DescribeSecurityGroupsResponse describeSecurityGroups(EC2DescribeSecurityGroups request) {
        EC2DescribeSecurityGroupsResponse response = new EC2DescribeSecurityGroupsResponse();
        try {
            response = listSecurityGroups( request.getGroupSet());
            EC2GroupFilterSet gfs = request.getFilterSet();
            if ( gfs != null ) {
            response = gfs.evaluate( response );
            }
        } catch( Exception e ) {
            logger.error( "EC2 DescribeSecurityGroups - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * CloudStack supports revoke only by using the ruleid of the ingress rule.
     * We list all security groups and find the matching group and use the first ruleId we find.
     * 
     * @param request
     * @return
     */
    public boolean revokeSecurityGroup( EC2AuthorizeRevokeSecurityGroup request )
    {
        try {
            String[] groupSet = new String[1];
            groupSet[0] = request.getName();
            String ruleId = null;

            EC2IpPermission[] items = request.getIpPermissionSet();

            EC2DescribeSecurityGroupsResponse response = listSecurityGroups( groupSet );
            EC2SecurityGroup[] groups = response.getGroupSet();
            if ( groups.length == 0 ) {
                throw new Exception("Unable to find security group name");
            }

            for (EC2SecurityGroup group : groups) {
                EC2IpPermission[] perms = group.getIpPermissionSet();
                for (EC2IpPermission perm : perms) {
                    ruleId = doesRuleMatch( items[0], perm );
                    if (ruleId != null) break;
                }
            }

            if (null == ruleId)
                throw new Exception("Specified Ip permission is invalid");
            CloudStackInfoResponse resp = getApi().revokeSecurityGroupIngress(ruleId);
            if (resp != null) {
                return resp.getSuccess();
            }
        } catch( Exception e ) {
            logger.error( "EC2 revokeSecurityGroupIngress" + " - " + e.getMessage());
            handleException(e);
        }
        return false;
    }

    /**
     * authorizeSecurityGroup
     * 
     * @param request - ip permission parameters
     */
    public boolean authorizeSecurityGroup(EC2AuthorizeRevokeSecurityGroup request ) {
        EC2IpPermission[] items = request.getIpPermissionSet();

        try {
            for (EC2IpPermission ipPerm : items) {
                EC2SecurityGroup[] groups = ipPerm.getUserSet();

                List<CloudStackKeyValue> secGroupList = new ArrayList<CloudStackKeyValue>();
                for (EC2SecurityGroup group : groups) {
                    CloudStackKeyValue pair = new CloudStackKeyValue();
                    pair.setKeyValue(group.getAccount(), group.getName());
                    secGroupList.add(pair);
                }
                CloudStackSecurityGroup resp = null;
                if (ipPerm.getProtocol().equalsIgnoreCase("icmp")) {
                    resp = getApi().authorizeSecurityGroupIngress(null, constructList(ipPerm.getIpRangeSet()), null, null,
                            ipPerm.getIcmpCode(), ipPerm.getIcmpType(), ipPerm.getProtocol(), null,
                            request.getName(), null, secGroupList);
                } else {
                    resp = getApi().authorizeSecurityGroupIngress(null, constructList(ipPerm.getIpRangeSet()), null,
                            ipPerm.getToPort().longValue(), null, null, ipPerm.getProtocol(), null, request.getName(),
                            ipPerm.getFromPort().longValue(), secGroupList);
                }
                if (resp != null ){
                    List<CloudStackIngressRule> ingressRules = resp.getIngressRules();
                    for (CloudStackIngressRule ingressRule : ingressRules)
                        if (ingressRule.getRuleId() == null) return false;
                } else {
                return false;
            }
			}
        } catch(Exception e) {
            logger.error( "EC2 AuthorizeSecurityGroupIngress - ", e);
            handleException(e);
        }
        return true;
    }

    /**
     * Does the permission from the request (left) match the permission from the cloudStack query (right).
     * If the cloudStack rule matches then we return its ruleId.
     * 
     * @param permLeft
     * @param permRight
     * @return ruleId of the cloudstack rule
     */
    private String doesRuleMatch(EC2IpPermission permLeft, EC2IpPermission permRight)
    {
        int matches = 0;

        if (null != permLeft.getIcmpType() && null != permLeft.getIcmpCode()) {
            if (null == permRight.getIcmpType() || null == permRight.getIcmpCode()) return null;

            if (!permLeft.getIcmpType().equalsIgnoreCase( permRight.getIcmpType())) return null;
            if (!permLeft.getIcmpCode().equalsIgnoreCase( permRight.getIcmpCode())) return null;
            matches++;
        }

        // -> "Valid Values for EC2 security groups: tcp | udp | icmp or the corresponding protocol number (6 | 17 | 1)."
        if (null != permLeft.getProtocol()) {
            if (null == permRight.getProtocol()) return null;

            String protocol = permLeft.getProtocol();
            if (protocol.equals( "6"  )) protocol = "tcp";
            else if (protocol.equals( "17" )) protocol = "udp";
            else if (protocol.equals( "1"  )) protocol = "icmp";

            if (!protocol.equalsIgnoreCase( permRight.getProtocol())) return null;
            matches++;
        }


        if (null != permLeft.getCIDR()) {
            if (null == permRight.getCIDR()) return null;

            if (!permLeft.getCIDR().equalsIgnoreCase( permRight.getCIDR())) return null;
            matches++;
        }

        // -> is the port(s) from the request (left) a match of the rule's port(s)
        if (0 != permLeft.getFromPort()) {
            // -> -1 means all ports match
            if (-1 != permLeft.getFromPort()) {
                if (permLeft.getFromPort().compareTo(permRight.getFromPort()) != 0 ||
                        permLeft.getToPort().compareTo(permRight.getToPort()) != 0)
                    return null;
            }
            matches++;
        }


        // -> was permLeft set up properly with at least one property to match?
        if ( 0 == matches )
            return null;
        else return permRight.getRuleId();
    }

    /**
     * Returns a list of all snapshots
     * 
     * @param request
     * @return
     */
    public EC2DescribeSnapshotsResponse describeSnapshots( EC2DescribeSnapshots request )
    {
        EC2DescribeSnapshotsResponse response = new EC2DescribeSnapshotsResponse();
        EC2SnapshotFilterSet sfs = request.getFilterSet();
        EC2TagKeyValue[] tagKeyValueSet = request.getResourceTagSet();

        try {
            response = listSnapshots( request.getSnapshotSet(),
                    getResourceTags(tagKeyValueSet));
            if (response == null) {
                return new EC2DescribeSnapshotsResponse();
            }
            EC2Snapshot[] snapshots = response.getSnapshotSet();
            // -> query to get the volume size for each snapshot
            HashMap<String, Long> volumeIdSize = new HashMap<String, Long>();
            for( EC2Snapshot snap : snapshots ) {
                Boolean duplicateVolume = false;
                Long size = null;
                if ( volumeIdSize.containsKey(snap.getVolumeId()) ) {
                    size = volumeIdSize.get(snap.getVolumeId());
                    duplicateVolume = true;
                    break;
                }
                if ( !duplicateVolume ) {
                    EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
                    volumes = listVolumes(snap.getVolumeId(), null, volumes, null);
                    EC2Volume[] volumeSet = volumes.getVolumeSet();
                    if (volumeSet.length > 0) size = volumeSet[0].getSize();
                    volumeIdSize.put(snap.getVolumeId(), size);
                }
                snap.setVolumeSize(size);
            }
            if (sfs != null) {
                response = sfs.evaluate( response );
            }
        } catch( Exception e ) {
            logger.error( "EC2 DescribeSnapshots - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * Creates a snapshot
     * 
     * @param volumeId
     * @return
     */
    public EC2Snapshot createSnapshot( String volumeId ) {
        EC2Snapshot ec2Snapshot = new EC2Snapshot();
        try {
            CloudStackSnapshot snap = getApi().createSnapshot(volumeId, null, null, null);
            if (snap == null) {
                throw new Exception("Unable to create snapshot");
            }
            ec2Snapshot.setId(snap.getId());
            ec2Snapshot.setName(snap.getName());
            ec2Snapshot.setType(snap.getSnapshotType());
            ec2Snapshot.setAccountName(snap.getAccountName());
            ec2Snapshot.setDomainId(snap.getDomainId());
            ec2Snapshot.setCreated(snap.getCreated());
            ec2Snapshot.setVolumeId(snap.getVolumeId());

            List<CloudStackVolume> vols = getApi().listVolumes(null, null, null, snap.getVolumeId(), null, null, null, null, null, null, null, null);

            if(vols.size() > 0) {
                assert(vols.get(0).getSize() != null);
                Long sizeInGB = vols.get(0).getSize().longValue()/1073741824;
                ec2Snapshot.setVolumeSize(sizeInGB);
            }
        } catch( Exception e ) {
            logger.error( "EC2 CreateSnapshot - ", e);
            handleException(e);
        }
        return ec2Snapshot;
    }

    /**
     * Deletes a snapshot
     * 
     * @param snapshotId
     * @return
     */
    public boolean deleteSnapshot(String snapshotId) {
        try {

            CloudStackInfoResponse resp = getApi().deleteSnapshot(snapshotId);
            if(resp != null) {
                return resp.getSuccess();
            }

            return false;
        } catch(Exception e) {
            logger.error( "EC2 DeleteSnapshot - ", e);
            handleException(e);
        }
        return false;
    }



    /**
     * Modify an existing template
     * 
     * @param request
     * @return
     */
    public boolean modifyImageAttribute( EC2ModifyImageAttribute request )
    {
        try {
            if(request.getAttribute().equals(ImageAttribute.launchPermission)){
                EC2ImageLaunchPermission[] launchPermissions = request.getLaunchPermissionSet();
                for (EC2ImageLaunchPermission launchPermission : launchPermissions) {
                    String accounts = "";
                    Boolean isPublic = null;
                    EC2ImageLaunchPermission.Operation operation =  launchPermission.getLaunchPermOp();
                    List<String> accountOrGroupList = launchPermission.getLaunchPermissionList();
                    if(accountOrGroupList != null && !accountOrGroupList.isEmpty()){
                        boolean first = true;
                        for(String accountOrGroup : accountOrGroupList){
                            if("all".equalsIgnoreCase(accountOrGroup)){
                                if(operation.equals(EC2ImageLaunchPermission.Operation.add)){
                                    isPublic = true;
                                }else{
                                    isPublic = false;
                                }
                            }else{
                                if(!first){
                                    accounts = accounts + ",";
                                }
                                accounts = accounts + accountOrGroup;
                                first = false;
                            }
                        }
                    }
                    CloudStackInfoResponse resp = getApi().updateTemplatePermissions(request.getImageId(), accounts, null, null, isPublic, operation.toString());
                    if (!resp.getSuccess())
                        return false;
                }
                return true;
            }else if(request.getAttribute().equals(ImageAttribute.description)){
                CloudStackTemplate resp = getApi().updateTemplate(request.getImageId(), null, request.getDescription(), null, null, null, null);
                if (resp != null) {
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            logger.error( "EC2 modifyImageAttribute - ", e);
            handleException(e);
        }
        return false;
    }

    public EC2ImageAttributes describeImageAttribute(EC2DescribeImageAttribute request) {
        EC2ImageAttributes imageAtts = new EC2ImageAttributes();

        try {
            imageAtts.setImageId(request.getImageId());
            if(request.getAttribute().equals(ImageAttribute.launchPermission)){
                CloudStackTemplatePermission tempPerm = getApi().listTemplatePermissions(request.getImageId(), null, null);
                if(tempPerm != null){
                    imageAtts.setDomainId(tempPerm.getDomainId());

                    List<String> accntList = tempPerm.getAccounts();
                    imageAtts.setAccountNamesWithLaunchPermission(accntList);

                    imageAtts.setIsPublic(tempPerm.getIsPublic());
                }
            }else if(request.getAttribute().equals(ImageAttribute.description)){
                EC2DescribeImagesResponse descriptionResp = new EC2DescribeImagesResponse();
                listTemplates(request.getImageId(), descriptionResp);
                if(descriptionResp.getImageSet() != null){
                    EC2Image[] images = descriptionResp.getImageSet();
                    imageAtts.setDescription(images[0].getDescription());
                }
            }

        } catch (Exception e) {
            logger.error( "EC2 describeImageAttribute - ", e);
            handleException(e);
        }

        return imageAtts;
    }

    // handlers
    /**
     * return password data from the instance
     * 
     * @param instanceId
     * @return
     */
    public EC2PasswordData getPasswordData(String instanceId) {
        EC2PasswordData passwdData = new EC2PasswordData();
        try {
            CloudStackPasswordData resp = getApi().getVMPassword(instanceId);
            if (resp != null) {
                passwdData.setInstanceId(instanceId);
                passwdData.setEncryptedPassword(resp.getEncryptedpassword());
            }
        } catch(Exception e) {
            logger.error("EC2 GetPasswordData - ", e);
            handleException(e);
        }
        return passwdData;
    }

    /**
     * Lists SSH KeyPairs on the system
     * 
     * @param request
     * @return
     */
    public EC2DescribeKeyPairsResponse describeKeyPairs( EC2DescribeKeyPairs request ) {
        EC2DescribeKeyPairsResponse response = new EC2DescribeKeyPairsResponse();
        try {
            response = listKeyPairs(request.getKeyNames());
            EC2KeyPairFilterSet kfs = request.getKeyFilterSet();
            if (kfs != null)
                response = kfs.evaluate(response);
        } catch(Exception e) {
            logger.error("EC2 DescribeKeyPairs - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * Delete SSHKeyPair
     * 
     * @param request
     * @return
     */
    public boolean deleteKeyPair( EC2DeleteKeyPair request ) {
        CloudStackInfoResponse resp = new CloudStackInfoResponse();
        String keyPairName = request.getKeyName();
        try {
            resp = getApi().deleteSSHKeyPair(keyPairName, null, null);
            if (resp == null) {
                throw new Exception("Ivalid CloudStack API response");
            }
        } catch(Exception e) {
            logger.error("EC2 DeleteKeyPair - ", e);
            handleException(e);
        }
        return resp.getSuccess();
    }

    /**
     * Create SSHKeyPair
     * 
     * @param request
     * @return
     */
    public EC2SSHKeyPair createKeyPair(EC2CreateKeyPair request) {
        String keyPairName = request.getKeyName();
        EC2SSHKeyPair response = new EC2SSHKeyPair();
        try {
            CloudStackKeyPair resp = getApi().createSSHKeyPair(keyPairName, null, null);
            if (resp == null) {
                throw new Exception("Ivalid CloudStack API response");
            }
            response.setFingerprint(resp.getFingerprint());
            response.setKeyName(resp.getName());
            response.setPrivateKey(resp.getPrivatekey());
        } catch (Exception e) {
            logger.error("EC2 CreateKeyPair - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * Import an existing SSH KeyPair
     * 
     * @param request
     * @return
     */
    public EC2SSHKeyPair importKeyPair( EC2ImportKeyPair request ) {
        EC2SSHKeyPair response = new EC2SSHKeyPair();
        try {
            CloudStackKeyPair resp = getApi().registerSSHKeyPair(request.getKeyName(), request.getPublicKeyMaterial());
            if (resp == null) {
                throw new Exception("Ivalid CloudStack API response");
            }
            response.setFingerprint(resp.getFingerprint());
            response.setKeyName(resp.getName());
            response.setPrivateKey(resp.getPrivatekey());
        } catch (Exception e) {
            logger.error("EC2 ImportKeyPair - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * list ip addresses that have been allocated
     * 
     * @param request
     * @return
     */
    public EC2DescribeAddressesResponse describeAddresses( EC2DescribeAddresses request ) {
        EC2DescribeAddressesResponse response = new EC2DescribeAddressesResponse();
        try {
            response = listAddresses(request.getPublicIpsSet());
            EC2AddressFilterSet afs = request.getFilterSet();
            if (afs != null)
                response = afs.evaluate(response);
        } catch(Exception e) {
            logger.error("EC2 DescribeAddresses - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * release an IP Address
     * 
     * @param request
     * @return
     */
    public boolean releaseAddress(EC2ReleaseAddress request) {
        try {
            List<CloudStackIpAddress> cloudIps = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null);
            if (cloudIps == null)
                throw new Exception("Specified ipAddress doesn't exist");
            CloudStackIpAddress cloudIp = cloudIps.get(0);
            CloudStackInfoResponse resp = getApi().disassociateIpAddress(cloudIp.getId());
            if (resp != null) {
                return resp.getSuccess();
            }
        } catch(Exception e) {
            logger.error("EC2 ReleaseAddress - ", e);
            handleException(e);
        }
        return false;
    }

    /**
     * Associate an address with an instance
     * 
     * @param request
     * @return
     */
    public boolean associateAddress( EC2AssociateAddress request ) {
        try {
            List<CloudStackIpAddress> cloudIps = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null);
            if (cloudIps == null)
                throw new Exception("Specified ipAddress doesn't exist");
            CloudStackIpAddress cloudIp = cloudIps.get(0);

            List<CloudStackUserVm> vmList = getApi().listVirtualMachines(null, null, true, null, null, null, null,
                    request.getInstanceId(), null, null, null, null, null, null, null, null, null);
            if (vmList == null || vmList.size() == 0) {
                throw new Exception("Instance not found");
            }
            CloudStackUserVm cloudVm = vmList.get(0);

            CloudStackInfoResponse resp = getApi().enableStaticNat(cloudIp.getId(), cloudVm.getId());
            if (resp != null) {
                return resp.getSuccess();
            }
        } catch(Exception e) {
            logger.error( "EC2 AssociateAddress - ", e);
            handleException(e);
        }
        return false;
    }

    /**
     * Disassociate an address from an instance
     * 
     * @param request
     * @return
     */
    public boolean disassociateAddress( EC2DisassociateAddress request ) {
        try {
            List<CloudStackIpAddress> cloudIps = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null);
            if (cloudIps == null)
                throw new Exception("Specified ipAddress doesn't exist");
            CloudStackIpAddress cloudIp = cloudIps.get(0);

            CloudStackInfoResponse resp = getApi().disableStaticNat(cloudIp.getId());
            if (resp != null) {
                return resp.getSuccess();
            }
        } catch(Exception e) {
            logger.error( "EC2 DisassociateAddress - ", e);
            handleException(e);
        }
        return false;
    }

    /**
     * Allocate an address
     * 
     * @param request
     * @return
     */
    public EC2Address allocateAddress() {
        EC2Address ec2Address = new EC2Address();
        try {
            // this gets our networkId
            CloudStackAccount caller = getCurrentAccount();

            CloudStackZone zone = findZone();
            //CloudStackNetwork net = findNetwork(zone);
//			CloudStackIpAddress resp = getApi().associateIpAddress(null, null, null, "0036952d-48df-4422-9fd0-94b0885e18cb");
            CloudStackIpAddress resp = getApi().associateIpAddress(zone.getId(), caller.getName(), caller.getDomainId(), null);
            ec2Address.setAssociatedInstanceId(resp.getId());

            if (resp.getIpAddress() == null) {
                List<CloudStackIpAddress> addrList = getApi().listPublicIpAddresses(null, null, null, null, null, null, null, null, null);
                if (addrList != null && addrList.size() > 0) {
                    for (CloudStackIpAddress addr: addrList) {
                        if (addr.getId().equalsIgnoreCase(resp.getId())) {
                            ec2Address.setIpAddress(addr.getIpAddress());
                        }
                    }
                }
            } else {
                ec2Address.setIpAddress(resp.getIpAddress());
            }
        } catch(Exception e) {
            logger.error( "EC2 AllocateAddress - ", e);
            handleException(e);
        }
        return ec2Address;
    }

    /**
     * List of templates available.  We only support the imageSet version of this call or when no search parameters are passed
     * which results in asking for all templates.
     * 
     * @param request
     * @return
     */
    public EC2DescribeImagesResponse describeImages(EC2DescribeImages request) {
        EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();
        try {
            String[] templateIds = request.getImageSet();
            EC2ImageFilterSet ifs = request.getFilterSet();

            if ( templateIds.length == 0 ) {
                images = listTemplates(null, images);
            } else {
                for (String s : templateIds) {
                    images = listTemplates(s, images);
                }
            }
            if (ifs != null)
                return ifs.evaluate(images);
        } catch( Exception e ) {
            logger.error( "EC2 DescribeImages - ", e);
            handleException(e);
        }
        return images;
    }

    /**
     * Create a template
     * Amazon API just gives us the instanceId to create the template from.
     * But our createTemplate function requires the volumeId and osTypeId.
     * So to get that we must make the following sequence of cloud API calls:
     * 1) listVolumes&virtualMachineId=   -- gets the volumeId
     * 2) listVirtualMachinees&id=        -- gets the templateId
     * 3) listTemplates&id=               -- gets the osTypeId
     * 
     * If we have to start and stop the VM in question then this function is
     * going to take a long time to complete.
     * 
     * @param request
     * @return
     */
    public EC2CreateImageResponse createImage(EC2CreateImage request)
    {
        EC2CreateImageResponse response = new EC2CreateImageResponse();
        boolean needsRestart = false;
        String volumeId      = null;
        try {
            // [A] Creating a template from a VM volume should be from the ROOT volume
            //     Also for this to work the VM must be in a Stopped state so we 'reboot' it if its not
            EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
            volumes = listVolumes( null, request.getInstanceId(), volumes, null );
            EC2Volume[] volSet = volumes.getVolumeSet();
            for (EC2Volume vol : volSet) {
                if (vol.getType().equalsIgnoreCase( "ROOT" )) {
                    String vmState = vol.getVMState();
                    if (vmState.equalsIgnoreCase( "running" ) || vmState.equalsIgnoreCase( "starting" )) {
                        needsRestart = true;
                        if (!stopVirtualMachine( request.getInstanceId() ))
                            throw new Exception("Instance must be in a stopped state");
                    }
                    volumeId = vol.getId();
                    break;
                }
            }

            // [B] The parameters must be in sorted order for proper signature generation
            EC2DescribeInstancesResponse instances = new EC2DescribeInstancesResponse();
            instances = lookupInstances( request.getInstanceId(), instances, null );
            EC2Instance[] instanceSet = instances.getInstanceSet();
            String templateId = instanceSet[0].getTemplateId();

            EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();
            images = listTemplates( templateId, images );
            EC2Image[] imageSet = images.getImageSet();
            String osTypeId = imageSet[0].getOsTypeId();

            CloudStackTemplate resp = getApi().createTemplate((request.getDescription() == null ? "" : request.getDescription()), request.getName(),
                    osTypeId, null, null, null, null, null, null, volumeId);
            if (resp == null || resp.getId() == null) {
                throw new Exception("Image couldn't be created");
            }

            //if template was created succesfully, create the new image response
            response.setId(resp.getId());

            // [C] If we stopped the virtual machine now we need to restart it
            if (needsRestart) {
                if (!startVirtualMachine( request.getInstanceId() ))
                        throw new Exception("Failed to start the stopped instance");
            }
        } catch( Exception e ) {
            logger.error( "EC2 CreateImage - ", e);
            handleException(e);
        }
        return response;
    }

    /**
     * Register a template
     * 
     * @param request
     * @return
     */
    public EC2CreateImageResponse registerImage(EC2RegisterImage request) {
        EC2CreateImageResponse image = new EC2CreateImageResponse();
        try {
            List<CloudStackTemplate> templates = getApi().registerTemplate((request.getDescription() == null ? request.getName() : request.getDescription()),
                    request.getFormat(), request.getHypervisor(), request.getName(), toOSTypeId(request.getOsTypeName()), request.getLocation(),
                    toZoneId(request.getZoneName(), null), null, null, null, null, null, null, null, null, null);
            if (templates != null) {
                // technically we will only ever register a single template...
                for (CloudStackTemplate template : templates) {
                    if (template != null && template.getId() != null) {
                        image.setId(template.getId().toString());
                        return image;
                    }
                }
            }
        } catch( Exception e ) {
            logger.error( "EC2 RegisterImage - ", e);
            handleException(e);
        }
        return image;
    }

    /**
     * Deregister a template(image)
     * Our implementation is different from Amazon in that we do delete the template
     * when we deregister it.   The cloud API has not deregister call.
     * 
     * @param image
     * @return
     */
    public boolean deregisterImage( EC2Image image )
    {
        try {
            CloudStackInfoResponse resp = getApi().deleteTemplate(image.getId(), null);
            return resp.getSuccess();
        } catch( Exception e ) {
            logger.error( "EC2 DeregisterImage - ", e);
            handleException(e);
        }
        return false;
    }

    /**
     * list instances
     * 
     * @param request
     * @return
     */
    public EC2DescribeInstancesResponse describeInstances(EC2DescribeInstances request ) {
        EC2DescribeInstancesResponse response = new EC2DescribeInstancesResponse();
        try {
            EC2TagKeyValue[] tagKeyValueSet = request.getResourceTagSet();
            response = listVirtualMachines( request.getInstancesSet(), request.getFilterSet(),
                    getResourceTags(tagKeyValueSet));
        } catch( Exception e ) {
            logger.error( "EC2 DescribeInstances - " ,e);
            handleException(e);
        }
        return response;
    }

    /**
     * list Zones
     * 
     * @param request
     * @return
     */
    public EC2DescribeAvailabilityZonesResponse describeAvailabilityZones(EC2DescribeAvailabilityZones request) {
        EC2DescribeAvailabilityZonesResponse availableZones = new EC2DescribeAvailabilityZonesResponse();
        try {
            availableZones = listZones(request.getZoneSet(), null);
            EC2AvailabilityZonesFilterSet azfs = request.getFilterSet();
            if ( azfs != null )
                availableZones = azfs.evaluate(availableZones);
        } catch( Exception e ) {
            logger.error( "EC2 DescribeAvailabilityZones - " ,e);
            handleException(e);
        }
        return availableZones;
    }

    /**
     * list volumes
     * 
     * @param request
     * @return
     */
    public EC2DescribeVolumesResponse describeVolumes( EC2DescribeVolumes request ) {
        EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
        EC2VolumeFilterSet vfs = request.getFilterSet();
        EC2TagKeyValue[] tagKeyValueSet = request.getResourceTagSet();
        try {
            String[] volumeIds = request.getVolumeSet();
            if ( 0 == volumeIds.length ){
                volumes = listVolumes( null, null, volumes, getResourceTags(tagKeyValueSet) );
            } else {
                for (String s : volumeIds)
                    volumes = listVolumes(s, null, volumes, getResourceTags(tagKeyValueSet) );
            }
            if ( vfs != null )
                volumes = vfs.evaluate( volumes );
        }  catch( Exception e ) {
            logger.error( "EC2 DescribeVolumes - ", e);
            handleException(e);
        }
        return volumes;
    }

    /**
     * Attach a volume to an instance
     * 
     * @param request
     * @return
     */
    public EC2Volume attachVolume( EC2Volume request ) {
        EC2Volume resp = new EC2Volume();
        try {
            request.setDeviceId(mapDeviceToCloudDeviceId(request.getDevice()));
            CloudStackVolume vol = getApi().attachVolume(request.getId(), request.getInstanceId(), request.getDeviceId());
            if(vol != null) {
                resp.setAttached(vol.getAttached());
                resp.setCreated(vol.getCreated());
                resp.setDevice(request.getDevice());
                resp.setDeviceId(vol.getDeviceId());
                resp.setHypervisor(vol.getHypervisor());
                resp.setId(vol.getId());
                resp.setInstanceId(vol.getVirtualMachineId());
                resp.setSize(vol.getSize());
                resp.setSnapshotId(vol.getSnapshotId());
                resp.setState(vol.getState());
                resp.setType(vol.getVolumeType());
                resp.setVMState(vol.getVirtualMachineState());
                resp.setAttachmentState(mapToAmazonVolumeAttachmentState(vol.getVirtualMachineState()));
                resp.setZoneName(vol.getZoneName());
            }
        } catch( Exception e ) {
            logger.error( "EC2 AttachVolume - ", e);
            handleException(e);
        }
        return resp;
    }

    /**
     * Detach a volume from an instance
     * 
     * @param request
     * @return
     */
    public EC2Volume detachVolume(EC2Volume request) {
        EC2Volume resp = new EC2Volume();
        try {
            // verifying if instanceId and deviceId provided is valid
            EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
            volumes = listVolumes(request.getId(), null, volumes, null);
            if (volumes != null) {
               EC2Volume[] volumeSet = volumes.getVolumeSet();
                if (request.getInstanceId() != null) {
                    if ( !request.getInstanceId().equalsIgnoreCase(volumeSet[0].getInstanceId()) )
                        throw new Exception("Volume is not attached to the Instance");
                }
                if (request.getDevice() != null) {
                    String devicePath = null;
                    if ( volumeSet[0].getDeviceId() != null )
                        devicePath = cloudDeviceIdToDevicePath( volumeSet[0].getHypervisor(), volumeSet[0].getDeviceId());
                    if ( !request.getDevice().equalsIgnoreCase(devicePath) )
                        throw new Exception("Volume is not attached to the Device");
                }
            }

            CloudStackVolume vol = getApi().detachVolume(null , request.getId(), null);
            if(vol != null) {
                resp.setAttached(vol.getAttached());
                resp.setCreated(vol.getCreated());
                resp.setDevice(request.getDevice());
                resp.setDeviceId(vol.getDeviceId());
                resp.setHypervisor(vol.getHypervisor());
                resp.setId(vol.getId());
                resp.setInstanceId(vol.getVirtualMachineId());
                resp.setSize(vol.getSize());
                resp.setSnapshotId(vol.getSnapshotId());
                resp.setState(vol.getState());
                resp.setType(vol.getVolumeType());
                resp.setVMState(vol.getVirtualMachineState());
                resp.setZoneName(vol.getZoneName());
            }
        } catch( Exception e ) {
            logger.error( "EC2 DetachVolume - ", e);
            handleException(e);
        }
        return resp;
    }

    /**
     * Create a volume
     * 
     * @param request
     * @return
     */
    public EC2Volume createVolume( EC2CreateVolume request ) {
        EC2Volume resp = new EC2Volume();
        try {
            // -> put either snapshotid or diskofferingid on the request
            String snapshotId = request.getSnapshotId();
            Long size = request.getSize();
            String diskOfferingId = null;
            String taggedDiskOffering = null;

            if (snapshotId == null) {
                List<CloudStackDiskOffering> disks = getApi().listDiskOfferings(null, null, null, null);
                for (CloudStackDiskOffering offer : disks) {
                    if (offer.isCustomized()) {
                        if (offer.getTags() == null) {
                            diskOfferingId = offer.getId();
                            break;
                        } else {
                            taggedDiskOffering = offer.getId();
                        }
                    }
                }
                if (diskOfferingId == null) {
                    diskOfferingId = taggedDiskOffering;
                }
                if (diskOfferingId == null) throw new EC2ServiceException(ServerError.InternalError, "No Customize Disk Offering Found");
            }

//			// -> no volume name is given in the Amazon request but is required in the cloud API
            CloudStackVolume vol = getApi().createVolume(UUID.randomUUID().toString(), null, diskOfferingId, null, size, snapshotId, toZoneId(request.getZoneName(), null));
            if (vol != null) {
                resp.setAttached(vol.getAttached());
                resp.setCreated(vol.getCreated());
//				resp.setDevice();
                resp.setDeviceId(vol.getDeviceId());
                resp.setHypervisor(vol.getHypervisor());
                resp.setId(vol.getId());
                resp.setInstanceId(vol.getVirtualMachineId());
                resp.setSize(vol.getSize());
                resp.setSnapshotId(vol.getSnapshotId());
                resp.setState(vol.getState());
                resp.setType(vol.getVolumeType());
                resp.setVMState(vol.getVirtualMachineState());
                resp.setAttachmentState("detached");
                resp.setZoneName(vol.getZoneName());
            }
        } catch( Exception e ) {
            logger.error( "EC2 CreateVolume - ", e);
            handleException(e);
        }
        return resp;
    }

    /**
     * Delete a volume
     * 
     * @param request
     * @return
     */
    public EC2Volume deleteVolume( EC2Volume request ) {
        try {
            CloudStackInfoResponse resp = getApi().deleteVolume(request.getId());
            if(resp != null) {
                request.setState("deleted");
            }
        } catch( Exception e ) {
            logger.error( "EC2 DeleteVolume - ", e);
            handleException(e);
        }
        return request;
    }

    /**
     * Create/Delete tags
     *
     * @param request
     * @param operation
     * @return
     */
    public boolean modifyTags( EC2Tags request, String operation) {
        try {
            List<CloudStackKeyValue> resourceTagList = new ArrayList<CloudStackKeyValue>();
            for ( EC2TagKeyValue resourceTag : request.getResourceTags()){
                CloudStackKeyValue pair = new CloudStackKeyValue();
                pair.setKeyValue(resourceTag.getKey(), resourceTag.getValue());
                resourceTagList.add(pair);
            }
            EC2TagTypeId[] resourceTypeSet = request.getResourceTypeSet();
            for (EC2TagTypeId resourceType : resourceTypeSet) {
                String cloudStackResourceType = mapToCloudStackResourceType(resourceType.getResourceType());
                List<String> resourceIdList = new ArrayList<String>();
                for ( String resourceId : resourceType.getResourceIds())
                    resourceIdList.add(resourceId);
                CloudStackInfoResponse resp = new CloudStackInfoResponse();
                if (operation.equalsIgnoreCase("create"))
                    resp = getApi().createTags(cloudStackResourceType, resourceIdList, resourceTagList);
                else if(operation.equalsIgnoreCase("delete"))
                    resp = getApi().deleteTags(cloudStackResourceType, resourceIdList, resourceTagList);
                else
                    throw new EC2ServiceException( ServerError.InternalError, "Unknown operation." );
                if (resp.getSuccess() == false)
                    return false;
            }
            return true;
        } catch (Exception e){
            logger.error( "EC2 Create/Delete Tags - ", e);
            handleException(e);
        }
        return false;
    }

    /**
     * Describe tags
     *
     * @param request
     * @return
     */
    public EC2DescribeTagsResponse describeTags (EC2DescribeTags request) {
        EC2DescribeTagsResponse tagResponse = new EC2DescribeTagsResponse();
        try {
            tagResponse = new EC2DescribeTagsResponse();
            List<CloudStackResourceTag> resourceTagList = getApi().listTags(null, null, null, true, null);

            if (resourceTagList != null && resourceTagList.size() > 0) {
                for (CloudStackResourceTag resourceTag: resourceTagList) {
                    EC2ResourceTag tag = new EC2ResourceTag();
                    tag.setResourceId(resourceTag.getResourceId());
                    tag.setResourceType(mapToAmazonResourceType(resourceTag.getResourceType()));
                    tag.setKey(resourceTag.getKey());
                    if (resourceTag.getValue() != null)
                        tag.setValue(resourceTag.getValue());
                    tagResponse.addTags(tag);
                }
            }

            EC2TagsFilterSet tfs = request.getFilterSet();
            if (tfs != null)
                tagResponse =  tfs.evaluate(tagResponse);
        } catch(Exception e) {
            logger.error("EC2 DescribeTags - ", e);
            handleException(e);
        }
        return tagResponse;
    }

    /**
     * Reboot an instance or instances
     * 
     * @param request
     * @return
     */
    public boolean rebootInstances(EC2RebootInstances request)
    {
        EC2Instance[] vms = null;

        // -> reboot is not allowed on destroyed (i.e., terminated) instances
        try {
            String[] instanceSet = request.getInstancesSet();
            EC2DescribeInstancesResponse previousState = listVirtualMachines( instanceSet, null, null );
            vms = previousState.getInstanceSet();

            // -> send reboot requests for each found VM
            for (EC2Instance vm : vms) {
                if (vm.getState().equalsIgnoreCase( "Destroyed" )) continue;

                CloudStackUserVm resp = getApi().rebootVirtualMachine(vm.getId());
                if (logger.isDebugEnabled())
                    logger.debug("Rebooting VM " + resp.getId() + " job " + resp.getJobId());
            }

            // -> if some specified VMs where not found we have to tell the caller
            if (instanceSet.length != vms.length)
                throw new Exception("One or more instanceIds do not exist, other instances rebooted.");

            return true;
        } catch( Exception e ) {
            logger.error( "EC2 RebootInstances - ", e );
            handleException(e);
        }
        return false;
    }

    /**
     * Using a template (AMI), launch n instances
     * 
     * @param request
     * @return
     */
    public EC2RunInstancesResponse runInstances(EC2RunInstances request) {
        EC2RunInstancesResponse instances = new EC2RunInstancesResponse();
        int createInstances    = 0;
        int canCreateInstances = -1;
        int countCreated       = 0;
        String groupIds   = null;
        String groupNames = null;

        try {
            // ugly...
            canCreateInstances = calculateAllowedInstances();
            if (-1 == canCreateInstances) canCreateInstances = request.getMaxCount();

            if (canCreateInstances < request.getMinCount()) {
                logger.info( "EC2 RunInstances - min count too big (" + request.getMinCount() + "), " + canCreateInstances + " left to allocate");
                throw new Exception("Min Count is greater than the number of instances left to allocate");
            }

            if ( canCreateInstances < request.getMaxCount())
                createInstances = request.getMinCount();
            else
                createInstances = request.getMaxCount();

            //find CS service Offering ID
            String instanceType = "m1.small";
            if(request.getInstanceType() != null){
                instanceType = request.getInstanceType();
            }
            CloudStackServiceOfferingVO svcOffering = getCSServiceOfferingId(instanceType);
            if(svcOffering == null){
                logger.info("No ServiceOffering found to be defined by name, please contact the administrator "+instanceType );
                throw new Exception("instanceType not found");
            }

            // zone stuff
            String zoneId = toZoneId(request.getZoneName(), null);

            List<CloudStackZone> zones = getApi().listZones(null, null, zoneId, null);
            if (zones == null || zones.size() == 0) {
                logger.info("EC2 RunInstances - zone [" + request.getZoneName() + "] not found!");
                throw new Exception("zone not found");
            }
            // we choose first zone?
            CloudStackZone zone = zones.get(0);

            // network
            //CloudStackNetwork network = findNetwork(zone);

            // for EC2 security groups either a group ID or a group name is accepted
            String[] sgIdList = request.getSecurityGroupIdSet();
            String[] sgNameList = request.getSecurityGroupNameSet();
            if (sgIdList.length != 0 && sgNameList.length != 0)
                throw new EC2ServiceException(ClientError.InvalidParameterCombination,
                        " for EC2 groups either a group ID or a group name is accepted");

            if (sgIdList.length != 0)
                groupIds = constructList(sgIdList);
            if (sgNameList.length != 0)
                groupNames = constructList(sgNameList);

            // now actually deploy the vms
            for( int i=0; i < createInstances; i++ ) {
                try{
                    CloudStackUserVm resp = getApi().deployVirtualMachine(svcOffering.getId(),
                            request.getTemplateId(), zoneId, null, null, null, null,
                            null, null, null, request.getKeyName(), null, null,
                            groupIds, groupNames, request.getSize().longValue(), request.getUserData());
                    EC2Instance vm = new EC2Instance();
                    vm.setId(resp.getId().toString());
                    vm.setName(resp.getName());
                    vm.setZoneName(resp.getZoneName());
                    vm.setTemplateId(resp.getTemplateId().toString());
                    if (resp.getSecurityGroupList() != null && resp.getSecurityGroupList().size() > 0) {
                        List<CloudStackSecurityGroup> securityGroupList = resp.getSecurityGroupList();
                        for (CloudStackSecurityGroup securityGroup : securityGroupList) {
                            EC2SecurityGroup param = new EC2SecurityGroup();
                            param.setId(securityGroup.getId());
                            param.setName(securityGroup.getName());
                            vm.addGroupName(param);
                        }
                    }
                    vm.setState(resp.getState());
                    vm.setCreated(resp.getCreated());
                    List <CloudStackNic> nicList = resp.getNics();
                    for (CloudStackNic nic : nicList) {
                        if (nic.getIsDefault()) {
                            vm.setPrivateIpAddress(nic.getIpaddress());
                            break;
                        }
                    }
                    vm.setIpAddress(resp.getIpAddress());
                    vm.setAccountName(resp.getAccountName());
                    vm.setDomainId(resp.getDomainId());
                    vm.setHypervisor( mapToAmazonHypervisorType(resp.getHypervisor()) );
                    vm.setServiceOffering( svcOffering.getName());
                    vm.setKeyPairName(resp.getKeyPairName());
                    instances.addInstance(vm);
                    countCreated++;
                }catch(Exception e){
                    logger.error("Failed to deploy VM number: "+ (i+1) +" due to error: "+e.getMessage());
                    break;
                }
            }
            if (0 == countCreated) {
                // TODO, we actually need to destroy left-over VMs when the exception is thrown
                throw new Exception("Insufficient Instance Capacity");
            }
            logger.debug("Could deploy "+ countCreated + " VM's successfully");
        } catch( Exception e ) {
            logger.error( "EC2 RunInstances - ", e);
            handleException(e);
        }
        return instances;
    }

    /**
     * Start an instance or instances
     * 
     * @param request
     * @return
     */
    public EC2StartInstancesResponse startInstances(EC2StartInstances request) {
        EC2StartInstancesResponse instances = new EC2StartInstancesResponse();
        EC2Instance[] vms = null;

        // -> first determine the current state of each VM (becomes it previous state)
        try {
            EC2DescribeInstancesResponse previousState = listVirtualMachines( request.getInstancesSet(), null, null );
            vms = previousState.getInstanceSet();

            // -> send start requests for each item
            for (EC2Instance vm : vms) {
                vm.setPreviousState(vm.getState());

                // -> if its already running then we don't care
                if (vm.getState().equalsIgnoreCase( "Running" ) || vm.getState().equalsIgnoreCase( "Destroyed" )) {
                    instances.addInstance(vm);
                    continue;
                }

                CloudStackUserVm resp = getApi().startVirtualMachine(vm.getId());
                if(resp != null){
                    vm.setState(resp.getState());
                    if(logger.isDebugEnabled())
                        logger.debug("Starting VM " + vm.getId() + " job " + resp.getJobId());
                }
                instances.addInstance(vm);
            }
        } catch( Exception e ) {
            logger.error( "EC2 StartInstances - ", e);
            handleException(e);
        }
        return instances;
    }

    /**
     * Stop an instance or instances
     * 
     * @param request
     * @return
     */
    public EC2StopInstancesResponse stopInstances(EC2StopInstances request) {
        EC2StopInstancesResponse instances = new EC2StopInstancesResponse();
        EC2Instance[] virtualMachines = null;

        // -> first determine the current state of each VM (becomes it previous state)
        try {
            String[] instanceSet = request.getInstancesSet();
            Boolean forced = request.getForce();

            EC2DescribeInstancesResponse previousState = listVirtualMachines( instanceSet, null, null );
            virtualMachines = previousState.getInstanceSet();

            // -> send stop requests for each item
            for (EC2Instance vm : virtualMachines) {
                vm.setPreviousState( vm.getState());
                CloudStackUserVm resp = null;
                if (request.getDestroyInstances()) {
                    if (vm.getState().equalsIgnoreCase( "Destroyed" )) {
                        instances.addInstance(vm);
                        continue;
                    }
                    resp = getApi().destroyVirtualMachine(vm.getId());
                    if(logger.isDebugEnabled())
                        logger.debug("Destroying VM " + vm.getId() + " job " + resp.getJobId());
                } else {
                    if (vm.getState().equalsIgnoreCase("Stopped") || vm.getState().equalsIgnoreCase("Destroyed")) {
                        instances.addInstance(vm);
                        continue;
                    }
                    resp = getApi().stopVirtualMachine(vm.getId(), forced);
                    if(logger.isDebugEnabled())
                        logger.debug("Stopping VM " + vm.getId() + " job " + resp.getJobId());
                }
                if (resp != null) {
                    vm.setState(resp.getState());
                    instances.addInstance(vm);
                }
            }
        } catch( Exception e ) {
            logger.error( "EC2 StopInstances - ", e);
            handleException(e);
        }
        return instances;
    }

    /**
     * @param request
     * @return
     * @throws Exception
     */
    public boolean modifyInstanceAttribute(EC2ModifyInstanceAttribute request) {
        boolean status = true;
        String instanceId = request.getInstanceId();
        try {
            // AWS requires VM to be in stopped state to modify 'InstanceType' and 'UserData'
            EC2DescribeInstancesResponse vmResponse = new EC2DescribeInstancesResponse();
            vmResponse = lookupInstances(instanceId, vmResponse, null);
            EC2Instance[] instances = vmResponse.getInstanceSet();
            if ( !instances[0].getState().equalsIgnoreCase("stopped")) {
                throw new Exception("Cannot modify, instance should be in stopped state");
            }

            if (request.getInstanceType() != null) {
                String instanceType = request.getInstanceType();
                CloudStackServiceOfferingVO svcOffering = getCSServiceOfferingId(instanceType);
                if (svcOffering == null)
                    throw new Exception("instanceType not found");
                CloudStackUserVm userVm = getApi().changeServiceForVirtualMachine(instanceId, svcOffering.getId());
                status = (userVm != null);
            }
            if (status != false && request.getUserData() != null) {
                CloudStackUserVm userVm = getApi().updateVirtualMachine(instanceId, null, null, null,
                        null, request.getUserData());
                status = (userVm != null);
            }
        } catch (Exception e) {
            logger.error("ModifyInstanceAttribute - ", e);
            handleException(e);
        }
        return status;
    }

    /**
     * RunInstances includes a min and max count of requested instances to create.
     * We have to be able to create the min number for the user or none at all.  So
     * here we determine what the user has left to create.
     * 
     * @return -1 means no limit exists, other positive numbers give max number left that
     *         the user can create.
     */
    private int calculateAllowedInstances() throws Exception {
        int maxAllowed = -1;

        CloudStackAccount ourAccount = getCurrentAccount();

        if (ourAccount == null) {
            // This should never happen, but
            // we will return -99999 if this happens...
            return -99999;
        }

        // if accountType is Admin == 1, then let's return -1
        if (ourAccount.getAccountType() == 1) return -1;

        // -> get the user limits on instances
        // "0" represents instances:
        // http://download.cloud.com/releases/2.2.0/api_2.2.8/user/listResourceLimits.html
        List<CloudStackResourceLimit> limits = getApi().listResourceLimits(null, null, null, null, "0");
        if (limits != null && limits.size() > 0) {
            maxAllowed = (int)limits.get(0).getMax().longValue();
            if (maxAllowed == -1)
                return -1;   // no limit

            EC2DescribeInstancesResponse existingVMS = listVirtualMachines( null, null, null );
            EC2Instance[] vmsList = existingVMS.getInstanceSet();
            return (maxAllowed - vmsList.length);
        } else {
            return 0;
        }
    }

    /**
     * Performs the cloud API listVirtualMachines one or more times.
     * 
     * @param virtualMachineIds - an array of instances we are interested in getting information on
     * @param ifs - filter out unwanted instances
     */
    private EC2DescribeInstancesResponse listVirtualMachines( String[] virtualMachineIds, EC2InstanceFilterSet ifs,
            List<CloudStackKeyValue> resourceTags ) throws Exception
            {
        EC2DescribeInstancesResponse instances = new EC2DescribeInstancesResponse();

        if (null == virtualMachineIds || 0 == virtualMachineIds.length) {
            instances = lookupInstances( null, instances, resourceTags );
        } else {
            for( int i=0; i <  virtualMachineIds.length; i++ ) {
                instances = lookupInstances( virtualMachineIds[i], instances, resourceTags );
            }
        }

        if ( null == ifs )
            return instances;
        else return ifs.evaluate( instances );
            }

    /**
     * Get one or more templates depending on the volumeId parameter.
     * 
     * @param volumeId   - if interested in one specific volume, null if want to list all volumes
     * @param instanceId - if interested in volumes for a specific instance, null if instance is not important
     */
    private EC2DescribeVolumesResponse listVolumes(String volumeId, String instanceId, EC2DescribeVolumesResponse volumes,
            List<CloudStackKeyValue> resourceTagSet)throws Exception {

        List<CloudStackVolume> vols = getApi().listVolumes(null, null, null, volumeId, null, null, null, null, null,
                instanceId, null, resourceTagSet);
        if(vols != null && vols.size() > 0) {
            for(CloudStackVolume vol : vols) {
                EC2Volume ec2Vol = new EC2Volume();
                ec2Vol.setId(vol.getId());
                if(vol.getAttached() != null)
                    ec2Vol.setAttached(vol.getAttached());
                ec2Vol.setCreated(vol.getCreated());

                if(vol.getDeviceId() != null)
                    ec2Vol.setDeviceId(vol.getDeviceId());
                ec2Vol.setHypervisor(vol.getHypervisor());

                if(vol.getSnapshotId() != null)
                    ec2Vol.setSnapshotId(vol.getSnapshotId());
                ec2Vol.setState(mapToAmazonVolState(vol.getState()));
                ec2Vol.setSize(vol.getSize());
                ec2Vol.setType(vol.getVolumeType());

                if(vol.getVirtualMachineId() != null) {
                    ec2Vol.setInstanceId(vol.getVirtualMachineId());
                    if (vol.getVirtualMachineState() != null) {
                        ec2Vol.setVMState(vol.getVirtualMachineState());
                        ec2Vol.setAttachmentState(mapToAmazonVolumeAttachmentState(vol.getVirtualMachineState()));
                    }
                } else {
                    ec2Vol.setAttachmentState("detached");
                }

                ec2Vol.setZoneName(vol.getZoneName());

                List<CloudStackKeyValue> resourceTags = vol.getTags();
                for(CloudStackKeyValue resourceTag : resourceTags) {
                    EC2TagKeyValue param = new EC2TagKeyValue();
                    param.setKey(resourceTag.getKey());
                    if (resourceTag.getValue() != null)
                        param.setValue(resourceTag.getValue());
                    ec2Vol.addResourceTag(param);
                }

                volumes.addVolume(ec2Vol);
            }
        }

        return volumes;
    }

    /**
     * Translate the given zone name into the required zoneId.  Query for
     * a list of all zones and match the zone name given.   Amazon uses zone
     * names while the Cloud API often requires the zoneId.
     * 
     * @param zoneName - (e.g., 'AH'), if null return the first zone in the available list
     * 
     * @return the zoneId that matches the given zone name
     */
    private String toZoneId(String zoneName, String domainId) throws Exception	{
        EC2DescribeAvailabilityZonesResponse zones = null;
        String[] interestedZones = null;

        if ( null != zoneName) {
            interestedZones = new String[1];
            interestedZones[0] = zoneName;
        }else {
            CloudStackZone zone = findZone();
            if(zone != null){
                return zone.getId();
            }
        }

        zones = listZones(interestedZones, domainId);

        if (zones == null || zones.getAvailabilityZoneSet().length == 0)
            throw new Exception("Unknown zoneName value");

        EC2AvailabilityZone[] zoneSet = zones.getAvailabilityZoneSet();
        return zoneSet[0].getId();
    }


    /**
     * Convert from the Amazon instanceType strings to Cloud serviceOfferingId
     * 
     */

    private CloudStackServiceOfferingVO getCSServiceOfferingId(String instanceType) throws Exception {
        try {
            if (instanceType == null)
                instanceType = "m1.small"; // default value
            return scvoDao.getSvcOfferingByName(instanceType);
        } catch(Exception e) {
            logger.error( "Error while retrieving ServiceOffering information by name - ", e);
            throw new Exception("No ServiceOffering found to be defined by name");
        }
    }

    /**
     * Convert from the Cloud serviceOfferingId to the Amazon instanceType strings based
     * on the loaded map.
     * 
     * @param serviceOfferingId
     * @return A valid value for the Amazon defined instanceType
     * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
     */
    private String serviceOfferingIdToInstanceType( String serviceOfferingId ) throws Exception {
        try{

            CloudStackServiceOfferingVO offering =  scvoDao.getSvcOfferingById(serviceOfferingId);
            if(offering == null){
                logger.warn( "No instanceType match for serviceOfferingId: [" + serviceOfferingId + "]" );
                return "m1.small";
            }
            return offering.getName();
        }
        catch(Exception e) {
            logger.error( "Error while retrieving ServiceOffering information by id - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }
    }

    /**
     * Match the value in the 'description' field of the listOsTypes response to get
     * the osTypeId.
     * 
     * @param osTypeName
     * @return the Cloud.com API osTypeId
     */
    private String toOSTypeId( String osTypeName ) throws Exception {
        try {
            List<CloudStackOsType> osTypes = getApi().listOsTypes(null, null, null);
            for (CloudStackOsType osType : osTypes) {
                if (osType.getDescription().toLowerCase().indexOf(osTypeName.toLowerCase()) != -1)
                    return osType.getId();
            }
            return null;
        } catch(Exception e) {
            logger.error( "List OS Types - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }

    }

    /**
     * More than one place we need to access the defined list of zones.  If given a specific
     * list of zones of interest, then only values from those zones are returned.
     * 
     * @param interestedZones - can be null, should be a subset of all zones
     * 
     * @return EC2DescribeAvailabilityZonesResponse
     */
    private EC2DescribeAvailabilityZonesResponse listZones(String[] interestedZones, String domainId)
            throws Exception  {
        EC2DescribeAvailabilityZonesResponse zones = new EC2DescribeAvailabilityZonesResponse();

        List<CloudStackZone> cloudZones = getApi().listZones(true, domainId, null, null);
        if(cloudZones != null && cloudZones.size() > 0) {
            for(CloudStackZone cloudZone : cloudZones) {
                boolean matched = false;
                if (interestedZones.length > 0) {
                    for (String zoneName : interestedZones){
                        if (zoneName.equalsIgnoreCase( cloudZone.getName())) {
                            matched = true;
                            break;
                        }
                    }
                } else {
                    matched = true;
                }
                if (!matched) continue;
                EC2AvailabilityZone ec2Zone = new EC2AvailabilityZone();
                ec2Zone.setId(cloudZone.getId().toString());
                ec2Zone.setMessage(cloudZone.getAllocationState());
                ec2Zone.setName(cloudZone.getName());

                zones.addAvailabilityZone(ec2Zone);
            }
        }
        return zones;
    }


    /**
     * Get information on one or more virtual machines depending on the instanceId parameter.
     * 
     * @param instanceId - if null then return information on all existing instances, otherwise
     *                     just return information on the matching instance.
     * @param instances  - a container object to fill with one or more EC2Instance objects
     * 
     * @return the same object passed in as the "instances" parameter modified with one or more
     *         EC2Instance objects loaded.
     */
    private EC2DescribeInstancesResponse lookupInstances( String instanceId, EC2DescribeInstancesResponse instances,
            List<CloudStackKeyValue> resourceTagSet )
                    throws Exception {

        String instId = instanceId != null ? instanceId : null;
        List<CloudStackUserVm> vms = getApi().listVirtualMachines(null, null, true, null, null, null, null,
                instId, null, null, null, null, null, null, null, null, resourceTagSet);

        if(vms != null && vms.size() > 0) {
            for(CloudStackUserVm cloudVm : vms) {
                EC2Instance ec2Vm = new EC2Instance();

                ec2Vm.setId(cloudVm.getId().toString());
                ec2Vm.setName(cloudVm.getName());
                ec2Vm.setZoneName(cloudVm.getZoneName());
                ec2Vm.setTemplateId(cloudVm.getTemplateId().toString());
                ec2Vm.setGroup(cloudVm.getGroup());
                ec2Vm.setState(cloudVm.getState());
                ec2Vm.setCreated(cloudVm.getCreated());
                ec2Vm.setIpAddress(cloudVm.getIpAddress());
                ec2Vm.setAccountName(cloudVm.getAccountName());
                ec2Vm.setDomainId(cloudVm.getDomainId());
                ec2Vm.setHypervisor( mapToAmazonHypervisorType(cloudVm.getHypervisor()) );
                ec2Vm.setRootDeviceType(cloudVm.getRootDeviceType());
                ec2Vm.setRootDeviceId(cloudVm.getRootDeviceId());
                ec2Vm.setServiceOffering(serviceOfferingIdToInstanceType(cloudVm.getServiceOfferingId().toString()));
                ec2Vm.setKeyPairName(cloudVm.getKeyPairName());

                List<CloudStackNic> nics = cloudVm.getNics();
                for(CloudStackNic nic : nics) {
                    if(nic.getIsDefault()) {
                        ec2Vm.setPrivateIpAddress(nic.getIpaddress());
                        break;
                    }
                }

                List<CloudStackKeyValue> resourceTags = cloudVm.getTags();
                for(CloudStackKeyValue resourceTag : resourceTags) {
                    EC2TagKeyValue param = new EC2TagKeyValue();
                    param.setKey(resourceTag.getKey());
                    if (resourceTag.getValue() != null)
                        param.setValue(resourceTag.getValue());
                    ec2Vm.addResourceTag(param);
                }

                if (cloudVm.getSecurityGroupList() != null && cloudVm.getSecurityGroupList().size() > 0) {
                    List<CloudStackSecurityGroup> securityGroupList = cloudVm.getSecurityGroupList();
                    for (CloudStackSecurityGroup securityGroup : securityGroupList) {
                        EC2SecurityGroup param = new EC2SecurityGroup();
                        param.setId(securityGroup.getId());
                        param.setName(securityGroup.getName());
                        ec2Vm.addGroupName(param);
                    }
                }

                instances.addInstance(ec2Vm);
            }
        }else{
            if(instanceId != null){
                //no such instance found
                throw new Exception("Instance not found");
            }
        }
        return instances;
    }


    /**
     * Get one or more templates depending on the templateId parameter.
     * 
     * @param templateId - if null then return information on all existing templates, otherwise
     *                     just return information on the matching template.
     * @param images     - a container object to fill with one or more EC2Image objects
     * 
     * @return the same object passed in as the "images" parameter modified with one or more
     *         EC2Image objects loaded.
     */
    private EC2DescribeImagesResponse listTemplates( String templateId, EC2DescribeImagesResponse images ) throws Exception {
        try {
            List<CloudStackTemplate> result = new ArrayList<CloudStackTemplate>();

            if(templateId != null){
                List<CloudStackTemplate> template = getApi().listTemplates("executable", null, null, null, templateId , null, null, null);
                if(template != null){
                    result.addAll(template);
                }
            }else{
                List<CloudStackTemplate> selfExecutable = getApi().listTemplates("selfexecutable", null, null, null, null, null, null, null);
                if(selfExecutable != null){
                    result.addAll(selfExecutable);
                }

                List<CloudStackTemplate> featured = getApi().listTemplates("featured", null, null, null, null, null, null, null);
                if(featured != null){
                    result.addAll(featured);
                }

                List<CloudStackTemplate> sharedExecutable = getApi().listTemplates("sharedexecutable", null, null, null, null, null, null, null);
                if(sharedExecutable != null){
                    result.addAll(sharedExecutable);
                }

                List<CloudStackTemplate> community = getApi().listTemplates("community", null, null, null, null, null, null, null);
                if(community != null){
                    result.addAll(community);
                }
            }

            if (result != null && result.size() > 0) {
                for (CloudStackTemplate temp : result) {
                    EC2Image ec2Image = new EC2Image();
                    ec2Image.setId(temp.getId().toString());
                    ec2Image.setAccountName(temp.getAccount());
                    ec2Image.setName(temp.getName());
                    ec2Image.setDescription(temp.getDisplayText());
                    ec2Image.setOsTypeId(temp.getOsTypeId().toString());
                    ec2Image.setIsPublic(temp.getIsPublic());
                    ec2Image.setState( temp.getIsReady() ? "available" : "pending");
                    ec2Image.setDomainId(temp.getDomainId());
                    if ( temp.getHyperVisor().equalsIgnoreCase("xenserver"))
                        ec2Image.setHypervisor("xen");
                    else if ( temp.getHyperVisor().equalsIgnoreCase("ovm"))
                        ec2Image.setHypervisor( "ovm"); // valid values for hypervisor is 'ovm' and 'xen'
                    else
                        ec2Image.setHypervisor("");
                    if (temp.getDisplayText() == null)
                        ec2Image.setArchitecture("");
                    else if (temp.getDisplayText().indexOf( "x86_64" ) != -1)
                        ec2Image.setArchitecture("x86_64");
                    else if (temp.getDisplayText().indexOf( "i386" ) != -1)
                        ec2Image.setArchitecture("i386");
                    else
                        ec2Image.setArchitecture("");
                    List<CloudStackKeyValue> resourceTags = temp.getTags();
                    for(CloudStackKeyValue resourceTag : resourceTags) {
                        EC2TagKeyValue param = new EC2TagKeyValue();
                        param.setKey(resourceTag.getKey());
                        if (resourceTag.getValue() != null)
                            param.setValue(resourceTag.getValue());
                        ec2Image.addResourceTag(param);
                    }
                    images.addImage(ec2Image);
                }
            }
            return images;
        } catch(Exception e) {
            logger.error( "List Templates - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }
    }

    /**
     * List security groups
     * 
     * @param interestedGroups
     * @return
     * @throws EC2ServiceException
     * @throws UnsupportedEncodingException
     * @throws SignatureException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws ParseException
     */
    private EC2DescribeSecurityGroupsResponse listSecurityGroups( String[] interestedGroups ) throws Exception {
        try {
            EC2DescribeSecurityGroupsResponse groupSet = new EC2DescribeSecurityGroupsResponse();

            List<CloudStackSecurityGroup> groups = getApi().listSecurityGroups(null, null, null, true, null, null, null);
            if (groups != null && groups.size() > 0)
                for (CloudStackSecurityGroup group : groups) {
                    boolean matched = false;
                    if (interestedGroups.length > 0) {
                        for (String groupName :interestedGroups) {
                            if (groupName.equalsIgnoreCase(group.getName())) {
                                matched = true;
                                break;
                            }
                        }
                    } else {
                        matched = true;
                    }
                    if (!matched) continue;
                    EC2SecurityGroup ec2Group = new EC2SecurityGroup();
                    // not sure if we should set both account and account name to accountname
                    ec2Group.setAccount(group.getAccountName());
                    ec2Group.setAccountName(group.getAccountName());
                    ec2Group.setName(group.getName());
                    ec2Group.setDescription(group.getDescription());
                    ec2Group.setDomainId(group.getDomainId());
                    ec2Group.setId(group.getId().toString());
                    toPermission(ec2Group, group);
                    List<CloudStackKeyValue> resourceTags = group.getTags();
                    for(CloudStackKeyValue resourceTag : resourceTags) {
                        EC2TagKeyValue param = new EC2TagKeyValue();
                             param.setKey(resourceTag.getKey());
                        if (resourceTag.getValue() != null)
                            param.setValue(resourceTag.getValue());
                        ec2Group.addResourceTag(param);
                    }
                    groupSet.addGroup(ec2Group);
                }
            return groupSet;
        } catch(Exception e) {
            logger.error( "List Security Groups - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }
    }

    private EC2DescribeKeyPairsResponse listKeyPairs( String[] keyNames ) throws Exception {
        try {
            EC2DescribeKeyPairsResponse keyPairSet = new EC2DescribeKeyPairsResponse();

            List<CloudStackKeyPair> keyPairs = getApi().listSSHKeyPairs(null, null, null);
            if (keyPairs != null && keyPairs.size() > 0) {
                for (CloudStackKeyPair keyPair : keyPairs) {
                    boolean matched = false;
                    if (keyNames.length > 0) {
                        for (String keyName : keyNames) {
                            if (keyName.equalsIgnoreCase(keyPair.getName())) {
                                matched = true;
                                break;
                            }
                        }
                    } else matched = true;
                    if (!matched) continue;
                    EC2SSHKeyPair ec2KeyPair = new EC2SSHKeyPair();
                    ec2KeyPair.setFingerprint(keyPair.getFingerprint());
                    ec2KeyPair.setKeyName(keyPair.getName());
                    ec2KeyPair.setPrivateKey(keyPair.getPrivatekey());

                    keyPairSet.addKeyPair(ec2KeyPair);
                }
            }
            return keyPairSet;
        } catch(Exception e) {
            logger.error( "List Keypairs - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }
    }

    private EC2DescribeAddressesResponse listAddresses(String[] addressNames) throws Exception {
        try {
            EC2DescribeAddressesResponse addressSet = new EC2DescribeAddressesResponse();

            List<CloudStackIpAddress> addresses = getApi().listPublicIpAddresses(null, null, null, null, null, null, null, null, null);
            if (addresses != null && addresses.size() > 0) {
                for (CloudStackIpAddress address : addresses) {
                    boolean matched = false;
                    if ( addressNames.length > 0) {
                        for (String addressName : addressNames) {
                            if (address.getIpAddress().equalsIgnoreCase(addressName)) {
                                matched = true;
                                break;
                            }
                        }
                    } else matched = true;

                    if (!matched) continue ;

                    EC2Address ec2Address = new EC2Address();
                    ec2Address.setIpAddress(address.getIpAddress());
                    if (address.getVirtualMachineId() != null)
                        ec2Address.setAssociatedInstanceId(address.getVirtualMachineId().toString());
                    addressSet.addAddress(ec2Address);
                }
            }
            return addressSet;
        } catch(Exception e) {
            logger.error( "List Addresses - ", e);
            throw new Exception(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private EC2DescribeSnapshotsResponse listSnapshots( String[] snapshotIds, List<CloudStackKeyValue> resourceTagSet) throws Exception {
        try {
            EC2DescribeSnapshotsResponse snapshotSet = new EC2DescribeSnapshotsResponse();

            List<CloudStackSnapshot> snapshots = getApi().listSnapshots(null, null, null, null, null, null, null, null, null, resourceTagSet);
            if ( snapshots != null && snapshots.size() > 0) {
                for ( CloudStackSnapshot snapshot : snapshots) {
                    boolean matched = false;
                    if ( snapshotIds.length > 0) {
                        for ( String snapshotId : snapshotIds) {
                            if (snapshot.getId().equalsIgnoreCase(snapshotId)) {
                                matched = true;
                                break;
                            }
                        }
                    } else matched = true;

                    if (!matched) continue ;

                    EC2Snapshot ec2Snapshot = new EC2Snapshot();
                    ec2Snapshot.setId(snapshot.getId());
                    ec2Snapshot.setName(snapshot.getName());
                    ec2Snapshot.setVolumeId(snapshot.getVolumeId());
                    ec2Snapshot.setType(snapshot.getSnapshotType());
                    ec2Snapshot.setState(snapshot.getState());
                    ec2Snapshot.setCreated(snapshot.getCreated());
                    ec2Snapshot.setAccountName(snapshot.getAccountName());
                    ec2Snapshot.setDomainId(snapshot.getDomainId());

                    List<CloudStackKeyValue> resourceTags = snapshot.getTags();
                    for( CloudStackKeyValue resourceTag : resourceTags) {
                        EC2TagKeyValue param = new EC2TagKeyValue();
                        param.setKey(resourceTag.getKey());
                        if ( resourceTag.getValue() != null)
                            param.setValue(resourceTag.getValue());
                        ec2Snapshot.addResourceTag(param);
                    }
                    snapshotSet.addSnapshot(ec2Snapshot);
                }
            }
            return snapshotSet;
        } catch(Exception e) {
            logger.error( "List Snapshots - ", e);
            throw new Exception(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    /**
     * Convert ingress rule to EC2IpPermission records
     * 
     * @param response
     * @param group
     * @return
     */
    private boolean toPermission(EC2SecurityGroup response, CloudStackSecurityGroup group ) {
        List<CloudStackIngressRule> rules = group.getIngressRules();

        if (rules == null || rules.isEmpty()) return false;

        for (CloudStackIngressRule rule : rules) {
            EC2IpPermission perm = new EC2IpPermission();
            perm.setProtocol(rule.getProtocol());
            perm.setFromPort(rule.getStartPort());
            perm.setToPort(rule.getEndPort());
            perm.setRuleId(rule.getRuleId() != null ? rule.getRuleId().toString() : new String());
            perm.setIcmpCode(rule.getIcmpCode() != null ? rule.getIcmpCode().toString() : new String());
            perm.setIcmpType(rule.getIcmpType() != null ? rule.getIcmpType().toString() : new String());
            perm.setCIDR(rule.getCidr());
            perm.addIpRange(rule.getCidr());

            if (rule.getAccountName() != null && rule.getSecurityGroupName() != null) {
                EC2SecurityGroup newGroup = new EC2SecurityGroup();
                newGroup.setAccount(rule.getAccountName());
                newGroup.setName(rule.getSecurityGroupName());
                perm.addUser(newGroup);
            }
            response.addIpPermission(perm);
        }
        return true;
    }

    /**
     * Find the current account based on the SecretKey
     * 
     * @return
     * @throws Exception
     */
    public CloudStackAccount getCurrentAccount() throws Exception {
        if (currentAccount != null) {
            // verify this is the same account!!!
            for (CloudStackUser user : currentAccount.getUser()) {
                if (user.getSecretkey() != null && user.getSecretkey().equalsIgnoreCase(UserContext.current().getSecretKey())) {
                    return currentAccount;
                }
            }
        }
        // otherwise let's find this user/account
        List<CloudStackAccount> accounts = getApi().listAccounts(null, null, null, null, null, null, null, null);
        for (CloudStackAccount account : accounts) {
            CloudStackUser[] users = account.getUser();
            for (CloudStackUser user : users) {
                String userSecretKey = user.getSecretkey();
                if (userSecretKey != null && userSecretKey.equalsIgnoreCase(UserContext.current().getSecretKey())) {
                    currentAccount = account;
                    return account;
                }
            }
        }
        // if we get here, there is something wrong...
        return null;
    }

    /**
     * List networkOfferings by zone with securityGroup enabled
     * 
     * @param zoneId
     * @return
     * @throws Exception
     */
    private CloudStackNetwork getNetworksWithSecurityGroupEnabled(String zoneId) throws Exception {
        List<CloudStackNetwork> networks = getApi().listNetworks(null, null, null, null, null, null, null, null, null, zoneId);
        List<CloudStackNetwork> netWithSecGroup = new ArrayList<CloudStackNetwork>();
        for (CloudStackNetwork network : networks ) {
            if (!network.getNetworkOfferingAvailability().equalsIgnoreCase("unavailable") && network.getSecurityGroupEnabled())
                netWithSecGroup.add(network);
        }
        // we'll take the first one
        return netWithSecGroup.get(0);
    }

    /**
     * Create a network
     * 
     * @param zoneId
     * @param offering
     * @param owner
     * @return
     * @throws Exception
     */
    private CloudStackNetwork createDefaultGuestNetwork(String zoneId, CloudStackNetworkOffering offering, CloudStackAccount owner) throws Exception {
        return getApi().createNetwork(owner.getName() + "-network", owner.getName() + "-network",  offering.getId(), zoneId, owner.getName(),
                owner.getDomainId(), true, null, null, null, null, null, null, null, null);
    }

    /**
     * List of networks without securityGroup enabled by zone
     * 
     * @param zoneId
     * @return
     * @throws Exception
     */
    private CloudStackNetwork getNetworksWithoutSecurityGroupEnabled(String zoneId) throws Exception {
        // grab current account
        CloudStackAccount caller = getCurrentAccount();

        //check if account has any networks in the system
        List<CloudStackNetwork> networks = getApi().listNetworks(caller.getName(), caller.getDomainId(), null, true, null, null, null, null, null, zoneId);

        //listRequired offerings in the system - the network created from this offering has to be specified in deployVm command
        List<CloudStackNetworkOffering> reuquiredOfferings = getApi().listNetworkOfferings("Required", null, null, null, true,  null, null, null, null, null, zoneId);
        if (reuquiredOfferings != null && !reuquiredOfferings.isEmpty()) {
            if (networks != null && !networks.isEmpty()) {
                //pick up the first required network from the network list
                for (CloudStackNetwork network : networks)  {
                    for (CloudStackNetworkOffering requiredOffering : reuquiredOfferings) {
                        logger.debug("[reqd/virtual} offering: " + requiredOffering.getId() + " network " + network.getNetworkOfferingId());
                        if (network.getNetworkOfferingId().equals(requiredOffering.getId())) {
                            return network;
                        }
                    }
                }
            } else {
                //create new network and return it
                return createDefaultGuestNetwork(zoneId, reuquiredOfferings.get(0), caller);
            }
        } else {
            //find all optional network offerings in the system
            List<CloudStackNetworkOffering> optionalOfferings = getApi().listNetworkOfferings("Optional", null, null, null, true, null, null, null, null, null, zoneId);
            if (optionalOfferings != null && !optionalOfferings.isEmpty()) {
                if (networks != null && !networks.isEmpty()) {
                    for (CloudStackNetwork network : networks) {
                        for (CloudStackNetworkOffering optionalOffering : optionalOfferings) {
                            logger.debug("[optional] offering: " + optionalOffering.getId() + " network " + network.getNetworkOfferingId());
                            if (network.getNetworkOfferingId().equals(optionalOffering.getId())) {
                                return network;
                            }
                        }
                    }
                }
            }
        }

        // if we get this far and haven't returned already return an error
        throw new Exception("Unable to find an appropriate network for account ");
    }

    /**
     * Find a suitable network to use for deployVM
     * 
     * @param zone
     * @return
     * @throws Exception
     */
    private CloudStackNetwork findNetwork(CloudStackZone zone) throws Exception {
        if (zone == null) return null;

        // for basic networking, we don't specify a networkid for deployvm
        if (zone.getNetworkType().equalsIgnoreCase("basic")) return null;

        if (zone.getSecurityGroupsEnabled()) {
            // find system security group enabled network
            return getNetworksWithSecurityGroupEnabled(zone.getId());

        } else {
            return getNetworksWithoutSecurityGroupEnabled(zone.getId());
        }
    }

    private CloudStackZone findZone() throws Exception {
        CloudStackAccount caller = getCurrentAccount();
        List<CloudStackZone> cloudZones;

        String defaultZoneId = getDefaultZoneId(caller.getId());
        if (defaultZoneId != null) {
            cloudZones = getApi().listZones(true, null, defaultZoneId, null);
        } else {
            // caller.getDomainId doesn't work in user mode
            // List<CloudStackZone> cloudZones = getApi().listZones(true, caller.getDomainId(), null, null);
            cloudZones = getApi().listZones(true, null, null, null);
        }
        if (cloudZones != null && cloudZones.size() > 0) {
            return cloudZones.get(0);
        }
        return null;
    }

    /**
     * Finds the defaultZone marked for the account
     */
    private String getDefaultZoneId(String accountId) throws Exception {
        try {
            return accDao.getDefaultZoneId(accountId);
        } catch(Exception e) {
            logger.error( "Error while retrieving Account information by id - ", e);
            throw new Exception("Unable to retrieve account account information");
        }
    }

    /**
     * Windows has its own device strings.
     * 
     * @param hypervisor
     * @param deviceId
     * @return
     */
    public String cloudDeviceIdToDevicePath( String hypervisor, String deviceId )
    {
        Integer devId = new Integer(deviceId);
        if (null != hypervisor && hypervisor.toLowerCase().contains( "windows" )) {
            switch( devId ) {
            case 1:  return "xvdb";
            case 2:  return "xvdc";
            case 3:  return "xvdd";
            case 4:  return "xvde";
            case 5:  return "xvdf";
            case 6:  return "xvdg";
            case 7:  return "xvdh";
            case 8:  return "xvdi";
            case 9:  return "xvdj";
            default: return new String( "" + deviceId );
            }
        } else {    // -> assume its unix
            switch( devId ) {
            case 1:  return "/dev/sdb";
            case 2:  return "/dev/sdc";
            case 3:  return "/dev/sdd";
            case 4:  return "/dev/sde";
            case 5:  return "/dev/sdf";
            case 6:  return "/dev/sdg";
            case 7:  return "/dev/sdh";
            case 8:  return "/dev/sdi";
            case 9:  return "/dev/sdj";
            default: return new String( "" + deviceId );
            }
        }
    }


    /**
     * Translate the device name string into a Cloud Stack deviceId.
     * deviceId 3 is reserved for CDROM and 0 for the ROOT disk
     * 
     * @param device string
     * @return deviceId value
     */
    private String mapDeviceToCloudDeviceId( String device ) throws Exception
    {
        if (device.equalsIgnoreCase( "/dev/sdb"  )) return "1";
        else if (device.equalsIgnoreCase( "/dev/sdc"  )) return "2";
        else if (device.equalsIgnoreCase( "/dev/sde"  )) return "4";
        else if (device.equalsIgnoreCase( "/dev/sdf"  )) return "5";
        else if (device.equalsIgnoreCase( "/dev/sdg"  )) return "6";
        else if (device.equalsIgnoreCase( "/dev/sdh"  )) return "7";
        else if (device.equalsIgnoreCase( "/dev/sdi"  )) return "8";
        else if (device.equalsIgnoreCase( "/dev/sdj"  )) return "9";

        else if (device.equalsIgnoreCase( "/dev/xvdb" )) return "1";
        else if (device.equalsIgnoreCase( "/dev/xvdc" )) return "2";
        else if (device.equalsIgnoreCase( "/dev/xvde" )) return "4";
        else if (device.equalsIgnoreCase( "/dev/xvdf" )) return "5";
        else if (device.equalsIgnoreCase( "/dev/xvdg" )) return "6";
        else if (device.equalsIgnoreCase( "/dev/xvdh" )) return "7";
        else if (device.equalsIgnoreCase( "/dev/xvdi" )) return "8";
        else if (device.equalsIgnoreCase( "/dev/xvdj" )) return "9";

        else if (device.equalsIgnoreCase( "xvdb"      )) return "1";
        else if (device.equalsIgnoreCase( "xvdc"      )) return "2";
        else if (device.equalsIgnoreCase( "xvde"      )) return "4";
        else if (device.equalsIgnoreCase( "xvdf"      )) return "5";
        else if (device.equalsIgnoreCase( "xvdg"      )) return "6";
        else if (device.equalsIgnoreCase( "xvdh"      )) return "7";
        else if (device.equalsIgnoreCase( "xvdi"      )) return "8";
        else if (device.equalsIgnoreCase( "xvdj"      )) return "9";

        else throw new Exception("Device is not supported");
    }

    /**
     * Map CloudStack instance state to Amazon state strings
     * 
     * @param state
     * @return
     */
    private String mapToAmazonVolState( String state )
    {
        if (state.equalsIgnoreCase( "Allocated" ) ||
                state.equalsIgnoreCase( "Creating"  ) ||
                state.equalsIgnoreCase( "Ready"     )) return "available";

        if (state.equalsIgnoreCase( "Destroy"   )) return "deleting";

        return "error";
    }

    /**
     * Map CloudStack VM state to Amazon volume attachment state
     *
     * @param CloudStack VM state
     * @return Amazon Volume attachment state
     */
    private String mapToAmazonVolumeAttachmentState (String vmState) {
        if ( vmState.equalsIgnoreCase("Running") || vmState.equalsIgnoreCase("Stopping") ||
                vmState.equalsIgnoreCase("Stopped") ) {
            return "attached";
        }
        else if (vmState.equalsIgnoreCase("Starting")) {
            return "attaching";
        }
        else { // VM state is 'destroyed' or 'error' or other
            return "detached";
        }
    }

    /**
     * Map Amazon resourceType to CloudStack resourceType
     *
     * @param Amazon resourceType
     * @return CloudStack resourceType
     */
    private String mapToCloudStackResourceType( String resourceType) {
        if (resourceType.equalsIgnoreCase("image"))
            return("template");
        else if(resourceType.equalsIgnoreCase("instance"))
            return("userVm");
        else if (resourceType.equalsIgnoreCase("security-group"))
            return("securityGroup");
        else
            return resourceType;
    }

    /**
     * Map Amazon resourceType to CloudStack resourceType
     *
     * @param CloudStack resourceType
     * @return Amazon resourceType
     */
    private String mapToAmazonResourceType( String resourceType) {
        if (resourceType.equalsIgnoreCase("template"))
            return("image");
        else if(resourceType.equalsIgnoreCase("userVm"))
            return("instance");
        else if(resourceType.equalsIgnoreCase("securityGroup"))
            return("security-group");
        else
            return (resourceType.toLowerCase());
    }

    /**
     * Map CloudStack hypervisor to CloudStack hypervisor
     *
     * @param CloudStack hypervisor
     * @return Amazon hypervisor
     */
    private String mapToAmazonHypervisorType( String hypervisor) {
        if (hypervisor.equalsIgnoreCase("Xenserver"))
            return("xen");
        else if(hypervisor.equalsIgnoreCase("Ovm"))
            return("ovm");
        else
            return ("");
    }

    /**
     * Stop an instance
     * Wait until one specific VM has stopped
     * 
     * @param instanceId
     * @return
     * @throws Exception
     */
    private boolean stopVirtualMachine( String instanceId) throws Exception {
        try {
            CloudStackUserVm resp = getApi().stopVirtualMachine(instanceId, false);
            if (logger.isDebugEnabled())
                logger.debug("Stopping VM " + instanceId );
            return resp != null;
        } catch(Exception e) {
            logger.error( "StopVirtualMachine - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }
    }

    /**
     * Start an existing stopped instance(VM)
     * 
     * @param instanceId
     * @return
     * @throws Exception
     */
    private boolean startVirtualMachine( String instanceId ) throws Exception {
        try {
            CloudStackUserVm resp = getApi().startVirtualMachine(instanceId);
            if (logger.isDebugEnabled())
                logger.debug("Starting VM " + instanceId );
            return resp != null;
        } catch(Exception e) {
            logger.error("StartVirtualMachine - ", e);
            throw new Exception( e.getMessage() != null ? e.getMessage() : e.toString() );
        }
    }

    /**
     * Cloud Stack API takes a comma separated list as a parameter.
     * 
     * @throws UnsupportedEncodingException
     */
    private String constructList( String[] elements ) throws UnsupportedEncodingException {
        if (null == elements || 0 == elements.length) return null;
        StringBuffer elementList = new StringBuffer();

        for( int i=0; i < elements.length; i++ ) {
            if (0 < i) elementList.append( "," );
            elementList.append( elements[i] );
        }
        return elementList.toString();
    }

    private List<CloudStackKeyValue> getResourceTags(EC2TagKeyValue[] tagKeyValueSet) {
        List<CloudStackKeyValue> resourceTags = new ArrayList<CloudStackKeyValue>();
        for (EC2TagKeyValue tagKeyValue : tagKeyValueSet) {
            CloudStackKeyValue resourceTag = new CloudStackKeyValue();
            resourceTag.setKeyValue(tagKeyValue.getKey(), tagKeyValue.getValue());
            resourceTags.add(resourceTag);
        }
        return resourceTags;
    }

    private void handleException( Exception e) {
        String[] error = e.getMessage().split("Error Code - ");
        String errorMessage = error[0];
        if (error.length == 2) { // error code has been supplied
            int errorCode = Integer.parseInt(error[1]);
            if (errorCode == 431) {
                if ( errorMessage.contains("Object vm_instance(uuid:") && errorMessage.contains(") does not exist") ) {
                    throw new EC2ServiceException( ClientError.InvalidInstanceID_NotFound,
                            "Specified Instance ID does not exist");
                } else if ( errorMessage.contains("Unable to find security group by name") ||
                        errorMessage.contains("Unable to find security group") ||
                        ( errorMessage.contains("Object security_group(uuid:") && errorMessage.contains(") does not exist") ) ||
                        errorMessage.contains("Unable to find group by name ") ) {
                    throw new EC2ServiceException( ClientError.InvalidGroup_NotFound,
                            "Specified Security Group does not exist");
                } else if ( errorMessage.contains("Invalid port numbers") ) {
                    throw new EC2ServiceException( ClientError.InvalidPermission_Malformed,
                            "Specified Port value is invalid");
                } else if (errorMessage.contains("Nonexistent account") && errorMessage.contains("when trying to authorize security group rule for security group") ) {
                        throw new EC2ServiceException( ClientError.InvalidPermission_Malformed,
                                "Specified account doesn't exist");
                } else if ( errorMessage.contains("Nonexistent group") && errorMessage.contains("unable to authorize security group rule") ) {
                    throw new EC2ServiceException( ClientError.InvalidPermission_Malformed,
                            "Specified source security group doesn't exist");
                } else if ( errorMessage.contains("Invalid protocol") ) {
                    throw new EC2ServiceException( ClientError.InvalidPermission_Malformed,
                            "Specified protocol is invalid");
                } else if ( errorMessage.contains("is an Invalid CIDR") ) {
                    throw new EC2ServiceException( ClientError.InvalidPermission_Malformed,
                            "Specified CIDR is invalid");
                 }else if ( errorMessage.contains("Nonexistent account") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Specified Account name is invalid");
                } else if ( errorMessage.contains("Object volumes(uuid:") && errorMessage.contains(") does not exist") ) {
                    throw new EC2ServiceException( ClientError.InvalidVolume_NotFound,
                            "Specified Volume ID doesn't exist");
                } else if ( errorMessage.contains("Object snapshots(uuid:") && errorMessage.contains(") does not exist") ) {
                    throw new EC2ServiceException( ClientError.InvalidSnapshot_NotFound,
                            "Specified Snapshot ID doesn't exist");
                } else if ( (errorMessage.contains("A key pair with name '") && errorMessage.contains("' does not exist")) ||
                            (errorMessage.contains("A key pair with name '") && errorMessage.contains("' was not found")) ) {
                    throw new EC2ServiceException( ClientError.InvalidKeyPair_NotFound,
                            "Specified Key pair name is invalid");
                } else if ( errorMessage.contains("A key pair with name '") && errorMessage.contains("' already exists") ) {
                    throw new EC2ServiceException( ClientError.InvalidKeyPair_Duplicate,
                            "Specified Key pair already exists");
                } else if ( errorMessage.contains("Unknown zoneName value") ) {
                    throw new EC2ServiceException( ClientError.InvalidZone_NotFound,
                            "Specified AvailabilityZone name is invalid");
                } else if ( errorMessage.contains("specify a volume that is not attached to any VM") ) {
                    throw new EC2ServiceException( ClientError.DependencyViolation,
                            "Specified Volume is attached to a VM");
                } else if ( errorMessage.contains("Object vm_template(uuid: ")&& errorMessage.contains(") does not exist") ) {
                    throw new EC2ServiceException( ClientError.InvalidAMIID_NotFound,
                            "Specified Image ID does not exist");
                } else if ( errorMessage.contains("unable to find template by id") ) {
                    throw new EC2ServiceException( ClientError.InvalidAMIID_NotFound,
                            "Specified Image ID does not exist");
                } else if ( errorMessage.contains("a group with name") && errorMessage.contains("already exists") ) {
                    throw new EC2ServiceException( ClientError.InvalidGroup_Duplicate,
                            "Specified Security Group already exists");
                } else if ( errorMessage.contains("specified volume is not attached to a VM") ) {
                    throw new EC2ServiceException( ClientError.IncorrectState,
                            "Specified volume is not in the correct state 'attached' for detachment");
                } else if ( errorMessage.contains("Snapshot with specified snapshotId is not in BackedUp state yet and can't be used for volume creation") ) {
                    throw new EC2ServiceException( ClientError.IncorrectState,
                            "Specified snapshot is not in the correct state 'completed' for volume creation");
                } else if ( errorMessage.contains("Can't delete snapshotshot 4 due to it is not in BackedUp Status") ) {
                    throw new EC2ServiceException( ClientError.IncorrectState,
                            "Specified snapshot is not in the correct state 'completed' for deletion");
                } else if ( errorMessage.contains("Public key is invalid") ) {
                    throw new EC2ServiceException( ClientError.InvalidKeyPair_Format,
                            "Format of the specified key is invalid");
                } else if ( errorMessage.contains("Invalid resource type") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Specified resourceId is invalid");
                } else if ( errorMessage.contains("Unable to find tags by parameters specified") ) {
                       throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Specified resourceTag for the specified resourceId doesn't exist");
                } else if ( errorMessage.contains("Failed to enable static nat for the ip address with specified ipId " +
                        "as vm with specified vmId is already associated with specified currentIp") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Specified publicIp is already associated to the specified VM");
                } else if ( errorMessage.contains("Specified IP address id is not associated with any vm Id") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Specified publicIp is not associated to any VM");
                } else if ( errorMessage.contains("specify a VM that is either running or stopped") ) {
                    throw new EC2ServiceException( ClientError.IncorrectInstanceState,
                            "Unable to attach. Specified instances is in an incorrect state");
                } else if ( errorMessage.contains("specify a valid data volume") ) {
                    throw new EC2ServiceException( ClientError.InvalidVolume_NotFound,
                            "Specified volume doen't exist");
                } else if ( errorMessage.contains("VolumeId is not in Ready state, but  in state Allocated. Cannot take snapshot") ) {
                    throw new EC2ServiceException( ClientError.IncorrectState,
                            "Cannot take snapshot. Specified volume is not in the correct state");
                } else if ( errorMessage.contains("Can't delete snapshot") && errorMessage.contains("it is not in BackedUp Status") ) {
                    throw new EC2ServiceException( ClientError.IncorrectState,
                            "Cannot delete snapshot. Specified snapshot is not in the correct state");
                } else if ( errorMessage.contains("Invalid port range") ) {
                    throw new EC2ServiceException( ClientError.InvalidPermission_Malformed,
                            "The specified port range is invalid");
                } else if ( errorMessage.contains("specify a valid User VM") ) {
                    throw new EC2ServiceException( ClientError.InvalidInstanceID_NotFound,
                            "Specified instance is invalid");
                } else if ( errorMessage.contains("No password for VM with specified id found") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "No password for VM with the specified id found");
                } else if ( errorMessage.contains("make sure the virtual machine is stopped and not in an error state before upgrading") ) {
                    throw new EC2ServiceException( ClientError.IncorrectInstanceState,
                            "Unable to modify. Specified instances is not in the correct state 'Stopped'");
                } else if ( errorMessage.contains("Not upgrading vm") && errorMessage.contains("it already has the" +
                        " requested service offering") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Unable to modify. Specified instance already has the requested instanceType");
                }
                // Can't enable static, ip address with specified id is a sourceNat ip address ?
                else {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "The value supplied for a parameter is invalid");
                }
            }
            else if (errorCode == 536) {
                if ( errorMessage.contains("Cannot delete group when it's in use by virtual machines") ) {
                       throw new EC2ServiceException( ClientError.InvalidGroup_InUse,
                            "Group is in use by a virtual machine");
                } else {
                       throw new EC2ServiceException( ClientError.DependencyViolation,
                            "Specified resource is in use");
                }
            }
            else if (errorCode == 531) {
                if ( errorMessage.contains("Acct") && errorMessage.contains("does not have permission to launch" +
                        " instances from Tmpl") ) {
                    throw new EC2ServiceException( ClientError.AuthFailure,
                            "User not authorized to operate on the specified AMI");
                } else {
                    throw new EC2ServiceException( ClientError.AuthFailure, "User not authorized");
                }
            }
            else if (errorCode == 530) {
               if ( errorMessage.contains("deviceId") && errorMessage.contains("is used by VM") ) {
                    throw new EC2ServiceException( ClientError.InvalidDevice_InUse,
                            "Specified Device is already being used by the VM");
                } else if (errorMessage.contains("Entity already exists") ){
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "Specified resource tag already exists");
                } else if ( errorMessage.contains("Template") && errorMessage.contains("has not been completely downloaded") ){
                    throw new EC2ServiceException( ClientError.InvalidAMIID_NotFound,
                            "Specified ImageId is unavailable");
                } else if ( errorMessage.contains("cannot stop VM") && errorMessage.contains("when it is in state Starting") ){
                    throw new EC2ServiceException( ClientError.IncorrectInstanceState,
                            "Unable to stop. One or more of the specified instances is in an incorrect state 'pending'");
                } else if ( errorMessage.contains("Failed to authorize security group ingress rule(s)") ) {
                    throw new EC2ServiceException(ClientError.InvalidParameterValue, "Specified Ip-permission is invalid" +
                            " or the Ip-permission already exists");
                } else if ( errorMessage.contains("Failed to reboot vm instance") ) {
                    throw new EC2ServiceException(ClientError.IncorrectInstanceState,
                            "Unable to reboot. One or more of the specified instances is in an incorrect state");
                } else if ( errorMessage.contains("specify a template that is not currently being downloaded") ) {
                    throw new EC2ServiceException(ClientError.IncorrectState,
                            "Unable to deregister. Image is not in the correct state 'available'");
                } else {
                    throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occured");
                }
            }
            else if (errorCode == 534) {
                if ( errorMessage.contains("Maximum number of resources of type 'volume' for account")
                        && errorMessage.contains("has been exceeded") ) {
                    throw new EC2ServiceException( ClientError.VolumeLimitExceeded,
                            "You have reached the limit on the number of volumes that can be created");
                } else if ( errorMessage.contains("Maximum number of resources of type 'public_ip' for account")
                        && errorMessage.contains("has been exceeded") ) {
                    throw new EC2ServiceException( ClientError.AddressLimitExceeded,
                            "You have reached the limit on the number of elastic ip addresses your account can have");
                } else if ( errorMessage.contains("Unable to apply save userdata entry on router") ) {
                    throw new EC2ServiceException( ClientError.InvalidParameterValue,
                            "The value supplied for parameter UserData is invalid");
                } else {
                    throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occured");
                }
            }
            else if (errorCode == 533) {
                if ( errorMessage.contains("Unable to create a deployment for VM") ) {
                    throw new EC2ServiceException( ClientError.InsufficientInstanceCapacity,
                            "There is insufficient capacity available to deploy a VM");
                } else if ( errorMessage.contains("Insufficient address capacity") ) {
                    throw new EC2ServiceException( ServerError.InsufficientAddressCapacity,
                            "Not enough available addresses to satisfy your minimum request");
                } else {
                    throw new EC2ServiceException( ServerError.InternalError, "There is insufficient capacity");
                }
            } else if (errorCode == 401) {
                if ( errorMessage.contains("Unauthorized") ) {
                    throw new EC2ServiceException(ClientError.AuthFailure, "User not authorised");
                } else {
                    throw new EC2ServiceException(ClientError.AuthFailure, "User not authorised");
                }
            } else {
               throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occured");
            }
        } else {
            if ( errorMessage.contains("Unknown zoneName value") ) {
                throw new EC2ServiceException( ClientError.InvalidZone_NotFound,
                        "AvailabilityZone name is invalid");
            } else if ( errorMessage.contains("No ServiceOffering found to be defined by name") ) {
                throw new EC2ServiceException(ClientError.InvalidParameterValue, "Specified InstanceType is invalid");
            } else if ( errorMessage.contains("Specified Ip permission is invalid") ) {
                throw new EC2ServiceException(ClientError.InvalidPermission_Malformed, "Specified Ip permission is invalid");
            } else if ( errorMessage.contains("One or more instanceIds do not exist, other instances rebooted") ) {
                throw new EC2ServiceException(ClientError.InvalidInstanceID_NotFound,
                        "One or more InstanceId doesn't exist, other instances rebooted");
            } else if ( errorMessage.contains("Device is not supported") ) {
                throw new EC2ServiceException(ClientError.InvalidParameterValue,
                        "Value specified for parameter Device is invalid");
            } else if ( errorMessage.contains("Volume is not attached to the Instance") ) {
                throw new EC2ServiceException(ClientError.InvalidAttachment_NotFound,
                        "Specified Volume is not attached to the specified Instance");
            } else if ( errorMessage.contains("Volume is not attached to the Device") ) {
                throw new EC2ServiceException(ClientError.InvalidAttachment_NotFound,
                        "Specified Volume is not attached to the specified device");
            } else if ( errorMessage.contains("Unable to create snapshot") ) {
                throw new EC2ServiceException(ServerError.InternalError,
                        "Unable to create snapshot");
            } else if ( errorMessage.contains("Instance must be in stopped state") ) {
                throw new EC2ServiceException(ClientError.IncorrectInstanceState,
                        "Specified instance is not in the correct state 'stopped'");
            } else if ( errorMessage.contains("Image couldn't be created") ) {
                throw new EC2ServiceException(ServerError.InternalError,
                        "Unable to create image");
            } else if ( errorMessage.contains("Failed to start the stopped instance") ) {
                throw new EC2ServiceException(ServerError.InternalError,
                        "Unable to start the instance that was stopped during image creation");
            } else if ( errorMessage.contains("One or more of instanceIds specified is in stopped state") ) {
                throw new EC2ServiceException(ClientError.IncorrectInstanceState,
                        "Unable to reboot. One or more of the specified instances is in an incorrect state 'stopped'");
            } else if ( errorMessage.contains("Specified ipAddress doesn't exist") ) {
                throw new EC2ServiceException(ClientError.InvalidParameterValue, "Specified publicIp doesn't exist");
            } else if ( errorMessage.contains("Min Count is greater than the number of instances left to allocate") ) {
                throw new EC2ServiceException(ClientError.InstanceLimitExceeded,
                        "Specified MinCount parameter is greater than the number of instances you can create");
            } else if ( errorMessage.contains("instanceType not found") ) {
                throw new EC2ServiceException(ClientError.InvalidParameterValue,
                        "Specified instanceType not found");
            } else if ( errorMessage.contains("zone not found") ) {
                throw new EC2ServiceException(ClientError.InvalidZone_NotFound,
                        "Specified zone doesn't exist");
            } else if ( errorMessage.contains("Both groupId and groupName has been specified") ) {
                throw new EC2ServiceException(ClientError.InvalidParameterCombination,
                        " for EC2 groups either a group ID or a group name is accepted");
            } else if ( errorMessage.contains("Insufficient Instance Capacity") ) {
                throw new EC2ServiceException(ServerError.InsufficientInstanceCapacity, "Insufficient Instance Capacity" );
            } else if ( errorMessage.contains("Unable to find security group name") ) {
                throw new EC2ServiceException(ClientError.InvalidGroup_NotFound, "Specified Security Group does not exist" );
            } else if ( errorMessage.contains("Instance not found") ) {
                throw new EC2ServiceException(ClientError.InvalidInstanceID_NotFound,
                        "One or more of the specified instanceId not found");
            } else if ( errorMessage.contains("Cannot modify, instance should be in stopped state") ) {
                throw new EC2ServiceException(ClientError.IncorrectInstanceState,
                        "Unable to modify instance attribute. Specified instance is not in the correct state 'stopped'");
            } else {
                throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occured");
            }
        }
    }
}
