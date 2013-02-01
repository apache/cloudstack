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

package com.cloud.stack;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.stack.models.ApiConstants;
import com.cloud.stack.models.CloudStackAccount;
import com.cloud.stack.models.CloudStackCapabilities;
import com.cloud.stack.models.CloudStackDiskOffering;
import com.cloud.stack.models.CloudStackEvent;
import com.cloud.stack.models.CloudStackEventType;
import com.cloud.stack.models.CloudStackExtractTemplate;
import com.cloud.stack.models.CloudStackIdentifier;
import com.cloud.stack.models.CloudStackInfoResponse;
import com.cloud.stack.models.CloudStackInstanceGroup;
import com.cloud.stack.models.CloudStackIpAddress;
import com.cloud.stack.models.CloudStackKeyPair;
import com.cloud.stack.models.CloudStackKeyValue;
import com.cloud.stack.models.CloudStackLoadBalancerRule;
import com.cloud.stack.models.CloudStackNetwork;
import com.cloud.stack.models.CloudStackNetworkOffering;
import com.cloud.stack.models.CloudStackOsCategory;
import com.cloud.stack.models.CloudStackOsType;
import com.cloud.stack.models.CloudStackPasswordData;
import com.cloud.stack.models.CloudStackPortForwardingRule;
import com.cloud.stack.models.CloudStackResourceLimit;
import com.cloud.stack.models.CloudStackResourceTag;
import com.cloud.stack.models.CloudStackSecurityGroup;
import com.cloud.stack.models.CloudStackSecurityGroupIngress;
import com.cloud.stack.models.CloudStackServiceOffering;
import com.cloud.stack.models.CloudStackSnapshot;
import com.cloud.stack.models.CloudStackSnapshotPolicy;
import com.cloud.stack.models.CloudStackTemplate;
import com.cloud.stack.models.CloudStackTemplatePermission;
import com.cloud.stack.models.CloudStackUserVm;
import com.cloud.stack.models.CloudStackVolume;
import com.cloud.stack.models.CloudStackZone;
import com.google.gson.reflect.TypeToken;

/**
 * The goal here is to wrap the actual CloudStack API calls...
 * 
 *
 */
public class CloudStackApi {
	protected final static Logger logger = Logger.getLogger(CloudStackApi.class);
	
	private CloudStackClient _client;
	
	private String apiKey;
	private String secretKey;
	
	/**
	 * 
	 */
	public CloudStackApi(String cloudStackServiceHost, String port, Boolean bSslEnabled) {
		if (port != null) {
		    // initialize port to 8080, incase port is NULL
		    int ourPort = 8080;
		    if (port != null) 
		        ourPort = Integer.parseInt(port);
			_client = new CloudStackClient(cloudStackServiceHost, ourPort, bSslEnabled);
		} else {
			_client = new CloudStackClient(cloudStackServiceHost);
		}
		apiKey = null;
		secretKey = null;
	}

	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @return the secretKey
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * @param secretKey the secretKey to set
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	
	
	// Virtual Machines

	/**
	 * deploy a virtual machine
	 * 
	 * @param serviceOfferingId
	 * @param templateId
	 * @param zoneId
	 * @param account
	 * @param diskOfferingId
	 * @param displayName
	 * @param domainId
	 * @param group
	 * @param hostId
	 * @param hypervisor
	 * @param keyPair
	 * @param name
	 * @param networkId
	 * @param securityGroupIds
	 * @param securityGroupNames
	 * @param size
	 * @param userData
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm deployVirtualMachine(String serviceOfferingId, String templateId, String zoneId, String account, String diskOfferingId, 
			String displayName, String domainId, String group, String hostId, String hypervisor, String keyPair, String name, String networkId, 
			String securityGroupIds, String securityGroupNames, Long size, String userData) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DEPLOY_VIRTUAL_MACHINE);
		if (cmd != null) {
			// these are required
			cmd.setParam(ApiConstants.SERVICE_OFFERING_ID, serviceOfferingId);
			cmd.setParam(ApiConstants.TEMPLATE_ID, templateId);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			// these aren't
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (diskOfferingId != null) cmd.setParam(ApiConstants.DISK_OFFERING_ID, diskOfferingId);
			if (displayName != null) cmd.setParam(ApiConstants.DISPLAY_NAME, displayName);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (group != null) cmd.setParam(ApiConstants.GROUP, group);
			if (hostId != null) cmd.setParam(ApiConstants.HOST_ID, hostId);
			if (hypervisor != null) cmd.setParam(ApiConstants.HYPERVISOR, hypervisor);
			if (keyPair != null) cmd.setParam(ApiConstants.SSH_KEYPAIR, keyPair);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (networkId != null) cmd.setParam(ApiConstants.NETWORK_IDS, networkId);
			if (securityGroupIds != null) cmd.setParam(ApiConstants.SECURITY_GROUP_IDS, securityGroupIds);
			if (securityGroupNames != null) cmd.setParam(ApiConstants.SECURITY_GROUP_NAMES, securityGroupNames);
			if (size != null) cmd.setParam(ApiConstants.SIZE, size.toString());
			if (userData != null) cmd.setParam(ApiConstants.USER_DATA, userData);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DEPLOY_VIRTUAL_MACHINE_RESPONSE, 
				ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
		
	}
	
	/**
	 * destroy's a virtual machine
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm destroyVirtualMachine(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DESTROY_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DESTROY_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, 
				CloudStackUserVm.class);
	}
	
	/**
	 * reboot a virtual machine
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm rebootVirtualMachine(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.REBOOT_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.REBOOT_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, 
				CloudStackUserVm.class);
	}
	
	/**
	 * start a virtual machine
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm startVirtualMachine(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.START_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.START_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
	}

	/**
	 * stop a virtual machine
	 * 
	 * @param id
	 * @param forced
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm stopVirtualMachine(String id, Boolean forced) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.STOP_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (forced != null) cmd.setParam(ApiConstants.FORCED, forced.toString());
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.STOP_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
	}
	
	/**
	 * reset password for virtual machine
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm resetPasswordForVirtualMachine(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.RESET_PASSWORD_FOR_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.RESET_PASSWORD_FOR_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
	}
	
	/**
	 * change service for virtual machine
	 * 
	 * @param id
	 * @param serviceOfferingId
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm changeServiceForVirtualMachine(String id, String serviceOfferingId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CHANGE_SERVICE_FOR_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.SERVICE_OFFERING_ID, serviceOfferingId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CHANGE_SERVICE_FOR_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
	}
	
	/**
	 * update a virtual machine
	 * 
	 * @param id
	 * @param displayName
	 * @param group
	 * @param haEnable
	 * @param osTypeId
	 * @param userData
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm updateVirtualMachine(String id, String displayName, String group, Boolean haEnable, String osTypeId, String userData) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_VIRTUAL_MACHINE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (displayName != null) cmd.setParam(ApiConstants.DISPLAY_NAME, displayName);
			if (group != null) cmd.setParam(ApiConstants.GROUP, group);
			if (haEnable != null) cmd.setParam(ApiConstants.HA_ENABLE, haEnable.toString());
			if (osTypeId != null) cmd.setParam(ApiConstants.OS_TYPE_ID, osTypeId);
			if (userData != null) cmd.setParam(ApiConstants.USER_DATA, userData);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.UPDATE_VIRTUAL_MACHINE_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
		
	}
	
	/**
	 * list virtual machines
	 * 
	 * @param account
	 * @param accountId
	 * @param forVirtualNetwork
	 * @param groupId
	 * @param hostId
	 * @param hypervisor
	 * @param id
	 * @param isRecursive
	 * @param keyWord
	 * @param name
	 * @param networkId
	 * @param podId
	 * @param state
	 * @param storageId
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
    public List<CloudStackUserVm> listVirtualMachines(String account, String accountId, Boolean listAll, Boolean forVirtualNetwork, String groupId, String hostId,
			String hypervisor, String id, Boolean isRecursive, String keyWord, String name, String networkId, String podId, String state, String storageId, 
            String zoneId, List<CloudStackKeyValue> resourceTags) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_VIRTUAL_MACHINES);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (accountId != null) cmd.setParam(ApiConstants.ACCOUNT_ID, accountId);
            if (listAll != null) cmd.setParam(ApiConstants.LIST_ALL, listAll.toString());
			if (forVirtualNetwork != null) cmd.setParam(ApiConstants.FOR_VIRTUAL_NETWORK, forVirtualNetwork.toString());
			if (groupId != null) cmd.setParam(ApiConstants.GROUP_ID, groupId);
			if (hostId != null) cmd.setParam(ApiConstants.HOST_ID, hostId);
			if (hypervisor != null) cmd.setParam(ApiConstants.HYPERVISOR, hypervisor);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isRecursive != null) cmd.setParam(ApiConstants.IS_RECURSIVE, isRecursive.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (networkId != null) cmd.setParam(ApiConstants.NETWORK_ID, networkId);
			if (podId != null) cmd.setParam(ApiConstants.POD_ID, podId);
			if (state != null) cmd.setParam(ApiConstants.STATE, state);
			if (storageId != null) cmd.setParam(ApiConstants.STORAGE_ID, storageId);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
            if (resourceTags != null && resourceTags.size() > 0)
                cmd = setParams(cmd, null, null, resourceTags);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_VIRTUAL_MACHINES_RESPONSE, ApiConstants.VIRTUAL_MACHINE, 
				new TypeToken<List<CloudStackUserVm>>() {}.getType());
	}

	/**
	 * get password from virtual machine
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackPasswordData getVMPassword(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.GET_VM_PASSWORD);
		if (cmd != null) 
			cmd.setParam(ApiConstants.ID, id);
		// TODO: This probably isn't right.  Need to test with an instance that has a VM Password  
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.GET_VM_PASSWORD_RESPONSE, ApiConstants.PASSWORD, CloudStackPasswordData.class);
	}
	
	// Templates
//<a href="user/createTemplate.html">createTemplate (A)</a>
	/**
	 * create a Template
	 * 
	 * @param displayText
	 * @param name
	 * @param osTypeId
	 * @param bits
	 * @param isFeatured
	 * @param isPublic
	 * @param passwordEnabled
	 * @param requiresHVM
	 * @param snapshotId
	 * @param volumeId
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplate createTemplate(String displayText, String name, String osTypeId, String bits, Boolean isFeatured, 
			Boolean isPublic, Boolean passwordEnabled, Boolean requiresHVM, String snapshotId, String volumeId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_TEMPLATE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			cmd.setParam(ApiConstants.NAME, name);
			cmd.setParam(ApiConstants.OS_TYPE_ID, osTypeId);
			if (bits != null) cmd.setParam(ApiConstants.BITS, bits);
			if (isFeatured != null) cmd.setParam(ApiConstants.IS_FEATURED, isFeatured.toString());
			if (isPublic != null) cmd.setParam(ApiConstants.IS_PUBLIC, isPublic.toString());
			if (passwordEnabled != null) cmd.setParam(ApiConstants.PASSWORD_ENABLED, passwordEnabled.toString());
			if (requiresHVM != null) cmd.setParam(ApiConstants.REQUIRES_HVM, requiresHVM.toString());
			if (snapshotId != null) cmd.setParam(ApiConstants.SNAPSHOT_ID, snapshotId);
			if (volumeId != null) cmd.setParam(ApiConstants.VOLUME_ID, volumeId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_TEMPLATE_RESPONSE, ApiConstants.TEMPLATE, CloudStackTemplate.class);
	}
	
	/**
	 * register a template
	 * 
	 * @param displayText
	 * @param format
	 * @param hypervisor
	 * @param name
	 * @param osTypeId
	 * @param url
	 * @param zoneId
	 * @param account
	 * @param bits
	 * @param checksum
	 * @param domainId
	 * @param isExtractable
	 * @param isFeatured
	 * @param isPublic
	 * @param passwordEnabled
	 * @param requiresHVM
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackTemplate> registerTemplate(String displayText, String format, String hypervisor, String name, String osTypeId, String url, 
			String zoneId, String account, String bits, String checksum, String domainId, Boolean isExtractable, Boolean isFeatured, Boolean isPublic,
			Boolean passwordEnabled, Boolean requiresHVM) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.REGISTER_TEMPLATE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			cmd.setParam(ApiConstants.FORMAT, format);
			cmd.setParam(ApiConstants.HYPERVISOR, hypervisor);
			cmd.setParam(ApiConstants.NAME, name);
			cmd.setParam(ApiConstants.OS_TYPE_ID, osTypeId);
			cmd.setParam(ApiConstants.URL, url);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (bits != null) cmd.setParam(ApiConstants.BITS, bits);
			if (checksum != null) cmd.setParam(ApiConstants.CHECKSUM, checksum);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (isExtractable != null) cmd.setParam(ApiConstants.IS_EXTRACTABLE, isExtractable.toString());
			if (isFeatured != null) cmd.setParam(ApiConstants.IS_FEATURED, isFeatured.toString());
			if (isPublic != null) cmd.setParam(ApiConstants.IS_PUBLIC, isPublic.toString());
			if (passwordEnabled != null) cmd.setParam(ApiConstants.PASSWORD_ENABLED, passwordEnabled.toString());
			if (requiresHVM != null) cmd.setParam(ApiConstants.REQUIRES_HVM, requiresHVM.toString());
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.REGISTER_TEMPLATE_RESPONSE, ApiConstants.TEMPLATE, new TypeToken<List<CloudStackTemplate>>() {}.getType());
	}
	
	/**
	 * update's a template
	 * 
	 * @param id
	 * @param bootable
	 * @param displayText
	 * @param format
	 * @param name
	 * @param osTypeId
	 * @param passwordEnabled
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplate updateTemplate(String id, Boolean bootable, String displayText, String format, String name, String osTypeId, 
			Boolean passwordEnabled) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_TEMPLATE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (bootable != null) cmd.setParam(ApiConstants.BOOTABLE, bootable.toString());
			if (displayText != null) cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			if (format != null) cmd.setParam(ApiConstants.FORMAT, format);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (osTypeId != null) cmd.setParam(ApiConstants.OS_TYPE_ID, osTypeId);
			if (passwordEnabled != null) cmd.setParam(ApiConstants.PASSWORD_ENABLED, passwordEnabled.toString());
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.UPDATE_TEMPLATE_RESPONSE, ApiConstants.TEMPLATE, CloudStackTemplate.class);
	}

	/**
	 * copy a template
	 * 
	 * @param id (required)
	 * @param destZoneId (required)
	 * @param sourceZoneId (required)
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplate copyTemplate(String id, String destZoneId, String sourceZoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.COPY_TEMPLATE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.DESTINATION_ZONE_ID, destZoneId);
			cmd.setParam(ApiConstants.SOURCE_ZONE_ID, sourceZoneId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.COPY_TEMPLATE_RESPONSE, ApiConstants.TEMPLATE, CloudStackTemplate.class);
	}

	/**
	 * Deletes a template from the system. All virtual machines using the deleted template will not be affected.
	 * 
	 * @param id (required)
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteTemplate(String id, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_TEMPLATE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_TEMPLATE_RESPONSE, null, CloudStackInfoResponse.class);
	}

	/**
	 * List all public, private, and privileged templates
	 * 
	 * @param templateFilter (required)
	 * @param account
	 * @param domainId
	 * @param hypervisor
	 * @param id
	 * @param keyWord
	 * @param name
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackTemplate> listTemplates(String templateFilter, String account, String domainId, String hypervisor, String id, 
			String keyWord, String name, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_TEMPLATES);
		if (cmd != null) {
			cmd.setParam(ApiConstants.TEMPLATE_FILTER, templateFilter);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (hypervisor != null) cmd.setParam(ApiConstants.HYPERVISOR, hypervisor);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_TEMPLATES_RESPONSE, ApiConstants.TEMPLATE, 
				new TypeToken<List<CloudStackTemplate>>() {}.getType());
	}
	
	/**
	 * Updates a template visibility permissions. A public template is visible to all accounts within the same domain. 
	 * A private template is visible only to the owner of the template. A priviledged template is a private template with account 
	 * permissions added. Only accounts specified under the template permissions are visible to them.
	 * 
	 * @param id
	 * @param accounts
	 * @param isExtractable
	 * @param isFeatured
	 * @param isPublic
	 * @param op
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse updateTemplatePermissions(String id, String accounts, Boolean isExtractable, Boolean isFeatured, Boolean isPublic, 
			String op) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_TEMPLATE_PERMISSIONS);
		if (cmd  != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (accounts != null) cmd.setParam(ApiConstants.ACCOUNTS, accounts);
			if (isExtractable != null) cmd.setParam(ApiConstants.IS_EXTRACTABLE, isExtractable.toString());
			if (isFeatured != null) cmd.setParam(ApiConstants.IS_FEATURED, isFeatured.toString());
			if (isPublic != null) cmd.setParam(ApiConstants.IS_PUBLIC, isPublic.toString());
			if (op != null) cmd.setParam(ApiConstants.OP, op);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.UPDATE_TEMPLATE_PERMISSIONS_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * List template visibility and all accounts that have permissions to view this template.
	 * 
	 * @param id
	 * @param account
	 * @param domainId
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplatePermission listTemplatePermissions(String id, String account, String domainId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_TEMPLATE_PERMISSIONS);
		if (cmd  != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
		}
        return _client.call(cmd, apiKey, secretKey, false, ApiConstants.LIST_TEMPLATE_PERMISSIONS_RESPONSE, ApiConstants.TEMPLATE_PERMISSION, 
                CloudStackTemplatePermission.class);
	}

	/**
	 * Extracts a template
	 * 
	 * @param id
	 * @param mode
	 * @param zoneId
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public CloudStackExtractTemplate extractTemplate(String id, String mode, String zoneId, String url) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.EXTRACT_TEMPLATE);
		if (cmd  != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.MODE, mode);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			if (url != null) cmd.setParam(ApiConstants.URL, url);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.EXTRACT_TEMPLATE_RESPONSE, null, CloudStackExtractTemplate.class);
	}
	
	// ISO's 
	/**
	 * Attaches an ISO to a virtual machine
	 * 
	 * @param id
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm attachIso(String id, String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.ATTACH_ISO);
		if (cmd  != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.ATTACH_ISO_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
	}
	
	/**
	 * Detaches any ISO file (if any) currently attached to a virtual machine.
	 * 
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
	public CloudStackUserVm detachIso(String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DETACH_ISO);
		if (cmd != null) {
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DETACH_ISO_RESPONSE, ApiConstants.VIRTUAL_MACHINE, CloudStackUserVm.class);
	}

	/**
	 * Lists all available ISO files.
	 * 
	 * @param account
	 * @param bootable
	 * @param domainId
	 * @param hypervisor
	 * @param id
	 * @param isoFilter
	 * @param isPublic
	 * @param isReady
	 * @param keyWord
	 * @param name
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackTemplate> listIsos(String account, Boolean bootable, String domainId, String hypervisor, String id, String isoFilter, 
			Boolean isPublic, Boolean isReady, String keyWord, String name, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_ISOS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (bootable != null) cmd.setParam(ApiConstants.BOOTABLE, bootable.toString());
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (hypervisor != null) cmd.setParam(ApiConstants.HYPERVISOR, hypervisor);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isoFilter != null) cmd.setParam(ApiConstants.ISO_FILTER, isoFilter);
			if (isPublic != null) cmd.setParam(ApiConstants.IS_PUBLIC, isPublic.toString());
			if (isReady != null) cmd.setParam(ApiConstants.IS_READY, isReady.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_ISOS_RESPONSE, ApiConstants.TEMPLATE, 
				new TypeToken<List<CloudStackTemplate>>() {}.getType());
	}
	
	/**
	 * Registers an existing ISO into the Cloud.com Cloud.
	 * 
	 * @param displayText
	 * @param name
	 * @param url
	 * @param zoneId
	 * @param account
	 * @param bootable
	 * @param domainId
	 * @param isExtractable
	 * @param isFeatured
	 * @param isPublic
	 * @param osTypeId
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplate registerIso(String displayText, String name, String url, String zoneId, String account, Boolean bootable, String domainId, 
			Boolean isExtractable, Boolean isFeatured, Boolean isPublic, String osTypeId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.REGISTER_ISO);
		if (cmd != null) {
			cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			cmd.setParam(ApiConstants.NAME, name);
			cmd.setParam(ApiConstants.URL, url);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (bootable != null) cmd.setParam(ApiConstants.BOOTABLE, bootable.toString());
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (isExtractable != null) cmd.setParam(ApiConstants.IS_EXTRACTABLE, isExtractable.toString());
			if (isFeatured != null) cmd.setParam(ApiConstants.IS_FEATURED, isFeatured.toString());
			if (isPublic != null) cmd.setParam(ApiConstants.IS_PUBLIC, isPublic.toString());
			if (osTypeId != null) cmd.setParam(ApiConstants.OS_TYPE_ID, osTypeId);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.REGISTER_ISO_RESPONSE, ApiConstants.TEMPLATE, CloudStackTemplate.class);
	}

	/**
	 * Updates an ISO
	 * 
	 * @param id
	 * @param bootable
	 * @param displayText
	 * @param format
	 * @param name
	 * @param osTypeId
	 * @param passwordEnabled
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplate updateIso(String id, Boolean bootable, String displayText, String format, String name, String osTypeId, 
			Boolean passwordEnabled) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_ISO);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (bootable != null) cmd.setParam(ApiConstants.BOOTABLE, bootable.toString());
			if (displayText != null) cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			if (format != null) cmd.setParam(ApiConstants.FORMAT, format);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (osTypeId != null) cmd.setParam(ApiConstants.OS_TYPE_ID, osTypeId);
			if (passwordEnabled != null) cmd.setParam(ApiConstants.PASSWORD_ENABLED, passwordEnabled.toString());
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.UPDATE_ISO_RESPONSE, ApiConstants.TEMPLATE, CloudStackTemplate.class);
	}
	
	/**
	 * Deletes an ISO
	 * 
	 * @param id
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteIso(String id, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_ISO);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_ISO_RESPONSE, null, CloudStackInfoResponse.class);
	}

	/**
	 * Copies a template from one zone to another
	 * 
	 * @param id
	 * @param destZoneId
	 * @param sourceZoneId
	 * @return
	 * @throws Exception
	 */
	public CloudStackTemplate copyIso(String id, String destZoneId, String sourceZoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.COPY_ISO);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.DESTINATION_ZONE_ID, destZoneId);
			cmd.setParam(ApiConstants.SOURCE_ZONE_ID, sourceZoneId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.COPY_ISO_RESPONSE, ApiConstants.TEMPLATE, CloudStackTemplate.class);
	}
	
	/**
	 * Updates ISO permissions
	 * 
	 * @param id
	 * @param accounts
	 * @param isExtractable
	 * @param isFeatured
	 * @param isPublic
	 * @param op
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse updateIsoPermissions(String id, String accounts, Boolean isExtractable, Boolean isFeatured, 
			Boolean isPublic, String op) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_ISO_PERMISSIONS);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (accounts != null) cmd.setParam(ApiConstants.ACCOUNTS, accounts);
			if (isExtractable != null) cmd.setParam(ApiConstants.IS_EXTRACTABLE, isExtractable.toString());
			if (isFeatured != null) cmd.setParam(ApiConstants.IS_FEATURED, isFeatured.toString());
			if (isPublic != null) cmd.setParam(ApiConstants.IS_PUBLIC, isPublic.toString());
			if (op != null) cmd.setParam(ApiConstants.OP, op);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.UPDATE_ISO_PERMISSIONS_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * List template visibility and all accounts that have permissions to view this template
	 * @param id
	 * @param account
	 * @param domainId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackTemplatePermission> listIsoPermissions(String id, String account, String domainId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_ISO_PERMISSIONS);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_ISO_PERMISSIONS_RESPONSE, ApiConstants.TEMPLATE, 
				new TypeToken<List<CloudStackTemplatePermission>>() {}.getType());
	}

	/**
	 * Extracts an iso 
	 * 
	 * @param id
	 * @param mode
	 * @param zoneId
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public CloudStackExtractTemplate extractIso(String id, String mode, String zoneId, String url) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.EXTRACT_ISO);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.MODE, mode);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			if (url != null) cmd.setParam(ApiConstants.URL, url);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.EXTRACT_ISO_RESPONSE, ApiConstants.TEMPLATE, CloudStackExtractTemplate.class);
	}
	
	// Volumes
	/**
	 * Attaches a disk volume to a virtual machine
	 * 
	 * @param id
	 * @param virtualMachineId
	 * @param deviceId
	 * @return
	 * @throws Exception
	 */
	public CloudStackVolume attachVolume(String id, String virtualMachineId, String deviceId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.ATTACH_VOLUME);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
			if (deviceId != null) cmd.setParam(ApiConstants.DEVICE_ID, deviceId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.ATTACH_VOLUME_RESPONSE, ApiConstants.VOLUME, CloudStackVolume.class);
	}

	/**
	 * Detaches a disk volume from a virtual machine
	 * 
	 * @param deviceId
	 * @param id
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
	public CloudStackVolume detachVolume(String deviceId, String id, String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DETACH_VOLUME);
		if (cmd != null) {
			if (deviceId != null) cmd.setParam(ApiConstants.DEVICE_ID, deviceId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (virtualMachineId != null) cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DETACH_VOLUME_RESPONSE, ApiConstants.VOLUME, CloudStackVolume.class);
	}
	
	/**
	 * Creates a disk volume from a disk offering. This disk volume must still be attached to a virtual machine to make use of it
	 * 
	 * @param name
	 * @param account
	 * @param diskOfferingId
	 * @param domainId
	 * @param size
	 * @param snapshotId
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public CloudStackVolume createVolume(String name, String account, String diskOfferingId, String domainId, Long size, String snapshotId, 
			String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_VOLUME);
		if (cmd != null) {
			cmd.setParam(ApiConstants.NAME, name);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (diskOfferingId != null) cmd.setParam(ApiConstants.DISK_OFFERING_ID, diskOfferingId);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (size != null) cmd.setParam(ApiConstants.SIZE, size.toString());
			if (snapshotId != null) cmd.setParam(ApiConstants.SNAPSHOT_ID, snapshotId);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_VOLUME_RESPONSE, ApiConstants.VOLUME, CloudStackVolume.class);
	}
	
	/**
	 * Deletes a detached disk volume
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteVolume(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_VOLUME);
		if (cmd != null)
			cmd.setParam(ApiConstants.ID, id);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_VOLUME_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * Lists all volumes
	 * 
	 * @param account
	 * @param domainId
	 * @param hostId
	 * @param id
	 * @param isRecursive
	 * @param keyWord
	 * @param name
	 * @param podId
	 * @param type
	 * @param virtualMachineId
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackVolume> listVolumes(String account, String domainId, String hostId, String id, Boolean isRecursive, String keyWord, String name,
            String podId, String type, String virtualMachineId, String zoneId, List<CloudStackKeyValue> resourceTags) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_VOLUMES);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (hostId != null) cmd.setParam(ApiConstants.HOST_ID, hostId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isRecursive != null) cmd.setParam(ApiConstants.IS_RECURSIVE, isRecursive.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (podId != null) cmd.setParam(ApiConstants.POD_ID, podId);
			if (type != null) cmd.setParam(ApiConstants.TYPE, type);
			if (virtualMachineId != null) cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
            if (resourceTags != null && resourceTags.size() > 0)
                cmd = setParams(cmd, null, null, resourceTags);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_VOLUMES_RESPONSE, ApiConstants.VOLUME, 
				new TypeToken<List<CloudStackVolume>>() {}.getType());
	}

	/**
	 * Extracts volume
	 * 
	 * @param id
	 * @param mode
	 * @param zoneId
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public CloudStackExtractTemplate extractVolume(String id, String mode, String zoneId, String url) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.EXTRACT_VOLUME);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.MODE, mode);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			if (url != null) cmd.setParam(ApiConstants.URL, url);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.EXTRACT_VOLUME_RESPONSE, ApiConstants.VOLUME, CloudStackExtractTemplate.class);
	}

    //Tags
    /**
     * Create tags
     *
     * @param resource type
     * @param resource id's
     * @param tags
     * @return
     * @throws Exception
     *
     */
    public CloudStackInfoResponse createTags(String resourceType, List<String>resourceIds,
            List<CloudStackKeyValue> resourceTags) throws Exception {
        CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_TAGS);
        cmd = setParams(cmd, resourceType, resourceIds, resourceTags);
        return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_TAGS_RESPONSE,
                null, CloudStackInfoResponse.class);
    }

    /**
     * Delete tags
     *
     * @param resource type
     * @param resource id's
     * @param tags
     * @return
     * @throws Exception
     *
     */
    public CloudStackInfoResponse deleteTags(String resourceType, List<String>resourceIds,
            List<CloudStackKeyValue> resourceTags) throws Exception {
        CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_TAGS);
        cmd = setParams(cmd, resourceType, resourceIds, resourceTags);
        return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_TAGS_RESPONSE,
                null, CloudStackInfoResponse.class);
    }

    public List<CloudStackResourceTag> listTags(String account, String domainId,
            Boolean isRecursive, Boolean listAll, String keyWord) throws Exception {
        CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_TAGS);
        if (cmd != null) {
            if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
            if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
            if (isRecursive != null) cmd.setParam(ApiConstants.IS_RECURSIVE, isRecursive.toString());
            if (listAll != null) cmd.setParam(ApiConstants.LIST_ALL, listAll.toString());
            if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
        }
        return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_TAGS_RESPONSE,
                ApiConstants.TAG , new TypeToken<List<CloudStackResourceTag>>() {}.getType());
    }

    private CloudStackCommand setParams(CloudStackCommand cmd, String resourceType, List<String>resourceIds,
            List<CloudStackKeyValue> resourceTags) {
        if (cmd != null) {
            if (resourceType != null)
                cmd.setParam(ApiConstants.RESOURCE_TYPE, resourceType);
            if (resourceIds != null && resourceIds.size() > 0) {
                String resourceIdList = resourceIds.get(0);
                for (int i=1 ; i<resourceIds.size(); i++)
                    resourceIdList = resourceIdList.concat(","+resourceIds.get(i));
                cmd.setParam(ApiConstants.RESOURCE_IDS, resourceIdList);
            }
            if (resourceTags != null && resourceTags.size() > 0) {
                int i=0;
                for (CloudStackKeyValue resourceTag : resourceTags) {
                    cmd.setParam(ApiConstants.TAGS+"["+i+"].key", resourceTag.getKey());
                    if (resourceTag.getValue() != null)
                        cmd.setParam(ApiConstants.TAGS+"["+i+"].value", resourceTag.getValue());
                    i++;
                }
            }
        }
        return cmd;
    }

	// Security Groups
	/**
	 * Creates a security group
	 * 
	 * @param name
	 * @param account
	 * @param description
	 * @param domainId
	 * @return
	 * @throws Exception
	 */
	public CloudStackSecurityGroup createSecurityGroup(String name, String account, String description, String domainId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_SECURITY_GROUP);
		if (cmd != null) {
			cmd.setParam(ApiConstants.NAME, name);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (description != null) cmd.setParam(ApiConstants.DESCRIPTION, description);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_SECURITY_GROUP_RESPONSE, ApiConstants.SECURITY_GROUP , CloudStackSecurityGroup.class);
		
	}
	
	/**
	 * Deletes a security group
	 * 
	 * @param account
	 * @param domainId
	 * @param id 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteSecurityGroup(String account, String domainId, String id, String name) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_SECURITY_GROUP);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.DELETE_SECURITY_GROUP_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * Authorizes a particular ingress rule for this security group
	 * 
	 * @param account
	 * @param cidrList
	 * @param domainId
	 * @param endPort
	 * @param icmpCode
	 * @param icmpType
	 * @param protocol
	 * @param securityGroupId
	 * @param securityGroupName
	 * @param startPort
	 * @param userSecurityGroupList List<CloudStackKeyValue>
	 * @return
	 * @throws Exception
	 */
    public CloudStackSecurityGroup authorizeSecurityGroupIngress(String account, String cidrList, String domainId, Long endPort,
            String icmpCode, String icmpType, String protocol, String securityGroupId, String securityGroupName, Long startPort,
            List<CloudStackKeyValue> userSecurityGroupList) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.AUTHORIZE_SECURITY_GROUP_INGRESS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (cidrList != null) cmd.setParam(ApiConstants.CIDR_LIST, cidrList);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (endPort != null) cmd.setParam(ApiConstants.END_PORT, endPort.toString());
			if (icmpCode != null) cmd.setParam(ApiConstants.ICMP_CODE, icmpCode);
			if (icmpType != null) cmd.setParam(ApiConstants.ICMP_TYPE, icmpType);
			if (protocol != null) cmd.setParam(ApiConstants.PROTOCOL, protocol);
			if (securityGroupId != null) cmd.setParam(ApiConstants.SECURITY_GROUP_ID, securityGroupId);
			if (securityGroupName != null) cmd.setParam(ApiConstants.SECURITY_GROUP_NAME, securityGroupName);
			if (startPort != null) cmd.setParam(ApiConstants.START_PORT, startPort.toString());
			if (userSecurityGroupList != null && userSecurityGroupList.size() > 0) {
				int i = 0;
				for (CloudStackKeyValue pair :userSecurityGroupList) {
					cmd.setParam(ApiConstants.USER_SECURITY_GROUP_LIST + "["+i+"].account", pair.getKey());
					cmd.setParam(ApiConstants.USER_SECURITY_GROUP_LIST + "["+i+"].group", pair.getValue());
					i++;
				}				
			}
		}
        return _client.call(cmd, apiKey, secretKey, true, ApiConstants.AUTHORIZE_SECURITY_GROUP_INGRESS_RESPONSE,
                ApiConstants.SECURITY_GROUP, CloudStackSecurityGroup.class);
	}

	/**
	 * Deletes a particular ingress rule from this security group.
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse revokeSecurityGroupIngress(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.REVOKE_SECURITY_GROUP_INGRESS);
		if (cmd != null) 
			cmd.setParam(ApiConstants.ID, id);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.REVOKE_SECURITY_GROUP_INGRESS_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * Lists security groups
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param keyWord
	 * @param securityGroupName
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
    public List<CloudStackSecurityGroup> listSecurityGroups(String account, String domainId, String id, Boolean listAll, String keyWord,
            String securityGroupName, String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_SECURITY_GROUPS);
		if (cmd != null) { 
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
            if (listAll != null) cmd.setParam(ApiConstants.LIST_ALL, listAll.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (securityGroupName != null) cmd.setParam(ApiConstants.SECURITY_GROUP_NAME, securityGroupName);
			if (virtualMachineId != null) cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_SECURITY_GROUPS_RESPONSE, ApiConstants.SECURITY_GROUP, 
				new TypeToken<List<CloudStackSecurityGroup>>() {}.getType());		
	}
	
	// Accounts 
	/**
	 * Lists accounts and provides detailed account information for listed accounts
	 * 
	 * @param accountType
	 * @param domainId
	 * @param id
	 * @param isCleanupRequired
	 * @param isRecursive
	 * @param keyWord
	 * @param name
	 * @param state
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackAccount> listAccounts(Long accountType, String domainId, String id, Boolean isCleanupRequired, Boolean isRecursive, 
			String keyWord, String name, String state) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_ACCOUNTS);
		if (cmd != null) { 
			if (accountType != null) cmd.setParam(ApiConstants.ACCOUNT_TYPE, accountType.toString());
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId); 
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isCleanupRequired != null) cmd.setParam(ApiConstants.IS_CLEANUP_REQUIRED, isCleanupRequired.toString());
			if (isRecursive != null) cmd.setParam(ApiConstants.IS_RECURSIVE, isRecursive.toString()); 
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (state != null) cmd.setParam(ApiConstants.STATE, state);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_ACCOUNTS_RESPONSE, ApiConstants.ACCOUNT, new TypeToken<List<CloudStackAccount>>() {}.getType());
	}

	// Snapshots
	/**
	 * Creates an instant snapshot of a volume
	 * 
	 * @param volumeId
	 * @param account
	 * @param domainId
	 * @param policyId
	 * @return
	 * @throws Exception
	 */
	public CloudStackSnapshot createSnapshot(String volumeId, String account, String domainId, String policyId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_SNAPSHOT);
		if (cmd != null) { 
			cmd.setParam(ApiConstants.VOLUME_ID, volumeId);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (policyId != null) cmd.setParam(ApiConstants.POLICY_ID, policyId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_SNAPSHOT_RESPONSE, ApiConstants.SNAPSHOT, CloudStackSnapshot.class);
	}
	
	/**
	 * list Snapshots
	 * 
	 * @param volumeId
	 * @param account
	 * @param domainId
	 * @param policyId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackSnapshot> listSnapshots(String account, String domainId, String id, String intervalType, Boolean isRecursive,
			String keyWord, String name, String snapshotType, String volumeId, List<CloudStackKeyValue> resourceTags) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_SNAPSHOTS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (intervalType != null) cmd.setParam(ApiConstants.INTERVAL_TYPE, intervalType);
			if (isRecursive != null) cmd.setParam(ApiConstants.IS_RECURSIVE, isRecursive.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (snapshotType != null) cmd.setParam(ApiConstants.SNAPSHOT_TYPE, snapshotType);
			if (volumeId != null) cmd.setParam(ApiConstants.VOLUME_ID, volumeId);
			if (resourceTags != null && resourceTags.size() > 0)
				cmd = setParams(cmd, null, null, resourceTags);			
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_SNAPSHOTS_RESPONSE, ApiConstants.SNAPSHOT, 
				new TypeToken<List<CloudStackSnapshot>>() {}.getType());
	}
	
	/**
	 * Deletes a snapshot of a disk volume
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteSnapshot(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_SNAPSHOT);
		if (cmd != null) 
			cmd.setParam(ApiConstants.ID, id);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_SNAPSHOT_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * Creates a snapshot policy for the account
	 *  
	 * @param intervalType
	 * @param maxSnaps
	 * @param schedule
	 * @param timeZone
	 * @param volumeId
	 * @return
	 * @throws Exception
	 */
	public CloudStackSnapshotPolicy createSnapshotPolicy(String intervalType, Long maxSnaps, String schedule, 
			String timeZone, String volumeId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_SNAPSHOT_POLICY);
		if (cmd != null) { 
			cmd.setParam(ApiConstants.INTERVAL_TYPE, intervalType);
			cmd.setParam(ApiConstants.MAX_SNAPS, maxSnaps.toString());
			cmd.setParam(ApiConstants.SCHEDULE, schedule);
			cmd.setParam(ApiConstants.TIMEZONE, timeZone);
			cmd.setParam(ApiConstants.VOLUME_ID, volumeId);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.CREATE_SNAPSHOT_POLICY_RESPONSE, ApiConstants.SNAPSHOT, CloudStackSnapshotPolicy.class);
	}
	
	/**
	 * Delete's snapshot policies for the account
	 * 
	 * @param id
	 * @param ids
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteSnapshotPolicies(String id, String ids) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_SNAPSHOT_POLICIES);
		if (cmd != null) {
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (ids != null) cmd.setParam(ApiConstants.IDS, ids);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.DELETE_SNAPSHOT_POLICIES_RESPONSE, null, CloudStackInfoResponse.class);
	}

	/**
	 * List snapshot policies for the account
	 * 
	 * @param volumeId
	 * @param account
	 * @param domainId
	 * @param keyWord
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackSnapshotPolicy> listSnapshotPolicies(String volumeId, String account, String domainId, String keyWord) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_SNAPSHOT_POLICIES);
		if (cmd != null) {
			cmd.setParam(ApiConstants.VOLUME_ID, volumeId);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_SNAPSHOT_POLICIES_RESPONSE, ApiConstants.SNAPSHOT, 
				new TypeToken<List<CloudStackSnapshotPolicy>>() {}.getType());
	}

	// Events
	/**
	 * List events
	 * 
	 * @param account
	 * @param domainId
	 * @param duration
	 * @param endDate
	 * @param entryTime
	 * @param id
	 * @param keyWord
	 * @param level
	 * @param startDate
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackEvent> listEvents(String account, String domainId, Long duration, String endDate, String entryTime, String id, String keyWord,
			String level, String startDate, String type) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_EVENTS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (duration != null) cmd.setParam(ApiConstants.DURATION, duration.toString());
			if (endDate != null) cmd.setParam(ApiConstants.END_DATE, endDate);
			if (entryTime != null) cmd.setParam(ApiConstants.ENTRY_TIME, entryTime);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (level != null) cmd.setParam(ApiConstants.LEVEL, level);
			if (startDate != null) cmd.setParam(ApiConstants.START_DATE, startDate);
			if (type != null) cmd.setParam(ApiConstants.TYPE, type);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_EVENTS_RESPONSE, ApiConstants.EVENT, 
				new TypeToken<List<CloudStackEvent>>() {}.getType());
	}

	/**
	 * List event types
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackEventType> listEventTypes() throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_EVENT_TYPES);
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_EVENT_TYPES_RESPONSE, ApiConstants.EVENT_TYPE,
				new TypeToken<List<CloudStackEventType>>() {}.getType());
	}
	
	
	// Guest OS
	/**
	 * list OS Types
	 * 
	 * @param id
	 * @param keyWord
	 * @param osCategoryId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackOsType> listOsTypes(String id, String keyWord, String osCategoryId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_OS_TYPES);
		if (cmd != null) {
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (osCategoryId != null) cmd.setParam(ApiConstants.OS_CATEGORY_ID, osCategoryId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_OS_TYPES_RESPONSE, ApiConstants.OS_TYPE, 
			new TypeToken<List<CloudStackOsType>>() {}.getType());
	}
	
	/**
	 * list OS Categories
	 * 
	 * @param id
	 * @param keyWord
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackOsCategory> listOsCategories(String id, String keyWord) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_OS_CATEGORIES);
		if (cmd != null) {
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_OS_CATEGORIES_RESPONSE, ApiConstants.OS_CATEGORY, 
				new TypeToken<List<CloudStackOsCategory>>() {}.getType());
	}
	
	// Service Offering
	/**
	 * list available Service offerings
	 * 
	 * @param domainId
	 * @param id
	 * @param isSystem
	 * @param keyWord
	 * @param name
	 * @param systemVmType
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackServiceOffering> listServiceOfferings(String domainId, String id, Boolean isSystem, String keyWord, String name, 
			String systemVmType, String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_SERVICE_OFFERINGS);
		if (cmd != null) {
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isSystem != null) cmd.setParam(ApiConstants.IS_SYSTEM, isSystem.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (systemVmType != null) cmd.setParam(ApiConstants.SYSTEM_VM_TYPE, systemVmType);
			if (virtualMachineId != null) cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_SERVICE_OFFERINGS_RESPONSE, ApiConstants.SERVICE_OFFERING, 
				new TypeToken<List<CloudStackServiceOffering>>() {}.getType());
	}
	
	// Disk Offerings
	/**
	 * list available disk offerings
	 * 
	 * @param domainId
	 * @param id
	 * @param keyWord
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackDiskOffering> listDiskOfferings(String domainId, String id, String keyWord, String name) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_DISK_OFFERINGS);
		if (cmd != null) {
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_DISK_OFFERINGS_RESPONSE, ApiConstants.DISK_OFFERING, 
				new TypeToken<List<CloudStackDiskOffering>>() {}.getType());
	}
	
	// SSH keys
	/**
	 * register an SSH Key Pair
	 * 
	 * @param name
	 * @param publicKey
	 * @return
	 * @throws Exception
	 */
	public CloudStackKeyPair registerSSHKeyPair(String name, String publicKey) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.REGISTER_SSH_KEY_PAIR);
		if (cmd != null) {
			cmd.setParam(ApiConstants.NAME, name);
			cmd.setParam(ApiConstants.PUBLIC_KEY, publicKey);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.REGISTER_SSH_KEY_PAIR_RESPONSE, ApiConstants.KEY_PAIR, CloudStackKeyPair.class);
	}
	
	/**
	 * Create an SSH Key Pair
	 * 
	 * @param name
	 * @param account
	 * @param domainId
	 * @return
	 * @throws Exception
	 */
	public CloudStackKeyPair createSSHKeyPair(String name, String account, String domainId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_SSH_KEY_PAIR);
		if (cmd != null) {
			cmd.setParam(ApiConstants.NAME, name);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.CREATE_SSH_KEY_PAIR_RESPONSE, ApiConstants.KEY_PAIR, CloudStackKeyPair.class);
	}
	
	/**
	 * delete an SSH Key Pair
	 * 
	 * @param name
	 * @param account
	 * @param domainId
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteSSHKeyPair(String name, String account, String domainId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_SSH_KEY_PAIR);
		if (cmd != null) {
			cmd.setParam(ApiConstants.NAME, name);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.DELETE_SSH_KEY_PAIR_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * return list of SSH Key Pairs
	 * 
	 * @param fingerprint
	 * @param keyWord
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackKeyPair> listSSHKeyPairs(String fingerprint, String keyWord, String name) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_SSH_KEY_PAIRS);
		if (cmd != null) {
			if (fingerprint != null) cmd.setParam(ApiConstants.FINGERPRINT, fingerprint);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_SSH_KEY_PAIRS_RESPONSE, "sshkeypair", 
				new TypeToken<List<CloudStackKeyPair>>() {}.getType());
	}

	// IpAddresses
	/**
	 * associate an ip address 
	 * 
	 * @param zoneId
	 * @param account
	 * @param domainId
	 * @param networkId
	 * @return
	 * @throws Exception
	 */
	public CloudStackIpAddress associateIpAddress(String zoneId, String account, String domainId, String networkId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.ASSOCIATE_IP_ADDRESS);
		if (cmd != null) {
		    // previous zoneId was required according to api docs, but Management Server UI doesn't use it...
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (networkId != null) cmd.setParam(ApiConstants.NETWORK_ID, networkId);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.ASSOCIATE_IP_ADDRESS_RESPONSE, null, CloudStackIpAddress.class);
	}
	
	/**
	 * disassociate an ipaddress from an instance
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse disassociateIpAddress(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DISASSOCIATE_IP_ADDRESS);
		if (cmd != null) cmd.setParam(ApiConstants.ID, id);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DISASSOCIATE_IP_ADDRESS_RESPONSE, null, CloudStackInfoResponse.class);
	}

	/**
	 * lists of allocate public ip addresses
	 * 
	 * @param account
	 * @param allocatedOnly
	 * @param domainId
	 * @param forVirtualNetwork
	 * @param id
	 * @return
	 */
	public List<CloudStackIpAddress> listPublicIpAddresses(String account, Boolean allocatedOnly, String domainId, Boolean forVirtualNetwork, String id,
			String ipAddress, String keyWord, String vlanId, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_PUBLIC_IP_ADDRESSES);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (allocatedOnly != null) cmd.setParam(ApiConstants.ALLOCATED_ONLY, allocatedOnly.toString());
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (forVirtualNetwork != null) cmd.setParam(ApiConstants.FOR_VIRTUAL_NETWORK, forVirtualNetwork.toString());
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (ipAddress != null) cmd.setParam(ApiConstants.IP_ADDRESS, ipAddress);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (vlanId != null) cmd.setParam(ApiConstants.VLAN_ID, vlanId);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_PUBLIC_IP_ADDRESSES_RESPONSE, ApiConstants.PUBLIC_IP_ADDRESS, 
				new TypeToken<List<CloudStackIpAddress>>() {}.getType());
	}

	// Firewall
	/**
	 * list port forwarding rules
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param ipAddressId
	 * @param keyWord
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackPortForwardingRule> listPortForwardingRules(String account, String domainId, String id, Long ipAddressId, 
			String keyWord) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_PORT_FORWARDING_RULES);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (ipAddressId != null) cmd.setParam(ApiConstants.IP_ADDRESS_ID, ipAddressId.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_PORT_FORWARDING_RULES_RESPONSE, ApiConstants.PORT_FORWARDING_RULE, 
				new TypeToken<List<CloudStackPortForwardingRule>>() {}.getType());
	}

	/**
	 * Create a Port Forwarding Rule
	 * 
	 * @param ipAddressId
	 * @param privatePort
	 * @param protocol
	 * @param publicPort
	 * @param virtualMachineId
	 * @param cidrList
	 * @param privateEndPort
	 * @param publicEndPort
	 * @return
	 * @throws Exception
	 */
	public CloudStackPortForwardingRule createPortForwardingRule(String ipAddressId, Long privatePort, String protocol, Long publicPort, 
			String virtualMachineId, String cidrList, Long privateEndPort, Long publicEndPort) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_PORT_FORWARDING_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.IP_ADDRESS_ID, ipAddressId);
			cmd.setParam(ApiConstants.PRIVATE_PORT, privatePort.toString());
			cmd.setParam(ApiConstants.PROTOCOL, protocol);
			cmd.setParam(ApiConstants.PUBLIC_PORT, publicPort.toString());
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
			if (cidrList != null) cmd.setParam(ApiConstants.CIDR_LIST, cidrList);
			if (privateEndPort != null) cmd.setParam(ApiConstants.PRIVATE_END_PORT, privateEndPort.toString());
			if (publicEndPort != null) cmd.setParam(ApiConstants.PUBLIC_END_PORT, publicEndPort.toString());
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_PORT_FORWARDING_RULE_RESPONSE, ApiConstants.PORT_FORWARDING_RULE, CloudStackPortForwardingRule.class);
	}
	
	/**
	 * Delete a Port Forwarding Rule
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deletePortForwardingRule(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_PORT_FORWARDING_RULE);
		if (cmd != null) cmd.setParam(ApiConstants.ID, id);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_PORT_FORWARDING_RULE_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	// NAT
	/**
	 * enable Static Nat
	 * 
	 * @param ipAddressId
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse enableStaticNat(String ipAddressId, String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.ENABLE_STATIC_NAT);
		if (cmd != null) {
			cmd.setParam(ApiConstants.IP_ADDRESS_ID, ipAddressId);
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.ENABLE_STATIC_NAT_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * Creates an ip forwarding rule
	 * 
	 * @param ipAddressId
	 * @param protocol
	 * @param startPort
	 * @param endPort
	 * @return
	 * @throws Exception
	 */
	public CloudStackPortForwardingRule createIpForwardingRule(String ipAddressId, String protocol, Long startPort, Long endPort) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_IP_FORWARDING_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.IP_ADDRESS_ID, ipAddressId);
			cmd.setParam(ApiConstants.PROTOCOL, protocol);
			cmd.setParam(ApiConstants.START_PORT, startPort.toString());
			if (endPort != null) cmd.setParam(ApiConstants.END_PORT, endPort.toString());
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_IP_FORWARDING_RULE_RESPONSE, ApiConstants.IP_FORWARDING_RULE, CloudStackPortForwardingRule.class);
	}

	/**
	 * Deletes an ip forwarding rule
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteIpForwardingRule(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_IP_FORWARDING_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_IP_FORWARDING_RULE_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * List the ip forwarding rules
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param ipAddressId
	 * @param keyWord
	 * @param virtualMachineId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackPortForwardingRule> listIpForwardingRules(String account, String domainId, String id, String ipAddressId, String keyWord, 
			String virtualMachineId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_IP_FORWARDING_RULES);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (ipAddressId != null) cmd.setParam(ApiConstants.IP_ADDRESS_ID, ipAddressId);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (virtualMachineId != null) cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_IP_FORWARDING_RULES_RESPONSE, ApiConstants.IP_FORWARDING_RULE, 
				new TypeToken<List<CloudStackPortForwardingRule>>() {}.getType());
	}
	
	/**
	 * Disables static rule for given ip address
	 * 
	 * @param ipAddressId
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse disableStaticNat(String ipAddressId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DISABLE_STATIC_NAT);
		if (cmd != null) 
			cmd.setParam(ApiConstants.IP_ADDRESS_ID, ipAddressId);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DISABLE_STATIC_NAT_RESPONSE, null, CloudStackInfoResponse.class);
	}

	// Load Balancer
	/**
	 * Creates a load balancer rule
	 * 
	 * @param algorithm
	 * @param name
	 * @param privatePort
	 * @param publicIpId
	 * @param publicPort
	 * @param description
	 * @return
	 * @throws Exception
	 */
	public CloudStackLoadBalancerRule createLoadBalancerRule(String algorithm, String name, Long privatePort, String publicIpId, 
			Long publicPort, String description) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_LOAD_BALANCER_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ALGORITHM, algorithm);
			cmd.setParam(ApiConstants.NAME, name);
			cmd.setParam(ApiConstants.PRIVATE_PORT, privatePort.toString());
			cmd.setParam(ApiConstants.PUBLIC_IP_ID, publicIpId);
			cmd.setParam(ApiConstants.PUBLIC_PORT, publicPort.toString());
			if (description != null) cmd.setParam(ApiConstants.DESCRIPTION, description);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.CREATE_LOAD_BALANCER_RULE_RESPONSE, ApiConstants.LOAD_BALANCER, 
				CloudStackLoadBalancerRule.class);
	}
	
	/**
	 * Deletes a load balancer rule
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteLoadBalancer(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_LOAD_BALANCER_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.DELETE_LOAD_BALANCER_RULE_RESPONSE, null, 
				CloudStackInfoResponse.class);
	}

	/**
	 * Removes a virtual machine or a list of virtual machines from a load balancer rule
	 * 
	 * @param id
	 * @param virtualMachineIds
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse removeFromLoadBalancerRule(String id, String virtualMachineIds) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.REMOVE_FROM_LOAD_BALANCER_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_IDS, virtualMachineIds);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.REMOVE_FROM_LOAD_BALANCER_RULE_RESPONSE, null, 
				CloudStackInfoResponse.class);
	}

	/**
	 * Assigns virtual machine or a list of virtual machines to a load balancer rule.
	 * 
	 * @param id
	 * @param virtualMachineIds
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse assignToLoadBalancerRule(String id, String virtualMachineIds) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.ASSIGN_TO_LOAD_BALANCER_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			cmd.setParam(ApiConstants.VIRTUAL_MACHINE_IDS, virtualMachineIds);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.ASSIGN_TO_LOAD_BALANCER_RULE_RESPONSE, null, 
				CloudStackInfoResponse.class);
	}
	
	/**
	 * Lists load balancer rules
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param keyWord
	 * @param name
	 * @param publicIpId
	 * @param virtualMachineId
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackLoadBalancerRule> listLoadBalancerRules(String account, String domainId, String id, String keyWord, 
			String name, String publicIpId, String virtualMachineId, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_LOAD_BALANCER_RULES);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (publicIpId != null) cmd.setParam(ApiConstants.PUBLIC_IP_ID, publicIpId);
			if (virtualMachineId != null) cmd.setParam(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineId);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_LOAD_BALANCER_RULES_RESPONSE, ApiConstants.LOAD_BALANCER, 
				new TypeToken<List<CloudStackLoadBalancerRule>>() {}.getType());
	}
	
	/**
	 * List all virtual machine instances that are assigned a load balancer rule
	 * 
	 * @param id
	 * @param applied
	 * @param keyWord
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackUserVm> listLoadBalancerRuleInstances(String id, Boolean applied, String keyWord) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_LOAD_BALANCER_RULE_INSTANCES);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (applied != null) cmd.setParam(ApiConstants.APPLIED, applied.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_LOAD_BALANCER_RULE_INSTANCES_RESPONSE, ApiConstants.VIRTUAL_MACHINE, 
				new TypeToken<List<CloudStackUserVm>>() {}.getType());
	}
		
	/**
	 * Updates load balancer
	 * 
	 * @param id
	 * @param algorithm
	 * @param description
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public CloudStackLoadBalancerRule updateLoadBalancerRule(String id, String algorithm, String description, String name) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_LOAD_BALANCER_RULE);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (algorithm != null) cmd.setParam(ApiConstants.ALGORITHM, algorithm);
			if (description != null) cmd.setParam(ApiConstants.DESCRIPTION, description);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.UPDATE_LOAD_BALANCER_RULE_RESPONSE, ApiConstants.LOAD_BALANCER, CloudStackLoadBalancerRule.class);
	}

	// VM Group
	
	/**
	 * create an instance group
	 * 
	 * @param name
	 * @param account
	 * @param domainId
	 * @return
	 * @throws Exception
	 */
	public CloudStackInstanceGroup createInstanceGroup(String name, String account, String domainId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_INSTANCE_GROUP);
		if (cmd != null) {
			cmd.setParam(ApiConstants.NAME, name);
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.CREATE_INSTANCE_GROUP_RESPONSE, ApiConstants.INSTANCE_GROUP, CloudStackInstanceGroup.class);
		
	}
	
	/**
	 * delete an instance group
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteInstanceGroup(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_INSTANCE_GROUP);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_INSTANCE_GROUP_RESPONSE, null, CloudStackInfoResponse.class);
	}

	/**
	 * Update an instance group
	 * 
	 * @param id
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public CloudStackInstanceGroup updateInstanceGroup(String id, String name) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_INSTANCE_GROUP);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.UPDATE_INSTANCE_GROUP_RESPONSE, ApiConstants.INSTANCE_GROUP, 
				CloudStackInstanceGroup.class);
	}
	
	/**
	 * List instance groups
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param keyWord
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackInstanceGroup> listInstanceGroups(String account, String domainId, String id, String keyWord, String name) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_INSTANCE_GROUPS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_INSTANCE_GROUPS_RESPONSE, ApiConstants.INSTANCE_GROUP, 
				new TypeToken<List<CloudStackInstanceGroup>>() {}.getType());

	}

	// Networks
	/**
	 * Creates a network
	 * 
	 * @param displayText
	 * @param name
	 * @param networkOfferingId
	 * @param zoneId
	 * @param account
	 * @param domainId
	 * @param isDefault
	 * @param startIp
	 * @param endIp
	 * @param gateway
	 * @param netmask
	 * @param isShared
	 * @param networkDomain
	 * @param tags
	 * @param vlan
	 * @return
	 * @throws Exception
	 */
	public CloudStackNetwork createNetwork(String displayText, String name, String networkOfferingId, String zoneId, String account, String domainId,
			Boolean isDefault, String startIp, String endIp, String gateway, String netmask, Boolean isShared, String networkDomain, String tags, 
			String vlan) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.CREATE_NETWORK);
		if (cmd != null) {
			// required params
			cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			cmd.setParam(ApiConstants.NAME, name);
			cmd.setParam(ApiConstants.NETWORK_OFFERING_ID, networkOfferingId);
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
			// optional params
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (endIp != null) cmd.setParam(ApiConstants.END_IP, endIp);
			if (gateway != null) cmd.setParam(ApiConstants.GATEWAY, gateway);
			if (isDefault != null) cmd.setParam(ApiConstants.IS_DEFAULT, isDefault.toString());
			if (isShared != null) cmd.setParam(ApiConstants.IS_SHARED, isShared.toString());
			if (netmask != null) cmd.setParam(ApiConstants.NETMASK, netmask);
			if (networkDomain != null) cmd.setParam(ApiConstants.NETWORK_DOMAIN, networkDomain);
			if (startIp != null) cmd.setParam(ApiConstants.START_IP, startIp);
			if (tags != null) cmd.setParam(ApiConstants.TAGS, tags);
			if (vlan != null) cmd.setParam(ApiConstants.VLAN, vlan);
		}
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.CREATE_NETWORK_RESPONSE, ApiConstants.NETWORK, 
				CloudStackNetwork.class);
	}
	
	/**
	 * delete a network
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackInfoResponse deleteNetwork(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.DELETE_NETWORK);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.DELETE_NETWORK_RESPONSE, null, CloudStackInfoResponse.class);
	}
	
	/**
	 * list Networks
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param isDefault
	 * @param isShared
	 * @param isSystem
	 * @param keyWord
	 * @param trafficType
	 * @param type
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackNetwork> listNetworks(String account, String domainId, String id, Boolean isDefault, Boolean isShared, Boolean isSystem, 
			String keyWord, String trafficType, String type, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_NETWORKS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isDefault != null) cmd.setParam(ApiConstants.IS_DEFAULT, isDefault.toString());
			if (isShared != null) cmd.setParam(ApiConstants.IS_SHARED, isShared.toString());
			if (isSystem != null) cmd.setParam(ApiConstants.IS_SYSTEM, isSystem.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (trafficType != null) cmd.setParam(ApiConstants.TRAFFIC_TYPE, trafficType);
			if (type != null) cmd.setParam(ApiConstants.TYPE, type);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_NETWORKS_RESPONSE, ApiConstants.NETWORK, 
				new TypeToken<List<CloudStackNetwork>>() {}.getType());
		
	}
	
	/**
	 * Reapplies all ip addresses for the particular network
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public CloudStackIpAddress restartNetwork(String id) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.RESTART_NETWORK);
		if (cmd != null) 
			cmd.setParam(ApiConstants.ID, id);
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.RESTART_NETWORK_RESPONSE, ApiConstants.PUBLIC_IP_ADDRESS, CloudStackIpAddress.class);
	}
	
	/**
	 * update a network
	 * 
	 * @param id
	 * @param displayText
	 * @param name
	 * @param networkDomain
	 * @param tags
	 * @return
	 * @throws Exception
	 */
	public CloudStackNetwork updateNetwork(String id, String displayText, String name, String networkDomain, String tags) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.UPDATE_NETWORK);
		if (cmd != null) {
			cmd.setParam(ApiConstants.ID, id);
			if (displayText != null) cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (networkDomain != null) cmd.setParam(ApiConstants.NETWORK_DOMAIN, networkDomain);
			if (tags != null) cmd.setParam(ApiConstants.TAGS, tags);
		}
		return _client.call(cmd, apiKey, secretKey, true, ApiConstants.UPDATE_NETWORK_RESPONSE, ApiConstants.NETWORK, CloudStackNetwork.class);
	}

	// Hypervisor
	/**
	 * list Hypervisors
	 * 
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackEventType> listHypervisors(String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_HYPERVISORS);
		if (cmd != null) 
			cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_HYPERVISORS_RESPONSE, ApiConstants.HYPERVISOR, 
				new TypeToken<List<CloudStackEventType>>() {}.getType());
	}
	
	// Zones
	/**
	 * list Zones
	 * 
	 * @param available
	 * @param domainId
	 * @param id
	 * @param keyWord
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackZone> listZones(Boolean available, String domainId, String id, String keyWord) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_ZONES);
		if (cmd != null) {
			if (available != null) cmd.setParam(ApiConstants.AVAILABLE, available.toString());
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_ZONES_RESPONSE, ApiConstants.ZONE, 
				new TypeToken<List<CloudStackZone>>() {}.getType());
		
	}

	// Network Offerings
	/**
	 * List available network offerings
	 * 
	 * @param availability
	 * @param displayText
	 * @param guestIpType
	 * @param id
	 * @param isDefault
	 * @param isShared
	 * @param keyWord
	 * @param name
	 * @param specifyVLan
	 * @param trafficType
	 * @param zoneId
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackNetworkOffering> listNetworkOfferings(String availability, String displayText, String guestIpType, String id, 
			Boolean isDefault, Boolean isShared, String keyWord, String name, String specifyVLan, String trafficType, String zoneId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_NETWORK_OFFERINGS);
		if (cmd != null) {
			if (availability != null) cmd.setParam(ApiConstants.AVAILABILITY, availability);
			if (displayText != null) cmd.setParam(ApiConstants.DISPLAY_TEXT, displayText);
			if (guestIpType != null) cmd.setParam(ApiConstants.GUEST_IP_TYPE, guestIpType);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (isDefault != null) cmd.setParam(ApiConstants.IS_DEFAULT, isDefault.toString());
			if (isShared != null) cmd.setParam(ApiConstants.IS_SHARED, isShared.toString());
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (name != null) cmd.setParam(ApiConstants.NAME, name);
			if (specifyVLan != null) cmd.setParam(ApiConstants.SPECIFY_VLAN, specifyVLan);
			if (trafficType != null) cmd.setParam(ApiConstants.TRAFFIC_TYPE, trafficType);
			if (zoneId != null) cmd.setParam(ApiConstants.ZONE_ID, zoneId);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_NETWORK_OFFERINGS_RESPONSE, ApiConstants.NETWORK_OFFERING, 
				new TypeToken<List<CloudStackNetworkOffering>>() {}.getType());
	}

	// Configuration
	/**
	 * list Capaibilities
	 * 
	 * @return
	 * @throws Exception
	 */
	public CloudStackCapabilities listCapabilities() throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_CAPABILITIES);
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.LIST_CAPABILITIES_RESPONSE, ApiConstants.CAPABILITY, CloudStackCapabilities.class);
	}

	// Limits
	/**
	 * list resource limits
	 * 
	 * @param account
	 * @param domainId
	 * @param id
	 * @param keyWord
	 * @param resourceType
	 * @return
	 * @throws Exception
	 */
	public List<CloudStackResourceLimit> listResourceLimits(String account, String domainId, String id, String keyWord, String resourceType) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_RESOURCE_LIMITS);
		if (cmd != null) {
			if (account != null) cmd.setParam(ApiConstants.ACCOUNT, account);
			if (domainId != null) cmd.setParam(ApiConstants.DOMAIN_ID, domainId);
			if (id != null) cmd.setParam(ApiConstants.ID, id);
			if (keyWord != null) cmd.setParam(ApiConstants.KEYWORD, keyWord);
			if (resourceType != null) cmd.setParam(ApiConstants.RESOURCE_TYPE, resourceType);
		}
		return _client.listCall(cmd, apiKey, secretKey, ApiConstants.LIST_RESOURCE_LIMITS_RESPONSE, ApiConstants.RESOURCE_LIMIT, 
				new TypeToken<List<CloudStackResourceLimit>>() {}.getType());
		
	}
	
	// Cloud Identifier
	/**
	 * Returns a cloud identifier
	 * 
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	public CloudStackIdentifier getCloudIdentifier(String userId) throws Exception {
		CloudStackCommand cmd = new CloudStackCommand(ApiConstants.LIST_RESOURCE_LIMITS);
		if (cmd != null) 
			cmd.setParam(ApiConstants.USER_ID, userId);
			
		return _client.call(cmd, apiKey, secretKey, false, ApiConstants.GET_CLOUD_IDENTIFIER_RESPONSE, ApiConstants.CLOUD_IDENTIFIER, CloudStackIdentifier.class);
	}
	
	
}
