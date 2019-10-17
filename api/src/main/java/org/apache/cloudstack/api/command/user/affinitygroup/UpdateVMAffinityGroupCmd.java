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
package org.apache.cloudstack.api.command.user.affinitygroup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "updateVMAffinityGroup",
            description = "Updates the affinity/anti-affinity group associations of a virtual machine. The VM has to be stopped and restarted for the "
                + "new properties to take effect.",
            responseObject = UserVmResponse.class,
        responseView = ResponseView.Restricted,
        entityType = {VirtualMachine.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class UpdateVMAffinityGroupCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVMAffinityGroupCmd.class.getName());
    private static final String s_name = "updatevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "The ID of the virtual machine")
    private Long id;

    @ACL
    @Parameter(name = ApiConstants.AFFINITY_GROUP_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = AffinityGroupResponse.class,
               description = "comma separated list of affinity groups id that are going to be applied to the virtual machine. "
                   + "Should be passed only when vm is created from a zone with Basic Network support." + " Mutually exclusive with securitygroupnames parameter")
    private List<Long> affinityGroupIdList;

    @ACL
    @Parameter(name = ApiConstants.AFFINITY_GROUP_NAMES,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               entityType = AffinityGroupResponse.class,
               description = "comma separated list of affinity groups names that are going to be applied to the virtual machine."
                   + " Should be passed only when vm is created from a zone with Basic Network support. " + "Mutually exclusive with securitygroupids parameter")
    private List<String> affinityGroupNameList;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public List<Long> getAffinityGroupIdList() {
        // transform group names to ids here
        if (affinityGroupNameList != null) {
            List<Long> affinityGroupIds = new ArrayList<Long>();
            for (String groupName : affinityGroupNameList) {
                Long groupId = _responseGenerator.getAffinityGroupId(groupName, getEntityOwnerId());
                if (groupId == null) {
                    throw new InvalidParameterValueException("Unable to find group by name " + groupName + " for account " + getEntityOwnerId());
                } else {
                    affinityGroupIds.add(groupId);
                }
            }
            return affinityGroupIds;
        } else {
            return affinityGroupIdList;
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "virtualmachine";
    }

    @Override
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException {
        if (affinityGroupNameList != null && affinityGroupIdList != null) {
            throw new InvalidParameterValueException("affinitygroupids parameter is mutually exclusive with affinitygroupnames parameter");
        }

        if (affinityGroupNameList == null && affinityGroupIdList == null) {
            throw new InvalidParameterValueException("affinitygroupids parameter or affinitygroupnames parameter must be given");
        }

        CallContext.current().setEventDetails("VM ID: " + getId());
        UserVm result = _affinityGroupService.updateVMAffinityGroups(getId(), getAffinityGroupIdList());
        ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
        dc.add(VMDetails.valueOf("affgrp"));
        EnumSet<VMDetails> details = EnumSet.copyOf(dc);

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", details, result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update VM's affinity groups");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_AFFINITY_GROUP_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "updating VM affinity group";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AffinityGroup;
    }

}
