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
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;

@APICommand(name = "updateNetwork", description = "Updates a network", responseObject = NetworkResponse.class, responseView = ResponseView.Full, entityType = {Network.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateNetworkCmdByAdmin extends UpdateNetworkCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkCmdByAdmin.class.getName());

    @Parameter(name= ApiConstants.HIDE_IP_ADDRESS_USAGE, type=CommandType.BOOLEAN, description="when true ip address usage for the network will not be exported by the listUsageRecords API")
    private Boolean hideIpAddressUsage;

    public Boolean getHideIpAddressUsage() {
        if (hideIpAddressUsage == null) {
            return false;
        }
        return hideIpAddressUsage;
    }

    @Override
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException{
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Couldn't find network by id");
        }

        Network result = _networkService.updateGuestNetwork(this);
        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(ResponseView.Full, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update network");
        }
    }

}
