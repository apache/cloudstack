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

import com.cloud.api.response.NuageVspDomainTemplateResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = ListNuageVspDomainTemplatesCmd.APINAME, responseObject = BaseResponse.class, description = "Lists Nuage VSP domain templates", since = "4.11.0", responseHasSensitiveInfo = false, authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.User})
public class ListNuageVspDomainTemplatesCmd extends BaseCmd {
    static final String APINAME = "listNuageVspDomainTemplates";

    @Inject
    private NuageVspManager _nuageVspManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "the domain ID")
    private Long domainId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            description = "the physical network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            description = "the zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.KEYWORD, type = CommandType.STRING,
    description = "filters the domain templates which contain the keyword")
    private String keyword;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getKeyword() {
        return keyword;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Long getZoneId() { return zoneId; }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            List<NuageVspDomainTemplateResponse> responses = _nuageVspManager.listNuageVspDomainTemplates(this);
            ListResponse<NuageVspDomainTemplateResponse> response = new ListResponse<>();
            response.setResponses(responses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override public long getEntityOwnerId() {
        return 0; //not owned by anyone
    }

}

