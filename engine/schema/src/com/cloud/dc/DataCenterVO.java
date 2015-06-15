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
package com.cloud.dc;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.cloud.network.Network.Provider;
import com.cloud.org.Grouping;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "data_center")
public class DataCenterVO implements DataCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name = null;

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

    @Column(name = "router_mac_address", updatable = false, nullable = false)
    private String routerMacAddress = "02:00:00:00:00:01";

    @Column(name = "guest_network_cidr")
    private String guestNetworkCidr = null;

    @Column(name = "domain_id")
    private Long domainId = null;

    @Column(name = "domain")
    private String domain;

    @Column(name = "networktype")
    @Enumerated(EnumType.STRING)
    NetworkType networkType;

    @Column(name = "dns_provider")
    private String dnsProvider;

    @Column(name = "dhcp_provider")
    private String dhcpProvider;

    @Column(name = "gateway_provider")
    private String gatewayProvider;

    @Column(name = "vpn_provider")
    private String vpnProvider;

    @Column(name = "userdata_provider")
    private String userDataProvider;

    @Column(name = "lb_provider")
    private String loadBalancerProvider;

    @Column(name = "firewall_provider")
    private String firewallProvider;

    @Column(name = "mac_address", nullable = false)
    @TableGenerator(name = "mac_address_sq", table = "data_center", pkColumnName = "id", valueColumnName = "mac_address", allocationSize = 1)
    private long macAddress = 1;

    @Column(name = "zone_token")
    private String zoneToken;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    // This is a delayed load value.  If the value is null,
    // then this field has not been loaded yet.
    // Call the dao to load it.
    @Transient
    Map<String, String> details;

    @Column(name = "allocation_state")
    @Enumerated(value = EnumType.STRING)
    AllocationState allocationState;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "is_security_group_enabled")
    boolean securityGroupEnabled;

    @Column(name = "is_local_storage_enabled")
    boolean localStorageEnabled;

    @Override
    public String getDnsProvider() {
        return dnsProvider;
    }

    public void setDnsProvider(String dnsProvider) {
        this.dnsProvider = dnsProvider;
    }

    @Override
    public String getDhcpProvider() {
        return dhcpProvider;
    }

    public void setDhcpProvider(String dhcpProvider) {
        this.dhcpProvider = dhcpProvider;
    }

    @Override
    public String getGatewayProvider() {
        return gatewayProvider;
    }

    public void setGatewayProvider(String gatewayProvider) {
        this.gatewayProvider = gatewayProvider;
    }

    @Override
    public String getLoadBalancerProvider() {
        return loadBalancerProvider;
    }

    public void setLoadBalancerProvider(String loadBalancerProvider) {
        this.loadBalancerProvider = loadBalancerProvider;
    }

    @Override
    public String getFirewallProvider() {
        return firewallProvider;
    }

    public void setFirewallProvider(String firewallProvider) {
        this.firewallProvider = firewallProvider;
    }

    public DataCenterVO(long id, String name, String description, String dns1, String dns2, String dns3, String dns4, String guestCidr, String domain, Long domainId,
            NetworkType zoneType, String zoneToken, String domainSuffix) {
        this(name, description, dns1, dns2, dns3, dns4, guestCidr, domain, domainId, zoneType, zoneToken, domainSuffix, false, false, null, null);
        this.id = id;
        this.allocationState = Grouping.AllocationState.Enabled;
        this.uuid = UUID.randomUUID().toString();
    }

    public DataCenterVO(String name, String description, String dns1, String dns2, String dns3, String dns4, String guestCidr, String domain, Long domainId,
            NetworkType zoneType, String zoneToken, String domainSuffix, boolean securityGroupEnabled, boolean localStorageEnabled, String ip6Dns1, String ip6Dns2) {
        this.name = name;
        this.description = description;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.ip6Dns1 = ip6Dns1;
        this.ip6Dns2 = ip6Dns2;
        this.internalDns1 = dns3;
        this.internalDns2 = dns4;
        this.guestNetworkCidr = guestCidr;
        this.domain = domain;
        this.domainId = domainId;
        this.networkType = zoneType;
        this.allocationState = Grouping.AllocationState.Enabled;
        this.securityGroupEnabled = securityGroupEnabled;
        this.localStorageEnabled = localStorageEnabled;

        if (zoneType == NetworkType.Advanced) {
            loadBalancerProvider = Provider.VirtualRouter.getName();
            firewallProvider = Provider.VirtualRouter.getName();
            dhcpProvider = Provider.VirtualRouter.getName();
            dnsProvider = Provider.VirtualRouter.getName();
            gatewayProvider = Provider.VirtualRouter.getName();
            vpnProvider = Provider.VirtualRouter.getName();
            userDataProvider = Provider.VirtualRouter.getName();
        } else if (zoneType == NetworkType.Basic) {
            dhcpProvider = Provider.VirtualRouter.getName();
            dnsProvider = Provider.VirtualRouter.getName();
            userDataProvider = Provider.VirtualRouter.getName();
            loadBalancerProvider = Provider.ElasticLoadBalancerVm.getName();
        }

        this.zoneToken = zoneToken;
        this.domain = domainSuffix;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getVpnProvider() {
        return vpnProvider;
    }

    public void setVpnProvider(String vpnProvider) {
        this.vpnProvider = vpnProvider;
    }

    @Override
    public String getUserDataProvider() {
        return userDataProvider;
    }

    public void setUserDataProvider(String userDataProvider) {
        this.userDataProvider = userDataProvider;
    }

    @Override
    public String getGuestNetworkCidr() {
        return guestNetworkCidr;
    }

    public void setGuestNetworkCidr(String guestNetworkCidr) {
        this.guestNetworkCidr = guestNetworkCidr;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getRouterMacAddress() {
        return routerMacAddress;
    }

    @Override
    public String getDns1() {
        return dns1;
    }

    @Override
    public String getDns2() {
        return dns2;
    }

    @Override
    public String getInternalDns1() {
        return internalDns1;
    }

    @Override
    public String getInternalDns2() {
        return internalDns2;
    }

    protected DataCenterVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setInternalDns1(String dns3) {
        this.internalDns1 = dns3;
    }

    public void setInternalDns2(String dns4) {
        this.internalDns2 = dns4;
    }

    public void setRouterMacAddress(String routerMacAddress) {
        this.routerMacAddress = routerMacAddress;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setNetworkType(NetworkType zoneNetworkType) {
        this.networkType = zoneNetworkType;
    }

    @Override
    public NetworkType getNetworkType() {
        return networkType;
    }

    @Override
    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public void setSecurityGroupEnabled(boolean enabled) {
        this.securityGroupEnabled = enabled;
    }

    @Override
    public boolean isLocalStorageEnabled() {
        return localStorageEnabled;
    }

    public void setLocalStorageEnabled(boolean enabled) {
        this.localStorageEnabled = enabled;
    }

    @Override
    public Map<String, String> getDetails() {
        return details;
    }

    @Override
    public void setDetails(Map<String, String> details2) {
        details = details2;
    }

    public String getDetail(String name) {
        return details != null ? details.get(name) : null ;
    }

    public void setDetail(String name, String value) {
        assert (details != null) : "Did you forget to load the details?";

        details.put(name, value);
    }

    @Override
    public AllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(AllocationState allocationState) {
        this.allocationState = allocationState;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataCenterVO)) {
            return false;
        }
        DataCenterVO that = (DataCenterVO)obj;
        return this.id == that.id;
    }

    @Override
    public String getZoneToken() {
        return zoneToken;
    }

    public void setZoneToken(String zoneToken) {
        this.zoneToken = zoneToken;
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

    public long getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(long macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public String getIp6Dns1() {
        return ip6Dns1;
    }

    public void setIp6Dns1(String ip6Dns1) {
        this.ip6Dns1 = ip6Dns1;
    }

    @Override
    public String getIp6Dns2() {
        return ip6Dns2;
    }

    public void setIp6Dns2(String ip6Dns2) {
        this.ip6Dns2 = ip6Dns2;
    }
}
