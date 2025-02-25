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
import org.apache.cloudstack.api.response.ApplicationLoadBalancerResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.Pair;

@APICommand(name = "listLoadBalancers", description = "Lists internal load balancers", responseObject = ApplicationLoadBalancerResponse.class, since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListApplicationLoadBalancersCmd extends BaseListTaggedResourcesCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, description = "the ID of the load balancer")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the load balancer")
    private String loadBalancerName;

    @Parameter(name = ApiConstants.SOURCE_IP, type = CommandType.STRING, description = "the source IP address of the load balancer")
    private String sourceIp;

    @Parameter(name = ApiConstants.SOURCE_IP_NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               description = "the network ID of the source IP address")
    private Long sourceIpNetworkId;

    @Parameter(name = ApiConstants.SCHEME, type = CommandType.STRING, description = "the scheme of the load balancer. Supported value is internal in the current release")
    private String scheme;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "the network ID of the load balancer")
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
        return loadBalancerName;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Long getSourceIpNetworkId() {
        return sourceIpNetworkId;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }

    public Scheme getScheme() {
        if (scheme != null) {
            if (scheme.equalsIgnoreCase(Scheme.Internal.toString())) {
                return Scheme.Internal;
            } else {
                throw new InvalidParameterValueException("Invalid value for scheme. Supported value is internal");
            }
        }
        return null;
    }

    public Long getNetworkId() {
        return networkId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends ApplicationLoadBalancerRule>, Integer> loadBalancers = _appLbService.listApplicationLoadBalancers(this);
        ListResponse<ApplicationLoadBalancerResponse> response = new ListResponse<ApplicationLoadBalancerResponse>();
        List<ApplicationLoadBalancerResponse> lbResponses = new ArrayList<ApplicationLoadBalancerResponse>();
        for (ApplicationLoadBalancerRule loadBalancer : loadBalancers.first()) {
            ApplicationLoadBalancerResponse lbResponse =
                _responseGenerator.createLoadBalancerContainerReponse(loadBalancer, _lbService.getLbInstances(loadBalancer.getId()));
            lbResponse.setObjectName("loadbalancer");
            lbResponses.add(lbResponse);
        }
        response.setResponses(lbResponses, loadBalancers.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
