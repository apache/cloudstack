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

import org.apache.cloudstack.api.ApiCommandResourceType;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang.BooleanUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "migrateVirtualMachine",
        description = "Attempts Migration of a VM to a different host or Root volume of the vm to a different storage pool",
        responseObject = UserVmResponse.class, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true)
public class MigrateVMCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID,
            type = CommandType.UUID,
            entityType = HostResponse.class,
            required = false,
            description = "Destination Host ID to migrate VM to.")
    private Long hostId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.STORAGE_ID,
            type = CommandType.UUID,
            entityType = StoragePoolResponse.class,
            required = false,
            description = "Destination storage pool ID to migrate VM volumes to. Required for migrating the root disk volume")
    private Long storageId;

    @Parameter(name = ApiConstants.AUTO_SELECT,
            since = "4.16.0",
            type = CommandType.BOOLEAN,
            description = "Automatically select a destination host which do not require storage migration, if hostId and storageId are not specified. false by default")
    private Boolean autoSelect;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getStoragePoolId() {
        return storageId;
    }

    public Boolean isAutoSelect() {
        return BooleanUtils.isNotFalse(autoSelect);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        String eventDescription;
        if (getHostId() != null) {
            eventDescription = String.format("Attempting to migrate VM id: %s to host Id: %s", getVirtualMachineId(), getHostId());
        } else if (getStoragePoolId() != null) {
            eventDescription = String.format("Attempting to migrate VM id: %s to storage pool Id: %s", getVirtualMachineId(), getStoragePoolId());
        } else {
            eventDescription = String.format("Attempting to migrate VM id: %s", getVirtualMachineId());
        }
        return eventDescription;
    }

    @Override
    public void execute() {
        if (getHostId() != null && getStoragePoolId() != null) {
            throw new InvalidParameterValueException("Only one of hostId and storageId can be specified");
        }

        UserVm userVm = _userVmService.getUserVm(getVirtualMachineId());
        if (userVm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + getVirtualMachineId());
        }

        Host destinationHost = null;
        // OfflineMigration performed when this parameter is specified
        StoragePool destStoragePool = null;
        if (getStoragePoolId() != null) {
            destStoragePool = _storageService.getStoragePool(getStoragePoolId());
            if (destStoragePool == null) {
                throw new InvalidParameterValueException("Unable to find the storage pool to migrate the VM");
            }
            CallContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to storage pool Id: " + getStoragePoolId());
        } else if (getHostId() != null) {
            destinationHost = _resourceService.getHost(getHostId());
            if (destinationHost == null) {
                throw new InvalidParameterValueException("Unable to find the host to migrate the VM, host id=" + getHostId());
            }
            if (destinationHost.getType() != Host.Type.Routing) {
                throw new InvalidParameterValueException("The specified host(" + destinationHost.getName() + ") is not suitable to migrate the VM, please specify another one");
            }
            CallContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to host Id: " + getHostId());
        } else if (! isAutoSelect()) {
            throw new InvalidParameterValueException("Please specify a host or storage as destination, or pass 'autoselect=true' to automatically select a destination host which do not require storage migration");
        }

        try {
            VirtualMachine migratedVm = null;
            if (getStoragePoolId() == null) {
                migratedVm = _userVmService.migrateVirtualMachine(getVirtualMachineId(), destinationHost);
            } else {
                migratedVm = _userVmService.vmStorageMigration(getVirtualMachineId(), destStoragePool);
            }
            if (migratedVm != null) {
                UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full, "virtualmachine", (UserVm) migratedVm).get(0);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate vm");
            }
        } catch (ResourceUnavailableException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (VirtualMachineMigrationException | ConcurrentOperationException | ManagementServerException e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getSyncObjType() {
        return (getSyncObjId() != null) ? BaseAsyncCmd.migrationSyncObject : null;
    }

    @Override
    public Long getSyncObjId() {
        if (getStoragePoolId() != null) {
            return getStoragePoolId();
        }
        // OfflineVmwareMigrations: undocumented feature;
        // OfflineVmwareMigrations: on implementing a maximum queue size for per storage migrations it seems counter intuitive for the user to not enforce it for hosts as well.
        if (getHostId() != null) {
            return getHostId();
        }
        return null;
    }

    @Override
    public Long getApiResourceId() {
        return virtualMachineId;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }
}
