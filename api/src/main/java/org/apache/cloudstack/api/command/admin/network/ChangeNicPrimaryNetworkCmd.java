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
package org.apache.cloudstack.api.command.admin.network;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "changeNicPrimaryNetwork", description = "Changes a multi-VLAN trunk NIC's primary (native/untagged) network to one of its already-associated networks. "
        + "Only allowed while the owning Instance is stopped. The operator is responsible for confirming the guest will "
        + "reacquire its address on the new primary network (e.g. via DHCP) after the next start - CloudStack cannot verify this from the host side.",
        responseObject = NicResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "24.0.0",
        authorized = {RoleType.Admin})
public class ChangeNicPrimaryNetworkCmd extends BaseAsyncCmd {
    private static final String s_name = "changenicprimarynetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = true,
            description = "the ID of the NIC whose primary network should be changed")
    private Long nicId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true,
            description = "the ID of the network to make primary; it must already be associated with this NIC")
    private Long networkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNicId() {
        return nicId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation ///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NIC_PRIMARY_NETWORK_CHANGE;
    }

    @Override
    public String getEventDescription() {
        return "Changing nic " + getResourceUuid(ApiConstants.NIC_ID) + "'s primary network to " + getResourceUuid(ApiConstants.NETWORK_ID);
    }

    private Nic getNic() {
        Nic nic = _entityMgr.findById(Nic.class, nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Can't find NIC for id specified");
        }
        return nic;
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, getNic().getInstanceId());
        if (vm == null) {
            return Account.ACCOUNT_ID_SYSTEM;
        }
        return vm.getAccountId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public Long getApiResourceId() {
        return getNic().getInstanceId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Nic ID: " + getResourceUuid(ApiConstants.NIC_ID));
        Nic result = _networkService.changeNicPrimaryNetwork(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to change nic's primary network. Refer to server logs for details.");
        }
        NicResponse response = _responseGenerator.createNicResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
