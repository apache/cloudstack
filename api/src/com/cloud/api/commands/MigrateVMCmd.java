/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@Implementation(description="Attempts Migration of a VM to a different host or Root volume of the vm to a different storage pool", responseObject=UserVmResponse.class)
public class MigrateVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateVMCmd.class.getName());

    private static final String s_name = "migratevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="host")
    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, required=false, description="Destination Host ID to migrate VM to. Required for live migrating a VM from host to host")
    private Long hostId;

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the ID of the virtual machine")
    private Long virtualMachineId;

    @IdentityMapper(entityTableName="storage_pool")
    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.LONG, required=false, description="Destination storage pool ID to migrate VM volumes to. Required for migrating the root disk volume")
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
        	UserContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to host Id: "+ getHostId());
        }
        
        StoragePool destStoragePool = null;
        if (getStoragePoolId() != null) {
        	destStoragePool = _storageService.getStoragePool(getStoragePoolId());
        	if (destStoragePool == null) {
        		throw new InvalidParameterValueException("Unable to find the storage pool to migrate the VM");
        	}
        	UserContext.current().setEventDetails("VM Id: " + getVirtualMachineId() + " to storage pool Id: "+ getStoragePoolId());
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
	            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to migrate vm");
	        }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
		} catch (ManagementServerException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
		} catch (VirtualMachineMigrationException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
		}  
    }
}
