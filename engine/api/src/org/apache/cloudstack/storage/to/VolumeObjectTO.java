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
package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataTO;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

import com.cloud.agent.api.to.DataStoreTO;

public class VolumeObjectTO implements DataTO {
    private String uuid;
    private VolumeType volumeType;
    private DiskFormat diskType;
    private DataStoreTO dataStore;
    private String name;
    private long size;
    private String path;
    private Long volumeId;
    private String vmName;
    private long accountId;

    public VolumeObjectTO() {

    }

    public VolumeObjectTO(VolumeInfo volume) {
        this.uuid = volume.getUuid();
        this.path = volume.getPath();
        this.accountId = volume.getAccountId();
        if (volume.getDataStore() != null) {
            this.dataStore = volume.getDataStore().getTO();
        } else {
            this.dataStore = null;
        }
        this.vmName = volume.getAttachedVmName();
        this.size = volume.getSize();
        this.setVolumeId(volume.getId());
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getPath() {
        return this.path;
    }

    public VolumeType getVolumeType() {
        return this.volumeType;
    }

    public DiskFormat getDiskType() {
        return this.diskType;
    }

    public DataStoreTO getDataStore() {
        return this.dataStore;
    }

    public void setDataStore(PrimaryDataStoreTO dataStore) {
        this.dataStore = dataStore;
    }

    public String getName() {
        return this.name;
    }

    public long getSize() {
        return this.size;
    }

    public DataObjectType getObjectType() {
        return DataObjectType.VOLUME;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }


}
