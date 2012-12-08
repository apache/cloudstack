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
package org.apache.cloudstack.api.admin.vm.command;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.IdentityMapper;
import org.apache.cloudstack.api.Implementation;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@Implementation(description="Recovers a virtual machine.", responseObject=UserVmResponse.class)
public class RecoverVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RecoverVMCmd.class.getName());

    private static final String s_name = "recovervirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    //@IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the virtual machine", entityType=UserVmResponse.class)
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
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
    public void execute() throws ResourceAllocationException{
        UserVm result = _userVmService.recoverVirtualMachine(this);
        if (result != null){
            UserVmResponse recoverVmResponse = _responseGenerator.createUserVmResponse("virtualmachine", result).get(0);
            recoverVmResponse.setResponseName(getCommandName());
            this.setResponseObject(recoverVmResponse);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to recover vm");
        }

    }
}
