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

import java.net.MalformedURLException;
import java.util.Map;

import com.cloud.utils.fsm.NoTransitionException;
import org.apache.cloudstack.api.command.user.volume.AssignVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ChangeOfferingForVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ExtractVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.GetUploadParamsForVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

public interface VolumeApiService {

    ConfigKey<Long> ConcurrentMigrationsThresholdPerDatastore = new ConfigKey<Long>("Advanced"
            , Long.class
            , "concurrent.migrations.per.target.datastore"
            , "0"
            , "Limits number of migrations that can be handled per datastore concurrently; default is 0 - unlimited"
            , true // not sure if this is to be dynamic
            , ConfigKey.Scope.Global);

    ConfigKey<Boolean> UseHttpsToUpload = new ConfigKey<Boolean>("Advanced",
            Boolean.class,
            "use.https.to.upload",
            "true",
            "Determines the protocol (HTTPS or HTTP) ACS will use to generate links to upload ISOs, volumes, and templates. When set as 'true', ACS will use protocol HTTPS, otherwise, it will use protocol HTTP. Default value is 'true'.",
            true,
            ConfigKey.Scope.StoragePool);

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
    Volume uploadVolume(UploadVolumeCmd cmd) throws ResourceAllocationException;

    GetUploadParamsResponse uploadVolume(GetUploadParamsForVolumeCmd cmd) throws ResourceAllocationException, MalformedURLException;

    boolean deleteVolume(long volumeId, Account caller);

    Volume attachVolumeToVM(AttachVolumeCmd command);

    Volume detachVolumeViaDestroyVM(long vmId, long volumeId);

    Volume detachVolumeFromVM(DetachVolumeCmd cmd);

    Snapshot takeSnapshot(Long volumeId, Long policyId, Long snapshotId, Account account, boolean quiescevm, Snapshot.LocationType locationType, boolean asyncBackup, Map<String, String> tags)
            throws ResourceAllocationException;

    Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType) throws ResourceAllocationException;

    Volume updateVolume(long volumeId, String path, String state, Long storageId, Boolean displayVolume, String customId, long owner, String chainInfo, String name);

    /**
     * Extracts the volume to a particular location.
     *
     * @param cmd
     *            the command specifying url (where the volume needs to be extracted to), zoneId (zone where the volume exists),
     *            id (the id of the volume)
     */
    String extractVolume(ExtractVolumeCmd cmd);

    Volume assignVolumeToAccount(AssignVolumeCmd cmd) throws ResourceAllocationException;

    boolean isDisplayResourceEnabled(Long id);

    void updateDisplay(Volume volume, Boolean displayVolume);

    Snapshot allocSnapshotForVm(Long vmId, Long volumeId, String snapshotName) throws ResourceAllocationException;

    /**
     *  Checks if the target storage supports the disk offering.
     *  This validation is consistent with the mechanism used to select a storage pool to deploy a volume when a virtual machine is deployed or when a data disk is allocated.
     *
     *  The scenarios when this method returns true or false is presented in the following table.
     *   <table border="1">
     *      <tr>
     *          <th>#</th><th>Disk offering tags</th><th>Storage tags</th><th>Does the storage support the disk offering?</th>
     *      </tr>
     *      <body>
     *      <tr>
     *          <td>1</td><td>A,B</td><td>A</td><td>NO</td>
     *      </tr>
     *      <tr>
     *          <td>2</td><td>A,B,C</td><td>A,B,C,D,X</td><td>YES</td>
     *      </tr>
     *      <tr>
     *          <td>3</td><td>A,B,C</td><td>X,Y,Z</td><td>NO</td>
     *      </tr>
     *      <tr>
     *          <td>4</td><td>null</td><td>A,S,D</td><td>YES</td>
     *      </tr>
     *      <tr>
     *          <td>5</td><td>A</td><td>null</td><td>NO</td>
     *      </tr>
     *      <tr>
     *          <td>6</td><td>null</td><td>null</td><td>YES</td>
     *      </tr>
     *      </body>
     *   </table>
     */
    boolean doesTargetStorageSupportDiskOffering(StoragePool destPool, String diskOfferingTags);

    Volume destroyVolume(long volumeId, Account caller, boolean expunge, boolean forceExpunge);

    Volume recoverVolume(long volumeId);

    void validateCustomDiskOfferingSizeRange(Long sizeInGB);

    boolean validateVolumeSizeInBytes(long size);

    Volume changeDiskOfferingForVolume(ChangeOfferingForVolumeCmd cmd) throws ResourceAllocationException;

    void publishVolumeCreationUsageEvent(Volume volume);

    boolean stateTransitTo(Volume vol, Volume.Event event) throws NoTransitionException;
}
