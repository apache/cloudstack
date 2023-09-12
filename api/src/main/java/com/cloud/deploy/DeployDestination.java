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
package com.cloud.deploy;

import java.io.Serializable;
import java.util.Map;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.host.Host;
import com.cloud.org.Cluster;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.utils.NumbersUtil;

public class DeployDestination implements Serializable {
    private static final long serialVersionUID = 7113840781939014695L;

    DataCenter _dc;
    Pod _pod;
    Cluster _cluster;
    Host _host;
    Map<Volume, StoragePool> _storage;

    /**
     * Display volume <-> storage pool mapping by default
     */
    boolean displayStorage = true;

    public DataCenter getDataCenter() {
        return _dc;
    }

    public Pod getPod() {
        return _pod;
    }

    public Cluster getCluster() {
        return _cluster;
    }

    public Host getHost() {
        return _host;
    }

    public Map<Volume, StoragePool> getStorageForDisks() {
        return _storage;
    }

    public DeployDestination(DataCenter dc, Pod pod, Cluster cluster, Host host) {
        _dc = dc;
        _pod = pod;
        _cluster = cluster;
        _host = host;
    }

    public DeployDestination(DataCenter dc, Pod pod, Cluster cluster, Host host, Map<Volume, StoragePool> storage, boolean displayStorage) {
        this(dc, pod, cluster, host);
        _storage = storage;
        this.displayStorage = displayStorage;
    }

    public DeployDestination() {
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(_host.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DeployDestination)) {
            return false;
        }
        DeployDestination that = (DeployDestination)obj;
        if (this._dc == null || that._dc == null) {
            return false;
        }
        if (this._dc.getId() != that._dc.getId()) {
            return false;
        }
        if (this._pod == null || that._pod == null) {
            return false;
        }
        if (this._pod.getId() != that._pod.getId()) {
            return false;
        }
        if (this._cluster == null || that._cluster == null) {
            return false;
        }
        if (this._cluster.getId() != that._cluster.getId()) {
            return false;
        }
        if (this._host == null || that._host == null) {
            return false;
        }
        return this._host.getId() == that._host.getId();
    }

    @Override
    public String toString() {

        Long dcId = null;
        Long podId = null;
        Long clusterId = null;
        Long hostId = null;

        if (_dc != null) {
            dcId = _dc.getId();
        }

        if (_pod != null) {
            podId = _pod.getId();
        }

        if (_cluster != null) {
            clusterId = _cluster.getId();
        }

        if (_host != null) {
            hostId = _host.getId();
        }

        StringBuilder destination = new StringBuilder("Dest[Zone(Id)-Pod(Id)-Cluster(Id)-Host(Id)-Storage(Volume(Id|Type-->Pool(Id))] : Dest[");
        destination.append("Zone(").append(dcId).append(")").append("-");
        destination.append("Pod(").append(podId).append(")").append("-");
        destination.append("Cluster(").append(clusterId).append(")").append("-");
        destination.append("Host(").append(hostId).append(")").append("-");
        destination.append("Storage(");
        if (displayStorage && _storage != null) {
            StringBuffer storageBuf = new StringBuffer();
            for (Volume vol : _storage.keySet()) {
                storageBuf.append("Volume(");
                storageBuf.append(vol.getId());
                storageBuf.append("|");
                storageBuf.append(vol.getVolumeType().name());
                storageBuf.append("-->Pool(");
                storageBuf.append(_storage.get(vol).getId());
                storageBuf.append(")");
            }
            destination.append(storageBuf);
        }
        return destination.append(")]").toString();
    }

    public boolean isDisplayStorage() {
        return displayStorage;
    }
}
