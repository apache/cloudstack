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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

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

@APICommand(name = "migrateVirtualMachine", description="Attempts Migration of a VM to a different host or Root volume of the vm to a different storage pool", responseObject=UserVmResponse.class)
public class MigrateVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateVMCmd.class.getName());

    private static final String s_name = "migratevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.UUID, entityType=HostResponse.class,
            required=false, description="Destination Host ID to migrate VM to. Required for live migrating a VM from host to host")
    private Long hostId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.UUID, entityType=StoragePoolResponse.class,
            required=false, description="Destination storage pool ID to migrate VM volumes to. Required for migrating the root disk volume")
    private Long storageId;

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


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

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
        return  "Attempting to migrate VM Id: " + getVirtualMachineId() + " to host Id: "+ getHostId();
    }

    @Override
    public void execute(){
        if (getHostId() == null && getStoragePoolId() == null) {
            throw new InvalidParameterValueException("either hostId or storageId must be specified");
        }

        if (getHostId() != null && getStoragePoolId() != null) {
            throw new InvalidParameterValueException("only one of hostId and storageId can be specified");
        }

        UserVm userVm = _userVmService.getUserVm(getVirtualMachineId());
        if (userVm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + getVirtualMachineId());
        }

        Host destinationHost = null;
        if (getHostId() != null) {
            destinationHost = _resourceService.getHost(getHostId());
            if (destinationHost == null) {
                throw new InvalidParameterValueException("Unable to find the host to migrate the VM, host id=" + getHostId());
            }
            CallContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to host Id: "+ getHostId());
        }

        StoragePool destStoragePool = null;
        if (getStoragePoolId() != null) {
            destStoragePool = _storageService.getStoragePool(getStoragePoolId());
            if (destStoragePool == null) {
                throw new InvalidParameterValueException("Unable to find the storage pool to migrate the VM");
            }
            CallContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to storage pool Id: "+ getStoragePoolId());
        }

        try{
            VirtualMachine migratedVm = null;
            if (getHostId() != null) {
                migratedVm = _userVmService.migrateVirtualMachine(getVirtualMachineId(), destinationHost);
            } else if (getStoragePoolId() != null) {
                migratedVm = _userVmService.vmStorageMigration(getVirtualMachineId(), destStoragePool);
            }
            if (migratedVm != null) {
                UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", (UserVm)migratedVm).get(0);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate vm");
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (ManagementServerException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (VirtualMachineMigrationException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
