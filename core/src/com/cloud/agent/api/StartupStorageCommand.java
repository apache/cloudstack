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

import java.util.HashMap;
import java.util.Map;

import com.cloud.storage.Volume;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.template.TemplateInfo;


public class StartupStorageCommand extends StartupCommand {
	
	String parent;
    Map<String, TemplateInfo> templateInfo;
    long totalSize;
    StoragePoolInfo poolInfo;
    Volume.StorageResourceType resourceType;
    StoragePoolType fsType;
    Map<String, String> hostDetails = new HashMap<String, String>();
    String nfsShare;

    public StartupStorageCommand() {
        super();
    }
    
    public StartupStorageCommand(String parent, StoragePoolType fsType, long totalSize, Map<String, TemplateInfo> info) {
        super();
        this.parent = parent;
        this.totalSize = totalSize;
        this.templateInfo = info;
        this.poolInfo = null;
        this.fsType = fsType;
    }
    

    public StartupStorageCommand(String parent, StoragePoolType fsType, Map<String, TemplateInfo> templateInfo, StoragePoolInfo poolInfo) {
		super();
		this.parent = parent;
		this.templateInfo = templateInfo;
		this.totalSize = poolInfo.capacityBytes;
		this.poolInfo = poolInfo;
		this.fsType = fsType;
	}

	public String getParent() {
        return parent;
    }
	
	public void setNfsShare(String nfsShare) {
	    this.nfsShare = nfsShare;
	}
	
	public String getNfsShare() {
	    return nfsShare;
	}
    
    public long getTotalSize() {
        return totalSize;
    }
    
	public Map<String, TemplateInfo> getTemplateInfo() {
		return templateInfo;
	}

	public void setTemplateInfo(Map<String, TemplateInfo> templateInfo) {
		this.templateInfo = templateInfo;
	}

	public StoragePoolInfo getPoolInfo() {
		return poolInfo;
	}

	public void setPoolInfo(StoragePoolInfo poolInfo) {
		this.poolInfo = poolInfo;
	}

	public Volume.StorageResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(Volume.StorageResourceType resourceType) {
		this.resourceType = resourceType;
	}

	/*For secondary storage*/
	public Map<String, String> getHostDetails() {
		return hostDetails;
	}
}
