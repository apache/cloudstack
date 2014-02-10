// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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
@Table(name = "mshost_peer")
public class ManagementServerHostPeerVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "owner_mshost", updatable = true, nullable = false)
    private long ownerMshost;

    @Column(name = "peer_mshost", updatable = true, nullable = false)
    private long peerMshost;

    @Column(name = "peer_runid", updatable = true, nullable = false)
    private long peerRunid;

    @Column(name = "peer_state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ManagementServerHost.State peerState;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", updatable = true, nullable = true)
    private Date lastUpdateTime;

    public ManagementServerHostPeerVO() {
    }

    public ManagementServerHostPeerVO(long ownerMshost, long peerMshost, long peerRunid, ManagementServerHost.State peerState) {
        this.ownerMshost = ownerMshost;
        this.peerMshost = peerMshost;
        this.peerRunid = peerRunid;
        this.peerState = peerState;

        lastUpdateTime = DateUtil.currentGMTTime();
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
