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
package org.apache.cloudstack.engine.subsystem.api.storage;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.framework.async.AsyncCallFuture;

import com.cloud.exception.ConcurrentOperationException;


public interface VolumeService {
    
    public class VolumeApiResult extends CommandResult {
        private final VolumeInfo volume;
        public VolumeApiResult(VolumeInfo volume) {
            this.volume = volume;
        }
        
        public VolumeInfo getVolume() {
            return this.volume;
        }
    }

    /**
     * Creates the volume based on the given criteria
     * 
     * @param cmd
     * 
     * @return the volume object
     */
    AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, DataStore store);

    /**
     * Delete volume
     * 
     * @param volumeId
     * @return
     * @throws ConcurrentOperationException
     */
    AsyncCallFuture<VolumeApiResult> expungeVolumeAsync(VolumeInfo volume);

    /**
     * 
     */
    boolean cloneVolume(long volumeId, long baseVolId);

    /**
     * 
     */
    AsyncCallFuture<VolumeApiResult> createVolumeFromSnapshot(VolumeInfo volume, DataStore store,  SnapshotInfo snapshot);


    VolumeEntity getVolumeEntity(long volumeId);

    AsyncCallFuture<VolumeApiResult> createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, TemplateInfo template);
    AsyncCallFuture<VolumeApiResult> copyVolume(VolumeInfo srcVolume, DataStore destStore);

    boolean destroyVolume(long volumeId) throws ConcurrentOperationException;

    AsyncCallFuture<VolumeApiResult> registerVolume(VolumeInfo volume, DataStore store);

}
