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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createLoadBalancer", description = "Creates an internal load balancer", responseObject = ApplicationLoadBalancerResponse.class, since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateApplicationLoadBalancerCmd extends BaseAsyncCreateCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the load balancer")
    private String loadBalancerName;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description of the load balancer", length = 4096)
    private String description;

    @Parameter(name = ApiConstants.NETWORK_ID,
               type = CommandType.UUID,
               required = true,
               entityType = NetworkResponse.class,
               description = "The guest network the load balancer will be created for")
    private Long networkId;

    @Parameter(name = ApiConstants.SOURCE_PORT,
               type = CommandType.INTEGER,
               required = true,
               description = "the source port the network traffic will be load balanced from")
    private Integer sourcePort;

    @Parameter(name = ApiConstants.ALGORITHM, type = CommandType.STRING, required = true, description = "load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name = ApiConstants.INSTANCE_PORT,
               type = CommandType.INTEGER,
               required = true,
               description = "the TCP port of the virtual machine where the network traffic will be load balanced to")
    private Integer instancePort;

    @Parameter(name = ApiConstants.SOURCE_IP, type = CommandType.STRING, description = "the source IP address the network traffic will be load balanced from")
    private String sourceIp;

    @Parameter(name = ApiConstants.SOURCE_IP_NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               required = true,
               description = "the network id of the source ip address")
    private Long sourceIpNetworkId;

    @Parameter(name = ApiConstants.SCHEME,
               type = CommandType.STRING,
               required = true,
               description = "the load balancer scheme. Supported value in this release is Internal")
    private String scheme;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Boolean getDisplay() {
        return display;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public Integer getPrivatePort() {
        return instancePort;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getName() {
        return loadBalancerName;
    }

    public Integer getSourcePort() {
        return sourcePort.intValue();
    }

    public String getProtocol() {
        return NetUtils.TCP_PROTO;
    }

    public long getAccountId() {
        //get account info from the network object
        Network ntwk = _networkService.getNetwork(networkId);
        if (ntwk == null) {
            throw new InvalidParameterValueException("Invalid network ID specified");
        }

        return ntwk.getAccountId();

    }

    public int getInstancePort() {
        return instancePort.intValue();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating load balancer: " + getName() + " account: " + getAccountId();

    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.LoadBalancerRule;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public long getSourceIpNetworkId() {
        return sourceIpNetworkId;
    }

    public Scheme getScheme() {
        if (scheme.equalsIgnoreCase(Scheme.Internal.toString())) {
            return Scheme.Internal;
        } else {
            throw new InvalidParameterValueException("Invalid value for scheme. Supported value is internal");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return getAccountId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {
        ApplicationLoadBalancerRule rule = null;
        try {
            CallContext.current().setEventDetails("Load Balancer Id: " + getEntityId());
            // State might be different after the rule is applied, so get new object here
            rule = _entityMgr.findById(ApplicationLoadBalancerRule.class, getEntityId());
            ApplicationLoadBalancerResponse lbResponse = _responseGenerator.createLoadBalancerContainerReponse(rule, _lbService.getLbInstances(getEntityId()));
            setResponseObject(lbResponse);
            lbResponse.setResponseName(getCommandName());
        } catch (Exception ex) {
            logger.warn("Failed to create load balancer due to exception ", ex);
        } finally {
            if (rule == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create load balancer");
            }
        }
    }

    @Override
    public void create() {
        try {

            ApplicationLoadBalancerRule result =
                _appLbService.createApplicationLoadBalancer(getName(), getDescription(), getScheme(), getSourceIpNetworkId(), getSourceIp(), getSourcePort(),
                    getInstancePort(), getAlgorithm(), getNetworkId(), getEntityOwnerId(), getDisplay());
            this.setEntityId(result.getId());
            this.setEntityUuid(result.getUuid());
        } catch (NetworkRuleConflictException e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        } catch (InsufficientAddressCapacityException e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, e.getMessage());
        } catch (InsufficientVirtualNetworkCapacityException e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, e.getMessage());
        }
    }
}
