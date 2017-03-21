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
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.InternalLoadBalancerElementResponse;
import org.apache.cloudstack.api.response.ProviderResponse;

import org.apache.cloudstack.api.response.ResponseGenerator;
import org.apache.cloudstack.api.response.VpcInlineLoadBalancerElementResponse;
import org.apache.cloudstack.network.element.VpcInlineLoadBalancerElementService;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.user.Account;

@APICommand(name = "createVpcInlineLoadBalancerElement", responseObject=InternalLoadBalancerElementResponse.class,
        description="Create an VPC Inline Load Balancer element.", since="4.10.0", authorized = {RoleType.Admin})
public class CreateVpcInlineLoadBalancerElementCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVpcInlineLoadBalancerElementCmd.class.getName());
    private static final String s_name = "createvpcinlineloadbalancerelementresponse";

    @Inject
    private VpcInlineLoadBalancerElementService _vpcInlineLbElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NETWORK_SERVICE_PROVIDER_ID, type=CommandType.UUID, entityType = ProviderResponse.class, required=true, description="the network service provider ID of the internal load balancer element")
    private Long nspId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setNspId(Long nspId) {
        this.nspId = nspId;
    }

    public Long getNspId() {
        return nspId;
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
    public void execute(){
        VirtualRouterProvider result = _vpcInlineLbElementService.getVpcInlineLoadBalancerElement(getEntityId());
        if (result != null) {
            VpcInlineLoadBalancerElementResponse response = ResponseGenerator.createVpcInlineLbElementResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Virtual Router entity to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        VirtualRouterProvider result = _vpcInlineLbElementService.addVpcInlineLoadBalancerElement(getNspId());
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add VPC Inline Load Balancer entity to physical network");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SERVICE_PROVIDER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "Adding physical network element Internal Load Balancer: " + getEntityId();
    }
}
