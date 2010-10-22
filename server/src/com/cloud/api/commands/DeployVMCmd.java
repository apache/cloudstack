/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroupVO;

@Implementation(method="deployVirtualMachine", description="Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.")
public class DeployVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeployVMCmd.class.getName());
    
    private static final String s_name = "deployvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name="diskofferingid", type=CommandType.LONG, description="the ID of the disk offering for the virtual machine. If the template is of ISO format, the diskOfferingId is for the root disk volume. Otherwise this parameter is used to dinidcate the offering for the data disk volume. If the templateId parameter passed is from a Template object, the diskOfferingId refers to a DATA Disk Volume created. If the templateId parameter passed is from an ISO object, the diskOfferingId refers to a ROOT Disk Volume created.")
    private Long diskOfferingId;

    @Parameter(name="displayname", type=CommandType.STRING, description="an optional user generated name for the virtual machine")
    private String displayName;

    @Parameter(name="domainid", type=CommandType.LONG, description="an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name="group", type=CommandType.STRING, description="an optional group for the virtual machine")
    private String group;

    @Parameter(name="hypervisor", type=CommandType.STRING, description="the hypervisor on which to deploy the virtual machine")
    private String hypervisor;

    @Parameter(name="networkgrouplist", type=CommandType.LIST, collectionType=CommandType.STRING, description="comma separated list of network groups that going to be applied to the virtual machine. Should be passed only when vm is created from service offering with Direct Attach Network support")
    private List<String> networkGroupList;

    @Parameter(name="serviceofferingid", type=CommandType.LONG, required=true, description="the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @Parameter(name="size", type=CommandType.LONG, description="the arbitrary size for the DATADISK volume. Mutually exclusive with diskOfferingId")
    private Long size;

    @Parameter(name="templateid", type=CommandType.LONG, required=true, description="the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name="userdata", type=CommandType.STRING, description="an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Currently only HTTP GET is supported. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding.")
    private String userData;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true, description="availability zone for the virtual machine")
    private Long zoneId;

    // unexposed parameter needed for serializing/deserializing the command
    @Parameter(name="password", type=CommandType.STRING, expose=false)
    private String password;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getGroup() {
        return group;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public List<String> getNetworkGroupList() {
        return networkGroupList;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getSize() {
        return size;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getUserData() {
        return userData;
    }

    public Long getZoneId() {
        return zoneId;
    }

    // not exposed parameter
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
        return "virtualmachine";
    }
    
    @Override
    public long getAccountId() {
        Account account = UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = ApiDBUtils.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "deploying Vm";
    }

    @Override @SuppressWarnings("unchecked")
    public UserVmResponse getResponse() {
        UserVm userVm = (UserVm)getResponseObject();

        UserVmResponse response = new UserVmResponse();
        response.setId(userVm.getId());
        response.setName(userVm.getName());
        response.setCreated(userVm.getCreated());
        response.setZoneId(userVm.getDataCenterId());
        response.setZoneName(ApiDBUtils.findZoneById(userVm.getDataCenterId()).getName());
        response.setIpAddress(userVm.getPrivateIpAddress());
        response.setServiceOfferingId(userVm.getServiceOfferingId());
        response.setHaEnable(userVm.isHaEnabled());

        InstanceGroupVO group = ApiDBUtils.findInstanceGroupForVM(userVm.getId());
        if (group != null) {
            response.setGroup(group.getName());
            response.setGroupId(group.getId());
        }

        if (userVm.getDisplayName() == null || userVm.getDisplayName().length() == 0) {
            response.setDisplayName(userVm.getName());
        } else {
            response.setDisplayName(userVm.getDisplayName());
        }
        
        if (userVm.getState() != null) {
            response.setState(userVm.getState().toString());
        }

        VMTemplateVO template = ApiDBUtils.findTemplateById(userVm.getTemplateId());
        
        Account acct = ApiDBUtils.findAccountById(Long.valueOf(userVm.getAccountId()));
        if (acct != null) {
            response.setAccountName(acct.getAccountName());
            response.setDomainId(acct.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
        }

        Long userId = UserContext.current().getUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }

        //this is for the case where the admin deploys a vm for a normal user
        User userExecutingCmd = ApiDBUtils.findUserById(userId);
        Account acctForUserExecutingCmd = ApiDBUtils.findAccountById(Long.valueOf(userExecutingCmd.getAccountId()));
        if ((BaseCmd.isAdmin(acctForUserExecutingCmd.getType()) && (userVm.getHostId() != null)) || (BaseCmd.isAdmin(acct.getType()) && (userVm.getHostId() != null))) {
            response.setHostName(ApiDBUtils.findHostById(userVm.getHostId()).getName());
            response.setHostId(userVm.getHostId());
        }
            
        String templateName = "none";
        boolean templatePasswordEnabled = false;
        String templateDisplayText = null;
        
        if (template != null) {
            templateName = template.getName();
            templatePasswordEnabled = template.getEnablePassword();
            templateDisplayText = template.getDisplayText();
            if (templateDisplayText == null) {
                templateDisplayText = templateName;
            }
        }
        
        if (templatePasswordEnabled) { // FIXME:  where will the password come from in this case?
            response.setPassword(getPassword());
        } 
        
        // ISO Info
        Long isoId = userVm.getIsoId();
        if (isoId != null) {
            VMTemplateVO iso = ApiDBUtils.findTemplateById(isoId.longValue());
            if (iso != null) {
                response.setIsoId(isoId.longValue());
                response.setIsoName(iso.getName());
                response.setTemplateId(isoId.longValue());
                response.setTemplateName(iso.getName());

                templateDisplayText = iso.getDisplayText();
                if(templateDisplayText == null)
                    templateDisplayText = iso.getName();
                response.setIsoDisplayText(templateDisplayText);
                response.setTemplateDisplayText(templateDisplayText);
            }
        } else {
            response.setTemplateId(userVm.getTemplateId());
            response.setTemplateName(templateName);
            response.setTemplateDisplayText(templateDisplayText);
            response.setPasswordEnabled(templatePasswordEnabled);
        }
        
        ServiceOffering offering = ApiDBUtils.findServiceOfferingById(userVm.getServiceOfferingId());
        response.setServiceOfferingId(userVm.getServiceOfferingId());
        response.setServiceOfferingName(offering.getName());

        response.setCpuNumber(offering.getCpu());
        response.setCpuSpeed(offering.getSpeed());
        response.setMemory(offering.getRamSize());

        VolumeVO rootVolume = ApiDBUtils.findRootVolume(userVm.getId());
        if (rootVolume != null) {
            response.setRootDeviceId(rootVolume.getDeviceId());
            StoragePoolVO storagePool = ApiDBUtils.findStoragePoolById(rootVolume.getPoolId());
            response.setRootDeviceType(storagePool.getPoolType().toString());
        }

        response.setGuestOsId(userVm.getGuestOSId());

        response.setNetworkGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(userVm.getId()));

        response.setResponseName(getName());
        return response;
    }
}
