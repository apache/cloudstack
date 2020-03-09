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

import java.util.ArrayList;
import java.util.EnumSet;

import com.cloud.vm.Nic;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "removeNicFromVirtualMachine", description = "Removes VM from specified network by deleting a NIC", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class RemoveNicFromVMCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveNicFromVMCmd.class);
    private static final String s_name = "removenicfromvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="Virtual Machine ID")
    private Long vmId;

    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = true, description = "NIC ID")
    private Long nicId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    public Long getNicId() {
        return nicId;
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
    public String getEventType() {
        return EventTypes.EVENT_NIC_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "Removing NIC " + this._uuidMgr.getUuid(Nic.class, getNicId()) + " from user vm: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId());
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getVmId());
        if (vm == null) {
             return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return vm.getAccountId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId()) + " Nic Id: " + this._uuidMgr.getUuid(Nic.class, getNicId()));
        UserVm result = _userVmService.removeNicFromVirtualMachine(this);
        ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
        dc.add(VMDetails.valueOf("nics"));
        EnumSet<VMDetails> details = EnumSet.copyOf(dc);
        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", details, result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove NIC from vm, see error log for details");
        }
    }
}
