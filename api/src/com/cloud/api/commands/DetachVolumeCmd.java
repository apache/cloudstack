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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.VolumeResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;

@Implementation(description="Detaches a disk volume from a virtual machine.", responseObject=VolumeResponse.class)
public class DetachVolumeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(DetachVolumeCmd.class.getName());
    private static final String s_name = "detachvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the disk volume")
    private Long id;

    @Parameter(name=ApiConstants.DEVICE_ID, type=CommandType.LONG, description="the device ID on the virtual machine where volume is detached from")
    private Long deviceId;

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="the ID of the virtual machine where the volume is detached from")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "volume";
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Volume;
    }
    
    public Long getInstanceId() {
    	return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Long volumeId = getId();
        if (volumeId != null) {
            Volume volume = _responseGenerator.findVolumeById(volumeId);
            if (volume != null) {
                return volume.getAccountId();
            }
        } else if (getVirtualMachineId() != null) {
            UserVm vm = _responseGenerator.findUserVmById(getVirtualMachineId());
            if (vm != null) {
                return vm.getAccountId();
            }
        }

        // invalid id, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_DETACH;
    }

    @Override
    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        if (id != null) {
            sb.append(": " + id);
        } else if ((deviceId != null) && (virtualMachineId != null)) {
            sb.append(" with device id: " + deviceId + " from vm: " + virtualMachineId);
        } else {
            sb.append(" <error:  either volume id or deviceId/vmId need to be specified>");
        }
        return  "detaching volume" + sb.toString();
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Volume Id: "+getId()+" VmId: "+getVirtualMachineId());
        Volume result = _userVmService.detachVolumeFromVM(this);
        if (result != null){
            VolumeResponse response = _responseGenerator.createVolumeResponse(result);
            response.setResponseName("volume");
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to detach volume");
        }
    }
}