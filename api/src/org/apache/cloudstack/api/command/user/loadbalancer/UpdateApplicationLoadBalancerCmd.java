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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.FirewallRule;

@APICommand(name = "updateLoadBalancer", description = "Updates a Load Balancer", responseObject = ApplicationLoadBalancerResponse.class, since = "4.4.0")
public class UpdateApplicationLoadBalancerCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateApplicationLoadBalancerCmd.class.getName());

    private static final String s_name = "updateloadbalancerresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, required = true, description = "the ID of the Load Balancer")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    public Long getId() {
        return id;
    }

    @Override
    public long getEntityOwnerId() {
        ApplicationLoadBalancerRule lb = _entityMgr.findById(ApplicationLoadBalancerRule.class, getId());
        if (lb != null) {
            return lb.getAccountId();
        } else {
            throw new InvalidParameterValueException("Can't find load balancer by id specified");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "updating load balancer: " + getId();
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        CallContext.current().setEventDetails("Load balancer Id: " + getId());
        ApplicationLoadBalancerRule rule = _appLbService.deleteApplicationLoadBalancer(getId(), this.getCustomId());
        ApplicationLoadBalancerResponse lbResponse = _responseGenerator.createLoadBalancerContainerReponse(rule, _lbService.getLbInstances(getId()));
        setResponseObject(lbResponse);
        lbResponse.setResponseName(getCommandName());
    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), FirewallRule.class);
        }
    }
}
