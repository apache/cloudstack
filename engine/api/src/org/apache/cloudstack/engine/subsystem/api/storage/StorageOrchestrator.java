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

import java.util.List;

import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

import com.cloud.deploy.DeploymentPlan;

public interface StorageOrchestrator {

    /**
     * Prepares all storage ready for a VM to start
     * @param vm
     * @param reservationId
     */
    void prepare(long vmId, DeploymentPlan plan, String reservationId);

    /**
     * Releases all storage that were used for a VM shutdown
     * @param vm
     * @param disks
     * @param reservationId
     */
    void release(long vmId, String reservationId);

    /**
     * Destroy all disks
     * @param disks
     * @param reservationId
     */
    void destroy(List<Long> disks, String reservationId);

    /**
     * Cancel a reservation
     * @param reservationId reservation to 
     */
    void cancel(String reservationId);
    
    /**
     * If attaching a volume in allocated state to a running vm, need to create this volume
     */
    void prepareAttachDiskToVM(long diskId, long vmId, String reservationId);
    
    boolean createVolume(VolumeEntity volume, long dataStoreId, DiskFormat diskType);
    boolean createVolumeFromTemplate(VolumeEntity volume, long dataStoreId, DiskFormat dis, TemplateEntity template);
    VolumeEntity allocateVolumeInDb(long size, VolumeType type,String volName, Long templateId);
}
