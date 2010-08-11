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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.NumbersUtil;

@Entity
@Table(name = "host_pod_ref")
public class HostPodVO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	long id;

	@Column(name = "name")
	private String name = null;

	@Column(name = "data_center_id")
	private long dataCenterId;
	
	@Column(name = "gateway")
	private String gateway;

	@Column(name = "cidr_address")
	private String cidrAddress;

	@Column(name = "cidr_size")
	private long cidrSize;

	@Column(name = "description")
	private String description;

	public HostPodVO(String name, long dcId, String gateway, String cidrAddress, long cidrSize, String description) {
		this.name = name;
		this.dataCenterId = dcId;
		this.gateway = gateway;
		this.cidrAddress = cidrAddress;
		this.cidrSize = cidrSize;
		this.description = description;
	}

	/*
	 * public HostPodVO(String name, long dcId) { this(null, name, dcId); }
	 */
	protected HostPodVO() {
	}

	public long getId() {
		return id;
	}

	public long getDataCenterId() {
		return dataCenterId;
	}

	public void setDataCenterId(long dataCenterId) {
		this.dataCenterId = dataCenterId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCidrAddress() {
		return cidrAddress;
	}

	public void setCidrAddress(String cidrAddress) {
		this.cidrAddress = cidrAddress;
	}

	public long getCidrSize() {
		return cidrSize;
	}

	public void setCidrSize(long cidrSize) {
		this.cidrSize = cidrSize;
	}
	
	public String getGateway() {
		return gateway;
	}
	
	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	// Use for comparisons only.
	public HostPodVO(Long id) {
	    this.id = id;
	}
	
	@Override
    public int hashCode() {
	    return  NumbersUtil.hash(id);
	}
	
	@Override
    public boolean equals(Object obj) {
	    if (obj instanceof HostPodVO) {
	        return id == ((HostPodVO)obj).id;
	    } else {
	        return false;
	    }
	}
}
