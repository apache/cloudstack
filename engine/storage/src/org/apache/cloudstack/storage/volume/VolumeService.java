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

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.image.TemplateInfo;

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
	 * 
	 */
    VolumeEntity allocateVolumeInDb(long size, VolumeType type, String volName, Long templateId);

    /**
     * Creates the volume based on the given criteria
     * 
     * @param cmd
     * 
     * @return the volume object
     */
    AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, long dataStoreId);

    /**
     * Delete volume
     * 
     * @param volumeId
     * @return
     * @throws ConcurrentOperationException
     */
    AsyncCallFuture<VolumeApiResult> deleteVolumeAsync(VolumeInfo volume);

    /**
     * 
     */
    boolean cloneVolume(long volumeId, long baseVolId);

    /**
     * 
     */
    boolean createVolumeFromSnapshot(long volumeId, long snapshotId);

    /**
     * 
     */
    String grantAccess(VolumeInfo volume, EndPoint endpointId);

    TemplateOnPrimaryDataStoreInfo grantAccess(TemplateOnPrimaryDataStoreInfo template, EndPoint endPoint);

    /**
     * 
     */
    boolean rokeAccess(long volumeId, long endpointId);

    VolumeEntity getVolumeEntity(long volumeId);

    AsyncCallFuture<VolumeApiResult> createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, TemplateInfo template);
}
