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

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.user.Account;

@APICommand(name = "updateAutoScaleVmGroup", description = "Updates an existing autoscale vm group.", responseObject = AutoScaleVmGroupResponse.class, entityType = {AutoScaleVmGroup.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateAutoScaleVmGroupCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateAutoScaleVmGroupCmd.class.getName());

    private static final String s_name = "updateautoscalevmgroupresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.MIN_MEMBERS,
               type = CommandType.INTEGER,
               description = "the minimum number of members in the vmgroup, the number of instances in the vm group will be equal to or more than this number.")
    private Integer minMembers;

    @Parameter(name = ApiConstants.MAX_MEMBERS,
               type = CommandType.INTEGER,
               description = "the maximum number of members in the vmgroup, The number of instances in the vm group will be equal to or less than this number.")
    private Integer maxMembers;

    @Parameter(name = ApiConstants.INTERVAL, type = CommandType.INTEGER, description = "the frequency at which the conditions have to be evaluated")
    private Integer interval;

    @Parameter(name = ApiConstants.SCALEUP_POLICY_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = AutoScalePolicyResponse.class,
               description = "list of scaleup autoscale policies")
    private List<Long> scaleUpPolicyIds;

    @Parameter(name = ApiConstants.SCALEDOWN_POLICY_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = AutoScalePolicyResponse.class,
               description = "list of scaledown autoscale policies")
    private List<Long> scaleDownPolicyIds;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = AutoScaleVmGroupResponse.class,
               required = true,
               description = "the ID of the autoscale group")
    private Long id;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the group to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        CallContext.current().setEventDetails("AutoScale Vm Group Id: " + getId());
        AutoScaleVmGroup result = _autoScaleService.updateAutoScaleVmGroup(this);
        if (result != null) {
            AutoScaleVmGroupResponse response = _responseGenerator.createAutoScaleVmGroupResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update autoscale VmGroup");
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getMinMembers() {
        return minMembers;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public Integer getInterval() {
        return interval;
    }

    public List<Long> getScaleUpPolicyIds() {
        return scaleUpPolicyIds;
    }

    public List<Long> getScaleDownPolicyIds() {
        return scaleDownPolicyIds;
    }

    public Boolean getDisplay() {
        return display;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AUTOSCALEVMGROUP_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating AutoScale Vm Group. Vm Group Id: " + getId();
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
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AutoScaleVmGroup;
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), AutoScaleVmGroup.class);
        }
    }
}
