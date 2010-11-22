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
 * @author Will Chan
 *
 */
@Entity
@Table(name=("user_ip_address"))
public class IPAddressVO implements IpAddress {
	@Column(name="account_id")
	private Long accountId = null;

    @Column(name="domain_id")
    private Long domainId = null;

	@Id
	@Column(name="public_ip_address")
	private String address = null;

	@Column(name="data_center_id", updatable=false)
	private long dataCenterId;
	
	@Column(name="source_nat")
	private boolean sourceNat;

	@Column(name="allocated")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date allocated;
	
	@Column(name="vlan_db_id")
	private long vlanDbId;

	@Column(name="one_to_one_nat")
	private boolean oneToOneNat;

	protected IPAddressVO() {
	}

	public IPAddressVO(String address, long dataCenterId, long vlanDbId, boolean sourceNat) {
		this.address = address;
		this.dataCenterId = dataCenterId;
		this.vlanDbId = vlanDbId;
		this.sourceNat = sourceNat;
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
    public Long getAccountId() {
		return accountId;
	}
    @Override
    public Long getDomainId() {
        return domainId;
    }
	@Override
    public Date getAllocated() {
		return allocated;
	}
	@Override
    public boolean isSourceNat() {
		return sourceNat;
	}

	@Override
    public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	@Override
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

	@Override
    public void setSourceNat(boolean sourceNat) {
		this.sourceNat = sourceNat;
	}
	
	@Override
    public boolean getSourceNat() {
		return this.sourceNat;
	}

	@Override
    public void setAllocated(Date allocated) {
		this.allocated = allocated;
	}
	
	@Override
    public long getVlanDbId() {
		return this.vlanDbId;
	}
	
	@Override
    public void setVlanDbId(long vlanDbId) {
		this.vlanDbId = vlanDbId;
	}

	@Override
    public boolean isOneToOneNat() {
		return oneToOneNat;
	}

	@Override
    public void setOneToOneNat(boolean oneToOneNat) {
		this.oneToOneNat = oneToOneNat;
	}

}
