//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.agent.api;

import com.cloud.storage.StoragePool;

/**
 * This currently assumes that both primary and secondary storage are mounted on the XenServer.
 */
public class CreateVolumeFromSnapshotCommand extends SnapshotCommand {

    protected CreateVolumeFromSnapshotCommand() {

    }

    /**
     * Given the UUID of a backed up snapshot VHD file on the secondary storage, the execute of this command does
     * 1) Get the parent chain of this VHD all the way up to the root, say VHDList
     * 2) Copy all the files in the VHDlist to some temp location
     * 3) Coalesce all the VHDs to one VHD which contains all the data of the volume. This invokes the DeletePreviousBackupCommand for each VHD
     * 4) Rename the UUID of this VHD
     * 5) Move this VHD to primary storage
     * @param primaryStoragePoolNameLabel   The primary storage Pool
     * @param secondaryStoragePoolURL This is what shows up in the UI when you click on Secondary storage.
     *                                 In the code, it is present as: In the vmops.host_details table, there is a field mount.parent. This is the value of that field
     *                                 If you have better ideas on how to get it, you are welcome.
     *                                 It may not be the UUID of the base copy of the snapshot, if no data was written since last snapshot.
     * @param templatePath             The install path of the template VHD on the secondary, if this a root volume
     */

    public CreateVolumeFromSnapshotCommand(StoragePool pool, String secondaryStoragePoolURL, Long dcId, Long accountId, Long volumeId, String backedUpSnapshotUuid,
            String backedUpSnapshotName, int wait) {
        super(pool, secondaryStoragePoolURL, backedUpSnapshotUuid, backedUpSnapshotName, dcId, accountId, volumeId);
        setWait(wait);
    }
}
