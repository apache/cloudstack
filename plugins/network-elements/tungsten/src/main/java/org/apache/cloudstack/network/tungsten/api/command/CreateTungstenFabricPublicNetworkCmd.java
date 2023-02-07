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

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.user.Account;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import java.util.List;

import javax.inject.Inject;

@APICommand(name = CreateTungstenFabricPublicNetworkCmd.APINAME, description = "create Tungsten-Fabric public network",
    responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTungstenFabricPublicNetworkCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTungstenFabricPublicNetworkCmd.class.getName());

    public static final String APINAME = "createTungstenFabricPublicNetwork";

    @Inject
    VlanDao vlanDao;
    @Inject
    NetworkModel networkModel;

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        SearchCriteria<VlanVO> sc = vlanDao.createSearchCriteria();
        sc.setParameters("network_id", publicNetwork.getId());
        List<VlanVO> pubVlanVOList = vlanDao.listVlansByNetworkId(publicNetwork.getId());

        if (!tungstenService.createPublicNetwork(zoneId)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to create Tungsten-Fabric public network");
        }

        for (VlanVO vlanVO : pubVlanVOList) {
            if (!tungstenService.addPublicNetworkSubnet(vlanVO)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to add Tungsten-Fabric public network subnet");
            }
        }

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("create Tungsten-Fabric public network successfully");
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
