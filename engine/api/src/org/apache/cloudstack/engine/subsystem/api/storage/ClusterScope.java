/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.storage.ScopeType;

public class ClusterScope extends AbstractScope {
    private ScopeType type = ScopeType.CLUSTER;
    private Long clusterId;
    private Long podId;
    private Long zoneId;

    public ClusterScope(Long clusterId, Long podId, Long zoneId) {
        super();
        this.clusterId = clusterId;
        this.podId = podId;
        this.zoneId = zoneId;
    }

    @Override
    public ScopeType getScopeType() {
        return this.type;
    }

    @Override
    public Long getScopeId() {
        return this.clusterId;
    }

    public Long getPodId() {
        return this.podId;
    }

    public Long getZoneId() {
        return this.zoneId;
    }

}
