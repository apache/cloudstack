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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "migrateVirtualMachineWithVolume",
            description = "Attempts Migration of a VM with its volumes to a different host",
            responseObject = UserVmResponse.class, entityType = {VirtualMachine.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class MigrateVirtualMachineWithVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateVMCmd.class.getName());

    private static final String s_name = "migratevirtualmachinewithvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID,
               type = CommandType.UUID,
               entityType = HostResponse.class,
               description = "Destination Host ID to migrate VM to.")
    private Long hostId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               required = true,
               description = "the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.MIGRATE_TO,
               type = CommandType.MAP,
               required = false,
               description = "Storage to pool mapping. This parameter specifies the mapping between a volume and a pool where you want to migrate that volume. Format of this " +
               "parameter: migrateto[volume-index].volume=<uuid>&migrateto[volume-index].pool=<uuid>Where, [volume-index] indicates the index to identify the volume that you " +
               "want to migrate, volume=<uuid> indicates the UUID of the volume that you want to migrate, and pool=<uuid> indicates the UUID of the pool where you want to " +
               "migrate the volume. Example: migrateto[0].volume=<71f43cd6-69b0-4d3b-9fbc-67f50963d60b>&migrateto[0].pool=<a382f181-3d2b-4413-b92d-b8931befa7e1>&" +
               "migrateto[1].volume=<88de0173-55c0-4c1c-a269-83d0279eeedf>&migrateto[1].pool=<95d6e97c-6766-4d67-9a30-c449c15011d1>&migrateto[2].volume=" +
               "<1b331390-59f2-4796-9993-bf11c6e76225>&migrateto[2].pool=<41fdb564-9d3b-447d-88ed-7628f7640cbc>")
    private Map migrateVolumeTo;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Map<String, String> getVolumeToPool() {
        Map<String, String> volumeToPoolMap = new HashMap<String, String>();
        if (MapUtils.isNotEmpty(migrateVolumeTo)) {
            Collection<?> allValues = migrateVolumeTo.values();
            Iterator<?> iter = allValues.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> volumeToPool = (HashMap<String, String>)iter.next();
                String volume = volumeToPool.get("volume");
                String pool = volumeToPool.get("pool");
                volumeToPoolMap.put(volume, pool);
            }
        }
        return volumeToPoolMap;
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

        return Account.ACCOUNT_ID_SYSTEM;
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
        if (hostId == null && MapUtils.isEmpty(migrateVolumeTo)) {
            throw new InvalidParameterValueException(String.format("Either %s or %s  must be passed for migrating the VM", ApiConstants.HOST_ID, ApiConstants.MIGRATE_TO));
        }

        UserVm userVm = _userVmService.getUserVm(getVirtualMachineId());
        if (userVm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + getVirtualMachineId());
        }

        if (!VirtualMachine.State.Running.equals(userVm.getState()) && hostId != null) {
            throw new InvalidParameterValueException(String.format("VM ID: %s is not in Running state to migrate it to new host", userVm.getUuid()));
        }

        if (!VirtualMachine.State.Stopped.equals(userVm.getState()) && hostId == null) {
            throw new InvalidParameterValueException(String.format("VM ID: %s is not in Stopped state to migrate, use %s parameter to migrate it to a new host", userVm.getUuid(), ApiConstants.HOST_ID));
        }

        try {
            VirtualMachine migratedVm = null;
            if (hostId != null) {
                Host destinationHost = _resourceService.getHost(getHostId());
                // OfflineVmwareMigration: destination host would have to not be a required parameter for stopped VMs
                if (destinationHost == null) {
                    throw new InvalidParameterValueException("Unable to find the host to migrate the VM, host id =" + getHostId());
                }
                migratedVm = _userVmService.migrateVirtualMachineWithVolume(getVirtualMachineId(), destinationHost, getVolumeToPool());
            } else if (MapUtils.isNotEmpty(migrateVolumeTo)) {
                migratedVm = _userVmService.vmStorageMigration(getVirtualMachineId(), getVolumeToPool());
            }
            if (migratedVm != null) {
                UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full, "virtualmachine", (UserVm)migratedVm).get(0);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to migrate vm");
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException | ManagementServerException | VirtualMachineMigrationException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
