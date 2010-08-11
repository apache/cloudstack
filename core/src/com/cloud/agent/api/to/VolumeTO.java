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
package com.cloud.agent.api.to;

import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.StorageResourceType;


public class VolumeTO {
    protected VolumeTO() {
    }
    
    private long id;
    private String name;
    private String mountPoint;
    private String path;
    private long size;
    private Volume.VolumeType type;
    private Volume.StorageResourceType resourceType;
    private StoragePoolType storagePoolType;
    private long poolId;
    
    public VolumeTO(long id, Volume.VolumeType type, Volume.StorageResourceType resourceType, StoragePoolType poolType, String name, String mountPoint, String path, long size) {
        this.id = id;
        this.name= name;
        this.path = path;
        this.size = size;
        this.type = type;
        this.resourceType = resourceType;
        this.storagePoolType = poolType;
        this.mountPoint = mountPoint;
    }
    
    public VolumeTO(VolumeVO volume, StoragePoolVO pool) {
        this.id = volume.getId();
        this.name = volume.getName();
        this.path = volume.getPath();
        this.size = volume.getSize();
        this.type = volume.getVolumeType();
        this.resourceType = volume.getStorageResourceType();
        this.storagePoolType = pool.getPoolType();
        this.mountPoint = volume.getFolder();
    }
    
    public VolumeTO(VMTemplateStoragePoolVO templatePoolRef, StoragePoolVO pool) {
    	this.id = templatePoolRef.getId();
    	this.path = templatePoolRef.getInstallPath();
    	this.size = templatePoolRef.getTemplateSize();
    	this.resourceType = StorageResourceType.STORAGE_POOL;
    	this.storagePoolType = pool.getPoolType();
    	this.mountPoint = pool.getPath();
    }

    public Volume.StorageResourceType getResourceType() {
        return resourceType;
    }
    
    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public Volume.VolumeType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
    
    public String getMountPoint() {
        return mountPoint;
    }
    
    public StoragePoolType getPoolType() {
        return storagePoolType;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Vol[").append(id).append("|").append(type).append("|").append(path).append("|").append(size).append("]").toString();
    }
}
