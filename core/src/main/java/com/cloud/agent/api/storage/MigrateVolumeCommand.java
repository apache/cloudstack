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

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;

public class MigrateVolumeCommand extends Command {
    long volumeId;
    String volumePath;
    StorageFilerTO pool;
    StorageFilerTO sourcePool;
    String attachedVmName;
    Volume.Type volumeType;
    String hostGuidInTargetCluster;

    private DataTO srcData;
    private DataTO destData;
    private Map<String, String> srcDetails;
    private Map<String, String> destDetails;

    public MigrateVolumeCommand(long volumeId, String volumePath, StoragePool pool, int timeout) {
        this.volumeId = volumeId;
        this.volumePath = volumePath;
        this.pool = new StorageFilerTO(pool);
        this.setWait(timeout);
    }

    public MigrateVolumeCommand(long volumeId, String volumePath, StoragePool pool, String attachedVmName, Volume.Type volumeType, int timeout) {
        this(volumeId,volumePath,pool,timeout);
        this.attachedVmName = attachedVmName;
        this.volumeType = volumeType;
        this.setWait(timeout);
    }

    public MigrateVolumeCommand(long volumeId, String volumePath, String attachedVmName, StoragePool sourcePool, StoragePool targetPool, String hostGuidInTargetCluster) {
        this(volumeId,volumePath,targetPool, attachedVmName, Volume.Type.UNKNOWN, -1);
        this.sourcePool = new StorageFilerTO(sourcePool);
        this.hostGuidInTargetCluster = hostGuidInTargetCluster;
    }

    public MigrateVolumeCommand(DataTO srcData, DataTO destData, Map<String, String> srcDetails, Map<String, String> destDetails, int timeout) {
        this.srcData = srcData;
        this.destData = destData;
        this.srcDetails = srcDetails;
        this.destDetails = destDetails;

        setWait(timeout);
    }

    @Override
    public boolean executeInSequence() {
        return true;
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

    public StorageFilerTO getSourcePool() {
        return sourcePool;
    }

    public StorageFilerTO getTargetPool() {
        return pool;
    }

    public String getAttachedVmName() {
        return attachedVmName;
    }

    public Volume.Type getVolumeType() {
        return volumeType;
    }

    public String getHostGuidInTargetCluster() {
        return hostGuidInTargetCluster;
    }

    public DataTO getSrcData() {
        return srcData;
    }

    public DataTO getDestData() {
        return destData;
    }

    public void setSrcDetails(Map<String, String> details) {
        srcDetails = details;
    }

    public Map<String, String> getSrcDetails() {
        return srcDetails;
    }

    public void setDestDetails(Map<String, String> details) {
        destDetails = details;
    }

    public Map<String, String> getDestDetails() {
        return destDetails;
    }

    public int getWaitInMillSeconds() {
        return getWait() * 1000;
    }
}
