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

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="data_center_view")
public class DataCenterJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="name")
    private String name;

    @Column(name="description")
    private String description = null;

    @Column(name="dns1")
    private String dns1 = null;

    @Column(name="dns2")
    private String dns2 = null;

    @Column(name="internal_dns1")
    private String internalDns1 = null;

    @Column(name="internal_dns2")
    private String internalDns2 = null;

    @Column(name="guest_network_cidr")
    private String guestNetworkCidr = null;

    @Column(name="domain")
    private String domain;

    @Column(name="networktype")
    @Enumerated(EnumType.STRING)
    NetworkType networkType;

    @Column(name="dhcp_provider")
    private String dhcpProvider;

    @Column(name="zone_token")
    private String zoneToken;

    @Column(name="allocation_state")
    @Enumerated(value=EnumType.STRING)
    AllocationState allocationState;

    @Column(name="is_security_group_enabled")
    boolean securityGroupEnabled;

    @Column(name="is_local_storage_enabled")
    boolean localStorageEnabled;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName;

    @Column(name="domain_path")
    private String domainPath;


    public DataCenterJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDns1() {
        return dns1;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public void setInternalDns1(String internalDns1) {
        this.internalDns1 = internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public void setInternalDns2(String internalDns2) {
        this.internalDns2 = internalDns2;
    }

    public String getGuestNetworkCidr() {
        return guestNetworkCidr;
    }

    public void setGuestNetworkCidr(String guestNetworkCidr) {
        this.guestNetworkCidr = guestNetworkCidr;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }

    public String getDhcpProvider() {
        return dhcpProvider;
    }

    public void setDhcpProvider(String dhcpProvider) {
        this.dhcpProvider = dhcpProvider;
    }

    public String getZoneToken() {
        return zoneToken;
    }

    public void setZoneToken(String zoneToken) {
        this.zoneToken = zoneToken;
    }

    public AllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(AllocationState allocationState) {
        this.allocationState = allocationState;
    }

    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public void setSecurityGroupEnabled(boolean securityGroupEnabled) {
        this.securityGroupEnabled = securityGroupEnabled;
    }


    public boolean isLocalStorageEnabled() {
        return localStorageEnabled;
    }

    public void setLocalStorageEnabled(boolean localStorageEnabled) {
        this.localStorageEnabled = localStorageEnabled;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
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


}
