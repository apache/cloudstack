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

}
