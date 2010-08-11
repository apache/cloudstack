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

package com.cloud.async.executor;

import com.cloud.serializer.Param;

public class AttachVolumeOperationResultObject {
    @Param(name="virtualmachineid")
    private Long virtualMachineId;

    @Param(name="vmname")
    private String vmName;

    @Param(name="vmdisplayname")
    private String vmDisplayName;
    
    @Param(name="vmstate")
    private String vmState;

	@Param(name="id")
    private Long volumeId;

    @Param(name="name")
    private String volumeName;
    
    @Param(name="storagetype")
	String storageType;

	public AttachVolumeOperationResultObject() {
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }       
        
    public String getVmDisplayName() {
        return vmDisplayName;
    }

    public void setVmDisplayName(String vmDisplayName) {
        this.vmDisplayName = vmDisplayName;
    }       
      
    public String getVmState() {
		return vmState;
	}

	public void setVmState(String vmState) {
		this.vmState = vmState;
	}

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }
    
    public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}
}
