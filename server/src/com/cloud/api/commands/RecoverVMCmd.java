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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.response.UserVmResponse;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroupVO;

@Implementation(method="recoverVirtualMachine", manager=Manager.UserVmManager, description="Recovers a virtual machine.")
public class RecoverVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RecoverVMCmd.class.getName());

    private static final String s_name = "recovervirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

	@Override @SuppressWarnings("unchecked")
	public UserVmResponse getResponse() {
        Boolean success = (Boolean)getResponseObject();
        UserVmResponse recoverVmResponse = new UserVmResponse();
        UserVm vm = ApiDBUtils.findUserVmById(id);        
        recoverVmResponse.setSuccess(success);
        recoverVmResponse.setResponseName(getName());
        recoverVmResponse.setId(vm.getId());
        recoverVmResponse.setName(vm.getName());
        recoverVmResponse.setCreated(vm.getCreated());
        recoverVmResponse.setZoneId(vm.getDataCenterId());
        recoverVmResponse.setZoneName(ApiDBUtils.findZoneById(vm.getDataCenterId()).getName());
        recoverVmResponse.setIpAddress(vm.getPrivateIpAddress());
        recoverVmResponse.setServiceOfferingId(vm.getServiceOfferingId());
        recoverVmResponse.setHaEnable(vm.isHaEnabled());
        if (vm.getDisplayName() == null || vm.getDisplayName().length() == 0) {
        	recoverVmResponse.setDisplayName(vm.getName());
        } else {
        	recoverVmResponse.setDisplayName(vm.getDisplayName());
        }

        InstanceGroupVO group = ApiDBUtils.findInstanceGroupForVM(vm.getId());
        if (group != null) {
        	recoverVmResponse.setGroup(group.getName());
        	recoverVmResponse.setGroupId(group.getId());
        }

        if (vm.getState() != null) {
        	recoverVmResponse.setState(vm.getState().toString());
        }

        Account acct = ApiDBUtils.findAccountById(vm.getAccountId());
        if (acct != null) {
        	recoverVmResponse.setAccountName(acct.getAccountName());
        	recoverVmResponse.setDomainId(acct.getDomainId());
        	recoverVmResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
        }

        if (BaseCmd.isAdmin(acct.getType()) && (vm.getHostId() != null)) {
        	recoverVmResponse.setHostName(ApiDBUtils.findHostById(vm.getHostId()).getName());
        	recoverVmResponse.setHostId(vm.getHostId());
        }
        
        String templateName = "ISO Boot";
        boolean templatePasswordEnabled = false;
        String templateDisplayText = "ISO Boot";
        
        VMTemplateVO template = ApiDBUtils.findTemplateById(vm.getTemplateId());
        if (template != null) {
            templateName = template.getName();
            templatePasswordEnabled = template.getEnablePassword();
            templateDisplayText = template.getDisplayText();
             if (templateDisplayText == null) {
                templateDisplayText = templateName;
             }
        }

        recoverVmResponse.setTemplateId(vm.getTemplateId());
        recoverVmResponse.setTemplateName(templateName);
        recoverVmResponse.setTemplateDisplayText(templateDisplayText);
        recoverVmResponse.setPasswordEnabled(templatePasswordEnabled);
        if (templatePasswordEnabled) {
        	recoverVmResponse.setPassword(null); // FIXME:  Where should password come from?  In the old framework, password was always passed
                                        //         in to composeResultObject() as null, so that behavior is preserved...
        } else {
        	recoverVmResponse.setPassword("");
        }

        String isoName = null;
        if (vm.getIsoId() != null) {
            VMTemplateVO iso = ApiDBUtils.findTemplateById(vm.getIsoId().longValue());
            if (iso != null) {
                isoName = iso.getName();
            }
        }

        recoverVmResponse.setIsoId(vm.getIsoId());
        recoverVmResponse.setIsoName(isoName);

        ServiceOffering offering = ApiDBUtils.findServiceOfferingById(vm.getServiceOfferingId());
        recoverVmResponse.setServiceOfferingId(vm.getServiceOfferingId());
        recoverVmResponse.setServiceOfferingName(offering.getName());

        recoverVmResponse.setCpuNumber(offering.getCpu());
        recoverVmResponse.setCpuSpeed(offering.getSpeed());
        recoverVmResponse.setMemory(offering.getRamSize());
        
        //Network groups
        recoverVmResponse.setNetworkGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(vm.getId()));

        recoverVmResponse.setResponseName(getName());
        return recoverVmResponse;
	}
}
