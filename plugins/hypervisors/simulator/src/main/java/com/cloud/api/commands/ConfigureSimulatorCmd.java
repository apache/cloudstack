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

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.agent.manager.SimulatorManager;
import com.cloud.api.response.MockResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.simulator.MockConfigurationVO;
import com.cloud.user.Account;

@APICommand(name = "configureSimulator", description = "configure simulator", responseObject = MockResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ConfigureSimulatorCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ConfigureSimulatorCmd.class.getName());
    private static final String s_name = "configuresimulatorresponse";

    @Inject
    SimulatorManager _simMgr;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class, description="configure range: in a zone")
    private Long zoneId;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.UUID, entityType=PodResponse.class, description="configure range: in a pod")
    private Long podId;

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.UUID, entityType=ClusterResponse.class, description="configure range: in a cluster")
    private Long clusterId;

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.UUID, entityType=HostResponse.class, description="configure range: in a host")
    private Long hostId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "which command needs to be configured")
    private String command;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.STRING, required = true, description = "configuration options for this command, which is separated by ;")
    private String values;

    @Parameter(name=ApiConstants.COUNT, type=CommandType.INTEGER, description="number of times the mock is active")
    private Integer count;

    @Parameter(name="jsonresponse", type=CommandType.STRING, description="agent command response to be returned", length=4096)
    private String jsonResponse;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        if (hostId != null && jsonResponse != null) {
            jsonResponse = jsonResponse.replace("\"hostId\":0", "\"hostId\":" + hostId);
        }
        Long id = _simMgr.configureSimulator(zoneId, podId, clusterId, hostId, command, values, count, jsonResponse);
        if (id == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to configure simulator");
        }

        MockConfigurationVO config = _simMgr.querySimulatorMock(id);
        if (config == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to query simulator mock");
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
