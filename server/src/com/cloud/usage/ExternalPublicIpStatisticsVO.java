/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.usage;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name="external_public_ip_statistics")
@PrimaryKeyJoinColumn(name="id")
public class ExternalPublicIpStatisticsVO {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;
	
	@Column(name="data_center_id", updatable=false)
	private long zoneId;
	
	@Column(name="account_id", updatable=false)
	private long accountId;
	
	@Column(name="public_ip_address")
	private String publicIpAddress;
	
	@Column(name="current_bytes_received")
	private long currentBytesReceived;
	
	@Column(name="current_bytes_sent")
	private long currentBytesSent;
	
	protected ExternalPublicIpStatisticsVO() {
	}
	
	public ExternalPublicIpStatisticsVO(long zoneId, long accountId, String publicIpAddress) {
		this.zoneId = zoneId;
		this.accountId = accountId;
		this.publicIpAddress = publicIpAddress;
		this.currentBytesReceived = 0;
		this.currentBytesSent = 0;
	}
	
	public Long getId() {
        return id;
    }
	
	public long getZoneId() {
        return zoneId;
    }
	
	public long getAccountId() {
		return accountId;
	}

	public String getPublicIpAddress() {
		return publicIpAddress;
	}

    public long getCurrentBytesReceived() {
		return currentBytesReceived;
	}
    
    public void setCurrentBytesReceived(long currentBytesReceived) {
		this.currentBytesReceived = currentBytesReceived;
	}

	public long getCurrentBytesSent() {
		return currentBytesSent;
	}	

	public void setCurrentBytesSent(long currentBytesSent) {
		this.currentBytesSent = currentBytesSent;
	}
	
}
