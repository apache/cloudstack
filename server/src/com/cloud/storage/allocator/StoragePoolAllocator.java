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
package com.cloud.storage.allocator;

import java.util.Set;

import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.host.Host;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

/**
 * Allocator for a disk.  This determines which StoragePool should
 * a disk be allocated to.
 */
public interface StoragePoolAllocator extends Adapter {
	
	StoragePool allocateToPool(DiskCharacteristicsTO dskCh, ServiceOffering offering, DataCenterVO dc, HostPodVO pod, Long cluster, VMInstanceVO vm, VMTemplateVO template, Set<? extends StoragePool> avoids);
	
	String chooseStorageIp(VirtualMachine vm, Host host, Host storage);
}
