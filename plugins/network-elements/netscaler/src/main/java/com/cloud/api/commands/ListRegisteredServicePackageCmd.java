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


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.api.response.NetScalerServicePackageResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.network.NetScalerServicePackageVO;

@APICommand(name = "listRegisteredServicePackages", responseObject = NetScalerServicePackageResponse.class, description = "lists registered service packages", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListRegisteredServicePackageCmd extends BaseListCmd {

    private static final String s_name = "listregisteredservicepackage";
    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException {
        try {
            List<NetScalerServicePackageVO> lrsPackages = _netsclarLbService.listRegisteredServicePackages(this);
            ListResponse<NetScalerServicePackageResponse> response = new ListResponse<NetScalerServicePackageResponse>();
            List<NetScalerServicePackageResponse> lbDevicesResponse = new ArrayList<NetScalerServicePackageResponse>();

            if (lrsPackages != null && !lrsPackages.isEmpty()) {
                for (NetScalerServicePackageVO lrsPackageVO : lrsPackages) {
                    NetScalerServicePackageResponse lbdeviceResponse = _netsclarLbService
                            .createRegisteredServicePackageResponse(lrsPackageVO);
                    lbDevicesResponse.add(lbdeviceResponse);
                }
            }

            response.setResponses(lbDevicesResponse);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
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
