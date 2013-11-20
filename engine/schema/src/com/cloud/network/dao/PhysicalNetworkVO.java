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
package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.cloud.network.PhysicalNetwork;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkConfigurationVO contains information about a specific physical network.
 *
 */
@Entity
@Table(name = "physical_network")
public class PhysicalNetworkVO implements PhysicalNetwork {
    @Id
    @TableGenerator(name = "physical_networks_sq",
                    table = "sequence",
                    pkColumnName = "name",
                    valueColumnName = "value",
                    pkColumnValue = "physical_networks_seq",
                    allocationSize = 1)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "data_center_id")
    long dataCenterId;

    @Column(name = "vnet")
    private String vnet = null;

    @Column(name = "speed")
    private String speed = null;

    @Column(name = "domain_id")
    Long domainId = null;

    @Column(name = "broadcast_domain_range")
    @Enumerated(value = EnumType.STRING)
    BroadcastDomainRange broadcastDomainRange;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    State state;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @Column(name = "tag")
    @CollectionTable(name = "physical_network_tags", joinColumns = @JoinColumn(name = "physical_network_id"))
    List<String> tags;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @Column(name = "isolation_method")
    @CollectionTable(name = "physical_network_isolation_methods", joinColumns = @JoinColumn(name = "physical_network_id"))
    List<String> isolationMethods;

    public PhysicalNetworkVO() {

    }

    public PhysicalNetworkVO(long id, long dataCenterId, String vnet, String speed, Long domainId, BroadcastDomainRange broadcastDomainRange, String name) {
        this.dataCenterId = dataCenterId;
        this.setVnet(vnet);
        this.setSpeed(speed);
        this.domainId = domainId;
        if (broadcastDomainRange != null) {
            this.broadcastDomainRange = broadcastDomainRange;
        } else {
            this.broadcastDomainRange = BroadcastDomainRange.POD;
        }
        this.state = State.Disabled;
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.id = id;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<String>();
    }

    public void addTag(String tag) {
        if (tags == null) {
            tags = new ArrayList<String>();
        }
        tags.add(tag);
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    @Override
    public BroadcastDomainRange getBroadcastDomainRange() {
        return broadcastDomainRange;
    }

    public void setBroadcastDomainRange(BroadcastDomainRange broadcastDomainRange) {
        this.broadcastDomainRange = broadcastDomainRange;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public List<String> getIsolationMethods() {
        return isolationMethods != null ? isolationMethods : new ArrayList<String>();
    }

    public void addIsolationMethod(String isolationMethod) {
        if (isolationMethods == null) {
            isolationMethods = new ArrayList<String>();
        }
        isolationMethods.add(isolationMethod);
    }

    public void setIsolationMethods(List<String> isolationMethods) {
        this.isolationMethods = isolationMethods;
    }

    public void setVnet(String vnet) {
        this.vnet = vnet;
    }

    @Override
    public List<Pair<Integer, Integer>> getVnet() {
        List<Pair<Integer, Integer>> vnetList = new ArrayList<Pair<Integer, Integer>>();
        if (vnet != null) {
            String[] Temp = vnet.split(",");
            String[] vnetSplit = null;
            for (String vnetRange : Temp) {
                vnetSplit = vnetRange.split("-");
                vnetList.add(new Pair<Integer, Integer>(Integer.parseInt(vnetSplit[0]), Integer.parseInt(vnetSplit[1])));
            }
        }
        return vnetList;
    }

    @Override
    public String getVnetString() {
        return vnet;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    @Override
    public String getSpeed() {
        return speed;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getName() {
        return name;
    }
}
