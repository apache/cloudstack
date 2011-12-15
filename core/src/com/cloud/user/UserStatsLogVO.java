/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="op_user_stats_log")
public class UserStatsLogVO {
	@Id
	@Column(name="user_stats_id")
	private long userStatsId;
	
	@Column(name="net_bytes_received")
	private long netBytesReceived;
	
	@Column(name="net_bytes_sent")
	private long netBytesSent;
	
	@Column(name="current_bytes_received")
	private long currentBytesReceived;
	
	@Column(name="current_bytes_sent")
	private long currentBytesSent;
	
	@Column(name="agg_bytes_received")
	private long aggBytesReceived;
	
	@Column(name="agg_bytes_sent")
	private long aggBytesSent;
	
	@Column(name="updated")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date updatedTime;
	
	public UserStatsLogVO(){
	}
	
	public UserStatsLogVO(long userStatsId, long netBytesReceived, long netBytesSent, long currentBytesReceived, long currentBytesSent, 
							long aggBytesReceived, long aggBytesSent, Date updatedTime) {
		this.userStatsId = userStatsId;
		this.netBytesReceived = netBytesReceived;
		this.netBytesSent = netBytesSent;
		this.currentBytesReceived = currentBytesReceived;
		this.currentBytesSent = currentBytesSent;
		this.aggBytesReceived = aggBytesReceived;
		this.aggBytesSent = aggBytesSent;
		this.updatedTime = updatedTime;
	}

	public Long getUserStatsId() {
        return userStatsId;
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

	public long getAggBytesReceived() {
		return aggBytesReceived;
	}

	public void setAggBytesReceived(long aggBytesReceived) {
		this.aggBytesReceived = aggBytesReceived;
	}

	public long getAggBytesSent() {
		return aggBytesSent;
	}

	public void setAggBytesSent(long aggBytesSent) {
		this.aggBytesSent = aggBytesSent;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

}
