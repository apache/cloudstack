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

package com.cloud.agent.api;

import com.cloud.storage.Storage.StoragePoolType;

public class AttachVolumeCommand extends Command {
	
	boolean attach;
	String vmName;
	StoragePoolType pooltype;
	String volumeFolder;
	String volumePath;
	String volumeName;
	Long deviceId;
	
	protected AttachVolumeCommand() {
	}
	
	public AttachVolumeCommand(boolean attach, String vmName, StoragePoolType pooltype, String volumeFolder, String volumePath, String volumeName, Long deviceId) {
		this.attach = attach;
		this.vmName = vmName;
		this.pooltype = pooltype;
		this.volumeFolder = volumeFolder;
		this.volumePath = volumePath;
		this.volumeName = volumeName;
		this.deviceId = deviceId;
	}
	
	@Override
    public boolean executeInSequence() {
        return true;
    }
	
	public boolean getAttach() {
		return attach;
	}
	
	public String getVmName() {
		return vmName;
	}
	
	public StoragePoolType getPooltype() {
        return pooltype;
    }

    public void setPooltype(StoragePoolType pooltype) {
        this.pooltype = pooltype;
    }

    public String getVolumeFolder() {
		return volumeFolder;
	}
	
	public String getVolumePath() {
		return volumePath;
	}
	
	public String getVolumeName() {
		return volumeName;
	}

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
	
}
