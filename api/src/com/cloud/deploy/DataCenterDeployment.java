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
    ReservationContext _context;

    @SuppressWarnings("unused")
    private DataCenterDeployment() {  // Hide this constructor
    }
    
    public DataCenterDeployment(DeploymentPlan that) {
        this(that.getDataCenterId(), that.getPodId(), that.getClusterId(), that.getHostId(), that.getPoolId(), that.getPhysicalNetworkId(), that.getReservationContext());
        _avoids = new ExcludeList(that.getAvoids());
    }
    
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
        _avoids = new ExcludeList();
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

    public void setPoolId(Long poolId) {
        _poolId = poolId;
    }

    public void setClusterId(Long clusterId) {
        _clusterId = clusterId;
    }

    public void setHostId(Long hostId) {
        _hostId = hostId;
    }

    public void setPodId(Long podId) {
        _podId = podId;
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
    public String toString() {
        StringBuilder str = new StringBuilder("DeploymentPlan[");
        if (_hostId != null) {
            str.append("Host=").append(_hostId);
        } else if (_clusterId != null) {
            str.append("Cluster=").append(_clusterId);
        } else if (_podId != null) {
            str.append("Pod=").append(_podId);
        } else {
            str.append("Zone=").append(_dcId);
        }

        if (_poolId != null) {
            str.append(", Storage=").append(_poolId);
        }

        if (_physicalNetworkId != null) {
            str.append(", Physical Network = ").append(_physicalNetworkId);
        }

        if (_avoids != null) {
            str.append(_avoids.toString());
        }

        return str.append("]").toString();
    }

}
