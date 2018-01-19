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
package com.cloud.dc;

public class PodCluster {
    HostPodVO _pod;
    ClusterVO _cluster;

    protected PodCluster() {
        super();
    }

    public PodCluster(HostPodVO pod, ClusterVO cluster) {
        _pod = pod;
        _cluster = cluster;
    }

    public HostPodVO getPod() {
        return _pod;
    }

    public ClusterVO getCluster() {
        return _cluster;
    }

    @Override
    public int hashCode() {
        return _pod.hashCode() ^ (_cluster != null ? _cluster.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PodCluster)) {
            return false;
        }

        PodCluster that = (PodCluster)obj;
        if (!this._pod.equals(that._pod)) {
            return false;
        }

        if (this._cluster == null && that._cluster == null) {
            return true;
        }

        if (this._cluster == null || that._cluster == null) {
            return false;
        }

        return this._cluster.equals(that._cluster);
    }
}
