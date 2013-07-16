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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;


@APICommand(name = "updateVirtualMachine", description="Updates properties of a virtual machine. The VM has to be stopped and restarted for the " +
        "new properties to take effect. UpdateVirtualMachine does not first check whether the VM is stopped. " +
        "Therefore, stop the VM manually before issuing this call.", responseObject=UserVmResponse.class)
public class UpdateVMCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateVMCmd.class.getName());
    private static final String s_name = "updatevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DISPLAY_NAME, type=CommandType.STRING, description="user generated name")
    private String displayName;

    @Parameter(name=ApiConstants.GROUP, type=CommandType.STRING, description="group of the virtual machine")
    private String group;

    @Parameter(name=ApiConstants.HA_ENABLE, type=CommandType.BOOLEAN, description="true if high-availability is enabled for the virtual machine, false otherwise")
    private Boolean haEnable;

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="The ID of the virtual machine")
    private Long id;

    @Parameter(name=ApiConstants.OS_TYPE_ID, type=CommandType.UUID, entityType=GuestOSResponse.class,
            description="the ID of the OS type that best represents this VM.")
    private Long osTypeId;

    @Parameter(name=ApiConstants.USER_DATA, type=CommandType.STRING, description="an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding. Using HTTP POST(via POST body), you can send up to 32K of data after base64 encoding.", length=32768)
    private String userData;

    @Parameter(name=ApiConstants.DISPLAY_VM, type=CommandType.BOOLEAN, description="an optional field, whether to the display the vm to the end user or not.")
    private Boolean displayVm;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE, type = CommandType.BOOLEAN, description = "true if VM contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory")
    protected Boolean isDynamicallyScalable;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayName() {
        return displayName;
    }

    public String getGroup() {
        return group;
    }

    public Boolean getHaEnable() {
        return haEnable;
    }

    public Long getId() {
        return id;
    }

    public String getUserData() {
        return userData;
    }

    public Boolean getDisplayVm() {
        return displayVm;
    }

    public Boolean isDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getOsTypeId() {
        return osTypeId;
    }

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
    public void execute() throws ResourceUnavailableException,
            InsufficientCapacityException, ServerApiException {
        CallContext.current().setEventDetails("Vm Id: "+getId());
        UserVm result = _userVmService.updateVirtualMachine(this);
        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update vm");
        }
    }
}
