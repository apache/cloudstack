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
package com.cloud.resource.storage;

import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.resource.ServerResource;

/**
 * StoragePoolHeadResource specifies the commands that must be implemented
 * by a storage pool head.  A storage pool head is a machine that can access
 * a primary storage.
 *
 */
public interface PrimaryStorageHeadResource extends ServerResource {
    /**
     * Downloads the template to the primary storage.
     * @param cmd
     * @return
     */
    DownloadAnswer execute(PrimaryStorageDownloadCommand cmd);
    
    /**
     * Creates volumes for the VM.
     * @param cmd
     * @return
     */
    CreateAnswer execute(CreateCommand cmd);
    
    /**
     * Destroys volumes for the VM.
     * @param cmd
     * @return
     */
    DestroyAnswer execute(DestroyCommand cmd);
}
