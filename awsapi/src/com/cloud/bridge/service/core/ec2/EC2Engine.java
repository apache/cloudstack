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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.cloud.bridge.persist.dao.OfferingDao;
import com.cloud.bridge.service.UserContext;
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
import com.cloud.stack.models.CloudStackSecurityGroup;
import com.cloud.stack.models.CloudStackSecurityGroupIngress;
import com.cloud.stack.models.CloudStackServiceOffering;
import com.cloud.stack.models.CloudStackSnapshot;
import com.cloud.stack.models.CloudStackTemplate;
import com.cloud.stack.models.CloudStackUser;
import com.cloud.stack.models.CloudStackUserVm;
import com.cloud.stack.models.CloudStackVolume;
import com.cloud.stack.models.CloudStackZone;

/**
 * EC2Engine processes the ec2 commands and calls their cloudstack analogs
 *
 */
public class EC2Engine {
	protected final static Logger logger = Logger.getLogger(EC2Engine.class);
	String managementServer = null;
	String cloudAPIPort = null;

	private CloudStackApi _eng = null;
	
	private CloudStackAccount currentAccount = null;

	public EC2Engine() throws IOException {
		loadConfigValues();
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
			
			OfferingDao ofDao = new OfferingDao();
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * Creates a security group
	 * 
	 * @param groupName
	 * @param groupDesc
	 * @return
	 */
	public Boolean createSecurityGroup(String groupName, String groupDesc) {
		try {
			CloudStackSecurityGroup grp = getApi().createSecurityGroup(groupName, null, groupDesc, null);
			if (grp != null && grp.getId() != null) {
				return true;
			}
			return false;
		} catch( Exception e ) {
			logger.error( "EC2 CreateSecurityGroup - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * returns a list of security groups
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeSecurityGroupsResponse describeSecurityGroups(EC2DescribeSecurityGroups request) 
	{
		try {
			EC2DescribeSecurityGroupsResponse response = listSecurityGroups( request.getGroupSet());
			EC2GroupFilterSet gfs = request.getFilterSet();

			if ( null == gfs )
				return response;
			else return gfs.evaluate( response );     
		} catch( Exception e ) {
			logger.error( "EC2 DescribeSecurityGroups - ", e);
			throw new EC2ServiceException(ServerError.InternalError, "An unexpected error occurred.");
		}
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
		if (null == request.getName()) throw new EC2ServiceException(ServerError.InternalError, "Name is a required parameter");
		try {   
			String[] groupSet = new String[1];
			groupSet[0] = request.getName();
			String ruleId = null;
	
			EC2IpPermission[] items = request.getIpPermissionSet();

			EC2DescribeSecurityGroupsResponse response = listSecurityGroups( groupSet );
			EC2SecurityGroup[] groups = response.getGroupSet();

			for (EC2SecurityGroup group : groups) {
				EC2IpPermission[] perms = group.getIpPermissionSet();
				for (EC2IpPermission perm : perms) {
					ruleId = doesRuleMatch( items[0], perm );
				}
			}

			if (null == ruleId)
				throw new EC2ServiceException(ClientError.InvalidGroup_NotFound, "Cannot find matching ruleid.");

			CloudStackInfoResponse resp = getApi().revokeSecurityGroupIngress(ruleId);
			if (resp != null && resp.getId() != null) {
				return resp.getSuccess();
			}
			return false;
		} catch( Exception e ) {
			logger.error( "EC2 revokeSecurityGroupIngress" + " - " + e.getMessage());
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		} 	
	}

	/**
	 * authorizeSecurityGroup
	 * 
	 * @param request - ip permission parameters
	 */
	public boolean authorizeSecurityGroup(EC2AuthorizeRevokeSecurityGroup request ) 
	{
		if (null == request.getName()) throw new EC2ServiceException(ServerError.InternalError, "Name is a required parameter");

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
				CloudStackSecurityGroupIngress resp = null;
				if (ipPerm.getProtocol().equalsIgnoreCase("icmp")) {
					resp = getApi().authorizeSecurityGroupIngress(null, constructCIDRList(ipPerm.getIpRangeSet()), null, null, 
							ipPerm.getToPort().toString(), ipPerm.getFromPort().toString(), ipPerm.getProtocol(), null, 
							request.getName(), null, secGroupList);
				} else {
					resp = getApi().authorizeSecurityGroupIngress(null, constructCIDRList(ipPerm.getIpRangeSet()), null, 
							ipPerm.getToPort().longValue(), null, null, ipPerm.getProtocol(), null, request.getName(), 
							ipPerm.getFromPort().longValue(), secGroupList);
				}
				if (resp != null && resp.getRuleId() != null) {
					return true;
				}
				return false;
			}
		} catch(Exception e) {
			logger.error( "EC2 AuthorizeSecurityGroupIngress - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
	 * Cloud Stack API takes a comma separated list of IP ranges as one parameter.
	 * 
	 * @throws UnsupportedEncodingException 
	 */
	private String constructCIDRList( String[] ipRanges ) throws UnsupportedEncodingException 
	{
		if (null == ipRanges || 0 == ipRanges.length) return null;  	
		StringBuffer cidrList = new StringBuffer();

		for( int i=0; i < ipRanges.length; i++ ) {
			if (0 < i) cidrList.append( "," );
			cidrList.append( ipRanges[i] );
		}
		return cidrList.toString();
	}

	/**
	 * Returns a list of all snapshots
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeSnapshotsResponse handleRequest( EC2DescribeSnapshots request ) 
	{
		EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
		EC2SnapshotFilterSet sfs = request.getFilterSet();

		try { 
			// -> query to get the volume size for each snapshot
			EC2DescribeSnapshotsResponse response = listSnapshots( request.getSnapshotSet());
			if (response == null) {
				return new EC2DescribeSnapshotsResponse();
			}
			EC2Snapshot[] snapshots = response.getSnapshotSet();
			for (EC2Snapshot snap : snapshots) {
				volumes = listVolumes(snap.getVolumeId(), null, volumes);
				EC2Volume[] volSet = volumes.getVolumeSet();
				if (0 < volSet.length) snap.setVolumeSize(volSet[0].getSize());
				volumes.reset();
			}

			if ( null == sfs )
				return response;
			else return sfs.evaluate( response );
		} catch( EC2ServiceException error ) {
			logger.error( "EC2 DescribeSnapshots - ", error);
			throw error;

		} catch( Exception e ) {
			logger.error( "EC2 DescribeSnapshots - ", e);
			throw new EC2ServiceException(ServerError.InternalError, "An unexpected error occurred.");
		}
	}

	/**
	 * Creates a snapshot
	 * 
	 * @param volumeId
	 * @return
	 */
	public EC2Snapshot createSnapshot( String volumeId ) {
		try {
			
			CloudStackSnapshot snap = getApi().createSnapshot(volumeId, null, null, null);
			if (snap == null) {
				throw new EC2ServiceException(ServerError.InternalError, "Unable to create snapshot!");
			}
			EC2Snapshot ec2Snapshot = new EC2Snapshot();

			ec2Snapshot.setId(snap.getId());
			ec2Snapshot.setName(snap.getName());
			ec2Snapshot.setType(snap.getSnapshotType());
			ec2Snapshot.setAccountName(snap.getAccountName());
			ec2Snapshot.setDomainId(snap.getDomainId());
			ec2Snapshot.setCreated(snap.getCreated());
			ec2Snapshot.setVolumeId(snap.getVolumeId());
			
			List<CloudStackVolume> vols = getApi().listVolumes(null, null, null, snap.getVolumeId(), null, null, null, null, null, null, null);

			if(vols.size() > 0) {
				assert(vols.get(0).getSize() != null);
				Long sizeInGB = vols.get(0).getSize().longValue()/1073741824;
				ec2Snapshot.setVolumeSize(sizeInGB);
			}

			return ec2Snapshot;
		} catch( Exception e ) {
			logger.error( "EC2 CreateSnapshot - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * Modify an existing template
	 * 
	 * @param request
	 * @return
	 */
	public boolean modifyImageAttribute( EC2Image request ) 
	{
		// TODO: This is incomplete
		EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();

		try {
			images = listTemplates( request.getId(), images );
			EC2Image[] imageSet = images.getImageSet();
			
			CloudStackTemplate resp = getApi().updateTemplate(request.getId(), null, request.getDescription(), null, imageSet[0].getName(), null, null);
			if (resp != null) {
				return true;
			}
			return false;
		} catch( Exception e ) {
			logger.error( "EC2 ModifyImage - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * If given a specific list of snapshots of interest, then only values from those snapshots are returned.
	 * 
	 * @param interestedShots - can be null, should be a subset of all snapshots
	 */
	private EC2DescribeSnapshotsResponse listSnapshots( String[] interestedShots ) throws Exception {
		EC2DescribeSnapshotsResponse snapshots = new EC2DescribeSnapshotsResponse();

		List<CloudStackSnapshot> cloudSnaps;
		if (interestedShots == null || interestedShots.length == 0) {
			cloudSnaps = getApi().listSnapshots(null, null, null, null, null, null, null, null, null);
		} else {
			cloudSnaps = new ArrayList<CloudStackSnapshot>();

			for(String id : interestedShots) {
				List<CloudStackSnapshot> tmpList = getApi().listSnapshots(null, null, id, null, null, null, null, null, null);
				cloudSnaps.addAll(tmpList);
			}
		}

		if (cloudSnaps == null) { 
			return null;
		}

		for(CloudStackSnapshot cloudSnapshot : cloudSnaps) {
			EC2Snapshot shot  = new EC2Snapshot();
			shot.setId(cloudSnapshot.getId());
			shot.setName(cloudSnapshot.getName());
			shot.setVolumeId(cloudSnapshot.getVolumeId());
			shot.setType(cloudSnapshot.getSnapshotType());
			shot.setState(cloudSnapshot.getState());
			shot.setCreated(cloudSnapshot.getCreated());
			shot.setAccountName(cloudSnapshot.getAccountName());
			shot.setDomainId(cloudSnapshot.getDomainId());

			snapshots.addSnapshot(shot);
		}
		return snapshots;
	}


	// handlers
	/**
	 * return password data from the instance
	 * 
	 * @param instanceId
	 * @return
	 */
	public EC2PasswordData getPasswordData(String instanceId) {
		try {
			CloudStackPasswordData resp = getApi().getVMPassword(instanceId);
			EC2PasswordData passwdData = new EC2PasswordData();
			if (resp != null) {
				passwdData.setInstanceId(instanceId);
				passwdData.setEncryptedPassword(resp.getEncryptedpassword());
			}
			return passwdData;
		} catch(Exception e) {
			logger.error("EC2 GetPasswordData - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}
	/**
	 * Lists SSH KeyPairs on the systme
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeKeyPairsResponse describeKeyPairs( EC2DescribeKeyPairs request ) {
		try {
			EC2KeyPairFilterSet filterSet = request.getKeyFilterSet();
			String[] keyNames = request.getKeyNames();
			List<CloudStackKeyPair> keyPairs = getApi().listSSHKeyPairs(null, null, null);
			List<EC2SSHKeyPair> keyPairsList = new ArrayList<EC2SSHKeyPair>();
	
			if (keyPairs != null) {
				// Let's trim the list of keypairs to only the ones listed in keyNames
				if (keyNames != null && keyNames.length > 0) {
					for (CloudStackKeyPair keyPair : keyPairs) {
						boolean matched = false;
						for (String keyName : keyNames) {
							if (keyPair.getName().contains(keyName)) {
								matched = true;
								break;
							}
						}
						if (matched == false) {
							keyPairs.remove(keyPair);
						}
					}
				}
	
				if (keyPairs.isEmpty() == true) {
					throw new EC2ServiceException(ServerError.InternalError, "No keypairs left!");
				}
	
				// this should be reworked... converting from CloudStackKeyPairResponse to EC2SSHKeyPair is dumb
				for (CloudStackKeyPair respKeyPair: keyPairs) {
					EC2SSHKeyPair ec2KeyPair = new EC2SSHKeyPair();
					ec2KeyPair.setFingerprint(respKeyPair.getFingerprint());
					ec2KeyPair.setKeyName(respKeyPair.getName());
					ec2KeyPair.setPrivateKey(respKeyPair.getPrivatekey());
					keyPairsList.add(ec2KeyPair);
				}
			}
			return filterSet.evaluate(keyPairsList);
		} catch(Exception e) {
			logger.error("EC2 DescribeKeyPairs - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * Delete SSHKeyPair
	 * 
	 * @param request
	 * @return
	 */
	public boolean deleteKeyPair( EC2DeleteKeyPair request ) {
		try {
			CloudStackInfoResponse resp = getApi().deleteSSHKeyPair(request.getKeyName(), null, null);
			if (resp == null) { 
				throw new Exception("Ivalid CloudStack API response");
			}

			return resp.getSuccess();
		} catch(Exception e) {
			logger.error("EC2 DeleteKeyPair - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * Create SSHKeyPair
	 * 
	 * @param request
	 * @return
	 */
	public EC2SSHKeyPair createKeyPair(EC2CreateKeyPair request) {
		try {
			CloudStackKeyPair resp = getApi().createSSHKeyPair(request.getKeyName(), null, null);
			if (resp == null) {
				throw new Exception("Ivalid CloudStack API response");
			}

			EC2SSHKeyPair response = new EC2SSHKeyPair();
			response.setFingerprint(resp.getFingerprint());
			response.setKeyName(resp.getName());
			response.setPrivateKey(resp.getPrivatekey());

			return response;
		} catch (Exception e) {
			logger.error("EC2 CreateKeyPair - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * Import an existing SSH KeyPair
	 * 
	 * @param request
	 * @return
	 */
	public EC2SSHKeyPair importKeyPair( EC2ImportKeyPair request ) {
		try {
			CloudStackKeyPair resp = getApi().registerSSHKeyPair(request.getKeyName(), request.getPublicKeyMaterial());
			if (resp == null) {
				throw new Exception("Ivalid CloudStack API response");
			}

			EC2SSHKeyPair response = new EC2SSHKeyPair();
			response.setFingerprint(resp.getFingerprint());
			response.setKeyName(resp.getName());
			response.setPrivateKey(resp.getPrivatekey());

			return response;
		} catch (Exception e) {
			logger.error("EC2 ImportKeyPair - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * list ip addresses that have been allocated
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeAddressesResponse describeAddresses( EC2DescribeAddresses request ) {
		try {
			List<CloudStackIpAddress> addrList = getApi().listPublicIpAddresses(null, null, null, null, null, null, null, null, null);

			EC2AddressFilterSet filterSet = request.getFilterSet();
			List<EC2Address> addressList = new ArrayList<EC2Address>();
			if (addrList != null && addrList.size() > 0) {
				for (CloudStackIpAddress addr: addrList) {
					// remember, if no filters are set, request.inPublicIpSet always returns true
					if (request.inPublicIpSet(addr.getIpAddress())) {
						EC2Address ec2Address = new EC2Address();
						ec2Address.setIpAddress(addr.getIpAddress());
						if (addr.getVirtualMachineId() != null) 
							ec2Address.setAssociatedInstanceId(addr.getVirtualMachineId().toString());
						addressList.add(ec2Address);
					}
				}
			}

			return filterSet.evaluate(addressList);
		} catch(Exception e) {
			logger.error("EC2 DescribeAddresses - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
		}
	}

	/**
	 * release an IP Address
	 * 
	 * @param request
	 * @return
	 */
	public boolean releaseAddress(EC2ReleaseAddress request) {
		try {
			CloudStackIpAddress cloudIp = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null).get(0);
			CloudStackInfoResponse resp = getApi().disassociateIpAddress(cloudIp.getId());
			if (resp != null) {
				return resp.getSuccess();
			}
		} catch(Exception e) {
			logger.error("EC2 ReleaseAddress - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
			CloudStackIpAddress cloudIp = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null).get(0);
			CloudStackUserVm cloudVm = getApi().listVirtualMachines(null, null, null, null, null, null, request.getInstanceId(), null, null, null, null, null, null, null, null).get(0);

			CloudStackInfoResponse resp = getApi().enableStaticNat(cloudIp.getId(), cloudVm.getId());
			if (resp != null) {
				return resp.getSuccess();
			}
		} catch(Exception e) {
			logger.error( "EC2 AssociateAddress - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
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
			CloudStackIpAddress cloudIp = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null).get(0);
			CloudStackInfoResponse resp = getApi().disassociateIpAddress(cloudIp.getId());
			if (resp != null) {
				return resp.getSuccess();
			}
		} catch(Exception e) {
			logger.error( "EC2 DisassociateAddress - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
		return false;
	}

	/**
	 * Allocate an address
	 * 
	 * @param request
	 * @return
	 */
	public EC2Address allocateAddress()
	{
		try {
            EC2Address ec2Address = new EC2Address();
            // this gets our networkId
            CloudStackAccount caller = getCurrentAccount();
            
            CloudStackZone zone = findZone();
            CloudStackNetwork net = findNetwork(zone);
//			CloudStackIpAddress resp = getApi().associateIpAddress(null, null, null, "0036952d-48df-4422-9fd0-94b0885e18cb");
            CloudStackIpAddress resp = getApi().associateIpAddress(null, null, null, net.getId());
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

			return ec2Address;
		} catch(Exception e) { 
			logger.error( "EC2 AllocateAddress - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * List of templates available.  We only support the imageSet version of this call or when no search parameters are passed
	 * which results in asking for all templates.
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeImagesResponse describeImages(EC2DescribeImages request) 
	{
		EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();

		try {
			String[] templateIds = request.getImageSet();

			if ( 0 == templateIds.length ) {
				return listTemplates(null, images);
			}
			for (String s : templateIds) {
				images = listTemplates(s, images);
			}
			return images;

		} catch( Exception e ) {
			logger.error( "EC2 DescribeImages - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
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
		EC2CreateImageResponse response = null;
		boolean needsRestart = false;
		String volumeId      = null;

		try {
			// [A] Creating a template from a VM volume should be from the ROOT volume
			//     Also for this to work the VM must be in a Stopped state so we 'reboot' it if its not
			EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
			volumes = listVolumes( null, request.getInstanceId(), volumes );
			EC2Volume[] volSet = volumes.getVolumeSet();
			for (EC2Volume vol : volSet) {
				if (vol.getType().equalsIgnoreCase( "ROOT" )) {
					String vmState = vol.getVMState();
					if (vmState.equalsIgnoreCase( "running" ) || vmState.equalsIgnoreCase( "starting" )) {
						needsRestart = true;
						if (!stopVirtualMachine( request.getInstanceId() ))
							throw new EC2ServiceException(ClientError.IncorrectState, "CreateImage - instance must be in a stopped state");
					}           		 
					volumeId = vol.getId();
					break;
				}
			}

			// [B] The parameters must be in sorted order for proper signature generation
			EC2DescribeInstancesResponse instances = new EC2DescribeInstancesResponse();
			instances = lookupInstances( request.getInstanceId(), instances );
			EC2Instance[] instanceSet = instances.getInstanceSet();
			String templateId = instanceSet[0].getTemplateId();

			EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();
			images = listTemplates( templateId, images );
			EC2Image[] imageSet = images.getImageSet();
			String osTypeId = imageSet[0].getOsTypeId();
			
			CloudStackTemplate resp = getApi().createTemplate((request.getDescription() == null ? "" : request.getDescription()), request.getName(), 
					osTypeId, null, null, null, null, null, null, volumeId);
			if (resp == null || resp.getId() == null) {
				throw new EC2ServiceException(ServerError.InternalError, "An upexpected error occurred.");
			}
			
			//if template was created succesfully, create the new image response
			response = new EC2CreateImageResponse();
			response.setId(resp.getId());

			// [C] If we stopped the virtual machine now we need to restart it
			if (needsRestart) {
				if (!startVirtualMachine( request.getInstanceId() )) 
					throw new EC2ServiceException(ServerError.InternalError, 
							"CreateImage - restarting instance " + request.getInstanceId() + " failed");
			}
			return response;

		} catch( Exception e ) {
			logger.error( "EC2 CreateImage - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * Register a template
	 *  
	 * @param request
	 * @return
	 */
	public EC2CreateImageResponse registerImage(EC2RegisterImage request) 
	{
		try {
		    CloudStackAccount caller = getCurrentAccount();
			if (null == request.getFormat()   || null == request.getName() || null == request.getOsTypeName() ||
					null == request.getLocation() || null == request.getZoneName())
				throw new EC2ServiceException(ServerError.InternalError, "Missing parameter - location/architecture/name");

			List<CloudStackTemplate> templates = getApi().registerTemplate((request.getDescription() == null ? request.getName() : request.getDescription()), 
					request.getFormat(), request.getHypervisor(), request.getName(), toOSTypeId(request.getOsTypeName()), request.getLocation(), 
					toZoneId(request.getZoneName(), null), null, null, null, null, null, null, null, null, null);
			if (templates != null) {
			    // technically we will only ever register a single template...
			    for (CloudStackTemplate template : templates) {
        			if (template != null && template.getId() != null) {
        				EC2CreateImageResponse image = new EC2CreateImageResponse();
        				image.setId(template.getId().toString());
        				return image;
        			}
			    }
			}
			return null;
		} catch( Exception e ) {
			logger.error( "EC2 RegisterImage - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * list instances
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeInstancesResponse describeInstances(EC2DescribeInstances request ) {
		try {
			return listVirtualMachines( request.getInstancesSet(), request.getFilterSet()); 
		} catch( Exception e ) {
			logger.error( "EC2 DescribeInstances - " ,e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * list Zones
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeAvailabilityZonesResponse handleRequest(EC2DescribeAvailabilityZones request) {	
		try {
		    CloudStackAccount caller = getCurrentAccount();
		    
			return listZones(request.getZoneSet(), null);

		} catch( EC2ServiceException error ) {
			logger.error( "EC2 DescribeAvailabilityZones - ", error);
			throw error;

		} catch( Exception e ) {
			logger.error( "EC2 DescribeAvailabilityZones - " ,e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * list volumes
	 * 
	 * @param request
	 * @return
	 */
	public EC2DescribeVolumesResponse handleRequest( EC2DescribeVolumes request ) {
		EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
		EC2VolumeFilterSet vfs = request.getFilterSet();

		try {   
			String[] volumeIds = request.getVolumeSet();
			if ( 0 == volumeIds.length ){
				volumes = listVolumes( null, null, volumes );
			} else {     
				for (String s : volumeIds) 
					volumes = listVolumes(s, null, volumes );
			}

			if ( null == vfs )
				return volumes;
			else return vfs.evaluate( volumes );     
		}  catch( Exception e ) {
			logger.error( "EC2 DescribeVolumes - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}

	/**
	 * Attach a volume to an instance
	 * 
	 * @param request
	 * @return
	 */
	public EC2Volume attachVolume( EC2Volume request ) {
		try {   
			request.setDeviceId(mapDeviceToCloudDeviceId(request.getDevice()));
			EC2Volume resp = new EC2Volume();
			
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
				resp.setZoneName(vol.getZoneName());
				return resp;
			}
			throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occurred." );
		} catch( Exception e ) {
			logger.error( "EC2 AttachVolume 2 - ", e);
			throw new EC2ServiceException( ServerError.InternalError, e.getMessage() != null ? e.getMessage() : e.toString());
		}   	    
	}

	/**
	 * Detach a volume from an instance
	 * 
	 * @param request
	 * @return
	 */
	public EC2Volume detachVolume(EC2Volume request) {
		try {
			CloudStackVolume vol = getApi().detachVolume(null, request.getId(), null);
			EC2Volume resp = new EC2Volume();
						
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
				return resp;
			}

			throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occurred." );
		} catch( Exception e ) {
			logger.error( "EC2 DetachVolume - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}   	    
	}

	/**
	 * Create a volume
	 * 
	 * @param request
	 * @return
	 */
	public EC2Volume createVolume( EC2CreateVolume request ) {
		try {
		    
		    CloudStackAccount caller = getCurrentAccount();
			// -> put either snapshotid or diskofferingid on the request
			String snapshotId = request.getSnapshotId();
			Long size = request.getSize();
			String diskOfferingId = null;

			if (snapshotId == null) {
				List<CloudStackDiskOffering> disks = getApi().listDiskOfferings(null, null, null, null);
				for (CloudStackDiskOffering offer : disks) {
					if (offer.isCustomized()) {
						diskOfferingId = offer.getId();
					}
				}
				if (diskOfferingId == null) throw new EC2ServiceException(ServerError.InternalError, "No Customize Disk Offering Found");
			}

//			// -> no volume name is given in the Amazon request but is required in the cloud API
			CloudStackVolume vol = getApi().createVolume(UUID.randomUUID().toString(), null, diskOfferingId, null, size, snapshotId, toZoneId(request.getZoneName(), null));
			if (vol != null) {
				EC2Volume resp = new EC2Volume();
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
				resp.setZoneName(vol.getZoneName());
				return resp;
			}
			return null;
		} catch( Exception e ) {
			logger.error( "EC2 CreateVolume - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}   	    
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
				return request;
			}

			throw new EC2ServiceException(ServerError.InternalError, "An unexpected error occurred.");
		} catch( Exception e ) {
			logger.error( "EC2 DeleteVolume 2 - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}   	    
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
			EC2DescribeInstancesResponse previousState = listVirtualMachines( instanceSet, null );
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
				throw new EC2ServiceException(ClientError.InvalidAMIID_NotFound, "One or more instanceIds do not exist, other instances rebooted.");

			return true;
		} catch( Exception e ) {
			logger.error( "EC2 RebootInstances - ", e );
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
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

		try {
		    CloudStackAccount caller = getCurrentAccount();
		    
			// ugly...
			canCreateInstances = calculateAllowedInstances();
			if (-1 == canCreateInstances) canCreateInstances = request.getMaxCount();

			if (canCreateInstances < request.getMinCount()) {
				logger.info( "EC2 RunInstances - min count too big (" + request.getMinCount() + "), " + canCreateInstances + " left to allocate");
				throw new EC2ServiceException(ClientError.InstanceLimitExceeded ,"Only " + canCreateInstances + " instance(s) left to allocate");	
			}

			if ( canCreateInstances < request.getMaxCount()) 
				createInstances = canCreateInstances;
			else 
				createInstances = request.getMaxCount();

			//find CS service Offering ID
			String instanceType = "m1.small";
			if(request.getInstanceType() != null){ 
			    instanceType = request.getInstanceType();
			}
			CloudStackServiceOffering svcOffering = getCSServiceOfferingId(instanceType);
			if(svcOffering == null){
			    logger.info("No ServiceOffering found to be defined by name, please contact the administrator "+instanceType );
			    throw new EC2ServiceException(ClientError.Unsupported, "instanceType: [" + instanceType + "] not found!");
			}
			
			// zone stuff
			String zoneId = toZoneId(request.getZoneName(), null);
			
			List<CloudStackZone> zones = getApi().listZones(null, null, zoneId, null);
			if (zones == null || zones.size() == 0) {
				logger.info("EC2 RunInstances - zone [" + request.getZoneName() + "] not found!");
				throw new EC2ServiceException(ClientError.InvalidZone_NotFound, "ZoneId [" + request.getZoneName() + "] not found!");
			}
			// we choose first zone?
			CloudStackZone zone = zones.get(0);

			// network
			CloudStackNetwork network = findNetwork(zone);

			// now actually deploy the vms
			for( int i=0; i < createInstances; i++ ) {
				CloudStackUserVm resp = getApi().deployVirtualMachine(svcOffering.getId(), 
						request.getTemplateId(), zoneId, null, null, null, null, 
						null, null, null, request.getKeyName(), null, (network != null ? network.getId() : null), 
						null, null, request.getSize().longValue(), request.getUserData());
				EC2Instance vm = new EC2Instance();
				vm.setId(resp.getId().toString());
				vm.setName(resp.getName());
				vm.setZoneName(resp.getZoneName());
				vm.setTemplateId(resp.getTemplateId().toString());
				if (resp.getSecurityGroupList() != null && resp.getSecurityGroupList().size() > 0) {
					// TODO, we have a list of security groups, just return the first one?
					CloudStackSecurityGroup securityGroup = resp.getSecurityGroupList().get(0);
					vm.setGroup(securityGroup.getName());
				}
				vm.setState(resp.getState());
				vm.setCreated(resp.getCreated());
				vm.setIpAddress(resp.getIpAddress());
				vm.setAccountName(resp.getAccountName());
				vm.setDomainId(resp.getDomainId());
				vm.setHypervisor(resp.getHypervisor());
				vm.setServiceOffering( svcOffering.getName());
				instances.addInstance(vm);
				countCreated++;
			}    		

			if (0 == countCreated) {
				// TODO, we actually need to destroy left-over VMs when the exception is thrown
				throw new EC2ServiceException(ServerError.InsufficientInstanceCapacity, "Insufficient Instance Capacity" );
			}

			return instances;
		} catch( Exception e ) {
			logger.error( "EC2 RunInstances - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
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
			EC2DescribeInstancesResponse previousState = listVirtualMachines( request.getInstancesSet(), null );
			vms = previousState.getInstanceSet();

			// -> send start requests for each item 
			for (EC2Instance vm : vms) {
				vm.setPreviousState(vm.getState());

				// -> if its already running then we don't care
				if (vm.getState().equalsIgnoreCase( "Running" ) || vm.getState().equalsIgnoreCase( "Destroyed" )) continue;

				CloudStackUserVm resp = getApi().startVirtualMachine(vm.getId());
				if(resp != null){
				    vm.setState(resp.getState());
	                if(logger.isDebugEnabled())
	                    logger.debug("Starting VM " + vm.getId() + " job " + resp.getJobId());
				}
				instances.addInstance(vm);
			}
			return instances;
		} catch( Exception e ) {
			logger.error( "EC2 StartInstances - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
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

			EC2DescribeInstancesResponse previousState = listVirtualMachines( instanceSet, null );
			virtualMachines = previousState.getInstanceSet();

			// -> send stop requests for each item 
			for (EC2Instance vm : virtualMachines) {
				vm.setPreviousState( vm.getState());
				CloudStackUserVm resp = null;
				if (request.getDestroyInstances()) {
					if (vm.getState().equalsIgnoreCase( "Destroyed" )) continue;
					resp = getApi().destroyVirtualMachine(vm.getId());
					if(logger.isDebugEnabled())
						logger.debug("Destroying VM " + vm.getId() + " job " + resp.getJobId());
				} else {
					if (vm.getState().equalsIgnoreCase("Stopped") || vm.getState().equalsIgnoreCase("Destroyed")) continue;
					resp = getApi().stopVirtualMachine(vm.getId(), false);
					if(logger.isDebugEnabled())
						logger.debug("Stopping VM " + vm.getId() + " job " + resp.getJobId());
				}
				if (resp != null) {
				    vm.setState(resp.getState());
				    instances.addInstance(vm);
				}
			}
			return instances;
		} catch( Exception e ) {
			logger.error( "EC2 StopInstances - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
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

			EC2DescribeInstancesResponse existingVMS = listVirtualMachines( null, null );
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
	private EC2DescribeInstancesResponse listVirtualMachines( String[] virtualMachineIds, EC2InstanceFilterSet ifs ) throws Exception 
	{
		EC2DescribeInstancesResponse instances = new EC2DescribeInstancesResponse();

		if (null == virtualMachineIds || 0 == virtualMachineIds.length) {
			instances = lookupInstances( null, instances );
		} else {
			for( int i=0; i <  virtualMachineIds.length; i++ ) {
				instances = lookupInstances( virtualMachineIds[i], instances );
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
	private EC2DescribeVolumesResponse listVolumes(String volumeId, String instanceId, EC2DescribeVolumesResponse volumes)throws Exception {

		List<CloudStackVolume> vols = getApi().listVolumes(null, null, null, volumeId, null, null, null, null, null, instanceId, null);
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

				if(vol.getVirtualMachineId() != null)
					ec2Vol.setInstanceId(vol.getVirtualMachineId());

				if(vol.getVirtualMachineState() != null)
					ec2Vol.setVMState(vol.getVirtualMachineState());
				ec2Vol.setZoneName(vol.getZoneName());

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
		}
		zones = listZones(interestedZones, domainId);

		if (zones == null || zones.getZoneIdAt( 0 ) == null) 
			throw new EC2ServiceException(ClientError.InvalidParameterValue, "Unknown zoneName value - " + zoneName);
		return zones.getZoneIdAt(0);
	}

	
	/**
	 * 
	 */
	
	private CloudStackServiceOffering getCSServiceOfferingId(String instanceType) throws Exception{
       try {
           if (null == instanceType) instanceType = "m1.small";                      

           List<CloudStackServiceOffering> svcOfferings = getApi().listServiceOfferings(null, null, null, null, null, 
                   null, null);
           
           if(svcOfferings == null || svcOfferings.isEmpty()){
               logger.debug("No ServiceOffering found to be defined by name: "+instanceType );
               return null;    
           }
           
           for(CloudStackServiceOffering offering : svcOfferings){
               if(instanceType.equalsIgnoreCase(offering.getName())){
                   return offering;
               }
           }
           
           return null;
        } catch(Exception e) {
            logger.error( "listServiceOfferings - ", e);
            throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
	private String serviceOfferingIdToInstanceType( String serviceOfferingId ) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException	{	

        try{
    	    List<CloudStackServiceOffering> svcOfferings = getApi().listServiceOfferings(null, serviceOfferingId, null, null, null, 
                    null, null);
    
            if(svcOfferings == null || svcOfferings.isEmpty()){
                logger.warn( "No instanceType match for serverOfferingId: [" + serviceOfferingId + "]" );
                return "m1.small";
            }
            
    		else return svcOfferings.get(0).getName();
        }
        catch(Exception e) {
            logger.error( "serviceOfferingIdToInstanceType - ", e);
            throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
	private EC2DescribeAvailabilityZonesResponse listZones(String[] interestedZones, String domainId) throws Exception 
	{    
		EC2DescribeAvailabilityZonesResponse zones = new EC2DescribeAvailabilityZonesResponse();

		List<CloudStackZone> cloudZones = getApi().listZones(true, domainId, null, null);

		if(cloudZones != null) {
			for(CloudStackZone cloudZone : cloudZones) {
				if ( null != interestedZones && 0 < interestedZones.length ) {
					for( int j=0; j < interestedZones.length; j++ ) {
						if (interestedZones[j].equalsIgnoreCase( cloudZone.getName())) {
							zones.addZone(cloudZone.getId().toString(), cloudZone.getName());
							break;
						}
					}
				} else { 
					zones.addZone(cloudZone.getId().toString(), cloudZone.getName());
				}
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
	private EC2DescribeInstancesResponse lookupInstances( String instanceId, EC2DescribeInstancesResponse instances ) 
			throws Exception {

		String instId = instanceId != null ? instanceId : null;
		List<CloudStackUserVm> vms = getApi().listVirtualMachines(null, null, null, null, null, null, 
				instId, null, null, null, null, null, null, null, null);
		
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
    			ec2Vm.setHypervisor(cloudVm.getHypervisor());
    			ec2Vm.setRootDeviceType(cloudVm.getRootDeviceType());
    			ec2Vm.setRootDeviceId(cloudVm.getRootDeviceId());
    			ec2Vm.setServiceOffering(serviceOfferingIdToInstanceType(cloudVm.getServiceOfferingId().toString()));
    
    			List<CloudStackNic> nics = cloudVm.getNics();
    			for(CloudStackNic nic : nics) {
    				if(nic.getIsDefault()) {
    					ec2Vm.setPrivateIpAddress(nic.getIpaddress());
    					break;
    				}
    			}
    			instances.addInstance(ec2Vm);
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
	private EC2DescribeImagesResponse listTemplates( String templateId, EC2DescribeImagesResponse images ) throws EC2ServiceException {
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
    				ec2Image.setIsReady(temp.getIsReady());
    				ec2Image.setDomainId(temp.getDomainId());
    				images.addImage(ec2Image);
    			}
            }
			return images;
		} catch(Exception e) {
			logger.error( "List Templates - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
	public EC2DescribeSecurityGroupsResponse listSecurityGroups( String[] interestedGroups ) throws Exception {
		try {
			EC2DescribeSecurityGroupsResponse groupSet = new EC2DescribeSecurityGroupsResponse();
			
			List<CloudStackSecurityGroup> groups = getApi().listSecurityGroups(null, null, null, null, null, null);
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
    
    				groupSet.addGroup(ec2Group);
    			}
			return groupSet;
		} catch(Exception e) {
			logger.error( "List Security Groups - ", e);
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
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
		throw new EC2ServiceException(ServerError.InternalError, "Unable to find an appropriate network for account " + caller.getName());
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
	    // caller.getDomainId doesn't work in user mode
//	    List<CloudStackZone> cloudZones = getApi().listZones(true, caller.getDomainId(), null, null);
	    List<CloudStackZone> cloudZones = getApi().listZones(true, null, null, null);
	    if (cloudZones != null && cloudZones.size() > 0) {
	        return cloudZones.get(0);
	    }
	    return null;
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
	private String mapDeviceToCloudDeviceId( String device ) 
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

		else throw new EC2ServiceException( ClientError.Unsupported, device + " is not supported" );
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
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
			throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
		}
	}
}
