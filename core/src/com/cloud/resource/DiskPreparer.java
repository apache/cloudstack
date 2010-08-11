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
package com.cloud.resource;

import com.cloud.storage.VolumeVO;
import com.cloud.storage.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.component.Adapter;

/**
 * DiskMounter mounts and unmounts disk for VMs
 * to consume.
 *
 */
public interface DiskPreparer extends Adapter {
    /**
     * Mounts a volumeVO and returns a path.
     * 
     * @param vol
     * @return
     */
    public String mount(String vmName, VolumeVO vol, BootloaderType type);
    
    /**
     * Unmounts
     */
    public boolean unmount(String path);

}
