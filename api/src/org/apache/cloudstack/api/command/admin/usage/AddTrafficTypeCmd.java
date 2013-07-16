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
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.TrafficTypeResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.user.Account;

@APICommand(name = "addTrafficType", description="Adds traffic type to a physical network", responseObject=TrafficTypeResponse.class, since="3.0.0")
public class AddTrafficTypeCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AddTrafficTypeCmd.class.getName());

    private static final String s_name = "addtraffictyperesponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            required=true, description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, required=true, description="the trafficType to be added to the physical network")
    private String trafficType;

    @Parameter(name=ApiConstants.XEN_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a XenServer host")
    private String xenLabel;

    @Parameter(name=ApiConstants.KVM_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a KVM host")
    private String kvmLabel;

    @Parameter(name=ApiConstants.VMWARE_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a VMware host")
    private String vmwareLabel;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="The VLAN id to be used for Management traffic by VMware host")
    private String vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getTrafficType() {
        return trafficType;
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

    public String getSimulatorLabel() {
        //simulators will have no labels
        return null;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getVlan() {
        return vlan;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("TrafficType Id: "+getEntityId());
        PhysicalNetworkTrafficType result = _networkService.getPhysicalNetworkTrafficType(getEntityId());
        if (result != null) {
            TrafficTypeResponse response = _responseGenerator.createTrafficTypeResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add traffic type to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        PhysicalNetworkTrafficType result = _networkService.addTrafficTypeToPhysicalNetwork(getPhysicalNetworkId(), getTrafficType(), getXenLabel(), getKvmLabel(), getVmwareLabel(), getSimulatorLabel(), getVlan());
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add traffic type to physical network");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TRAFFIC_TYPE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "Adding physical network traffic type: " + getEntityId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.TrafficType;
    }
}
