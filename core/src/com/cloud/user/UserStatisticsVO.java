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

package com.cloud.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="user_statistics")
public class UserStatisticsVO {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;
	
	@Column(name="data_center_id", updatable=false)
	private long dataCenterId;
	
	@Column(name="account_id", updatable=false)
	private long accountId;
	
	@Column(name="net_bytes_received")
	private long netBytesReceived;
	
	@Column(name="net_bytes_sent")
	private long netBytesSent;
	
	@Column(name="current_bytes_received")
	private long currentBytesReceived;
	
	@Column(name="current_bytes_sent")
	private long currentBytesSent;
	
	protected UserStatisticsVO() {
	}
	
	public UserStatisticsVO(long accountId, long dcId) {
		this.accountId = accountId;
		this.netBytesReceived = 0;
		this.netBytesSent = 0;
		currentBytesReceived = 0;
		currentBytesSent = 0;
		dataCenterId = dcId;
	}

	public long getAccountId() {
		return accountId;
	}

	public Long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
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

    public long getNetBytesReceived() {
		return netBytesReceived;
	}

	public long getNetBytesSent() {
		return netBytesSent;
	}

	public void setNetBytesReceived(long netBytesReceived) {
		this.netBytesReceived = netBytesReceived;
	}

	public void setNetBytesSent(long netBytesSent) {
		this.netBytesSent = netBytesSent;
	}
}
