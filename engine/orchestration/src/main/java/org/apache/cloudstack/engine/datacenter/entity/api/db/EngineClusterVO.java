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
package org.apache.cloudstack.engine.datacenter.entity.api.db;

import com.cloud.cpu.CPU;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.org.Managed.ManagedState;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.StateMachine;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.util.CPUArchConverter;
import org.apache.cloudstack.util.HypervisorTypeConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "cluster")
public class EngineClusterVO implements EngineCluster, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "name")
    String name;

    @Column(name = "guid")
    String guid;

    @Column(name = "data_center_id")
    long dataCenterId;

    @Column(name = "pod_id")
    long podId;

    @Column(name = "hypervisor_type")
    @Convert(converter = HypervisorTypeConverter.class)
    String hypervisorType;

    @Column(name = "cluster_type")
    @Enumerated(value = EnumType.STRING)
    Cluster.ClusterType clusterType;

    @Column(name = "allocation_state")
    @Enumerated(value = EnumType.STRING)
    AllocationState allocationState;

    @Column(name = "arch")
    @Convert(converter = CPUArchConverter.class)
    private CPU.CPUArch arch;

    @Column(name = "managed_state")
    @Enumerated(value = EnumType.STRING)
    ManagedState managedState;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "uuid")
    String uuid;

    //orchestration

    @Column(name = "owner")
    private String owner = null;

    @Column(name = GenericDao.CREATED_COLUMN)
    protected Date created;

    @Column(name = "lastUpdated", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    protected Date lastUpdated;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value = EnumType.STRING)
    @StateMachine(state = State.class, event = Event.class)
    @Column(name = "engine_state", updatable = true, nullable = false, length = 32)
    protected State state = null;

    public EngineClusterVO() {
        clusterType = Cluster.ClusterType.CloudManaged;
        allocationState = Grouping.AllocationState.Enabled;

        this.uuid = UUID.randomUUID().toString();
        this.state = State.Disabled;
    }

    public EngineClusterVO(long dataCenterId, long podId, String name) {
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.name = name;
        this.clusterType = Cluster.ClusterType.CloudManaged;
        this.allocationState = Grouping.AllocationState.Enabled;
        this.managedState = ManagedState.Managed;
        this.uuid = UUID.randomUUID().toString();
        this.state = State.Disabled;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getPodId() {
        return podId;
    }

    @Override
    public Cluster.ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(Cluster.ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    @Override
    public AllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(AllocationState allocationState) {
        this.allocationState = allocationState;
    }

    @Override
    public ManagedState getManagedState() {
        return managedState;
    }

    public void setManagedState(ManagedState managedState) {
        this.managedState = managedState;
    }

    public EngineClusterVO(long clusterId) {
        this.id = clusterId;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EngineClusterVO)) {
            return false;
        }
        EngineClusterVO that = (EngineClusterVO)obj;
        return this.id == that.id;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.getType(hypervisorType);
    }

    public void setHypervisorType(String hy) {
        hypervisorType = hy;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public State getState() {
        return state;
    }

    @Override
    public CPU.CPUArch getArch() {
        return arch;
    }

    public void setArch(CPU.CPUArch arch) {
        this.arch = arch;
    }

    @Override
    public PartitionType partitionType() {
        return PartitionType.Cluster;
    }
}
