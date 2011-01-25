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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.uservm.UserVm;

@Entity
@Table(name="user_vm")
@DiscriminatorValue(value="User")
@PrimaryKeyJoinColumn(name="id")
public class UserVmVO extends VMInstanceVO implements UserVm {

    @Column(name="domain_router_id", updatable=true, nullable=true)
    Long domainRouterId;

    @Column(name="vnet", length=10, updatable=true, nullable=true)
    String vnet;

    @Column(name="guest_ip_address")
    String guestIpAddress;
    
    @Column(name="guest_mac_address")
    String guestMacAddress;
    
    @Column(name="guest_netmask")
    String guestNetmask;

    @Column(name="iso_id", nullable=true, length=17)
    private Long isoId = null;
    
    @Column(name="external_ip_address")
	String externalIpAddress;

    @Column(name="external_mac_address")
	String externalMacAddress;

    @Column(name="external_vlan_db_id")
	private Long externalVlanDbId;
    
    @Column(name="user_data", updatable=true, nullable=true, length=2048)
    private String userData;
    
    @Column(name="display_name", updatable=true, nullable=true)
    private String displayName;
    
    transient String password;

    @Transient
    Map<String, String> details;
    
    @Override
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

	@Override
    public String getGuestIpAddress() {
		return guestIpAddress;
	}

	public void setGuestIpAddress(String guestIpAddress) {
		this.guestIpAddress = guestIpAddress;
		setPrivateIpAddress(guestIpAddress);
	}

	@Override
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
	}
	
    @Override
    public Long getIsoId() {
        return isoId;
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
                    String instanceName,
                    String displayName,
                    long templateId,
                    HypervisorType hypervisorType,
                    long guestOsId,
                    boolean haEnabled,
                    long domainId,
                    long accountId,
                    long serviceOfferingId,
                    String userData,
                    String name) {
        super(id, serviceOfferingId, name, instanceName, Type.User, templateId, hypervisorType, guestOsId, domainId, accountId, haEnabled);
        this.userData = userData;
        this.displayName = displayName != null ? displayName : null;
    	this.details = new HashMap<String, String>();
    }
    
    protected UserVmVO() {
        super();
    }

	public String getExternalIpAddress() {
		return externalIpAddress;
	}

	public void setIsoId(Long id) {
	    this.isoId = id;
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

    @Override
	public void setUserData(String userData) {
		this.userData = userData;
	}

    @Override
	public String getUserData() {
		return userData;
	}
	
	@Override
	public String getDisplayName() {
	    return displayName;
	}
	
	public void setDisplayName(String displayName) {
	    this.displayName = displayName;
	}
	
    public Map<String, String> getDetails() {
        return details;
    }
    
    @Override
    public String getDetail(String name) {
        assert (details != null) : "Did you forget to load the details?";
        
        return details != null ? details.get(name) : null;
    }
    
    public void setDetail(String name, String value) {
        assert (details != null) : "Did you forget to load the details?";
        
        details.put(name, value);
    }
    
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
	
}
