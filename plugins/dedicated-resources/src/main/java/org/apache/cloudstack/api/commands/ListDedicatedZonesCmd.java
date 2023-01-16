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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DedicateZoneResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.dedicated.DedicatedService;

import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.DedicatedResources;
import com.cloud.utils.Pair;

@APICommand(name = "listDedicatedZones", description = "List dedicated zones.", responseObject = DedicateZoneResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDedicatedZonesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListDedicatedZonesCmd.class.getName());

    @Inject
    DedicatedService _dedicatedservice;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the ID of the Zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "the ID of the domain associated with the zone")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the name of the account associated with the zone. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.AFFINITY_GROUP_ID,
               type = CommandType.UUID,
               entityType = AffinityGroupResponse.class,
               description = "list dedicated zones by affinity group")
    private Long affinityGroupId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getAffinityGroupId() {
        return affinityGroupId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends DedicatedResourceVO>, Integer> result = _dedicatedservice.listDedicatedZones(this);
        ListResponse<DedicateZoneResponse> response = new ListResponse<DedicateZoneResponse>();
        List<DedicateZoneResponse> Responses = new ArrayList<DedicateZoneResponse>();
        if (result != null) {
            for (DedicatedResources resource : result.first()) {
                DedicateZoneResponse zoneResponse = _dedicatedservice.createDedicateZoneResponse(resource);
                Responses.add(zoneResponse);
            }
            response.setResponses(Responses, result.second());
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to list dedicated zones");
        }
    }
}
