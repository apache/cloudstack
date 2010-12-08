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

package com.cloud.network;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A bean representing a public IP Address
 *
 */
@Entity
@Table(name=("user_ip_address"))
public class IPAddressVO implements IpAddress {
	@Column(name="account_id")
	private Long allocatedToAccountId = null;

    @Column(name="domain_id")
    private Long allocatedInDomainId = null;

	@Id
	@Column(name="public_ip_address")
	private String address = null;

	@Column(name="data_center_id", updatable=false)
	private long dataCenterId;
	
	@Column(name="source_nat")
	private boolean sourceNat;

	@Column(name="allocated")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date allocatedTime;
	
	@Column(name="vlan_db_id")
	private long vlanId;

	@Column(name="one_to_one_nat")
	private boolean oneToOneNat;
	
	@Column(name="state")
	private State state;
	
	@Column(name="mac_address")
	private long macAddress;
	
	@Column(name="network_id")
	private Long associatedNetworkId;

	protected IPAddressVO() {
	}
	
	@Override
    public boolean readyToUse() {
	    return state == State.Allocated;
	}

	public IPAddressVO(String address, long dataCenterId, long macAddress, long vlanDbId, boolean sourceNat) {
		this.address = address;
		this.dataCenterId = dataCenterId;
		this.vlanId = vlanDbId;
		this.sourceNat = sourceNat;
		this.allocatedInDomainId = null;
		this.allocatedToAccountId = null;
		this.allocatedTime = null;
		this.state = State.Free;
		this.macAddress = macAddress;
	}
	
    public long getMacAddress() {
	    return macAddress;
	}
	
	@Override
    public long getDataCenterId() {
	    return dataCenterId; 
	}

	@Override
    public String getAddress() {
		return address;
	}
	
	@Override
    public Long getAllocatedToAccountId() {
		return allocatedToAccountId;
	}
	
	public Long getAssociatedNetworkId() {
	    return associatedNetworkId;
	}
	
	public void setAssociatedNetworkId(Long networkId) {
	    this.associatedNetworkId = networkId;
	}
	
    @Override
    public Long getAllocatedInDomainId() {
        return allocatedInDomainId;
    }
    
	@Override
    public Date getAllocatedTime() {
		return allocatedTime;
	}

    public void setAllocatedToAccountId(Long accountId) {
		this.allocatedToAccountId = accountId;
	}

    public void setAllocatedInDomainId(Long domainId) {
        this.allocatedInDomainId = domainId;
    }

    public void setSourceNat(boolean sourceNat) {
		this.sourceNat = sourceNat;
	}
	
	@Override
    public boolean isSourceNat() {
		return sourceNat;
	}

    public void setAllocatedTime(Date allocated) {
		this.allocatedTime = allocated;
	}
	
	@Override
    public long getVlanId() {
		return this.vlanId;
	}
	
    public void setVlanId(long vlanDbId) {
		this.vlanId = vlanDbId;
	}

	@Override
    public boolean isOneToOneNat() {
		return oneToOneNat;
	}

    public void setOneToOneNat(boolean oneToOneNat) {
		this.oneToOneNat = oneToOneNat;
	}
    
    @Override
    public long getDomainId() {
        return allocatedInDomainId == null ? -1 : allocatedInDomainId;
    }
    
    @Override
    public long getAccountId() {
        return allocatedToAccountId == null ? -1 : allocatedToAccountId;
    }
    
    @Override
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
	
	@Override
    public String toString() {
	    return new StringBuilder("Ip[").append(address).append("-").append(dataCenterId).append("]").toString();
	}
}
