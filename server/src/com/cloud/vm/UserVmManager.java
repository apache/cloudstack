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

import java.util.HashMap;
import java.util.List;

import com.cloud.agent.api.VmStatsEntry;
import com.cloud.async.executor.OperationResponse;
import com.cloud.async.executor.StartVMExecutor;
import com.cloud.async.executor.StopVMExecutor;
import com.cloud.async.executor.VMOperationParam;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.AccountVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.VirtualMachine.Event;

/**
 *
 * UserVmManager contains all of the code to work with user VMs.
 * 
 */
public interface UserVmManager extends VirtualMachineManager<UserVmVO> {

	static final int MAX_USER_DATA_LENGTH_BYTES = 2048;
    /**
     * @param hostId get all of the virtual machines that belong to one host.
     * @return collection of VirtualMachine.
     */
    List<? extends UserVm> getVirtualMachines(long hostId);
    
    /**
     * @param vmId id of the virtual machine.
     * @return VirtualMachine
     */
    UserVmVO getVirtualMachine(long vmId);
    
    /**
     * creates a virtual machine.
     * @param userId the id of the user performing the action
     * @param account account creating the virtual machine.
     * @param dc data center to deploy it in.
     * @param offering the service offering that comes with it.
     * @param template template to base the virtual machine on. Can either represent an ISO, or a normal template.
     * @param diskOffering the disk offering for the root disk (deploying from ISO) or the data disk (deploying from a normal template)
     * @return UserVmVO if created; null if not.
     */
    UserVmVO createVirtualMachine(Long vmId, long userId, AccountVO account, DataCenterVO dc, ServiceOfferingVO offering, VMTemplateVO template, DiskOfferingVO diskOffering, String displayName, String userData, List<StoragePoolVO> avoids, long startEventId, long size) throws InsufficientStorageCapacityException, ResourceAllocationException;
    
	UserVmVO createDirectlyAttachedVM(Long vmId, long userId, AccountVO account, DataCenterVO dc, ServiceOfferingVO offering, VMTemplateVO template, DiskOfferingVO diskOffering, String displayName, String userData, List<StoragePoolVO> a, List<NetworkGroupVO> networkGroupVO, long startEventId, long size) throws ResourceAllocationException;

	UserVmVO createDirectlyAttachedVMExternal(Long vmId, long userId, AccountVO account, DataCenterVO dc, ServiceOfferingVO offering, VMTemplateVO template, DiskOfferingVO diskOffering, String displayName, String userData, List<StoragePoolVO> a, List<NetworkGroupVO> networkGroupVO, long startEventId, long size) throws ResourceAllocationException;

    boolean destroyVirtualMachine(long userId, long vmId);
//    OperationResponse executeDestroyVM(DestroyVMExecutor executor, VMOperationParam param);
    
    
    /**
     * Attaches an ISO to the virtual CDROM device of the specified VM. Will eject any existing virtual CDROM if isoPath is null.
     * @param vmId
     * @param isoId
     * @param attach whether to attach or detach the given iso
     * @return
     */
    boolean attachISOToVM(long vmId, long isoId, boolean attach);
    
    /**
     * Start the virtual machine.
     * @param userId the id of the user performing the action
     * @param vmId the id of the virtual machine.
     * @param isoPath path of the ISO from which the VM should be booted (optional)
     * @throws ExecutionException 
     * @throws StorageUnavailableException 
     * @throws ConcurrentOperationException 
     */
    UserVmVO startVirtualMachine(long userId, long vmId, String isoPath, long startEventId) throws ExecutionException, StorageUnavailableException, ConcurrentOperationException;
    boolean executeStartVM(StartVMExecutor executor, VMOperationParam param);
    
    /**
     * Start the virtual machine.
     * @param userId the id of the user performing the action
     * @param vmId the id of the virtual machine.
     * @param password the password that the user wants to use to access the virtual machine
     * @param isoPath path of the ISO from which the VM should be booted (optional)
     * @throws ExecutionException 
     * @throws StorageUnavailableException 
     * @throws ConcurrentOperationException 
     */
    UserVmVO startVirtualMachine(long userId, long vmId, String password, String isoPath, long startEventId) throws ExecutionException, StorageUnavailableException, ConcurrentOperationException;
    
    /**
     * Stops the virtual machine
     * @param userId the id of the user performing the action
     * @param vmId
     * @param eventId -- id of the scheduled event for stopping vm
     * @return true if stopped; false if problems.
     */
    boolean stopVirtualMachine(long userId, long vmId, long eventId);
    void completeStopCommand(long userId, UserVmVO vm, Event e, long startEventId);
    

    /**
     * Obtains statistics for a list of host or VMs; CPU and network utilization
     * @param host ID
     * @param host name
     * @param list of VM IDs or host id
     * @return GetVmStatsAnswer
     */
    HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds);
    
    /**
     * Clean the network rules for the given VM
     * @param userId
     * @param instanceId the id of the instance for which the network rules should be cleaned
     */
    void cleanNetworkRules(long userId, long instanceId);
    
    /**
     * Releases a guest IP address for a VM. If the VM is on a direct attached network, will also unassign the IP address.
     * @param userVm
     */
    void releaseGuestIpAddress(UserVmVO userVm);

    boolean deleteVmGroup(long groupId);

    boolean addInstanceToGroup(long userVmId, String group);

    InstanceGroupVO getGroupForVm(long vmId);
    
    void removeInstanceFromGroup(long vmId);
    
    @Deprecated
    OperationResponse executeStopVM(StopVMExecutor executor, VMOperationParam param);

}
