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

package com.cloud.cluster;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.DateUtil;

@Entity
@Table(name="mshost_peer")
public class ManagementServerHostPeerVO {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="owner_mshost", updatable=true, nullable=false)
    private long ownerMshost;
    
    @Column(name="peer_mshost", updatable=true, nullable=false)
    private long peerMshost;
    
    @Column(name="peer_runid", updatable=true, nullable=false)
    private long peerRunid;

    @Column(name="peer_state", updatable = true, nullable=false)
    @Enumerated(value=EnumType.STRING)
    private ManagementServerHost.State peerState;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_update", updatable=true, nullable=true)
    private Date lastUpdateTime;

    public ManagementServerHostPeerVO() {
    }
    
    public ManagementServerHostPeerVO(long ownerMshost, long peerMshost, long peerRunid, ManagementServerHost.State peerState) {
        this.ownerMshost = ownerMshost;
        this.peerMshost = peerMshost;
        this.peerRunid = peerRunid;
        this.peerState = peerState;
        
        this.lastUpdateTime = DateUtil.currentGMTTime();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOwnerMshost() {
        return ownerMshost;
    }

    public void setOwnerMshost(long ownerMshost) {
        this.ownerMshost = ownerMshost;
    }

    public long getPeerMshost() {
        return peerMshost;
    }

    public void setPeerMshost(long peerMshost) {
        this.peerMshost = peerMshost;
    }

    public long getPeerRunid() {
        return peerRunid;
    }

    public void setPeerRunid(long peerRunid) {
        this.peerRunid = peerRunid;
    }

    public ManagementServerHost.State getPeerState() {
        return peerState;
    }

    public void setPeerState(ManagementServerHost.State peerState) {
        this.peerState = peerState;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
