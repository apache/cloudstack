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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="usage_network")
public class UsageNetworkVO {
	@Id
    @Column(name="account_id")
    private long accountId;

	@Column(name="zone_id")
	private long zoneId;
	
	@Column(name="host_id")
	private long hostId;
	
	@Column(name="host_type")
	private String hostType;
	
	@Column(name="network_id")
	private Long networkId;

	
	@Column(name="bytes_sent")
	private long bytesSent;

    @Column(name="bytes_received")
    private long bytesReceived;

    @Column(name="agg_bytes_received")
    private long aggBytesReceived;
    
    @Column(name="agg_bytes_sent")
    private long aggBytesSent;
    
	@Column(name="event_time_millis")
	private long eventTimeMillis = 0;
	
	protected UsageNetworkVO() {
	}

	public UsageNetworkVO(Long accountId, long zoneId, long hostId, String hostType, Long networkId, long bytesSent, long bytesReceived, long aggBytesReceived, long aggBytesSent, long eventTimeMillis) {
		this.accountId = accountId;
		this.zoneId = zoneId;
		this.hostId = hostId;
		this.hostType = hostType;
		this.networkId = networkId;
		this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
		this.aggBytesReceived = aggBytesReceived;
		this.aggBytesSent = aggBytesSent;
		this.eventTimeMillis = eventTimeMillis;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public long getZoneId() {
	    return zoneId;
	}
	public void setZoneId(long zoneId) {
	    this.zoneId = zoneId;
	}

	public Long getBytesSent() {
		return bytesSent;
	}
	
	public void setBytesSent(Long bytesSent) {
	    this.bytesSent = bytesSent;
	}

    public Long getBytesReceived() {
        return bytesReceived;
    }
    
    public void setBytes(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public long getEventTimeMillis() {
	    return eventTimeMillis;
	}
	public void setEventTimeMillis(long eventTimeMillis) {
	    this.eventTimeMillis = eventTimeMillis;
	}

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getHostId() {
        return hostId;
    }
    
    public String getHostType() {
        return hostType;
    }
    
    public Long getNetworkId() {
        return networkId;
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
}
