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
package com.cloud.vm;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name="user_vm")
@DiscriminatorValue(value="User")
@PrimaryKeyJoinColumn(name="id")
public class UserVmVO extends VMInstanceVO implements UserVm {

    @Column(name="account_id", updatable=false, nullable=false)
    private long accountId = -1;
    
    @Column(name="domain_id", updatable=false, nullable=false)
    private long domainId = -1;
    
    @Column(name="domain_router_id", updatable=true, nullable=true)
    Long domainRouterId;

    @Column(name="service_offering_id", updatable=true, nullable=false)
    long serviceOfferingId;
    
    @Column(name="vnet", length=10, updatable=true, nullable=true)
    String vnet;

    @Column(name="guest_ip_address")
    String guestIpAddress;
    
    @Column(name="guest_mac_address")
    String guestMacAddress;
    
    @Column(name="guest_netmask")
    String guestNetmask;

    @Column(name="external_ip_address")
	String externalIpAddress;

    @Column(name="external_mac_address")
	String externalMacAddress;

    @Column(name="external_vlan_db_id")
	private Long externalVlanDbId;
    
    @Column(name="user_data", updatable=true, nullable=true, length=2048)
    private String userData;

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public String getGuestIpAddress() {
		return guestIpAddress;
	}

	public void setGuestIpAddress(String guestIpAddress) {
		this.guestIpAddress = guestIpAddress;
		setPrivateIpAddress(guestIpAddress);
	}

	public String getGuestMacAddress() {
		return guestMacAddress;
	}

	public void setGuestMacAddress(String guestMacAddress) {
		this.guestMacAddress = guestMacAddress;
		setPrivateMacAddress(guestMacAddress);

	}

	public String getGuestNetmask() {
		return guestNetmask;
	}

	public void setGuestNetmask(String guestNetmask) {
		this.guestNetmask = guestNetmask;
		setPrivateNetmask(guestNetmask);
	}

    @Override
    public Long getDomainRouterId() {
        return domainRouterId;
    }
    
    public void setDomainRouterId(long domainRouterId) {
        this.domainRouterId = domainRouterId;
    }

    public void setVnet(String vnet) {
        this.vnet = vnet;
    }

    @Override
    public long getServiceOfferingId() {
        return serviceOfferingId;
    }
    
    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    @Override
    public String getVnet() {
        return vnet;
    }
    
    public UserVmVO(long id,
                    String name,
                    long templateId,
                    long guestOSId,
                    long accountId,
                    long domainId,
                    long serviceOfferingId,
                    String guestMacAddress,
                    String guestIpAddress,
                    String guestNetMask,
                    String externalIpAddress,
                    String externalMacAddress,
                    Long vlanDbId,
                    Long routerId,
                    Long podId,
                    long dcId,
                    boolean haEnabled,
                    String displayName,
                    String group,
                    String userData) {
        super(id, name, Type.User, templateId, guestOSId, guestMacAddress, guestIpAddress, guestNetMask, dcId, podId, haEnabled, null, displayName, group);
        this.serviceOfferingId = serviceOfferingId;
        this.domainRouterId = routerId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.guestIpAddress = guestIpAddress;
        this.guestNetmask = guestNetMask;
        this.guestMacAddress = guestMacAddress;
        this.externalIpAddress = externalIpAddress;
        this.externalMacAddress = externalMacAddress;
        this.setUserData(userData);
        this.setExternalVlanDbId(vlanDbId);
    }

    protected UserVmVO() {
        super();
    }

	public String getExternalIpAddress() {
		return externalIpAddress;
	}

	public void setExternalIpAddress(String externalIpAddress) {
		this.externalIpAddress = externalIpAddress;
	}

	public String getExternalMacAddress() {
		return externalMacAddress;
	}

	public void setExternalMacAddress(String externalMacAddress) {
		this.externalMacAddress = externalMacAddress;
	}

	public void setExternalVlanDbId(Long vlanDbId) {
		this.externalVlanDbId = vlanDbId;
	}

	public Long getExternalVlanDbId() {
		return externalVlanDbId;
	}

	public void setUserData(String userData) {
		this.userData = userData;
	}

	public String getUserData() {
		return userData;
	}
}
