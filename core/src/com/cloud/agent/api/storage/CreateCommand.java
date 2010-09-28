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
package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.StoragePoolTO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;

public class CreateCommand extends Command {
    private long volId;
    private StoragePoolTO pool;
    private DiskProfile diskCharacteristics;
    private String templateUrl;
    private long size;
    private String instanceName;
    
    protected CreateCommand() {
        super();
    }

    /**
     * Construction for template based volumes.
     * 
     * @param vol
     * @param vm
     * @param diskCharacteristics
     * @param templateUrl
     * @param pool
     */
    public CreateCommand(VolumeVO vol, VMInstanceVO vm, DiskProfile diskCharacteristics, String templateUrl, StoragePoolVO pool) {
        this(vol, vm, diskCharacteristics, pool, 0);
        this.templateUrl = templateUrl;
    }

    /**
     * Construction for regular volumes.
     * 
     * @param vol
     * @param vm
     * @param diskCharacteristics
     * @param pool
     */
    public CreateCommand(VolumeVO vol, VMInstanceVO vm, DiskProfile diskCharacteristics, StoragePoolVO pool, long size) {
        this.volId = vol.getId();
        this.diskCharacteristics = diskCharacteristics;        
        this.pool = new StoragePoolTO(pool);
        this.templateUrl = null;
        this.size = size;
        //this.instanceName = vm.getInstanceName();
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }
    
    public StoragePoolTO getPool() {
        return pool;
    }
    
    public DiskProfile getDiskCharacteristics() {
        return diskCharacteristics;
    }
    
    public long getVolumeId() {
        return volId;
    }
    
    public long getSize(){
    	return this.size;
    }
    
    public String getInstanceName() {
    	return instanceName;
    }
}
