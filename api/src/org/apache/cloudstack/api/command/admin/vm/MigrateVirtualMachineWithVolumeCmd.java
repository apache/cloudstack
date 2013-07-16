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

import org.apache.cloudstack.api.*;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

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

@APICommand(name = "migrateVirtualMachineWithVolume", description="Attempts Migration of a VM with its volumes to a different host", responseObject=UserVmResponse.class)
public class MigrateVirtualMachineWithVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateVMCmd.class.getName());

    private static final String s_name = "migratevirtualmachinewithvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.UUID, entityType=HostResponse.class,
            required=true, description="Destination Host ID to migrate VM to.")
    private Long hostId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.MIGRATE_TO, type = CommandType.MAP, required=false,
            description = "Map of pool to which each volume should be migrated (volume/pool pair)")
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
        if (migrateVolumeTo != null && !migrateVolumeTo.isEmpty()) {
            Collection<?> allValues = migrateVolumeTo.values();
            Iterator<?> iter = allValues.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> volumeToPool = (HashMap<String, String>) iter.next();
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
        return  "Attempting to migrate VM Id: " + getVirtualMachineId() + " to host Id: "+ getHostId();
    }

    @Override
    public void execute(){
        UserVm userVm = _userVmService.getUserVm(getVirtualMachineId());
        if (userVm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + getVirtualMachineId());
        }

        Host destinationHost = _resourceService.getHost(getHostId());
        if (destinationHost == null) {
            throw new InvalidParameterValueException("Unable to find the host to migrate the VM, host id =" + getHostId());
        }

        try{
            VirtualMachine migratedVm = _userVmService.migrateVirtualMachineWithVolume(getVirtualMachineId(),
                    destinationHost, getVolumeToPool());
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
