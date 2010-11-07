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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmService;

@Implementation(method="startVirtualMachine", manager=UserVmService.class, description="Starts a virtual machine.")
public class StartVm2Cmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(StartVMCmd.class.getName());

    private static final String s_name = "startvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the virtual machine")
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
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        //TODO - put method stub here
        return null;
    }

    @Override
    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "virtualmachine";
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
        return EventTypes.EVENT_VM_START;
    }

    @Override
    public String getEventDescription() {
        return  "starting user vm: " + getId();
    }

	@Override @SuppressWarnings("unchecked")
	public UserVmResponse getResponse() {
	    UserVm vm = (UserVm)getResponseObject();

	    UserVmResponse response = new UserVmResponse();
	    response.setId(vm.getId());
	    response.setName(vm.getHostName());
	    response.setCreated(vm.getCreated());
	    response.setZoneId(vm.getDataCenterId());
	    response.setZoneName(ApiDBUtils.findZoneById(vm.getDataCenterId()).getName());
	    response.setIpAddress(vm.getPrivateIpAddress());
	    response.setServiceOfferingId(vm.getServiceOfferingId());
	    response.setHaEnable(vm.isHaEnabled());
        if (vm.getDisplayName() == null || vm.getDisplayName().length() == 0) {
            response.setDisplayName(vm.getHostName());
        } else {
            response.setDisplayName(vm.getDisplayName());
        }

        InstanceGroupVO group = ApiDBUtils.findInstanceGroupForVM(vm.getId());
        if (group != null) {
            response.setGroup(group.getName());
            response.setGroupId(group.getId());
        }

        if (vm.getState() != null) {
            response.setState(vm.getState().toString());
        }

        Account acct = ApiDBUtils.findAccountById(vm.getAccountId());
        if (acct != null) {
            response.setAccountName(acct.getAccountName());
            response.setDomainId(acct.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
        }

        if (BaseCmd.isAdmin(acct.getType()) && (vm.getHostId() != null)) {
            response.setHostName(ApiDBUtils.findHostById(vm.getHostId()).getName());
            response.setHostId(vm.getHostId());
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

        response.setTemplateId(vm.getTemplateId());
        response.setTemplateName(templateName);
        response.setTemplateDisplayText(templateDisplayText);
        response.setPasswordEnabled(templatePasswordEnabled);
        if (templatePasswordEnabled) {
            response.setPassword(null); // FIXME:  Where should password come from?  In the old framework, password was always passed
                                        //         in to composeResultObject() as null, so that behavior is preserved...
        } else {
            response.setPassword("");
        }

        String isoName = null;
        if (vm.getIsoId() != null) {
            VMTemplateVO iso = ApiDBUtils.findTemplateById(vm.getIsoId().longValue());
            if (iso != null) {
                isoName = iso.getName();
            }
        }

        response.setIsoId(vm.getIsoId());
        response.setIsoName(isoName);

        ServiceOffering offering = ApiDBUtils.findServiceOfferingById(vm.getServiceOfferingId());
        response.setServiceOfferingId(vm.getServiceOfferingId());
        response.setServiceOfferingName(offering.getName());

        response.setCpuNumber(offering.getCpu());
        response.setCpuSpeed(offering.getSpeed());
        response.setMemory(offering.getRamSize());

        VolumeVO rootVolume = ApiDBUtils.findRootVolume(vm.getId());
        if (rootVolume != null) {
            response.setRootDeviceId(rootVolume.getDeviceId());
            StoragePoolVO storagePool = ApiDBUtils.findStoragePoolById(rootVolume.getPoolId());
            response.setRootDeviceType(storagePool.getPoolType().toString());
        }

        response.setGuestOsId(vm.getGuestOSId());

        //Network groups
        response.setNetworkGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(vm.getId()));
        response.setObjectName("virtualmachine");
        response.setResponseName(getName());
        //response.setResponseName(getResultObjectName());
        return response;
	}
}
