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

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.User;

@APICommand(name = "migrateNetwork", description = "moves a network to another physical network",
            responseObject = NetworkResponse.class,
            responseView = ResponseView.Restricted,
            entityType = {Network.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false,
            since = "4.11.0",
            authorized = {RoleType.Admin})
public class MigrateNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateNetworkCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "the ID of the network")
    protected Long id;

    @Parameter(name = ApiConstants.NETWORK_OFFERING_ID, type = CommandType.UUID, entityType = NetworkOfferingResponse.class, required = true, description = "network offering ID")
    private Long networkOfferingId;

    @Parameter(name = ApiConstants.RESUME, type = CommandType.BOOLEAN, description = "true if previous network migration cmd failed")
    private Boolean resume;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public Boolean getResume() {
        return resume != null ? resume : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Networkd id=" + id + " doesn't exist");
        } else {
            return _networkService.getNetwork(id).getAccountId();
        }
    }

    @Override
    public void execute() {
        User callerUser = _accountService.getActiveUser(CallContext.current().getCallingUserId());
        Account callerAccount = _accountService.getActiveAccountById(callerUser.getAccountId());
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Couldn't find network by id");
        }

        Network result =
            _networkService.migrateGuestNetwork(getId(), getNetworkOfferingId(), callerAccount, callerUser, getResume());

        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(ResponseView.Restricted, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update network");
        }
    }

    @Override
    public String getEventDescription() {
        StringBuilder eventMsg = new StringBuilder("Migrating network: " + getId());
        if (getNetworkOfferingId() != null) {
            Network network = _networkService.getNetwork(getId());
            if (network == null) {
                throw new InvalidParameterValueException("Network id=" + id + " doesn't exist");
            }
            if (network.getNetworkOfferingId() != getNetworkOfferingId()) {
                NetworkOffering oldOff = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
                NetworkOffering newOff = _entityMgr.findById(NetworkOffering.class, getNetworkOfferingId());
                if (newOff == null) {
                    throw new InvalidParameterValueException("Network offering id supplied is invalid");
                }

                eventMsg.append(". Original network offering id: " + oldOff.getUuid() + ", new network offering id: " + newOff.getUuid());
            }
        }

        return eventMsg.toString();
    }

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

}
