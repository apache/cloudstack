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

import java.util.List;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;

/**
 * Manages allocating resources to vms.
 */
public interface VmManager extends Manager {
    
    <T extends VMInstanceVO> VirtualMachineProfile allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkConfigurationVO, NicProfile>> networks, 
            DeploymentPlan plan,
            AccountVO owner) throws InsufficientCapacityException, StorageUnavailableException;
    
    <T extends VMInstanceVO> VirtualMachineProfile allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Long rootSize,
            Pair<DiskOfferingVO, Long> dataDiskOffering,
            List<Pair<NetworkConfigurationVO, NicProfile>> networks,
            DeploymentPlan plan,
            AccountVO owner) throws InsufficientCapacityException, StorageUnavailableException;
    
    <T extends VMInstanceVO> VirtualMachineProfile allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<Pair<NetworkConfigurationVO, NicProfile>> networkProfiles,
            DeploymentPlan plan,
            AccountVO owner) throws InsufficientCapacityException, StorageUnavailableException;
    
    <T extends VMInstanceVO> T start(T vm, DeploymentPlan plan, VirtualMachineChecker checker) throws InsufficientCapacityException, StorageUnavailableException, ConcurrentOperationException;
    
    <T extends VMInstanceVO> T stop(T vm) throws AgentUnavailableException, ConcurrentOperationException;
    
    void destroy();
    
}
