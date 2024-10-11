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

import com.cloud.dc.DataCenter;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricLBHealthMonitorResponse;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;
import org.apache.cloudstack.network.tungsten.service.TungstenService;

import javax.inject.Inject;

@APICommand(name = UpdateTungstenFabricLBHealthMonitorCmd.APINAME, description = "update Tungsten-Fabric loadbalancer health monitor",
    responseObject = TungstenFabricLBHealthMonitorResponse.class, requestHasSensitiveInfo = false,
    responseHasSensitiveInfo = false)
public class UpdateTungstenFabricLBHealthMonitorCmd extends BaseAsyncCreateCmd {
    public static final String APINAME = "updateTungstenFabricLBHealthMonitor";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.LBID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, required = true, description = "the ID of lb rule")
    private Long lbId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "loadbalancer health monitor type")
    private String type;

    @Parameter(name = ApiConstants.RETRY, type = CommandType.INTEGER, required = true, description = "loadbalancer health monitor retry")
    private int retry;

    @Parameter(name = ApiConstants.TIMEOUT, type = CommandType.INTEGER, required = true, description = "loadbalancer health monitor timeout")
    private int timeout;

    @Parameter(name = ApiConstants.INTERVAL, type = CommandType.INTEGER, required = true, description = "loadbalancer health monitor interval")
    private int interval;

    @Parameter(name = ApiConstants.HTTP_METHOD, type = CommandType.STRING, description = "loadbalancer health monitor http method")
    private String httpMethod;

    @Parameter(name = ApiConstants.EXPECTED_CODE, type = CommandType.STRING, description = "loadbalancer health monitor expected code")
    private String expectedCode;

    @Parameter(name = ApiConstants.URL_PATH, type = CommandType.STRING, description = "loadbalancer health monitor url path")
    private String urlPath;

    @Override
    public void create() throws ResourceAllocationException {
        TungstenService.MonitorType monitorType;
        try {
            monitorType = TungstenService.MonitorType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid parameter:" + type);
        }

        if (monitorType == TungstenService.MonitorType.HTTP) {
            if (httpMethod == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid http method parameter");
            }

            try {
                TungstenService.HttpType.valueOf(httpMethod);
            } catch (IllegalArgumentException e) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid parameter:" + httpMethod);
            }

            if (expectedCode == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid expected code parameter");
            }

            if (urlPath == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid url path parameter");
            }
        }

        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO =
            tungstenService.updateTungstenFabricLBHealthMonitor(
            lbId, type, retry, timeout, interval, httpMethod, expectedCode, urlPath);

        if (tungstenFabricLBHealthMonitorVO != null) {
            setEntityId(tungstenFabricLBHealthMonitorVO.getId());
            setEntityUuid(tungstenFabricLBHealthMonitorVO.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                "Failed to create Tungsten Fabric Health Monitor entity");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_UPDATE_LB_HEALTH_MONITOR;
    }

    @Override
    public String getEventDescription() {
        return "update Tungsten-Fabric LoadBalancer health monitor";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO;
        boolean result = tungstenService.applyLBHealthMonitor(lbId);

        if (result) {
            tungstenFabricLBHealthMonitorVO = _entityMgr.findById(TungstenFabricLBHealthMonitorVO.class, getEntityId());
            LoadBalancer loadBalancer = _entityMgr.findById(LoadBalancer.class, lbId);
            Network network = _entityMgr.findById(Network.class, loadBalancer.getNetworkId());
            DataCenter dataCenter = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
            TungstenFabricLBHealthMonitorResponse response = new TungstenFabricLBHealthMonitorResponse(
                    tungstenFabricLBHealthMonitorVO, dataCenter);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }

        if (!result) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update health monitor");
        }
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
