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
package org.apache.cloudstack.api.command.admin.address;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AcquirePodIpCmdResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "acquirePodIpAddresses", description = "Allocates IP addresses in respective Pod of a Zone", responseObject = AcquirePodIpCmdResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AcquirePodIpCmdByAdmin extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(AcquirePodIpCmdByAdmin.class.getName());
    private static final String s_name = "acquirepodipaddress";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the  zone in which your pod lies")
    private Long zoneId;

    @Parameter(name = ApiConstants.GUEST_CIDR_ADDRESS, type = CommandType.STRING, entityType = ZoneResponse.class, required = false, description = "CIDR for finding Pod")
    private String cidr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    private long getZoneId() {
        return zoneId;
    }

    private String getCidr() {
        return cidr;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException {
        AcquirePodIpCmdResponse pod_ip = null;
        pod_ip = _networkService.allocatePodIp(_accountService.getAccount(getEntityOwnerId()), getZoneId(), getCidr());
        if (pod_ip != null) {
            pod_ip.setResponseName(getCommandName());
            pod_ip.setObjectName(getCommandName());
            setResponseObject(pod_ip);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign IP address");
        }
    }

    @Override
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return CallContext.current().getCallingAccount().getAccountId();
    }

}