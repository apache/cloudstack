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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import com.cloud.api.response.NetscalerControlCenterResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetScalerControlCenterVO;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listNetscalerControlCenter", responseObject = NetscalerControlCenterResponse.class, description = "list control center", requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class ListNetscalerControlCenterCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListNetscalerControlCenterCmd.class.getName());
    private static final String s_name = "listNetscalerControlCenter";

    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;

    public static String getsName() {
        return s_name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException {
        try {
            List<NetScalerControlCenterVO> lncCenters = _netsclarLbService.listNetscalerControlCenter(this);
            if (lncCenters != null) {
                ListResponse<NetscalerControlCenterResponse> response = new ListResponse<NetscalerControlCenterResponse>();
                List<NetscalerControlCenterResponse> lncCentersResponse = new ArrayList<NetscalerControlCenterResponse>();
                if (lncCenters != null && !lncCenters.isEmpty()) {
                    for (NetScalerControlCenterVO lncCentersVO : lncCenters) {
                        NetscalerControlCenterResponse lncCentreResponse = _netsclarLbService
                                .createNetscalerControlCenterResponse(lncCentersVO);
                        lncCentersResponse.add(lncCentreResponse);
                    }
                }

                response.setResponses(lncCentersResponse);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        "Failed to list Net scalar Control Center due to some  internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
