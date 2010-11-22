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

@Entity
@Table(name="vlan")
public class VlanVO implements Vlan {
	    
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id") 
	Long id;
	
	@Column(name="vlan_id") 
	String vlanId;
	
	@Column(name="vlan_gateway") 
	String vlanGateway;
	
	@Column(name="vlan_netmask") 
	String vlanNetmask;
	
	@Column(name="data_center_id") 
	long dataCenterId;
	
	@Column(name="description") 
	String ipRange;
	
	@Column(name="vlan_type")
	@Enumerated(EnumType.STRING) 
	VlanType vlanType;
	
	public VlanVO(VlanType vlanType, String vlanTag, String vlanGateway, String vlanNetmask, long dataCenterId, String ipRange) {
		this.vlanType = vlanType;
		this.vlanId = vlanTag;
		this.vlanGateway = vlanGateway;
		this.vlanNetmask = vlanNetmask;
		this.dataCenterId = dataCenterId;
		this.ipRange = ipRange;
	}
	
	public VlanVO() {
		
	}
	
	public Long getId() {
		return id;
	}
	
	public String getVlanId() {
		return vlanId;
	}

	public String getVlanGateway() {
		return vlanGateway;
	}
	
	public void setVlanGateway(String vlanGateway) {
		this.vlanGateway = vlanGateway;
	}
    
	public String getVlanNetmask() {
        return vlanNetmask;
    }
	
	public long getDataCenterId() {
		return dataCenterId;
	}

	public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}

	public String getIpRange() {
		return ipRange;
	}

	public void setVlanType(VlanType vlanType) {
		this.vlanType = vlanType;
	}

	public VlanType getVlanType() {
		return vlanType;
	}
}
