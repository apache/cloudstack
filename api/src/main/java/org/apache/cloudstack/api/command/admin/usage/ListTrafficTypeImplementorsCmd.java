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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TrafficTypeImplementorResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Networks.TrafficType;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listTrafficTypeImplementors",
            description = "Lists implementors of implementor of a network traffic type or implementors of all network traffic types",
            responseObject = TrafficTypeImplementorResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListTrafficTypeImplementorsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListTrafficTypeImplementorsCmd.class);

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.TRAFFIC_TYPE,
               type = CommandType.STRING,
               description = "Optional. The network traffic type, if specified, return its implementor. Otherwise, return all traffic types with their implementor")
    private String trafficType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getTrafficType() {
        return trafficType;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        List<Pair<TrafficType, String>> results = _networkService.listTrafficTypeImplementor(this);
        ListResponse<TrafficTypeImplementorResponse> response = new ListResponse<TrafficTypeImplementorResponse>();
        List<TrafficTypeImplementorResponse> responses = new ArrayList<TrafficTypeImplementorResponse>();
        for (Pair<TrafficType, String> r : results) {
            TrafficTypeImplementorResponse p = new TrafficTypeImplementorResponse();
            p.setTrafficType(r.first().toString());
            p.setImplementor(r.second());
            p.setObjectName("traffictypeimplementorresponse");
            responses.add(p);
        }

        response.setResponses(responses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    }
