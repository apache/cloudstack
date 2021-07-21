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
package org.apache.cloudstack.api.command.user.loadbalancer;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LoadBalancerConfigResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "deleteLoadBalancerConfig", description = "Deletes a load balancer config.",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.15",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteLoadBalancerConfigCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(DeleteLoadBalancerConfigCmd.class.getName());
    private static final String s_name = "deleteloadbalancerconfigresponse";
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = LoadBalancerConfigResponse.class,
               required = true,
               description = "the ID of the load balancer config")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        boolean result = _lbConfigService.deleteLoadBalancerConfig(this);

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete load balancer config");
        }
    }

    @Override
    public String getSyncObjType() {
        return LoadBalancerHelper.getSyncObjType(_entityMgr, getId());
    }

    @Override
    public Long getSyncObjId() {
        return LoadBalancerHelper.getSyncObjId(_entityMgr, getId());
    }

    @Override
    public long getEntityOwnerId() {
        return LoadBalancerHelper.getEntityOwnerId(_entityMgr, getId());
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CONFIG_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "deleting load balancer config: " + getId();
    }

}
