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

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@APICommand(name = "resetUserDataForVirtualMachine", responseObject = UserVmResponse.class, description = "Resets the UserData for virtual machine. " +
        "The virtual machine must be in a \"Stopped\" state.", responseView = ResponseObject.ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true, since = "4.18.0")
public class ResetVMUserDataCmd extends BaseCmd implements UserCmd {

    public static final Logger s_logger = Logger.getLogger(ResetVMUserDataCmd.class.getName());

    private static final String s_name = "resetuserdataforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "The ID of the virtual machine")
    private Long id;

    @Parameter(name = ApiConstants.USER_DATA,
            type = CommandType.STRING,
            description = "an optional binary data that can be sent to the virtual machine upon a successful deployment. " +
                    "This binary data must be base64 encoded before adding it to the request. " +
                    "Using HTTP GET (via querystring), you can send up to 4KB of data after base64 encoding. " +
                    "Using HTTP POST(via POST body), you can send up to 1MB of data after base64 encoding." +
                    "You also need to change vm.userdata.max.length value",
            length = 1048576)
    private String userData;

    @Parameter(name = ApiConstants.USER_DATA_ID, type = CommandType.UUID, entityType = UserDataResponse.class, description = "the ID of the userdata")
    private Long userdataId;

    @Parameter(name = ApiConstants.USER_DATA_DETAILS, type = CommandType.MAP, description = "used to specify the parameters values for the variables in userdata.")
    private Map userdataDetails;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the virtual machine")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

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
    public String getUserData() {
        return userData;
    }

    public Long getUserdataId() {
        return userdataId;
    }

    public Map<String, String> getUserdataDetails() {
        Map<String, String> userdataDetailsMap = new HashMap<String, String>();
        if (userdataDetails != null && userdataDetails.size() != 0) {
            Collection parameterCollection = userdataDetails.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                for (Map.Entry<String,String> entry: value.entrySet()) {
                    userdataDetailsMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return userdataDetailsMap;
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
        UserVm result = _userVmService.resetVMUserData(this);

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to reset vm SSHKey");
        }
    }
}
