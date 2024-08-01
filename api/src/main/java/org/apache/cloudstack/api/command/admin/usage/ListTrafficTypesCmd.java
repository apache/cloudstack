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
package org.apache.cloudstack.api.command.admin.usage;

import java.util.ArrayList;
import java.util.List;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.api.response.TrafficTypeResponse;

import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listTrafficTypes", description = "Lists traffic types of a given physical network.", responseObject = ProviderResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListTrafficTypesCmd extends BaseListCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               required = true,
               description = "the Physical Network ID")
    private Long physicalNetworkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Pair<List<? extends PhysicalNetworkTrafficType>, Integer> trafficTypes = _networkService.listTrafficTypes(getPhysicalNetworkId());
        ListResponse<TrafficTypeResponse> response = new ListResponse<TrafficTypeResponse>();
        List<TrafficTypeResponse> trafficTypesResponses = new ArrayList<TrafficTypeResponse>();
        if(trafficTypes != null) {
            for (PhysicalNetworkTrafficType trafficType : trafficTypes.first()) {
                TrafficTypeResponse trafficTypeResponse = _responseGenerator.createTrafficTypeResponse(trafficType);
                trafficTypesResponses.add(trafficTypeResponse);
            }
            response.setResponses(trafficTypesResponses, trafficTypes.second());
            response.setResponseName(getCommandName());
        }
        this.setResponseObject(response);
    }
}
