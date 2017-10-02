//
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
//

package com.cloud.api.commands;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = AssociateNuageVspDomainTemplateCmd.APINAME, responseObject = SuccessResponse.class,
            description = "associate a vpc with a domain template", authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.User},
            since = "4.11.0")
public class AssociateNuageVspDomainTemplateCmd extends BaseCmd {
    static final String APINAME = "associateNuageVspDomainTemplate";

    @Inject
    NuageVspManager _nuageVspManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, required = true, description = "VPC ID")
    private Long vpcId;


    @Parameter(name = VspConstants.NUAGE_VSP_API_DOMAIN_TEMPLATE, type = CommandType.STRING, required = true, description = "the name of the domain template")
    private String domainTemplate;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the physical network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,  description = "the zone ID")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVpcId() {
        return vpcId;
    }

    public String getDomainTemplate() { return domainTemplate; }

    public Long getZoneId() { return zoneId; }

    public Long getPhysicalNetworkId() { return physicalNetworkId; }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            boolean result =_nuageVspManager.associateNuageVspDomainTemplate(this);
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setResponseName(getCommandName());
            response.setSuccess(result);
            this.setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

}
