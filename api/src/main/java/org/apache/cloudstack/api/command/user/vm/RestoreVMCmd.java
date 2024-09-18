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

import com.cloud.vm.VmDetailConstants;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

import java.util.Map;

@APICommand(name = "restoreVirtualMachine", description = "Restore a VM to original template/ISO or new template/ISO", responseObject = UserVmResponse.class, since = "3.0.0", responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class RestoreVMCmd extends BaseAsyncCmd implements UserCmd {
    private static final String s_name = "restorevmresponse";

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="Virtual Machine ID")
    private Long vmId;

    @Parameter(name = ApiConstants.TEMPLATE_ID,
               type = CommandType.UUID,
               entityType = TemplateResponse.class,
               description = "an optional template Id to restore vm from the new template. This can be an ISO id in case of restore vm deployed using ISO")
    private Long templateId;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
               type = CommandType.UUID,
               entityType = DiskOfferingResponse.class,
               description = "Override root volume's diskoffering.", since = "4.19.1")
    private Long rootDiskOfferingId;

    @Parameter(name = ApiConstants.ROOT_DISK_SIZE,
               type = CommandType.LONG,
               description = "Override root volume's size (in GB). Analogous to details[0].rootdisksize, which takes precedence over this parameter if both are provided",
               since = "4.19.1")
    private Long rootDiskSize;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, since = "4.19.1",
               description = "used to specify the custom parameters")
    private Map details;

    @Parameter(name = ApiConstants.EXPUNGE,
               type = CommandType.BOOLEAN,
               description = "Optional field to expunge old root volume after restore.",
               since = "4.19.1")
    private Boolean expungeRootDisk;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_RESTORE;
    }

    @Override
    public String getEventDescription() {
        return "Restore a VM to original template or specific snapshot";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException {
        UserVm result;
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId()));
        result = _userVmService.restoreVM(this);
        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to restore vm " + getVmId());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getVmId());
        if (vm == null) {
             return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return vm.getAccountId();
    }

    public long getVmId() {
        return vmId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    // TODO - Remove vmid param and make it "id" in 5.0 so that we don't have two getters
    public Long getId() {
        return getVmId();
    }

    public Long getRootDiskOfferingId() {
        return rootDiskOfferingId;
    }

    public Map<String, String> getDetails() {
        Map<String, String> customparameterMap = convertDetailsToMap(details);
        if (rootDiskSize != null && !customparameterMap.containsKey(VmDetailConstants.ROOT_DISK_SIZE)) {
            customparameterMap.put(VmDetailConstants.ROOT_DISK_SIZE, rootDiskSize.toString());
        }
        return customparameterMap;
    }

    public Boolean getExpungeRootDisk() {
        return expungeRootDisk != null && expungeRootDisk;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }
}
