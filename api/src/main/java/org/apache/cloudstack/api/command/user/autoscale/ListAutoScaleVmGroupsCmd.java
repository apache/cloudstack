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
package org.apache.cloudstack.api.command.user.autoscale;

import java.util.ArrayList;
import java.util.List;


import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.AutoScaleVmGroup;

@APICommand(name = "listAutoScaleVmGroups", description = "Lists autoscale vm groups.", responseObject = AutoScaleVmGroupResponse.class, entityType = {AutoScaleVmGroup.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAutoScaleVmGroupsCmd extends BaseListProjectAndAccountResourcesCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AutoScaleVmGroupResponse.class, description = "the ID of the autoscale vm group")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the autoscale vmgroup", since = "4.18.0")
    private String name;

    @Parameter(name = ApiConstants.LBID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, description = "the ID of the loadbalancer")
    private Long loadBalancerId;

    @Parameter(name = ApiConstants.VMPROFILE_ID, type = CommandType.UUID, entityType = AutoScaleVmProfileResponse.class, description = "the ID of the profile")
    private Long profileId;

    @Parameter(name = ApiConstants.POLICY_ID, type = CommandType.UUID, entityType = AutoScalePolicyResponse.class, description = "the ID of the policy")
    private Long policyId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the availability zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getLoadBalancerId() {
        return loadBalancerId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        if (id != null && (loadBalancerId != null || profileId != null || policyId != null))
            throw new InvalidParameterValueException("When id is specified other parameters need not be specified");

        List<? extends AutoScaleVmGroup> autoScaleGroups = _autoScaleService.listAutoScaleVmGroups(this);
        ListResponse<AutoScaleVmGroupResponse> response = new ListResponse<AutoScaleVmGroupResponse>();
        List<AutoScaleVmGroupResponse> responses = new ArrayList<AutoScaleVmGroupResponse>();
        if (autoScaleGroups != null) {
            for (AutoScaleVmGroup autoScaleVmGroup : autoScaleGroups) {
                AutoScaleVmGroupResponse autoScaleVmGroupResponse = _responseGenerator.createAutoScaleVmGroupResponse(autoScaleVmGroup);
                autoScaleVmGroupResponse.setObjectName("autoscalevmgroup");
                responses.add(autoScaleVmGroupResponse);
            }
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
