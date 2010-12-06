/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.dc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name="data_center")
public class DataCenterVO implements DataCenter {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
	@Column(name="name")
	private String name = null;
    
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
    
    @Column(name="router_mac_address", updatable = false, nullable=false)
    private String routerMacAddress = "02:00:00:00:00:01";
    
    @Column(name="vnet")
    private String vnet = null;

	@Column(name="guest_network_cidr")
    private String guestNetworkCidr = null;
    
    @Column(name="domain_id")
    private Long domainId = null;

    @Column(name="domain")
    private String domain = null;
    
    @Column(name="networktype")
    @Enumerated(EnumType.STRING) 
    DataCenterNetworkType networkType;
    
    @Column(name="dns_provider")
    private String dnsProvider;
    
    @Column(name="dhcp_provider")
    private String dhcpProvider; 
    
    @Column(name="gateway_provider")
    private String gatewayProvider;
    
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

    @Column(name="lb_provider")
    private String loadBalancerProvider;
    
    @Column(name="firewall_provider")
    private String firewallProvider;
    
    @Column(name="mac_address", updatable = false, nullable=false)
    @TableGenerator(name="mac_address_sq", table="data_center", pkColumnName="id", valueColumnName="mac_address", allocationSize=1)
    private long macAddress = 1;
    
    public DataCenterVO(long id, String name, String description, String dns1, String dns2, String dns3, String dns4, String vnet, String guestCidr, String domain, Long domainId, DataCenterNetworkType zoneType) {
        this(name, description, dns1, dns2, dns3, dns4, vnet, guestCidr, domain, domainId, zoneType);
        this.id = id;
	}

    public DataCenterVO(String name, String description, String dns1, String dns2, String dns3, String dns4, String vnet, String guestCidr, String domain, Long domainId, DataCenterNetworkType zoneType) {
        this.name = name;
        this.description = description;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.internalDns1 = dns3;
        this.internalDns2 = dns4;
        this.vnet = vnet;
        this.guestNetworkCidr = guestCidr;
        this.domain = domain;
        this.domainId = domainId;
        this.networkType = zoneType;
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
    
    public void setVnet(String vnet) {
        this.vnet = vnet;
    }

    @Override
    public String getDns1() {
        return dns1;
    }
    
    @Override
    public String getVnet() {
        return vnet;
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
    public String getGuestNetworkCidr()
    {
    	return guestNetworkCidr;
    }
    
    public void setGuestNetworkCidr(String guestNetworkCidr)
    {
    	this.guestNetworkCidr = guestNetworkCidr;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public void setNetworkType(DataCenterNetworkType zoneNetworkType) {
        this.networkType = zoneNetworkType;
    }

    @Override
    public DataCenterNetworkType getNetworkType() {
        return networkType;
    }
    
}
