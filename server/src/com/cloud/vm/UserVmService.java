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
package com.cloud.vm;

import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateTemplateCmd;
import com.cloud.api.commands.CreateVMGroupCmd;
import com.cloud.api.commands.DeleteVMGroupCmd;
import com.cloud.api.commands.DeployVm2Cmd;
import com.cloud.api.commands.DestroyVMCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.RebootVMCmd;
import com.cloud.api.commands.RecoverVMCmd;
import com.cloud.api.commands.ResetVMPasswordCmd;
import com.cloud.api.commands.StartVMCmd;
import com.cloud.api.commands.StopVMCmd;
import com.cloud.api.commands.StopVm2Cmd;
import com.cloud.api.commands.UpdateVMCmd;
import com.cloud.api.commands.UpgradeVMCmd;
import com.cloud.async.executor.OperationResponse;
import com.cloud.async.executor.RebootVMExecutor;
import com.cloud.async.executor.VMOperationParam;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.ExecutionException;

public interface UserVmService extends Manager {
    /**
     * Destroys one virtual machine
     * @param userId the id of the user performing the action
     * @param vmId the id of the virtual machine.
     */
    UserVm destroyVm(DestroyVMCmd cmd);
    
    /**
     * Resets the password of a virtual machine.
     * @param cmd - the command specifying vmId, password
     * @return the VM if reset worked successfully, null otherwise
     */
    UserVm resetVMPassword(ResetVMPasswordCmd cmd);
    
    /**
     * Attaches the specified volume to the specified VM
     * @param cmd - the command specifying volumeId and vmId
     * @return the Volume object if attach worked successfully.
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    Volume attachVolumeToVM(AttachVolumeCmd cmd);
    
    /**
     * Detaches the specified volume from the VM it is currently attached to.
     * @param cmd - the command specifying volumeId
     * @return the Volume object if detach worked successfully.
     * @throws InvalidParameterValueException 
     */
    Volume detachVolumeFromVM(DetachVolumeCmd cmmd);
    
    UserVm startVirtualMachine(StartVMCmd cmd) throws StorageUnavailableException, ExecutionException, ConcurrentOperationException;
    UserVm stopVirtualMachine(StopVMCmd cmd);
    UserVm rebootVirtualMachine(RebootVMCmd cmd);
    UserVm updateVirtualMachine(UpdateVMCmd cmd);
    UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException;
    
    @Deprecated
    OperationResponse executeRebootVM(RebootVMExecutor executor, VMOperationParam param);
    
    /**
     * Create a template database record in preparation for creating a private template.
     * @param cmd the command object that defines the name, display text, snapshot/volume, bits, public/private, etc.
     * for the private template
     * @return the vm template object if successful, null otherwise
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    VMTemplateVO createPrivateTemplateRecord(CreateTemplateCmd cmd);
    
    /**
     * Creates a private template from a snapshot of a VM
     * @param cmd - the command specifying snapshotId, name, description
     * @return a template if successfully created, null otherwise
     * @throws InvalidParameterValueException
     */
    VMTemplateVO createPrivateTemplate(CreateTemplateCmd cmd);
    
    /**
     * Creates a User VM in the database and returns the VM to the caller.
     *  
     * @param cmd Command to deploy.
     * @return UserVm object if successful.
     * 
     * @throws InsufficientCapacityException if there is insufficient capacity to deploy the VM.
     * @throws ConcurrentOperationException if there are multiple users working on the same VM or in the same environment.
     * @throws ResourceUnavailableException if the resources required to deploy the VM is not currently available.
     * @throws PermissionDeniedException if the caller doesn't have any access rights to the VM.
     * @throws InvalidParameterValueException if the parameters are incorrect. 
     */
    UserVm createVirtualMachine(DeployVm2Cmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Starts the virtual machine created from createVirtualMachine.
     *  
     * @param cmd Command to deploy.
     * @return UserVm object if successful.
     * @throws InsufficientCapacityException if there is insufficient capacity to deploy the VM.
     * @throws ConcurrentOperationException if there are multiple users working on the same VM.
     * @throws ResourceUnavailableException if the resources required the deploy the VM is not currently available.
     */
    UserVm startVirtualMachine(DeployVm2Cmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Creates a vm group.
     * @param name - name of the group
     * @param accountId - accountId
     */
    InstanceGroupVO createVmGroup(CreateVMGroupCmd cmd);

    boolean deleteVmGroup(DeleteVMGroupCmd cmd);

    /**
     * upgrade the service offering of the virtual machine
     * @param cmd - the command specifying vmId and new serviceOfferingId
     * @return the vm
     * @throws InvalidParameterValueException 
     */
    UserVm upgradeVirtualMachine(UpgradeVMCmd cmd);
    
    UserVm stopVirtualMachine(StopVm2Cmd cmd) throws ConcurrentOperationException;
}
