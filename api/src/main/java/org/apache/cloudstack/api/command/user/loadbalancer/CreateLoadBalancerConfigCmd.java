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
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.LoadBalancerConfigResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.LoadBalancerConfig;
import com.cloud.network.rules.LoadBalancerConfig.Scope;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "createLoadBalancerConfig", description = "Creates a load balancer config",
        responseObject = LoadBalancerConfigResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateLoadBalancerConfigCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerConfigCmd.class.getName());

    private static final String s_name = "createloadbalancerconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.SCOPE,
               type = CommandType.STRING,
               required = true,
               description = "the scope of the config: network, vpc or rule")
    private String scope;

    @Parameter(name = ApiConstants.NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               description = "the ID of network to update")
    private Long networkId;

    @Parameter(name = ApiConstants.VPC_ID,
               type = CommandType.UUID,
               entityType = VpcResponse.class,
               description = "the ID of vpc to update")
    private Long vpcId;

    @Parameter(name = ApiConstants.LOAD_BALANCER_ID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               description = "the ID of the load balancer rule to update")
    private Long loadBalancerId;

    @Parameter(name = ApiConstants.NAME,
               type = CommandType.STRING,
               required = true,
               description = "name of the load balancer config")
    private String name;

    @Parameter(name = ApiConstants.VALUE,
               type = CommandType.STRING,
               required = true,
               description = "value of the load balancer config")
    private String value;

    @Parameter(name = ApiConstants.FORCED,
               type = CommandType.BOOLEAN,
               required = false,
               description = "Force add a load balancer config. Existing config will be removed")
    private Boolean forced;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getScope() {
        return scope;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getLoadBalancerId() {
        return loadBalancerId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isForced() {
        return (forced != null) ? forced : false;
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

        LoadBalancerConfig config = null;
        try {
            config = _entityMgr.findById(LoadBalancerConfig.class, getEntityId());
            LoadBalancerConfigResponse lbConfigResponse = new LoadBalancerConfigResponse();
            if (config != null) {
                lbConfigResponse = _responseGenerator.createLoadBalancerConfigResponse(config);
                setResponseObject(lbConfigResponse);
            }
            lbConfigResponse.setResponseName(getCommandName());
        } catch (Exception ex) {
            s_logger.warn("Failed to create LB config due to exception ", ex);
        }
    }

    @Override
    public void create() {
        try {
            LoadBalancerConfig result = _lbConfigService.createLoadBalancerConfig(this);
            this.setEntityId(result.getId());
            this.setEntityUuid(result.getUuid());
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    @Override
    public String getSyncObjType() {
        if (networkId != null) {
            return BaseAsyncCmd.networkSyncObject;
        } else if (vpcId != null) {
            return BaseAsyncCmd.vpcSyncObject;
        }
        return null;
    }

    @Override
    public Long getSyncObjId() {
        if (networkId != null) {
            return getNetworkId();
        } else if (vpcId != null) {
            return getVpcId();
        }
        return null;
    }

    @Override
    public long getEntityOwnerId() {
        if (Scope.Network.name().equalsIgnoreCase(scope) && networkId != null) {
            Network network = _entityMgr.findById(Network.class, networkId);
            if (network != null) {
                return network.getAccountId();
            }
        } else if (Scope.Vpc.name().equalsIgnoreCase(scope) && vpcId != null) {
            Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);
            if (vpc != null) {
                return vpc.getAccountId();
            }
        } else if (Scope.LoadBalancerRule.name().equalsIgnoreCase(scope) && loadBalancerId != null) {
            LoadBalancer lb = _entityMgr.findById(LoadBalancer.class, loadBalancerId);
            if (lb != null) {
                return lb.getAccountId();
            }
        }
        throw new InvalidParameterValueException("Unable to find the entity owner");
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CONFIG_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating load balancer: " + getName();
    }
}
