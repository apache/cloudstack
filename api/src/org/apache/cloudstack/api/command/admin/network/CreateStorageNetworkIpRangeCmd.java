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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StorageNetworkIpRangeResponse;

import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "createStorageNetworkIpRange",
            description = "Creates a Storage network IP range.",
            responseObject = StorageNetworkIpRangeResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class CreateStorageNetworkIpRangeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateStorageNetworkIpRangeCmd.class);

    private static final String s_name = "createstoragenetworkiprangeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.POD_ID,
               type = CommandType.UUID,
               entityType = PodResponse.class,
               required = true,
               description = "UUID of pod where the ip range belongs to")
    private Long podId;

    @Parameter(name = ApiConstants.START_IP, type = CommandType.STRING, required = true, description = "the beginning IP address")
    private String startIp;

    @Parameter(name = ApiConstants.END_IP, type = CommandType.STRING, description = "the ending IP address")
    private String endIp;

    @Parameter(name = ApiConstants.VLAN,
               type = CommandType.INTEGER,
               description = "Optional. The vlan the ip range sits on, default to Null when it is not specificed which means you network is not on any Vlan. This is mainly for Vmware as other hypervisors can directly reterive bridge from pyhsical network traffic type table")
    private Integer vlan;

    @Parameter(name = ApiConstants.NETMASK, type = CommandType.STRING, required = true, description = "the netmask for storage network")
    private String netmask;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, required = true, description = "the gateway for storage network")
    private String gateway;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEndIp() {
        return endIp;
    }

    public Long getPodId() {
        return podId;
    }

    public String getStartIp() {
        return startIp;
    }

    public Integer getVlan() {
        return vlan;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getGateWay() {
        return gateway;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_STORAGE_IP_RANGE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating storage ip range from " + getStartIp() + " to " + getEndIp() + " with vlan " + getVlan();
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            StorageNetworkIpRange result = _storageNetworkService.createIpRange(this);
            StorageNetworkIpRangeResponse response = _responseGenerator.createStorageNetworkIpRangeResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn("Create storage network IP range failed", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
