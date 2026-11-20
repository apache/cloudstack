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
package org.apache.cloudstack.api.command.admin.nic;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.PacketCaptureResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.packetcapture.PacketCaptureService;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;

@APICommand(name = "getPacketCaptureStatus", responseObject = PacketCaptureResponse.class, entityType = {Nic.class},
        responseHasSensitiveInfo = false,
        requestHasSensitiveInfo = false,
        description = "Returns whether packet capture is enabled on an Instance NIC and whether a capture is " +
                "currently running on the host. Only supported on KVM.",
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class GetPacketCaptureStatusCmd extends BaseCmd {

    @Inject
    private PacketCaptureService packetCaptureService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = true,
            description = "The ID of the NIC to get the packet capture status of")
    private Long nicId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getNicId() {
        return nicId;
    }

    /////////////////////////////////////////////////////
    /////////////////// Implementation //////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            PacketCaptureResponse response = packetCaptureService.getPacketCaptureStatus(getNicId());
            response.setObjectName("packetcapture");
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
