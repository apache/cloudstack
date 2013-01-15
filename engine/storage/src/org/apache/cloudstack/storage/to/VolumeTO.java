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

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public class VolumeTO {
    private final String uuid;
    private final String path;
    private  VolumeType volumeType;
    private  DiskFormat diskType;
    private PrimaryDataStoreTO dataStore;
    private  String name;
    private final long size;
    public VolumeTO(VolumeInfo volume) {
        this.uuid = volume.getUuid();
        this.path = volume.getUri();
        //this.volumeType = volume.getType();
        //this.diskType = volume.getDiskType();
        if (volume.getDataStore() != null) {
            this.dataStore = new PrimaryDataStoreTO((PrimaryDataStoreInfo)volume.getDataStore());
        } else {
            this.dataStore = null;
        }
        //this.name = volume.getName();
        this.size = volume.getSize();
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
    
    public PrimaryDataStoreTO getDataStore() {
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
}
