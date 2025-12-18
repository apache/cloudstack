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
package org.apache.cloudstack.api.command.admin.usage;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.TrafficTypeResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.user.Account;

@APICommand(name = "updateTrafficType", description = "Updates traffic type of a physical network", responseObject = TrafficTypeResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateTrafficTypeCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TrafficTypeResponse.class, required = true, description = "traffic type id")
    private Long id;

    @Parameter(name = ApiConstants.XENSERVER_NETWORK_LABEL,
               type = CommandType.STRING,
               description = "The network name label of the physical device dedicated to this traffic on a XenServer host")
    private String xenLabel;

    @Parameter(name = ApiConstants.KVM_NETWORK_LABEL,
               type = CommandType.STRING,
               description = "The network name label of the physical device dedicated to this traffic on a KVM host")
    private String kvmLabel;

    @Parameter(name = ApiConstants.VMWARE_NETWORK_LABEL,
               type = CommandType.STRING,
               description = "The network name label of the physical device dedicated to this traffic on a VMware host")
    private String vmwareLabel;

    @Parameter(name = ApiConstants.HYPERV_NETWORK_LABEL,
               type = CommandType.STRING,
               description = "The network name label of the physical device dedicated to this traffic on a Hyperv host")
    private String hypervLabel;

    @Parameter(name = ApiConstants.OVM3_NETWORK_LABEL,
               type = CommandType.STRING,
               description = "The network name of the physical device dedicated to this traffic on an OVM3 host")
    private String ovm3Label;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getXenLabel() {
        return xenLabel;
    }

    public String getKvmLabel() {
        return kvmLabel;
    }

    public String getVmwareLabel() {
        return vmwareLabel;
    }

    public String getHypervLabel() {
        return hypervLabel;
    }

    public String getOvm3Label() {
        return ovm3Label;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        PhysicalNetworkTrafficType result = _networkService.updatePhysicalNetworkTrafficType(getId(), getXenLabel(), getKvmLabel(), getVmwareLabel(), getHypervLabel(), getOvm3Label());
        if (result != null) {
            TrafficTypeResponse response = _responseGenerator.createTrafficTypeResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update traffic type");
        }
    }

    @Override
    public String getEventDescription() {
        return "Updating Traffic Type: " + getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TRAFFIC_TYPE_UPDATE;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.TrafficType;
    }
}
