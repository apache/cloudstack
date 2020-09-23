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
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "updateLoadBalancerConfig", description = "Updates a load balancer config",
        responseObject = LoadBalancerConfigResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.15",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateLoadBalancerConfigCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(UpdateLoadBalancerConfigCmd.class.getName());
    private static final String s_name = "updateloadbalancerconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = LoadBalancerConfigResponse.class,
               required = true,
               description = "the ID of the load balancer config to update")
    private Long id;

    @Parameter(name = ApiConstants.VALUE,
               type = CommandType.STRING,
               required = true,
               description = "value of the load balancer config")
    private String value;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
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
        LoadBalancerConfig result = _lbConfigService.updateLoadBalancerConfig(this);
        if (result != null) {
            LoadBalancerConfigResponse response = _responseGenerator.createLoadBalancerConfigResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update load balancer config");
        }
    }

    @Override
    public String getSyncObjType() {
        LoadBalancerConfig config = _entityMgr.findById(LoadBalancerConfig.class, getId());
        if (config == null) {
            throw new InvalidParameterValueException("Unable to find load balancer config: " + id);
        }
        if (config.getNetworkId() != null) {
            return BaseAsyncCmd.networkSyncObject;
        } else if (config.getVpcId() != null) {
            return BaseAsyncCmd.vpcSyncObject;
        }
        return null;
    }

    @Override
    public Long getSyncObjId() {
        LoadBalancerConfig config = _entityMgr.findById(LoadBalancerConfig.class, getId());
        if (config == null) {
            throw new InvalidParameterValueException("Unable to find load balancer config: " + id);
        }
        if (config.getNetworkId() != null) {
            return config.getNetworkId();
        } else if (config.getVpcId() != null) {
            return config.getVpcId();
        }
        return null;
    }

    @Override
    public long getEntityOwnerId() {
        LoadBalancerConfig config = _entityMgr.findById(LoadBalancerConfig.class, getId());
        if (config != null) {
            if (config.getNetworkId() != null) {
                Network network = _entityMgr.findById(Network.class, config.getNetworkId());
                if (network != null) {
                    return network.getAccountId();
                }
            } else if (config.getVpcId() != null) {
                Vpc vpc = _entityMgr.findById(Vpc.class, config.getVpcId());
                if (vpc != null) {
                    return vpc.getAccountId();
                }
            } else if (config.getLoadBalancerId() != null) {
                FirewallRule rule = _entityMgr.findById(FirewallRule.class, config.getLoadBalancerId());
                if (rule != null) {
                    return rule.getAccountId();
                }
            }
        }
        throw new InvalidParameterValueException("Unable to find the entity owner");
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CONFIG_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "updating load balancer config" + getId();
    }
}
