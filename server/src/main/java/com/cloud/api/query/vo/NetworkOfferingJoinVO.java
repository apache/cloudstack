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

import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "network_offering_view")
public class NetworkOfferingJoinVO extends BaseViewVO implements NetworkOffering {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "unique_name")
    private String uniqueName;

    @Column(name = "display_text")
    private String displayText;

    @Column(name = "nw_rate")
    private Integer rateMbps;

    @Column(name = "mc_rate")
    private Integer multicastRateMbps;

    @Column(name = "traffic_type")
    @Enumerated(value = EnumType.STRING)
    private Networks.TrafficType trafficType;

    @Column(name = "tags", length = 4096)
    private String tags;

    @Column(name = "system_only")
    private boolean systemOnly;

    @Column(name = "specify_vlan")
    private boolean specifyVlan;

    @Column(name = "service_offering_id")
    private Long serviceOfferingId;

    @Column(name = "conserve_mode")
    private boolean conserveMode;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "default")
    private boolean isDefault;

    @Column(name = "availability")
    @Enumerated(value = EnumType.STRING)
    NetworkOffering.Availability availability;

    @Column(name = "dedicated_lb_service")
    private boolean dedicatedLB;

    @Column(name = "shared_source_nat_service")
    private boolean sharedSourceNat;

    @Column(name = "sort_key")
    private int sortKey;

    @Column(name = "redundant_router_service")
    private boolean redundantRouter;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private NetworkOffering.State state = NetworkOffering.State.Disabled;

    @Column(name = "guest_type")
    @Enumerated(value = EnumType.STRING)
    private Network.GuestType guestType;

    @Column(name = "elastic_ip_service")
    private boolean elasticIp;

    @Column(name = "eip_associate_public_ip")
    private boolean eipAssociatePublicIp;

    @Column(name = "elastic_lb_service")
    private boolean elasticLb;

    @Column(name = "specify_ip_ranges")
    private boolean specifyIpRanges = false;

    @Column(name = "inline")
    private boolean inline;

    @Column(name = "is_persistent")
    private boolean persistent;

    @Column(name = "internal_lb")
    private boolean internalLb;

    @Column(name = "public_lb")
    private boolean publicLb;

    @Column(name = "egress_default_policy")
    private boolean egressdefaultpolicy;

    @Column(name = "concurrent_connections")
    private Integer concurrentConnections;

    @Column(name = "keep_alive_enabled")
    private boolean keepAliveEnabled = false;

    @Column(name = "supports_streched_l2")
    private boolean supportsStrechedL2 = false;

    @Column(name = "supports_public_access")
    private boolean supportsPublicAccess = false;

    @Column(name = "for_vpc")
    private boolean forVpc;

    @Column(name = "service_package_id")
    private String servicePackageUuid = null;

    @Column(name = "domain_id")
    private String domainId = null;

    @Column(name = "domain_uuid")
    private String domainUuid = null;

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

    public NetworkOfferingJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Integer getRateMbps() {
        return rateMbps;
    }

    public Integer getMulticastRateMbps() {
        return multicastRateMbps;
    }

    public Networks.TrafficType getTrafficType() {
        return trafficType;
    }

    public String getTags() {
        return tags;
    }

    public boolean isSystemOnly() {
        return systemOnly;
    }

    public boolean isSpecifyVlan() {
        return specifyVlan;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public boolean isConserveMode() {
        return conserveMode;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public NetworkOffering.Availability getAvailability() {
        return availability;
    }

    public boolean isDedicatedLB() {
        return dedicatedLB;
    }

    public boolean isSharedSourceNat() {
        return sharedSourceNat;
    }

    public int getSortKey() {
        return sortKey;
    }

    public boolean isRedundantRouter() {
        return redundantRouter;
    }

    public NetworkOffering.State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    public Network.GuestType getGuestType() {
        return guestType;
    }

    public boolean isElasticIp() {
        return elasticIp;
    }

    public boolean isAssociatePublicIP() {
        return eipAssociatePublicIp;
    }

    public boolean isElasticLb() {
        return elasticLb;
    }

    public boolean isSpecifyIpRanges() {
        return specifyIpRanges;
    }

    public boolean isInline() {
        return inline;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isInternalLb() {
        return internalLb;
    }

    public boolean isPublicLb() {
        return publicLb;
    }

    public boolean isEgressDefaultPolicy() {
        return egressdefaultpolicy;
    }

    public Integer getConcurrentConnections() {
        return this.concurrentConnections;
    }

    public boolean isKeepAliveEnabled() {
        return keepAliveEnabled;
    }

    public boolean isSupportingStrechedL2() {
        return supportsStrechedL2;
    }

    public boolean isSupportingPublicAccess() {
        return supportsPublicAccess;
    }

    public boolean isForVpc() {
        return forVpc;
    }

    public void setForVpc(boolean forVpc) { this.forVpc = forVpc; }

    public String getServicePackage() {
        return servicePackageUuid;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getInternetProtocol() {
        return internetProtocol;
    }
}
