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
package org.apache.cloudstack.api.command.admin.vm;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "assignVirtualMachine",
            description = "Change ownership of a VM from one account to another. This API is available for Basic zones with security groups and Advanced zones with guest networks. A root administrator can reassign a VM from any account to any other account in any domain. A domain administrator can reassign a VM to any account in the same domain.",
            responseObject = UserVmResponse.class,
        since = "3.0.0", entityType = {VirtualMachine.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class AssignVMCmd extends BaseCmd  {
    public static final Logger s_logger = Logger.getLogger(AssignVMCmd.class.getName());

    private static final String s_name = "assignvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               required = true,
               description = "id of the VM to be moved")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account name of the new VM owner.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain id of the new VM owner.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the new VM owner.")
    private Long projectId;

    //Network information
    @Parameter(name = ApiConstants.NETWORK_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = NetworkResponse.class,
               description = "list of new network ids in which the moved VM will participate. In case no network ids are provided the VM will be part of the default network for that zone. "
                   +
                   "In case there is no network yet created for the new account the default network will be created.")
    private List<Long> networkIds;

    @Parameter(name = ApiConstants.SECURITY_GROUP_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = SecurityGroupResponse.class,
               description = "list of security group ids to be applied on the virtual machine. " +
                   "In case no security groups are provided the VM is part of the default security group.")
    private List<Long> securityGroupIdList;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return virtualMachineId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public List<Long> getNetworkIds() {
        return networkIds;
    }

    public List<Long> getSecurityGroupIdList() {
        return securityGroupIdList;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        try {
            UserVm userVm = _userVmService.moveVMToUser(this);
            if (userVm == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move vm");
            }
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full, "virtualmachine", userVm).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (InvalidParameterValueException e){
            e.printStackTrace();
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (Exception e) {
            s_logger.error("Failed to move vm due to: " + e.getStackTrace());
            if (e.getMessage() != null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move vm due to " + e.getMessage());
            } else if (e.getCause() != null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move vm due to " + e.getCause());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move vm");
            }
        }

    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getVmId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

}
