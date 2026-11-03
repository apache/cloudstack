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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;

import com.cloud.offering.NetworkOffering;

@APICommand(name = "createNetworkOffering", description = "Creates a network offering.", responseObject = NetworkOfferingResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateNetworkOfferingCmd extends NetworkOfferingBaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TRAFFIC_TYPE,
            type = CommandType.STRING,
            required = true,
            description = "The traffic type for the network offering. Supported type in current release is GUEST only")
    private String traffictype;

    @Parameter(name = ApiConstants.GUEST_IP_TYPE, type = CommandType.STRING, required = true, description = "Guest type of the network offering: Shared or Isolated")
    private String guestIptype;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getTraffictype() {
        return traffictype;
    }

    public String getGuestIpType() {
        return guestIptype;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        NetworkOffering result = _configService.createNetworkOffering(this);
        if (result != null) {
            NetworkOfferingResponse response = _responseGenerator.createNetworkOfferingResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create network offering");
        }
    }
}
