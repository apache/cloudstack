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
import java.util.HashMap;
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
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancerConfig;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "replaceLoadBalancerConfigs", description = "Replaces load balancer configs of vpc/network/rule",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ReplaceLoadBalancerConfigsCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ReplaceLoadBalancerConfigsCmd.class.getName());
    private static final String s_name = "replaceloadbalancerconfigsresponse";

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
               description = "configs list, Example: config[0].name=timout&config[0].value=60000")
    private Map configList;


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

    public Map getConfigList() {
        if (configList == null || configList.isEmpty()) {
            return null;
        }

        Map<String, String> configMap = new HashMap<>();
        if (MapUtils.isNotEmpty(configList)) {
            for (Map<String, String> config : (Collection<Map<String, String>>)configList.values()) {
                String name = config.get("name");
                String value = config.get("value");
                configMap.put(name, value);
            }
        }
        return configMap;
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
        List<? extends LoadBalancerConfig> configs = _lbConfigService.replaceLoadBalancerConfigs(this);
        SuccessResponse response = new SuccessResponse(getCommandName());
        this.setResponseObject(response);
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
            return networkId;
        } else if (vpcId != null) {
            return vpcId;
        }
        return null;
    }

    @Override
    public long getEntityOwnerId() {
        if (networkId != null) {
            Network network = _entityMgr.findById(Network.class, networkId);
            if (network != null) {
                return network.getAccountId();
            }
        } else if (vpcId != null) {
            Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);
            if (vpc != null) {
                return vpc.getAccountId();
            }
        }
        throw new InvalidParameterValueException("Unable to find the entity owner");
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
