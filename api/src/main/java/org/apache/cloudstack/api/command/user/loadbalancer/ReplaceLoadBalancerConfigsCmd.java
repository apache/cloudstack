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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "replaceLoadBalancerConfigs", description = "Replaces load balancer configs of vpc/network/rule",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.17",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ReplaceLoadBalancerConfigsCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(ReplaceLoadBalancerConfigsCmd.class.getName());
    private static final String RESPONSE_NAME = "replaceloadbalancerconfigsresponse";

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

    @Parameter(name = ApiConstants.CONFIG,
            type = CommandType.MAP,
            description = "configs list, Example: config[0].global.maxconn=40960")
    private Map<String, String> configList;


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

    public Map<String, String> getConfigList() {
        if (MapUtils.isEmpty(configList)) {
            return null;
        }

        Collection<String> paramsCollection = configList.values();
        return (Map<String, String>) (paramsCollection.toArray())[0];
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return RESPONSE_NAME;
    }

    @Override
    public void execute() {
        List<? extends LoadBalancerConfig> configs = _lbConfigService.replaceLoadBalancerConfigs(this);
        SuccessResponse response = new SuccessResponse(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getSyncObjType() {
        return LoadBalancerHelper.getSyncObjType(networkId, vpcId);
    }

    @Override
    public Long getSyncObjId() {
        return LoadBalancerHelper.getSyncObjId(networkId, vpcId);
    }

    @Override
    public long getEntityOwnerId() {
        if (networkId != null) {
            return LoadBalancerHelper.getEntityOwnerId(_entityMgr, Network.class, networkId);
        }
        return LoadBalancerHelper.getEntityOwnerId(_entityMgr, Vpc.class, vpcId);
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CONFIG_REPLACE;
    }

    @Override
    public String getEventDescription() {
        return "replacing load balancer config";
    }
}
