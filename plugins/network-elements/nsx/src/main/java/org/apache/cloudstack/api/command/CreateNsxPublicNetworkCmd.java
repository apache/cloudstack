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
package org.apache.cloudstack.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.service.NsxService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = CreateNsxPublicNetworkCmd.APINAME, description = "Create NSX Public Network for a Zone",
        responseObject = NetworkResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false, since = "4.19.0.0")
public class CreateNsxPublicNetworkCmd extends BaseCmd {

    public static final String APINAME = "createNsxPublicNetwork";

    public static final Logger s_logger = Logger.getLogger(CreateNsxPublicNetworkCmd.class.getName());

    @Inject
    private NsxService nsxService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
            , description = "the ID of zone")
    private Long zoneId;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            NetworkVO network = nsxService.createNsxPublicNetwork(zoneId);
            NetworkResponse response = _responseGenerator.createNetworkResponse(ResponseObject.ResponseView.Restricted, network);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            String msg = String.format("Failed to create NSX Public Network on zone %s: %s", zoneId, e.getMessage());
            s_logger.error(msg, e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
