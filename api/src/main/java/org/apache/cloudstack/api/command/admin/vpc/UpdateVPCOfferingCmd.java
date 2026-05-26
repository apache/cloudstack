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
package org.apache.cloudstack.api.command.admin.vpc;

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.offering.DomainAndZoneIdResolver;
import org.apache.cloudstack.api.response.VpcOfferingResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.user.Account;

@APICommand(name = "updateVPCOffering", description = "Updates VPC offering", responseObject = VpcOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateVPCOfferingCmd extends BaseAsyncCmd implements DomainAndZoneIdResolver {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VpcOfferingResponse.class, required = true, description = "The ID of the VPC offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the VPC offering")
    private String vpcOffName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "The display text of the VPC offering")
    private String displayText;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "Update state for the VPC offering; " + "supported states - Enabled/Disabled")
    private String state;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.STRING,
            description = "The ID of the containing domain(s) as comma separated string, public for public offerings",
            length = 4096)
    private String domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.STRING,
            description = "The ID of the containing zone(s) as comma separated string, all for all zones offerings",
            length = 4096,
            since = "4.13")
    private String zoneIds;

    @Parameter(name = ApiConstants.SORT_KEY, type = CommandType.INTEGER, description = "Sort key of the VPC offering, integer")
    private Integer sortKey;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getVpcOfferingName() {
        return vpcOffName;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public List<Long> getDomainIds() {
        return resolveDomainIds(domainIds, id, _vpcProvSvc::getVpcOfferingDomains, "VPC offering");
    }

    public List<Long> getZoneIds() {
        return resolveZoneIds(zoneIds, id, _vpcProvSvc::getVpcOfferingZones, "VPC offering");
    }

    public Integer getSortKey() {
        return sortKey;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        VpcOffering result = _vpcProvSvc.updateVpcOffering(this);
        if (result != null) {
            VpcOfferingResponse response = _responseGenerator.createVpcOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update VPC offering");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPC_OFFERING_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating VPC offering with ID:" + getResourceUuid(ApiConstants.ID);
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VpcOffering;
    }
}
