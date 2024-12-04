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

import org.apache.cloudstack.management.ManagementServerHost;

@Entity
@Table(name = "mshost_peer_view")
public class ManagementServerHostPeerJoinVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "peer_state")
    @Enumerated(value = EnumType.STRING)
    private ManagementServerHost.State peerState;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update")
    private Date lastUpdateTime;

    @Column(name = "owner_mshost_id")
    private long ownerMshostId;

    @Column(name = "owner_mshost_msid")
    private long ownerMshostMsId;

    @Column(name = "owner_mshost_runid")
    private long ownerMshostRunId;

    @Column(name = "owner_mshost_name")
    private String ownerMshostName;

    @Column(name = "owner_mshost_uuid")
    private String ownerMshostUuid;

    @Column(name = "owner_mshost_state")
    private String ownerMshostState;

    @Column(name = "owner_mshost_service_ip")
    private String ownerMshostServiceIp;

    @Column(name = "owner_mshost_service_port")
    private Integer ownerMshostServicePort;

    @Column(name = "peer_mshost_id")
    private long peerMshostId;

    @Column(name = "peer_mshost_msid")
    private long peerMshostMsId;

    @Column(name = "peer_mshost_runid")
    private long peerMshostRunId;

    @Column(name = "peer_mshost_name")
    private String peerMshostName;

    @Column(name = "peer_mshost_uuid")
    private String peerMshostUuid;

    @Column(name = "peer_mshost_state")
    private String peerMshostState;

    @Column(name = "peer_mshost_service_ip")
    private String peerMshostServiceIp;

    @Column(name = "peer_mshost_service_port")
    private Integer peerMshostServicePort;

    public ManagementServerHostPeerJoinVO() {
    }

    public long getId() {
        return id;
    }

    public ManagementServerHost.State getPeerState() {
        return peerState;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public long getOwnerMshostId() {
        return ownerMshostId;
    }

    public long getOwnerMshostMsId() {
        return ownerMshostMsId;
    }

    public long getOwnerMshostRunId() {
        return ownerMshostRunId;
    }

    public String getOwnerMshostName() {
        return ownerMshostName;
    }

    public String getOwnerMshostUuid() {
        return ownerMshostUuid;
    }

    public String getOwnerMshostState() {
        return ownerMshostState;
    }

    public String getOwnerMshostServiceIp() {
        return ownerMshostServiceIp;
    }

    public Integer getOwnerMshostServicePort() {
        return ownerMshostServicePort;
    }

    public long getPeerMshostId() {
        return peerMshostId;
    }

    public long getPeerMshostMsId() {
        return peerMshostMsId;
    }

    public long getPeerMshostRunId() {
        return peerMshostRunId;
    }

    public String getPeerMshostName() {
        return peerMshostName;
    }

    public String getPeerMshostUuid() {
        return peerMshostUuid;
    }

    public String getPeerMshostState() {
        return peerMshostState;
    }

    public String getPeerMshostServiceIp() {
        return peerMshostServiceIp;
    }

    public Integer getPeerMshostServicePort() {
        return peerMshostServicePort;
    }
}
