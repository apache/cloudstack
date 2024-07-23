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
package com.cloud.network.vpc;

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

import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vpc_offerings")
public class VpcOfferingVO implements VpcOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    String name;

    @Column(name = "unique_name")
    String uniqueName;

    @Column(name = "display_text")
    String displayText;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    State state = State.Disabled;

    @Column(name = "default")
    boolean isDefault = false;

    @Column(name = "for_nsx")
    boolean forNsx = false;

    @Column(name = "network_mode")
    NetworkOffering.NetworkMode networkMode;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = "service_offering_id")
    Long serviceOfferingId;

    @Column(name = "supports_distributed_router")
    boolean supportsDistributedRouter=false;

    @Column(name = "supports_region_level_vpc")
    boolean offersRegionLevelVPC = false;

    @Column(name = "redundant_router_service")
    boolean redundantRouter = false;

    @Column(name = "sort_key")
    int sortKey;

    @Column(name="routing_mode")
    @Enumerated(value = EnumType.STRING)
    private NetworkOffering.RoutingMode routingMode;

    @Column(name = "specify_as_number")
    private Boolean specifyAsNumber = false;

    public VpcOfferingVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public VpcOfferingVO(String name, String displayText, Long serviceOfferingId) {
        this.name = name;
        this.displayText = displayText;
        this.uniqueName = name;
        this.serviceOfferingId = serviceOfferingId;
        this.uuid = UUID.randomUUID().toString();
        this.state = State.Disabled;
    }

    public VpcOfferingVO(final String name, final String displayText, final boolean isDefault, final Long serviceOfferingId,
                         final boolean supportsDistributedRouter, final boolean offersRegionLevelVPC,
                         final boolean redundantRouter) {
        this(name, displayText, serviceOfferingId);
        this.isDefault = isDefault;
        this.supportsDistributedRouter = supportsDistributedRouter;
        this.offersRegionLevelVPC = offersRegionLevelVPC;
        this.redundantRouter = redundantRouter;
    }

    public VpcOfferingVO(String name, String displayText, boolean isDefault, Long serviceOfferingId,
                         boolean supportsDistributedRouter, boolean offersRegionLevelVPC) {
        this(name, displayText, serviceOfferingId);
        this.isDefault = isDefault;
        this.supportsDistributedRouter = supportsDistributedRouter;
        this.offersRegionLevelVPC = offersRegionLevelVPC;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public boolean isForNsx() {
        return forNsx;
    }

    public void setForNsx(boolean forNsx) {
        this.forNsx = forNsx;
    }

    public NetworkOffering.NetworkMode getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(NetworkOffering.NetworkMode networkMode) {
        this.networkMode =  networkMode;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[VPC Offering [");
        return buf.append(id).append("-").append(name).append("]").toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    @Override
    public boolean isSupportsDistributedRouter() {
        return supportsDistributedRouter;
    }

    @Override
    public boolean isOffersRegionLevelVPC() {
        return offersRegionLevelVPC;
    }

    @Override
    public boolean isRedundantRouter() {
        return this.redundantRouter;
    }

    public void setSortKey(int key) {
        sortKey = key;
    }

    public int getSortKey() {
        return sortKey;
    }

    @Override
    public NetworkOffering.RoutingMode getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(NetworkOffering.RoutingMode routingMode) {
        this.routingMode = routingMode;
    }

    @Override
    public Boolean isSpecifyAsNumber() {
        return specifyAsNumber;
    }

    public void setSpecifyAsNumber(Boolean specifyAsNumber) {
        this.specifyAsNumber = specifyAsNumber;
    }
}
