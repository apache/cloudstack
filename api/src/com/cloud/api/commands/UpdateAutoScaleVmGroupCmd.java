//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AutoScaleVmGroupResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description = "Updates an existing autoscale vm group.", responseObject = AutoScaleVmGroupResponse.class)
public class UpdateAutoScaleVmGroupCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateAutoScaleVmGroupCmd.class.getName());

    private static final String s_name = "updateautoscalevmgroupresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.MIN_MEMBERS, type = CommandType.INTEGER, required = true, description = "the minimum number of members in the vmgroup, the number of instances in the vm group will be equal to or more than this number.")
    private int minMembers;

    @Parameter(name = ApiConstants.MAX_MEMBERS, type = CommandType.INTEGER, required = true, description = "the maximum number of members in the vmgroup, The number of instances in the vm group will be equal to or less than this number.")
    private int maxMembers;

    @IdentityMapper(entityTableName = "autoscale_policies")
    @Parameter(name = ApiConstants.SCALEUP_POLICY_IDS, type = CommandType.LIST, collectionType = CommandType.LONG, description = "list of provision autoscale policies")
    private List<Long> scaleUpPolicyIds;

    @IdentityMapper(entityTableName = "autoscale_policies")
    @Parameter(name = ApiConstants.SCALEDOWN_POLICY_IDS, type = CommandType.LIST, collectionType = CommandType.LONG, description = "list of de-provision autoscale policies")
    private List<Long> scaleDownPolicyIds;

    @IdentityMapper(entityTableName = "autoscale_vmgroups")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "the ID of the autoscale group")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        UserContext.current().setEventDetails("AutoScale Vm Group Id: " + getId());
        AutoScaleVmGroup result = _autoScaleService.updateAutoScaleVmGroup(this);
        if (result != null) {
            AutoScaleVmGroupResponse response = _responseGenerator.createAutoScaleVmGroupResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update autoscale VmGroup");
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public int getMinMembers() {
        return minMembers;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public List<Long> getScaleUpPolicyIds() {
        return scaleUpPolicyIds;
    }

    public List<Long> getScaleDownPolicyIds() {
        return scaleDownPolicyIds;
    }

    @Override
    public String getEventType() {
        return "Update AutoScale Vm Group";
    }

    @Override
    public String getEventDescription() {
        return EventTypes.EVENT_AUTOSCALEVMGROUP_UPDATE;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        AutoScaleVmGroup autoScaleVmGroup = _entityMgr.findById(AutoScaleVmGroup.class, getId());
        if (autoScaleVmGroup != null) {
            return autoScaleVmGroup.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
        // tracked
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.AutoScaleVmGroup;
    }

}
