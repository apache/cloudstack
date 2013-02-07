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

package org.apache.cloudstack.api.command.user.region.ha.gslb;

import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.log4j.Logger;

@APICommand(name = "createGlobalLoadBalancerRule", description="Creates a global load balancer rule",
        responseObject=GlobalLoadBalancerResponse.class)
public class CreateGlobalLoadBalancerRuleCmd extends BaseAsyncCreateCmd {

    public static final Logger s_logger = Logger.getLogger(CreateGlobalLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "creategloballoadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ALGORITHM, type=CommandType.STRING, required=true, description="load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule", length=4096)
    private String description;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the load balancer rule")
    private String globalLoadBalancerRuleName;

    @Parameter(name=ApiConstants.REGION_ID, type= CommandType.UUID, entityType = RegionResponse.class, required=true, description="region where the global load balancer is going to be created.")
    private Long regionId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the global load balancer. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class, description="the domain ID associated with the load balancer")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public String getGlobalLoadBalancerRuleName() {
        return globalLoadBalancerRuleName;
    }


    public String getName() {
        return globalLoadBalancerRuleName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {

        UserContext callerContext = UserContext.current();

        try {
            UserContext.current().setEventDetails("Rule Id: " + getEntityId());
        } catch (Exception ex) {

        }finally {

        }
    }

    @Override
    public void create() {

    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GLOBAL_LOAD_BALANCER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating a global load balancer: " + getName() + " for account: " + getAccountName();

    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.FirewallRule;
    }

    @Override
    public long getEntityOwnerId() {
        return getAccountId();
    }

    public String getAccountName() {
        return accountName;
    }

    public long getAccountId() {
        Account account = null;
        if ((domainId != null) && (accountName != null)) {
            account = _responseGenerator.findAccountByNameDomain(accountName, domainId);
            if (account != null) {
                return account.getId();
            } else {
                throw new InvalidParameterValueException("Unable to find account " + account + " in domain id=" + domainId);
            }
        } else {
            throw new InvalidParameterValueException("Can't define IP owner. Either specify account/domainId or publicIpId");
        }
    }
}
