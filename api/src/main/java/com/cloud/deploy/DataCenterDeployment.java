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

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.vm.ReservationContext;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataCenterDeployment implements DeploymentPlan {
    long _dcId;
    Long _podId;
    Long _clusterId;
    Long _poolId;
    Long _hostId;
    Long _physicalNetworkId;
    ExcludeList _avoids = null;
    boolean _recreateDisks;
    ReservationContext _context;
    List<Long> preferredHostIds = new ArrayList<>();
    boolean migrationPlan;
    Map<Long, Integer> hostPriorities = new HashMap<>();

    public DataCenterDeployment(long dataCenterId) {
        this(dataCenterId, null, null, null, null, null);
    }

    public DataCenterDeployment(long dataCenterId, Long podId, Long clusterId, Long hostId, Long poolId, Long physicalNetworkId) {
        this(dataCenterId, podId, clusterId, hostId, poolId, physicalNetworkId, null);
    }

    public DataCenterDeployment(long dataCenterId, Long podId, Long clusterId, Long hostId, Long poolId, Long physicalNetworkId, ReservationContext context) {
        _dcId = dataCenterId;
        _podId = podId;
        _clusterId = clusterId;
        _hostId = hostId;
        _poolId = poolId;
        _physicalNetworkId = physicalNetworkId;
        _context = context;
    }

    @Override
    public long getDataCenterId() {
        return _dcId;
    }

    @Override
    public Long getPodId() {
        return _podId;
    }

    @Override
    public Long getClusterId() {
        return _clusterId;
    }

    @Override
    public Long getHostId() {
        return _hostId;
    }

    @Override
    public Long getPoolId() {
        return _poolId;
    }

    @Override
    public ExcludeList getAvoids() {
        return _avoids;
    }

    @Override
    public void setAvoids(ExcludeList avoids) {
        _avoids = avoids;
    }

    @Override
    public Long getPhysicalNetworkId() {
        return _physicalNetworkId;
    }

    @Override
    public ReservationContext getReservationContext() {
        return _context;
    }

    @Override
    public void setPreferredHosts(List<Long> hostIds) {
        this.preferredHostIds = new ArrayList<>(hostIds);
    }

    @Override
    public List<Long> getPreferredHosts() {
        return this.preferredHostIds;
    }

    public void setMigrationPlan(boolean migrationPlan) {
        this.migrationPlan = migrationPlan;
    }

    @Override
    public boolean isMigrationPlan() {
        return migrationPlan;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "_dcId", "_podId", "_clusterId", "_poolId", "_hostid", "_physicalNetworkId",
                "migrationPlan");
    }

    @Override
    public void adjustHostPriority(Long hostId, HostPriorityAdjustment adjustment) {
        Integer currentPriority = hostPriorities.get(hostId);
        if (currentPriority == null) {
            currentPriority = DEFAULT_HOST_PRIORITY;
        } else if (currentPriority.equals(PROHIBITED_HOST_PRIORITY)) {
            return;
        }
        if (HostPriorityAdjustment.HIGHER.equals(adjustment)) {
            hostPriorities.put(hostId, currentPriority + ADJUST_HOST_PRIORITY_BY);
        } else if (HostPriorityAdjustment.LOWER.equals(adjustment)) {
            hostPriorities.put(hostId, currentPriority - ADJUST_HOST_PRIORITY_BY);
        } else if (HostPriorityAdjustment.DEFAULT.equals(adjustment)) {
            hostPriorities.put(hostId, DEFAULT_HOST_PRIORITY);
        } else if (HostPriorityAdjustment.PROHIBIT.equals(adjustment)) {
            hostPriorities.put(hostId, PROHIBITED_HOST_PRIORITY);
        }
    }

    @Override
    public Map<Long, Integer> getHostPriorities() {
        return hostPriorities;
    }

    @Override
    public void setHostPriorities(Map<Long, Integer> priorities) {
        this.hostPriorities = priorities;
    }
}
