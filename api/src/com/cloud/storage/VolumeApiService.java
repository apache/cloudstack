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
package com.cloud.storage;

import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

public interface VolumeApiService {
    /**
     * Creates the database object for a volume based on the given criteria
     *
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the volume object
     * @throws PermissionDeniedException
     */
    Volume allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException;
    
    /**
     * Creates the volume based on the given criteria
     *
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the volume object
     */
    Volume createVolume(CreateVolumeCmd cmd);


    /**
     * Resizes the volume based on the given criteria
     * 
     * @param cmd
     *            the API command wrapping the criteria
     * @return the volume object
     */
    Volume resizeVolume(ResizeVolumeCmd cmd);
    
    Volume migrateVolume(Long volumeId, Long storagePoolId) throws ConcurrentOperationException;

    /**
     * Uploads the volume to secondary storage
     *
     * @param UploadVolumeCmd cmd
     *
     * @return Volume object
     */
    Volume uploadVolume(UploadVolumeCmd cmd)    throws ResourceAllocationException;

    boolean deleteVolume(long volumeId, Account caller) throws ConcurrentOperationException;

    Volume attachVolumeToVM(AttachVolumeCmd command);

    Volume detachVolumeFromVM(DetachVolumeCmd cmmd);
}
