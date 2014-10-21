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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

// NOTE: This particular table is totally internal to the CS MS.
// Do not ever include a uuid/guid field in this table. We just
// need it map cluster Ids with Cisco Nexus VSM Ids.

@Entity
@Table(name = "cluster_vsm_map")
public class ClusterVSMMapVO {

    @Column(name = "cluster_id")
    long clusterId;

    @Column(name = "vsm_id")
    long vsmId;

    public ClusterVSMMapVO(long clusterId, long vsmId) {
        this.clusterId = clusterId;
        this.vsmId = vsmId;
    }

    public ClusterVSMMapVO() {
        // Do nothing.
    }

    public long getClusterId() {
        return clusterId;
    }

    public long getVsmId() {
        return vsmId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public void setVsmId(long vsmId) {
        this.vsmId = vsmId;
    }
}
