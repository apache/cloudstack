/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
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

    @Column(name="net_bytes_received")
    private long netBytesReceived;
    
    @Column(name="net_bytes_sent")
    private long netBytesSent;
    
    @Column(name="current_bytes_received")
    private long currentBytesReceived;
    
    @Column(name="current_bytes_sent")
    private long currentBytesSent;

	@Column(name="event_time_millis")
	private long eventTimeMillis = 0;
	
	protected UsageNetworkVO() {
	}

	public UsageNetworkVO(Long accountId, long zoneId, long hostId, String hostType, Long networkId, long bytesSent, long bytesReceived, long netBytesReceived, long netBytesSent, long currentBytesReceived, 
	        long currentBytesSent, long eventTimeMillis) {
		this.accountId = accountId;
		this.zoneId = zoneId;
		this.hostId = hostId;
		this.hostType = hostType;
		this.networkId = networkId;
		this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
		this.netBytesReceived = netBytesReceived;
		this.netBytesSent = netBytesSent;
		this.currentBytesReceived = currentBytesReceived;
		this.currentBytesSent = currentBytesSent;
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

    public long getCurrentBytesReceived() {
        return currentBytesReceived;
    }

    public long getCurrentBytesSent() {
        return currentBytesSent;
    }

    public long getNetBytesReceived() {
        return netBytesReceived;
    }

    public long getNetBytesSent() {
        return netBytesSent;
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
}
