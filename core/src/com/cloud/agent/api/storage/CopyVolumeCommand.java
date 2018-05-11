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

package com.cloud.agent.api.storage;

import java.util.Map;

import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

public class CopyVolumeCommand extends StorageNfsVersionCommand {
    private long volumeId;
    private String volumePath;
    private StorageFilerTO pool;
    private String secondaryStorageURL;
    private boolean toSecondaryStorage;
    private String vmName;
    private DataTO srcData;
    private Map<String, String> srcDetails;
    private boolean executeInSequence;

    public CopyVolumeCommand() {
    }

    public CopyVolumeCommand(long volumeId, String volumePath, StoragePool pool, String secondaryStorageURL, boolean toSecondaryStorage, int wait,
            boolean executeInSequence) {
        this.volumeId = volumeId;
        this.volumePath = volumePath;
        this.pool = new StorageFilerTO(pool);
        this.secondaryStorageURL = secondaryStorageURL;
        this.toSecondaryStorage = toSecondaryStorage;
        setWait(wait);
        this.executeInSequence = executeInSequence;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public String getSecondaryStorageURL() {
        return secondaryStorageURL;
    }

    public boolean toSecondaryStorage() {
        return toSecondaryStorage;
    }

    public String getVmName() {
        return vmName;
    }

    public void setSrcData(DataTO srcData) {
        this.srcData = srcData;
    }

    public DataTO getSrcData() {
        return srcData;
    }

    public void setSrcDetails(Map<String, String> srcDetails) {
        this.srcDetails = srcDetails;
    }

    public Map<String, String> getSrcDetails() {
        return srcDetails;
    }
}
