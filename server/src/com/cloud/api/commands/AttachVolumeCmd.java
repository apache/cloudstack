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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.VolumeResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmManager;

@Implementation(method="attachVolumeToVM", manager=UserVmManager.class, description="Attaches a disk volume to a virtual machine.")
public class AttachVolumeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(AttachVolumeCmd.class.getName());
    private static final String s_name = "attachvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DEVICE_ID, type=CommandType.LONG, description="the ID of the device to map the volume to within the guest OS. " +
    																"If no deviceId is passed in, the next available deviceId will be chosen. " +
    																"Possible values for a Linux OS are:" +
    																"* 1 - /dev/xvdb" +
    																"* 2 - /dev/xvdc" +
    																"* 4 - /dev/xvde" +
    																"* 5 - /dev/xvdf" +
    																"* 6 - /dev/xvdg" +
    																"* 7 - /dev/xvdh" +
    																"* 8 - /dev/xvdi" +
    																"* 9 - /dev/xvdj")
    private Long deviceId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the disk volume")
    private Long id;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="	the ID of the virtual machine")
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
	    response.setVolumeType(volume.getVolumeType().toString());
	    response.setResponseName(getName());

		return response;
	}
}
