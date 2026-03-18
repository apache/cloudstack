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
package org.apache.cloudstack.api.command.user.vm;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

import java.util.ArrayList;
import java.util.EnumSet;

@APICommand(name = "updateVmNic", description = "Updates the specified VM NIC", responseObject = NicResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = { RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User })
public class UpdateVmNicCmd extends BaseAsyncCmd {

    @ACL
    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = true, description = "NIC ID")
    private Long nicId;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, description = "If true, sets the NIC state to UP; otherwise, sets the NIC state to DOWN")
    private Boolean enabled;

    public Long getNicId() {
        return nicId;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NIC_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Updating NIC %s.", getResourceUuid(ApiConstants.NIC_ID));
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmByNicId(nicId);
        if (vm == null) {
            return Account.ACCOUNT_ID_SYSTEM;
        }
        return vm.getAccountId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails(String.format("NIC ID: %s", getResourceUuid(ApiConstants.NIC_ID)));

        UserVm result = _userVmService.updateVirtualMachineNic(this);

        ArrayList<ApiConstants.VMDetails> dc = new ArrayList<>();
        dc.add(ApiConstants.VMDetails.valueOf("nics"));
        EnumSet<ApiConstants.VMDetails> details = EnumSet.copyOf(dc);

        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseObject.ResponseView.Restricted, "virtualmachine", details, result).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update NIC from VM.");
        }
    }
}
