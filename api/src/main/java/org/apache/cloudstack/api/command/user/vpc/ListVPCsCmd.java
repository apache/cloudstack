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
package org.apache.cloudstack.api.command.user.vpc;

import java.util.ArrayList;
import java.util.List;

import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.network.vpc.Vpc;
import com.cloud.utils.Pair;


@APICommand(name = "listVPCs", description = "Lists VPCs", responseObject = VpcResponse.class, responseView = ResponseView.Restricted, entityType = {Vpc.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVPCsCmd extends BaseListTaggedResourcesCmd implements UserCmd {
    private static final String s_name = "listvpcsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    ////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "list VPC by id")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "list by zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list by name of the VPC")
    private String vpcName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "List by display text of " + "the VPC")
    private String displayText;

    @Parameter(name = ApiConstants.CIDR, type = CommandType.STRING, description = "list by cidr of the VPC. All VPC "
        + "guest networks' cidrs should be within this CIDR")
    private String cidr;

    @Parameter(name = ApiConstants.VPC_OFF_ID, type = CommandType.UUID, entityType = VpcOfferingResponse.class, description = "list by ID of the VPC offering")
    private Long VpcOffId;

    @Parameter(name = ApiConstants.SUPPORTED_SERVICES, type = CommandType.LIST, collectionType = CommandType.STRING, description = "list VPC supporting certain services")
    private List<String> supportedServices;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list VPCs by state")
    private String state;

    @Parameter(name = ApiConstants.RESTART_REQUIRED, type = CommandType.BOOLEAN, description = "list VPCs by restartRequired option")
    private Boolean restartRequired;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN,
            description = "flag to display the resource icon for VPCs")
    private Boolean showIcon;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public String getCidr() {
        return cidr;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getVpcOffId() {
        return VpcOffId;
    }

    public Long getId() {
        return id;
    }

    public List<String> getSupportedServices() {
        return supportedServices;
    }

    public String getState() {
        return state;
    }

    public Boolean getRestartRequired() {
        return restartRequired;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }

    public Boolean getShowIcon() {
        return showIcon != null ? showIcon : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends Vpc>, Integer> vpcs =
            _vpcService.listVpcs(this);
        ListResponse<VpcResponse> response = new ListResponse<VpcResponse>();
        List<VpcResponse> vpcResponses = new ArrayList<VpcResponse>();
        for (Vpc vpc : vpcs.first()) {
            VpcResponse offeringResponse = _responseGenerator.createVpcResponse(getResponseView(), vpc);
            vpcResponses.add(offeringResponse);
        }

        response.setResponses(vpcResponses, vpcs.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
        if (response != null && response.getCount() > 0 && getShowIcon()) {
            updateVpcResponse(response.getResponses());
        }
    }

    private void updateVpcResponse(List<VpcResponse> response) {
        for (VpcResponse vpcResponse : response) {
            ResourceIcon resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.Vpc, vpcResponse.getId());
            if (resourceIcon == null) {
                continue;
            }
            ResourceIconResponse iconResponse = _responseGenerator.createResourceIconResponse(resourceIcon);
            vpcResponse.setResourceIconResponse(iconResponse);
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
