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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress;
import com.cloud.user.Account;

@APICommand(name = "disassociateIpAddress", description = "Disassociates an IP address from the account.", responseObject = SuccessResponse.class,
 requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, entityType = { IpAddress.class })
public class DisassociateIPAddrCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = IPAddressResponse.class, description = "the ID of the public IP address"
        + " to disassociate. Mutually exclusive with the ipaddress parameter")
    private Long id;

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING,  since="4.19.0", description="IP Address to be disassociated."
        +  " Mutually exclusive with the id parameter")
    private String ipAddress;

    // unexposed parameter needed for events logging
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, expose = false)
    private Long ownerId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getIpAddressId() {
       if (id != null & ipAddress != null) {
           throw new InvalidParameterValueException("id parameter is mutually exclusive with ipaddress parameter");
       }

       if (id != null) {
            return id;
        } else if (ipAddress != null) {
            IpAddress ip = getIpAddressByIp(ipAddress);
            return ip.getId();
        }

        throw new InvalidParameterValueException("Please specify either IP address or IP address ID");
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws InsufficientAddressCapacityException {
        Long ipAddressId = getIpAddressId();
        CallContext.current().setEventDetails("IP ID: " + ipAddressId);
        boolean result = false;
        if (!isPortable()) {
            result = _networkService.releaseIpAddress(ipAddressId);
        } else {
            result = _networkService.releasePortableIpAddress(ipAddressId);
        }
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to disassociate IP address");
        }
    }

    @Override
    public String getEventType() {
        if (!isPortable()) {
            return EventTypes.EVENT_NET_IP_RELEASE;
        } else {
            return EventTypes.EVENT_PORTABLE_IP_RELEASE;
        }
    }

    @Override
    public String getEventDescription() {
        return ("Disassociating IP address with ID=" + id);
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            IpAddress ip = getIpAddress();
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
        IpAddress ip = getIpAddress();
        return ip.getAssociatedWithNetworkId();
    }

    private IpAddress getIpAddressById(Long id) {
        IpAddress ip = _entityMgr.findById(IpAddress.class, id);

        if (ip == null) {
            throw new InvalidParameterValueException("Unable to find IP address by ID=" + id);
        } else {
            return ip;
        }
    }

    private IpAddress getIpAddressByIp(String ipAddress) {
        IpAddress ip = _networkService.getIp(ipAddress);
        if (ip == null) {
            throw new InvalidParameterValueException("Unable to find IP address by IP address=" + ipAddress);
        } else {
            return ip;
        }
    }

    private IpAddress getIpAddress() {
        if (id != null & ipAddress != null) {
            throw new InvalidParameterValueException("id parameter is mutually exclusive with ipaddress parameter");
        }

        if (id != null) {
            return getIpAddressById(id);
        } else if (ipAddress != null){
            return getIpAddressByIp(ipAddress);
        }

        throw new InvalidParameterValueException("Please specify either IP address or IP address ID");
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.IpAddress;
    }

    @Override
    public Long getApiResourceId() {
        return getIpAddressId();
    }

    private boolean isPortable() {
        IpAddress ip = getIpAddress();
        return ip.isPortable();
    }
}
