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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "data_center_view")
public class DataCenterJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description = null;

    @Column(name = "dns1")
    private String dns1 = null;

    @Column(name = "dns2")
    private String dns2 = null;

    @Column(name = "ip6_dns1")
    private String ip6Dns1 = null;

    @Column(name = "ip6_dns2")
    private String ip6Dns2 = null;

    @Column(name = "internal_dns1")
    private String internalDns1 = null;

    @Column(name = "internal_dns2")
    private String internalDns2 = null;

    @Column(name = "guest_network_cidr")
    private String guestNetworkCidr = null;

    @Column(name = "domain")
    private String domain;

    @Column(name = "networktype")
    @Enumerated(EnumType.STRING)
    NetworkType networkType;

    @Column(name = "dhcp_provider")
    private String dhcpProvider;

    @Column(name = "zone_token")
    private String zoneToken;

    @Column(name = "allocation_state")
    @Enumerated(value = EnumType.STRING)
    AllocationState allocationState;

    @Column(name = "is_security_group_enabled")
    boolean securityGroupEnabled;

    @Column(name = "is_local_storage_enabled")
    boolean localStorageEnabled;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = "affinity_group_id")
    private long affinityGroupId;

    @Column(name = "affinity_group_uuid")
    private String affinityGroupUuid;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "sort_key")
    private int sortKey;

    public DataCenterJoinVO() {
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

    public String getDescription() {
        return description;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public String getGuestNetworkCidr() {
        return guestNetworkCidr;
    }

    public String getDomain() {
        return domain;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public String getDhcpProvider() {
        return dhcpProvider;
    }

    public String getZoneToken() {
        return zoneToken;
    }

    public AllocationState getAllocationState() {
        return allocationState;
    }

    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public boolean isLocalStorageEnabled() {
        return localStorageEnabled;
    }

    public Date getRemoved() {
        return removed;
    }

    public long getDomainId() {
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

    public String getIp6Dns1() {
        return ip6Dns1;
    }

    public String getIp6Dns2() {
        return ip6Dns2;
    }

    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }

    public long getAccountId() {
        return accountId;
    }

    public int getSortKey() {
        return sortKey;
    }
}
