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
import org.apache.cloudstack.api.command.user.volume.ExtractVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.GetUploadParamsForVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;

import java.net.MalformedURLException;

public interface VolumeApiService {
    /**
     * Creates the database object for a volume based on the given criteria
     *
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the volume object
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
     * @throws ResourceAllocationException
     */
    Volume resizeVolume(ResizeVolumeCmd cmd) throws ResourceAllocationException;

    Volume migrateVolume(MigrateVolumeCmd cmd);

    /**
     * Uploads the volume to secondary storage
     *
     * @return Volume object
     */
    Volume uploadVolume(UploadVolumeCmd cmd)    throws ResourceAllocationException;

    GetUploadParamsResponse uploadVolume(GetUploadParamsForVolumeCmd cmd) throws ResourceAllocationException, MalformedURLException;

    boolean deleteVolume(long volumeId, Account caller) throws ConcurrentOperationException;

    Volume attachVolumeToVM(AttachVolumeCmd command);

    Volume detachVolumeFromVM(DetachVolumeCmd cmd);

    Snapshot takeSnapshot(Long volumeId, Long policyId, Long snapshotId, Account account, boolean quiescevm, Snapshot.LocationType locationType) throws ResourceAllocationException;

    Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType) throws ResourceAllocationException;

    Volume updateVolume(long volumeId, String path, String state, Long storageId, Boolean displayVolume, String customId, long owner, String chainInfo);

    /**
     * Extracts the volume to a particular location.
     *
     * @param cmd
     *            the command specifying url (where the volume needs to be extracted to), zoneId (zone where the volume
     *            exists),
     *            id (the id of the volume)
     *
     */
    String extractVolume(ExtractVolumeCmd cmd);

    boolean isDisplayResourceEnabled(Long id);

    void updateDisplay(Volume volume, Boolean displayVolume);

    Snapshot allocSnapshotForVm(Long vmId, Long volumeId, String snapshotName) throws ResourceAllocationException;
}
