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

package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vpc_offering_view")
public class VpcOfferingJoinVO implements VpcOffering {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

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
    VpcOffering.State state = VpcOffering.State.Disabled;

    @Column(name = "default")
    boolean isDefault = false;

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

    @Column(name = "for_nsx")
    boolean forNsx = false;

    @Column(name = "network_mode")
    NetworkOffering.NetworkMode networkMode;

    @Column(name = "domain_id")
    private String domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "zone_id")
    private String zoneId = null;

    @Column(name = "zone_uuid")
    private String zoneUuid = null;

    @Column(name = "zone_name")
    private String zoneName = null;

    @Column(name = "internet_protocol")
    private String internetProtocol = null;

    @Column(name="routing_mode")
    @Enumerated(value = EnumType.STRING)
    private NetworkOffering.RoutingMode routingMode;

    @Column(name = "specify_as_number")
    private Boolean specifyAsNumber = false;

    public VpcOfferingJoinVO() {
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
    public VpcOffering.State getState() {
        return state;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isForNsx() {
        return forNsx;
    }

    @Override
    public NetworkOffering.NetworkMode getNetworkMode() {
        return networkMode;
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
        return redundantRouter;
    }

    public int getSortKey() {
        return sortKey;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getInternetProtocol() {
        return internetProtocol;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[VPC Offering [");
        return buf.append(id).append("-").append(name).append("]").toString();
    }
}
