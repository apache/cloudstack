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
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.network.rules.LoadBalancer;
import com.cloud.utils.Pair;

@APICommand(name = "listLoadBalancerRules", description = "Lists load balancer rules.", responseObject = LoadBalancerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListLoadBalancerRulesCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListLoadBalancerRulesCmd.class.getName());


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, description = "the ID of the load balancer rule")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the load balancer rule")
    private String loadBalancerRuleName;

    @Parameter(name = ApiConstants.PUBLIC_IP_ID,
               type = CommandType.UUID,
               entityType = IPAddressResponse.class,
               description = "the public IP address ID of the load balancer rule")
    private Long publicIpId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "the ID of the virtual machine of the load balancer rule")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the availability zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list by network ID the rule belongs to")
    private Long networkId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerRuleName;
    }

    public Long getPublicIpId() {
        return publicIpId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends LoadBalancer>, Integer> loadBalancers = _lbService.searchForLoadBalancers(this);
        ListResponse<LoadBalancerResponse> response = new ListResponse<LoadBalancerResponse>();
        List<LoadBalancerResponse> lbResponses = new ArrayList<LoadBalancerResponse>();
        if (loadBalancers != null) {
            for (LoadBalancer loadBalancer : loadBalancers.first()) {
                LoadBalancerResponse lbResponse = _responseGenerator.createLoadBalancerResponse(loadBalancer);
                lbResponse.setObjectName("loadbalancerrule");
                lbResponses.add(lbResponse);
            }
            response.setResponses(lbResponses, loadBalancers.second());
        }
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
