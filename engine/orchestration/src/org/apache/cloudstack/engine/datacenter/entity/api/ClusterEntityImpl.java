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

package org.apache.cloudstack.engine.datacenter.entity.api;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineClusterVO;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.org.Managed.ManagedState;
import com.cloud.utils.fsm.NoTransitionException;

public class ClusterEntityImpl implements ClusterEntity {

    private DataCenterResourceManager manager;

    private EngineClusterVO clusterVO;

    public ClusterEntityImpl(String clusterId, DataCenterResourceManager manager) {
        this.manager = manager;
        this.clusterVO = this.manager.loadCluster(clusterId);
    }

    @Override
    public boolean enable() {
        try {
            manager.changeState(this, Event.EnableRequest);
        } catch (NoTransitionException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean disable() {
        try {
            manager.changeState(this, Event.DisableRequest);
        } catch (NoTransitionException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deactivate() {
        try {
            manager.changeState(this, Event.DeactivateRequest);
        } catch (NoTransitionException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean reactivate() {
        try {
            manager.changeState(this, Event.ActivatedRequest);
        } catch (NoTransitionException e) {
            return false;
        }
        return true;
    }

    @Override
    public State getState() {
        return clusterVO.getState();
    }

    @Override
    public void persist() {
        manager.saveCluster(clusterVO);
    }

    @Override
    public String getUuid() {
        return clusterVO.getUuid();
    }

    @Override
    public long getId() {
        return clusterVO.getId();
    }

    @Override
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedTime() {
        return clusterVO.getCreated();
    }

    @Override
    public Date getLastUpdatedTime() {
        return clusterVO.getLastUpdated();
    }

    @Override
    public String getOwner() {
        return clusterVO.getOwner();
    }

    @Override
    public Map<String, String> getDetails() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return clusterVO.getName();
    }

    @Override
    public long getDataCenterId() {
        return clusterVO.getDataCenterId();
    }

    @Override
    public long getPodId() {
        return clusterVO.getPodId();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return clusterVO.getHypervisorType();
    }

    @Override
    public ClusterType getClusterType() {
        return clusterVO.getClusterType();
    }

    @Override
    public AllocationState getAllocationState() {
        return clusterVO.getAllocationState();
    }

    @Override
    public ManagedState getManagedState() {
        return clusterVO.getManagedState();
    }

    public void setOwner(String owner) {
        clusterVO.setOwner(owner);
    }

    public void setName(String name) {
        clusterVO.setName(name);
    }

}
