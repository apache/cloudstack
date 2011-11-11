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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;

@Entity
@Table(name="vlan")
public class VlanVO implements Vlan, Identity {
	    
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id") 
	Long id;
	
	@Column(name="vlan_id") 
	String vlanTag;
	
	@Column(name="vlan_gateway") 
	String vlanGateway;
	
	@Column(name="vlan_netmask") 
	String vlanNetmask;
	
	@Column(name="data_center_id") 
	long dataCenterId;
	
	@Column(name="description") 
	String ipRange;
	
    @Column(name="network_id")
    Long networkId;
	
    @Column(name="physical_network_id")
    Long physicalNetworkId;
    
	@Column(name="vlan_type")
	@Enumerated(EnumType.STRING) 
	VlanType vlanType;

    @Column(name="uuid")
    String uuid;
	
	public VlanVO(VlanType vlanType, String vlanTag, String vlanGateway, String vlanNetmask, long dataCenterId, String ipRange, Long networkId, Long physicalNetworkId) {
		this.vlanType = vlanType;
		this.vlanTag = vlanTag;
		this.vlanGateway = vlanGateway;
		this.vlanNetmask = vlanNetmask;
		this.dataCenterId = dataCenterId;
		this.ipRange = ipRange;
		this.networkId = networkId;
		this.uuid = UUID.randomUUID().toString();
		this.physicalNetworkId = physicalNetworkId;
	}
	
	public VlanVO() {
		this.uuid = UUID.randomUUID().toString();
	}
	
	@Override
    public long getId() {
		return id;
	}
	
	@Override
    public String getVlanTag() {
		return vlanTag;
	}

	@Override
    public String getVlanGateway() {
		return vlanGateway;
	}
    
	@Override
    public String getVlanNetmask() {
        return vlanNetmask;
    }
	
	@Override
    public long getDataCenterId() {
		return dataCenterId;
	}

	@Override
    public String getIpRange() {
		return ipRange;
	}

	@Override
    public VlanType getVlanType() {
		return vlanType;
	}

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
    @Override
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }
    
}
