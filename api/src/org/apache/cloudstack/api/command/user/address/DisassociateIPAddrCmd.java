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
package org.apache.cloudstack.api.command.user.address;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress;
import com.cloud.user.Account;

@APICommand(name = "disassociateIpAddress", description="Disassociates an ip address from the account.", responseObject=SuccessResponse.class)
public class DisassociateIPAddrCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DisassociateIPAddrCmd.class.getName());

    private static final String s_name = "disassociateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID,entityType = IPAddressResponse.class,
            required=true, description="the id of the public ip address" +
            " to disassociate")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType = AccountResponse.class,
            expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getIpAddressId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws InsufficientAddressCapacityException{
        CallContext.current().setEventDetails("Ip Id: " + getIpAddressId());
        boolean result = false;
        if (!isPortable(id)) {
            result = _networkService.releaseIpAddress(getIpAddressId());
        } else {
            result = _networkService.releasePortableIpAddress(getIpAddressId());
        }
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to disassociate ip address");
        }
    }

    @Override
    public String getEventType() {
        if (!isPortable(id)) {
            return EventTypes.EVENT_NET_IP_RELEASE;
        } else {
            return EventTypes.EVENT_PORTABLE_IP_RELEASE;
        }
    }

    @Override
    public String getEventDescription() {
        return  ("Disassociating ip address with id=" + id);
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            IpAddress ip = getIpAddress(id);
            if (ip == null) {
                throw new InvalidParameterValueException("Unable to find ip address by id=" + id);
            }
            ownerId = ip.getAccountId();
        }

        if (ownerId == null) {
            return Account.ACCOUNT_ID_SYSTEM;
        }
        return ownerId;
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        IpAddress ip = getIpAddress(id);
        return ip.getAssociatedWithNetworkId();
    }

    private IpAddress getIpAddress(long id) {
        IpAddress ip = _entityMgr.findById(IpAddress.class, id);

        if (ip == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id=" + id);
        } else {
            return ip;
        }
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IpAddress;
    }

    @Override
    public Long getInstanceId() {
        return getIpAddressId();
    }

    private boolean isPortable(long id) {
        IpAddress ip = getIpAddress(id);
        return  ip.isPortable();
    }
}
