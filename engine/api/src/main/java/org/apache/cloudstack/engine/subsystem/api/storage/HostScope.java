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
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

public class HostScope extends AbstractScope {
    private ScopeType type = ScopeType.HOST;
    private Long hostId;
    private Long clusterId;
    private Long zoneId;

    public HostScope(Long hostId, Long clusterId, Long zoneId) {
        super();
        this.hostId = hostId;
        this.clusterId = clusterId;
        this.zoneId = zoneId;
    }

    @Override
    public ScopeType getScopeType() {
        return this.type;
    }

    @Override
    public Long getScopeId() {
        return this.hostId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public String toString() {
        return String.format("HostScope %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "zoneId", "clusterId", "hostId"));
    }
}
