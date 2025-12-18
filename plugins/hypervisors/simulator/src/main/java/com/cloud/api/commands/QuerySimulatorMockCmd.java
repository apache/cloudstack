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
package com.cloud.api.commands;

import com.cloud.agent.manager.SimulatorManager;
import com.cloud.api.response.MockResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.simulator.MockConfigurationVO;
import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;

import javax.inject.Inject;


@APICommand(name = "querySimulatorMock", description="query simulator mock", responseObject=MockResponse.class)
public class QuerySimulatorMockCmd extends BaseCmd {
    private static final String s_name = "querysimulatormockresponse";

    @Inject SimulatorManager _simMgr;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="id of the configured mock")
    private Long id;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        MockConfigurationVO config = _simMgr.querySimulatorMock(id);
        if (config == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to query mock");
        }

        MockResponse response = new MockResponse();
        response.setId(config.getId());
        response.setZoneId(config.getDataCenterId());
        response.setPodId(config.getPodId());
        response.setClusterId(config.getClusterId());
        response.setHostId(config.getHostId());
        response.setName(config.getName());
        response.setCount(config.getCount());
        response.setResponseName("simulatormock");
        this.setResponseObject(response);
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
