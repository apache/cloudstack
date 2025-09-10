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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;

import com.cloud.org.Grouping;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.StateMachine;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

@Entity
@Table(name = "host_pod_ref")
public class EngineHostPodVO implements EnginePod, Identity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "name")
    private String name = null;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "cidr_address")
    private String cidrAddress;

    @Column(name = "cidr_size")
    private int cidrSize;

    @Column(name = "description")
    private String description;

    @Column(name = "allocation_state")
    @Enumerated(value = EnumType.STRING)
    AllocationState allocationState;

    @Column(name = "external_dhcp")
    private Boolean externalDhcp;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "uuid")
    private String uuid;

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

    public EngineHostPodVO(String name, long dcId, String gateway, String cidrAddress, int cidrSize, String description) {
        this.name = name;
        this.dataCenterId = dcId;
        this.gateway = gateway;
        this.cidrAddress = cidrAddress;
        this.cidrSize = cidrSize;
        this.description = description;
        this.allocationState = Grouping.AllocationState.Enabled;
        this.externalDhcp = false;
        this.uuid = UUID.randomUUID().toString();
        this.state = State.Disabled;
    }

    /*
     * public HostPodVO(String name, long dcId) { this(null, name, dcId); }
     */
    protected EngineHostPodVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCidrAddress() {
        return cidrAddress;
    }

    public void setCidrAddress(String cidrAddress) {
        this.cidrAddress = cidrAddress;
    }

    @Override
    public int getCidrSize() {
        return cidrSize;
    }

    public void setCidrSize(int cidrSize) {
        this.cidrSize = cidrSize;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public AllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(AllocationState allocationState) {
        this.allocationState = allocationState;
    }

    // Use for comparisons only.
    public EngineHostPodVO(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public boolean getExternalDhcp() {
        return externalDhcp;
    }

    public void setExternalDhcp(boolean use) {
        externalDhcp = use;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EngineHostPodVO) {
            return id == ((EngineHostPodVO)obj).id;
        } else {
            return false;
        }
    }

    public Date getRemoved() {
        return removed;
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
    public String toString() {
        return String.format("EngineHostPod %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "id", "uuid", "name"));
    }
}
