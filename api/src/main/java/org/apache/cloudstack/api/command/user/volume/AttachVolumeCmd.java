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
package org.apache.cloudstack.api.command.user.volume;

import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "attachVolume", description = "Attaches a disk volume to a virtual machine.", responseObject = VolumeResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AttachVolumeCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(AttachVolumeCmd.class.getName());
    private static final String s_name = "attachvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DEVICE_ID, type = CommandType.LONG, description = "The ID of the device to map the volume to the guest OS. "
        + "If no deviceID is informed, the next available deviceID will be chosen. Use 0 when volume needs to be attached as ROOT.<br>When using a linux operating system and the hypervisor XenServer, the devices IDs will be mapped as follows:"
        + "<ul><li>0 maps to /dev/xvda;</li><li>1 maps to /dev/xvdb;</li><li>2 maps /dev/xvdc and so on.</li></ul>"
        + "Please refer to the docs of your hypervisor for the correct mapping of the deviceID and the actual logical disk structure.")
    private Long deviceId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "the ID of the disk volume")
    private Long id;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="    the ID of the virtual machine")
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
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Volume;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _responseGenerator.findVolumeById(getId());
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
        return  "attaching volume: " + this._uuidMgr.getUuid(Volume.class, getId()) + " to vm: " + this._uuidMgr.getUuid(VirtualMachine.class, getVirtualMachineId());
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Volume Id: " + this._uuidMgr.getUuid(Volume.class, getId()) + " VmId: " + this._uuidMgr.getUuid(VirtualMachine.class, getVirtualMachineId()));
        Volume result = _volumeService.attachVolumeToVM(this);
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(getResponseView(), result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to attach volume");
        }
    }
}
