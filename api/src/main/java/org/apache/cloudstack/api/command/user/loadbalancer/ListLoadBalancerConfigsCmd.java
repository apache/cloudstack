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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.LoadBalancerConfigResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

@APICommand(name = "listLoadBalancerConfigs", description = "Lists load balancer configs.",
        responseObject = LoadBalancerConfigResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.17",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListLoadBalancerConfigsCmd extends BaseListCmd {
    public static final Logger LOGGER = Logger.getLogger(ListLoadBalancerConfigsCmd.class.getName());

    private static final String RESPONSE_NAME = "listloadbalancerconfigsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = LoadBalancerConfigResponse.class,
            description = "the ID of the load balancer config")
    private Long id;

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
            description = "name of the load balancer config")
    private String name;

    @Parameter(name = ApiConstants.LIST_ALL,
            type = CommandType.BOOLEAN,
            description = "If set to true, list all available configs for the scope. Default value is false")
    private Boolean listAll;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

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

    public boolean listAll() {
        return BooleanUtils.toBoolean(listAll);
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return RESPONSE_NAME;
    }

    @Override
    public void execute() {
        List<? extends LoadBalancerConfig> configs = _lbConfigService.searchForLoadBalancerConfigs(this);
        ListResponse<LoadBalancerConfigResponse> response = new ListResponse<LoadBalancerConfigResponse>();
        List<LoadBalancerConfigResponse> lbConfigResponses = new ArrayList<LoadBalancerConfigResponse>();
        if (configs != null) {
            lbConfigResponses = _responseGenerator.createLoadBalancerConfigResponse(configs);
            response.setResponses(lbConfigResponses);
        }
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
