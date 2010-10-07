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
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.VolumeResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@Implementation(method="attachVolumeToVM", manager=Manager.UserVmManager, description="Attaches a disk volume to a virtual machine.")
public class AttachVolumeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(AttachVolumeCmd.class.getName());
    private static final String s_name = "attachvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="deviceid", type=CommandType.LONG)
    private Long deviceId;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="virtualmachineid", type=CommandType.LONG, required=true)
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
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
        VolumeVO volume = ApiDBUtils.findVolumeById(getId());
        if (volume == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return volume.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_ATTACH;
    }

    @Override
    public String getEventDescription() {
        return  "attaching volume: " + getId() + " to vm: " + getVirtualMachineId();
    }

	@Override @SuppressWarnings("unchecked")
	public VolumeResponse getResponse() {
	    VolumeVO volume = ApiDBUtils.findVolumeById(id);
	    VolumeResponse response = new VolumeResponse();
	    UserVm instance = ApiDBUtils.findUserVmById(volume.getInstanceId());
	    response.setVirtualMachineName(instance.getName());
	    response.setVirtualMachineDisplayName(instance.getDisplayName());
	    response.setVirtualMachineId(instance.getId());
	    response.setVirtualMachineState(instance.getState().toString());
	    response.setStorageType("shared"); // NOTE: You can never attach a local disk volume but if that changes, we need to change this
	    response.setId(volume.getId());
	    response.setName(volume.getName());
	    response.setResponseName(getName());

		return response;
	}
}
