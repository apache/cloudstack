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

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.vm.DiskProfile;

public class CreateVolumeFromVMSnapshotCommand extends Command {

    protected String path;
    protected String name;
    protected Boolean fullClone;
    protected String storagePoolUuid;
    private StorageFilerTO pool;
    private DiskProfile diskProfile;
    private Long volumeId;

    public DiskProfile getDskch() {
        return diskProfile;
    }

    public String getPath() {
        return path;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    protected CreateVolumeFromVMSnapshotCommand() {

    }

    public CreateVolumeFromVMSnapshotCommand(String path, String name, Boolean fullClone, String storagePoolUuid) {
        this.path = path;
        this.name = name;
        this.fullClone = fullClone;
        this.storagePoolUuid = storagePoolUuid;
    }

    public CreateVolumeFromVMSnapshotCommand(String path, String name, Boolean fullClone, String storagePoolUuid, StorageFilerTO pool, DiskProfile diskProfile,
            Long volumeId) {
        this.path = path;
        this.name = name;
        this.fullClone = fullClone;
        this.storagePoolUuid = storagePoolUuid;
        this.pool = pool;
        this.diskProfile = diskProfile;
        this.volumeId = volumeId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getName() {
        return name;
    }

    public Boolean getFullClone() {
        return fullClone;
    }

    public String getStoragePoolUuid() {
        return storagePoolUuid;
    }

    public StorageFilerTO getPool() {
        return pool;
    }
}
