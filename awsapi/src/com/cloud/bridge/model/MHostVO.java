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
package com.cloud.bridge.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity
@Table(name = "mhost")
public class MHostVO implements Serializable {
    private static final long serialVersionUID = 4848254624679753930L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "MHostKey", nullable = false)
    private String hostKey;

    @Column(name = "Host")
    private String host;

    @Column(name = "Version")
    private String version;

    @Column(name = "LastHeartbeatTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastHeartbeatTime;

    @Transient
    private Set<SHostVO> localSHosts = new HashSet<SHostVO>();

    @Transient
    private Set<MHostMountVO> mounts = new HashSet<MHostMountVO>();

    public MHostVO() {
    }

    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public String getHostKey() {
        return hostKey;
    }

    public void setHostKey(String hostKey) {
        this.hostKey = hostKey;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(Date lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public Set<SHostVO> getLocalSHosts() {
        return localSHosts;
    }

    public void setLocalSHosts(Set<SHostVO> localSHosts) {
        this.localSHosts = localSHosts;
    }

    public Set<MHostMountVO> getMounts() {
        return mounts;
    }

    public void setMounts(Set<MHostMountVO> mounts) {
        this.mounts = mounts;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof MHostVO))
            return false;

        return hostKey == ((MHostVO)other).getHostKey();
    }

    @Override
    public int hashCode() {
        return hostKey.hashCode();
    }
}
