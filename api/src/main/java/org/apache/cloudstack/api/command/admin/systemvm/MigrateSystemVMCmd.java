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
package org.apache.cloudstack.api.command.admin.systemvm;

import java.util.HashMap;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
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
import com.cloud.vm.VirtualMachine;

@APICommand(name = "migrateSystemVm", description = "Attempts Migration of a system virtual machine to the host specified.", responseObject = SystemVmResponse.class, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class MigrateSystemVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateSystemVMCmd.class.getName());

    private static final String s_name = "migratesystemvmresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID,
               type = CommandType.UUID,
               entityType = HostResponse.class,
               description = "destination Host ID to migrate VM to")
    private Long hostId;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = SystemVmResponse.class,
               required = true,
               description = "the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.STORAGE_ID,
            since = "4.16.0",
            type = CommandType.UUID,
            entityType = StoragePoolResponse.class,
            description = "Destination storage pool ID to migrate VM volumes to. Required for migrating the root disk volume")
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

    public Long getStorageId() {
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
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        return "Attempting to migrate VM Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getVirtualMachineId()) + " to host Id: " + this._uuidMgr.getUuid(Host.class, getHostId());
    }

    @Override
    public void execute() {
        if (getHostId() == null && getStorageId() == null) {
            throw new InvalidParameterValueException("Either hostId or storageId must be specified");
        }

        if (getHostId() != null && getStorageId() != null) {
            throw new InvalidParameterValueException("Only one of hostId and storageId can be specified");
        }
        try {
            //FIXME : Should not be calling UserVmService to migrate all types of VMs - need a generic VM layer
            VirtualMachine migratedVm = null;
            if (getHostId() != null) {
                Host destinationHost = _resourceService.getHost(getHostId());
                if (destinationHost == null) {
                    throw new InvalidParameterValueException("Unable to find the host to migrate the VM, host id=" + getHostId());
                }
                if (destinationHost.getType() != Host.Type.Routing) {
                    throw new InvalidParameterValueException("The specified host(" + destinationHost.getName() + ") is not suitable to migrate the VM, please specify another one");
                }
                CallContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to host Id: " + getHostId());
                migratedVm = _userVmService.migrateVirtualMachineWithVolume(getVirtualMachineId(), destinationHost, new HashMap<String, String>());
            } else if (getStorageId() != null) {
                // OfflineMigration performed when this parameter is specified
                StoragePool destStoragePool = _storageService.getStoragePool(getStorageId());
                if (destStoragePool == null) {
                    throw new InvalidParameterValueException("Unable to find the storage pool to migrate the VM");
                }
                CallContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to storage pool Id: " + getStorageId());
                migratedVm = _userVmService.vmStorageMigration(getVirtualMachineId(), destStoragePool);
            }
            if (migratedVm != null) {
                // return the generic system VM instance response
                SystemVmResponse response = _responseGenerator.createSystemVmResponse(migratedVm);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate the system vm");
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
