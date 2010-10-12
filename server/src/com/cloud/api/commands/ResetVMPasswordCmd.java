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
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroupVO;

@Implementation(method="resetVMPassword", manager=Manager.UserVmManager, description="Resets the password for virtual machine. " +
																					"The virtual machine must be in a \"Stopped\" state and the template must already " +
																					"support this feature for this command to take effect. [async]")
public class ResetVMPasswordCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(ResetVMPasswordCmd.class.getName());

	private static final String s_name = "resetpasswordforvirtualmachineresponse";
	
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long id;

    // unexposed parameter needed for serializing/deserializing the command
    @Parameter(name="password", type=CommandType.STRING, expose=false)
    private String password;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

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

    @Override
    public long getAccountId() {
        UserVm vm = ApiDBUtils.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_RESETPASSWORD;
    }

    @Override
    public String getEventDescription() {
        return  "resetting password for vm: " + getId();
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
        
        response.setNetworkGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(userVm.getId()));

        response.setResponseName(getName());
        return response;
	}
}
