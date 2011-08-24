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
 * @author-aj
 */

package com.cloud.netapp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="netapp_volume")
public class NetappVolumeVO {
	
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
	
	@Column(name="ip_address")
    private String ipAddress;

    @Column(name="aggregate_name")
	private String aggregateName;

    @Column(name="pool_id")
    private Long poolId;

	@Column(name="pool_name")
    private String poolName;
	
	@Column(name="volume_name")
    private String volumeName;

	@Column(name="username")
    private String username;
    
    @Column(name="password")
    private String password;

    @Column(name="snapshot_policy")
    private String snapshotPolicy;
    
    @Column(name="snapshot_reservation")
    private Integer snapshotReservation;
    
    @Column(name="volume_size")
    private String volumeSize;
    
    @Column(name="round_robin_marker")
    private int roundRobinMarker;

	public NetappVolumeVO(){
		
	}
	
	public NetappVolumeVO(String ipAddress, String aggName, Long poolId, String volName, String volSize, String snapshotPolicy, int snapshotReservation, String username, String password, int roundRobinMarker, String poolName) {
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
	
    public Long getId() {
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
