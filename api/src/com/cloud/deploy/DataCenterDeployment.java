/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
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
