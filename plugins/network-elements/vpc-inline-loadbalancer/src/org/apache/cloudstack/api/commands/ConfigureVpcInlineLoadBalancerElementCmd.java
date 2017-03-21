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

package org.apache.cloudstack.api.commands;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ResponseGenerator;
import org.apache.cloudstack.api.response.VpcInlineLoadBalancerElementResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.network.element.VpcInlineLoadBalancerElementService;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.user.Account;

@APICommand(name = "configureVpcInlineLoadBalancerElement", responseObject=VpcInlineLoadBalancerElementResponse.class,
            description="Configures an VpcInline Load Balancer element.", since="4.10.0", authorized = {RoleType.Admin})
public class ConfigureVpcInlineLoadBalancerElementCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ConfigureVpcInlineLoadBalancerElementCmd.class.getName());
    private static final String s_name = "configurevpcinlineloadbalancerelementresponse";

    @Inject
    private VpcInlineLoadBalancerElementService _vpcInlineLbElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = VpcInlineLoadBalancerElementResponse.class,
            description="configure VPC inline load balancer elements by id")
    private Long id;

    @Parameter(name=ApiConstants.NSP_ID, type=CommandType.UUID, entityType = ProviderResponse.class,
            description="configure VPC inline load balancer elements by network service provider id")
    private Long nspId;

    @Parameter(name=ApiConstants.ENABLED, type=CommandType.BOOLEAN, required=true, description="Enables/Disables the VpcInline Load Balancer element")
    private Boolean enabled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }

    public Long getNspId() {
        return nspId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ELEMENT_CONFIGURE;
    }

    @Override
    public String getEventDescription() {
        return  "configuring vpc inline load balancer element: " + id;
    }

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        VirtualRouterProvider result;
        if (id != null) {
            result = _vpcInlineLbElementService.configureVpcInlineLoadBalancerElement(getId(), getEnabled());
        } else {
            result = _vpcInlineLbElementService.configureVpcInlineLoadBalancerElementByNspId(getNspId(), getEnabled());
        }

        if (result != null){
            VpcInlineLoadBalancerElementResponse routerResponse = ResponseGenerator.createVpcInlineLbElementResponse(result);
            routerResponse.setResponseName(getCommandName());
            this.setResponseObject(routerResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to configure the vpc inline load balancer element");
        }
    }
}
