/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.volume;

import org.apache.cloudstack.platform.subsystem.api.storage.Volume;

import com.cloud.api.commands.CreateVolumeCmd;

public interface VolumeService {
	 /**
     * Creates the database object for a volume based on the given criteria
     * 
     * @param cmd
     * @return the volume object
     * @throws PermissionDeniedException
     */
    Volume allocVolumeInDB(CreateVolumeCmd cmd);

    /**
     * Creates the volume based on the given criteria
     * 
     * @param cmd
     *            
     * @return the volume object
     */
    Volume createVolume(CreateVolumeCmd cmd);

    /**
     * Delete volume
     * @param volumeId
     * @return
     * @throws ConcurrentOperationException
     */
    boolean deleteVolume(long volumeId);

    /**
     * Migrate volume to another storage pool
     * @param volumeId
     * @param storagePoolId
     * @return
     * @throws ConcurrentOperationException
     */
    Volume migrateVolume(Long volumeId, Long storagePoolId);
    
    /**
     * Copy volume another storage pool, a new volume will be created on destination storage pool
     * @param volumeId
     * @param destStoragePoolId
     * @return
     */
    Volume copyVolume(Long volumeId, Long destStoragePoolId);
}
