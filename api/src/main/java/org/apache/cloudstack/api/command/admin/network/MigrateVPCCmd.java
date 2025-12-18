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

import java.util.HashMap;
import java.util.Map;


import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.user.User;

@APICommand(name = "migrateVPC",
            description = "moves a vpc to another physical network",
            responseObject = VpcResponse.class,
            responseView = ResponseObject.ResponseView.Restricted,
            entityType = {Vpc.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false,
            since = "4.11.0",
            authorized = {RoleType.Admin})
public class MigrateVPCCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name= ApiConstants.VPC_ID, type=CommandType.UUID, entityType = VpcResponse.class,
            required=true, description = "the ID of the vpc")
    protected Long id;

    @Parameter(name = ApiConstants.VPC_OFF_ID, type = CommandType.UUID, entityType = VpcOfferingResponse.class, required=true, description = "vpc offering ID")
    private Long vpcOfferingId;

    @Parameter(name = ApiConstants.TIER_NETWORK_OFFERINGS, type = CommandType.MAP, description = "network offering ids for each network in the vpc. Example: tierNetworkOfferings[0].networkId=networkId1&tierNetworkOfferings[0].networkOfferingId=newNetworkofferingId1&tierNetworkOfferings[1].networkId=networkId2&tierNetworkOfferings[1].networkOfferingId=newNetworkofferingId2")
    private Map<Integer, HashMap<String, String>> tierNetworkOfferings;

    @Parameter(name = ApiConstants.RESUME, type = CommandType.BOOLEAN, description = "true if previous network migration cmd failed")
    private Boolean resume;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getVpcOfferingId() {
        return vpcOfferingId;
    }

    public Boolean getResume() {
        return resume == null ? false : resume;
    }

    public Map<String, String> getTierNetworkOfferings() {
        HashMap<String, String> flatMap = new HashMap<>();

        if (tierNetworkOfferings == null) {
            return flatMap;
        }

        for (HashMap<String, String> map : tierNetworkOfferings.values()) {
            flatMap.put(map.get("networkid"), map.get("networkofferingid"));
        }

        return flatMap;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        User callerUser = _accountService.getActiveUser(CallContext.current().getCallingUserId());
        Account callerAccount = _accountService.getActiveAccountById(callerUser.getAccountId());

        Vpc result =
                _networkService.migrateVpcNetwork(getId(), getVpcOfferingId(), getTierNetworkOfferings(), callerAccount, callerUser, getResume());

        if (result != null) {
            VpcResponse response = _responseGenerator.createVpcResponse(ResponseObject.ResponseView.Restricted, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate vpc");
        }
    }

    @Override
    public String getEventDescription() { return "Migrating vpc: " + getId() + " to new vpc offering (" + vpcOfferingId + ")";  }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_MIGRATE;
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return id;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

}
