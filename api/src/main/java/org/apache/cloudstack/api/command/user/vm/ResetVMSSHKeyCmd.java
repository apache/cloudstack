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

package org.apache.cloudstack.api.command.user.vm;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = "resetSSHKeyForVirtualMachine", responseObject = UserVmResponse.class, description = "Resets the SSH Key for the Instance. " +
        "The Instance must be in a \"Stopped\" state. [async]", responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ResetVMSSHKeyCmd extends BaseAsyncCmd implements UserCmd {

    public static final Logger s_logger = Logger.getLogger(ResetVMSSHKeyCmd.class.getName());

    private static final String s_name = "resetSSHKeyforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "The ID of the Instance")
    private Long id;

    @Deprecated
    @Parameter(name = ApiConstants.SSH_KEYPAIR, type = CommandType.STRING ,description = "Name of the SSH key pair used to login to the Instance")
    String name;

    @Parameter(name = ApiConstants.SSH_KEYPAIRS, type = CommandType.LIST, collectionType = CommandType.STRING, since="4.17", description = "Names of the SSH key pairs to be used to login to the Instance")
    List<String> names;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "An optional Account for the SSH key. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "An optional domainId for the Instance. If the Account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "An optional project for the SSH key")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getNames() {
        List<String> keypairnames = new ArrayList<String>();
        if (names != null) {
            keypairnames = names;
        }
        if (name != null && !name.isEmpty()) {
            keypairnames.add(name);
        }
        return keypairnames;
    }

    public Long getId() {
        return id;
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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_RESETSSHKEY;
    }

    @Override
    public String getEventDescription() {
        return "resetting SSHKey for Instance: " + getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException {

        CallContext.current().setEventDetails("Vm Id: " + getId());
        UserVm result = _userVmService.resetVMSSHKey(this);

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to reset  SSHKey");
        }
    }

}
