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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = CreateTungstenFabricManagementNetworkCmd.APINAME, description = "create Tungsten-Fabric management network",
    responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo =
    false)
public class CreateTungstenFabricManagementNetworkCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTungstenFabricManagementNetworkCmd.class.getName());
    public static final String APINAME = "createTungstenFabricManagementNetwork";

    @Inject
    HostPodDao podDao;

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, required = true,
        description = "the ID of pod")
    private Long podId;

    public Long getPodId() {
        return podId;
    }

    public void setPodId(final Long podId) {
        this.podId = podId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        HostPodVO pod = podDao.findById(podId);

        if (!tungstenService.createManagementNetwork(pod.getDataCenterId())) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                "Unable to create Tungsten-Fabric management network");
        }

        if (!tungstenService.addManagementNetworkSubnet(pod)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                "Unable to add Tungsten-Fabric management network subnet");
        }

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("create Tungsten-Fabric management network successfully");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
