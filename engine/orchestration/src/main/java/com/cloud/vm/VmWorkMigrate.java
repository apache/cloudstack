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
package com.cloud.vm;

import java.util.HashMap;
import java.util.Map;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.org.Cluster;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.utils.db.EntityManager;

public class VmWorkMigrate extends VmWork {
    private static final long serialVersionUID = 1689203333114836522L;

    Long zoneId;
    Long podId;
    Long clusterId;
    Long hostId;
    private Map<String, String> storage;
    long srcHostId;

    public VmWorkMigrate(long userId, long accountId, long vmId, String handlerName,
            long srcHostId, DeployDestination dst) {
        super(userId, accountId, vmId, handlerName);
        setConstructorParams(srcHostId, dst);
    }

    public VmWorkMigrate(VmWork vmWork, long srcHostId, DeployDestination dest) {
        super(vmWork);
        setConstructorParams(srcHostId, dest);
    }

    private void setConstructorParams(long srcHostId, DeployDestination dest) {
        this.srcHostId = srcHostId;
        this.zoneId = dest.getDataCenter() != null ? dest.getDataCenter().getId() : null;
        this.podId = dest.getPod() != null ? dest.getPod().getId() : null;
        this.clusterId = dest.getCluster() != null ? dest.getCluster().getId() : null;
        this.hostId = dest.getHost() != null ? dest.getHost().getId() : null;
        if (dest.getStorageForDisks() != null) {
            this.storage = new HashMap<>(dest.getStorageForDisks().size());
            for (Map.Entry<Volume, StoragePool> entry : dest.getStorageForDisks().entrySet()) {
                this.storage.put(entry.getKey().getUuid(), entry.getValue().getUuid());
            }
        } else {
            this.storage = null;
        }
    }

    public DeployDestination getDeployDestination() {
        DataCenter zone = zoneId != null ? s_entityMgr.findById(DataCenter.class, zoneId) : null;
        Pod pod = podId != null ? s_entityMgr.findById(Pod.class, podId) : null;
        Cluster cluster = clusterId != null ? s_entityMgr.findById(Cluster.class, clusterId) : null;
        Host host = hostId != null ? s_entityMgr.findById(Host.class, hostId) : null;

        Map<Volume, StoragePool> vols = null;

        boolean displayStorage = true;
        if (storage != null) {
            vols = new HashMap<Volume, StoragePool>(storage.size());
            for (Map.Entry<String, String> entry : storage.entrySet()) {
                Volume volume = s_entityMgr.findByUuid(Volume.class, entry.getKey());
                if (displayStorage && volume.isDeployAsIs()) {
                    displayStorage = false;
                }
                vols.put(volume, s_entityMgr.findByUuid(StoragePool.class, entry.getValue()));
            }
        }

        DeployDestination dest = new DeployDestination(zone, pod, cluster, host, vols, displayStorage);
        return dest;
    }

    public long getSrcHostId() {
        return srcHostId;
    }

    static private EntityManager s_entityMgr;

    static public void init(EntityManager entityMgr) {
        s_entityMgr = entityMgr;
    }
}
