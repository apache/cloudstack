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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@APICommand(name = "updateDefaultNicForVirtualMachine", description="Changes the default NIC on a VM", responseObject=UserVmResponse.class)

public class UpdateDefaultNicForVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateDefaultNicForVMCmd.class);
    private static final String s_name = "updatedefaultnicforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="Virtual Machine ID")
    private Long vmId;

    @Parameter(name=ApiConstants.NIC_ID, type=CommandType.UUID, entityType=NicResponse.class,
            required=true, description="NIC ID")
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
        return EventTypes.EVENT_NIC_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return  "Updating NIC " + getNicId() + " on user vm: " + getVmId();
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
    public void execute(){
        CallContext.current().setEventDetails("Vm Id: "+getVmId() + " Nic Id: " + getNicId());
        UserVm result = _userVmService.updateDefaultNicForVirtualMachine(this);
        ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
        dc.add(VMDetails.valueOf("nics"));
        EnumSet<VMDetails> details = EnumSet.copyOf(dc);
        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", details, result).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);                                                                                                                                                                                                                                                                              
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to set default nic for VM. Refer to server logs for details.");
        }
    }
}
