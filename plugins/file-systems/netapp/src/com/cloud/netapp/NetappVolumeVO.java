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
package com.cloud.netapp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "netapp_volume")
public class NetappVolumeVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "aggregate_name")
    private String aggregateName;

    @Column(name = "pool_id")
    private Long poolId;

    @Column(name = "pool_name")
    private String poolName;

    @Column(name = "volume_name")
    private String volumeName;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "snapshot_policy")
    private String snapshotPolicy;

    @Column(name = "snapshot_reservation")
    private Integer snapshotReservation;

    @Column(name = "volume_size")
    private String volumeSize;

    @Column(name = "round_robin_marker")
    private int roundRobinMarker;

    public NetappVolumeVO() {

    }

    public NetappVolumeVO(String ipAddress, String aggName, Long poolId, String volName, String volSize, String snapshotPolicy, int snapshotReservation, String username,
            String password, int roundRobinMarker, String poolName) {
        this.ipAddress = ipAddress;
        this.aggregateName = aggName;
        this.poolId = poolId;
        this.username = username;
        this.password = password;
        this.volumeName = volName;
        this.volumeSize = volSize;
        this.snapshotPolicy = snapshotPolicy;
        this.snapshotReservation = snapshotReservation;
        this.roundRobinMarker = roundRobinMarker;
        this.poolName = poolName;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getRoundRobinMarker() {
        return roundRobinMarker;
    }

    public void setRoundRobinMarker(int roundRobinMarker) {
        this.roundRobinMarker = roundRobinMarker;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getSnapshotPolicy() {
        return snapshotPolicy;
    }

    public void setSnapshotPolicy(String snapshotPolicy) {
        this.snapshotPolicy = snapshotPolicy;
    }

    public Integer getSnapshotReservation() {
        return snapshotReservation;
    }

    public void setSnapshotReservation(Integer snapshotReservation) {
        this.snapshotReservation = snapshotReservation;
    }

    public String getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(String volumeSize) {
        this.volumeSize = volumeSize;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAggregateName() {
        return aggregateName;
    }

    public void setAggregateName(String aggregateName) {
        this.aggregateName = aggregateName;
    }

    public Long getPoolId() {
        return poolId;
    }

    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
