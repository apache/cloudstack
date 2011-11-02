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
package com.cloud.dc;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.org.Managed.ManagedState;
import com.cloud.org.Grouping;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="cluster")
public class ClusterVO implements Cluster, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="guid")
    String guid;
    
    @Column(name="data_center_id")
    long dataCenterId;
    
    @Column(name="pod_id")
    long podId;
    
    @Column(name="hypervisor_type")
    String hypervisorType;

    @Column(name="cluster_type")
    @Enumerated(value=EnumType.STRING)
    Cluster.ClusterType clusterType;
    
    @Column(name="allocation_state")
    @Enumerated(value=EnumType.STRING)
    AllocationState allocationState;
    
    @Column(name="managed_state")
    @Enumerated(value=EnumType.STRING)
    ManagedState managedState;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="uuid")
    String uuid;
    
    public ClusterVO() {
    	clusterType = Cluster.ClusterType.CloudManaged;
    	allocationState = Grouping.AllocationState.Enabled;
    	
    	this.uuid = UUID.randomUUID().toString();
    }
    
    public ClusterVO(long dataCenterId, long podId, String name) {
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.name = name;
    	this.clusterType = Cluster.ClusterType.CloudManaged;
    	this.allocationState = Grouping.AllocationState.Enabled;
    	this.managedState = ManagedState.Managed;
    	this.uuid = UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getPodId() {
        return podId;
    }
    
    public Cluster.ClusterType getClusterType() {
    	return clusterType;
    }
    
    public void setClusterType(Cluster.ClusterType clusterType) {
    	this.clusterType = clusterType;
    }
    
    public AllocationState getAllocationState() {
    	return allocationState;
    }
    
    public void setAllocationState(AllocationState allocationState) {
		this.allocationState = allocationState;
    }
    
    public ManagedState getManagedState() {
        return managedState;
    }

    public void setManagedState(ManagedState managedState) {
        this.managedState = managedState;
    }

    public ClusterVO(long clusterId) {
        this.id = clusterId;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClusterVO)) {
            return false;
        }
        ClusterVO that = (ClusterVO)obj;
        return this.id == that.id;
    }
    
    public HypervisorType getHypervisorType() {
    	return HypervisorType.getType(hypervisorType);
    }

	public void setHypervisorType(String hy) {
    	hypervisorType = hy;
    }
	
    public String getGuid() {
        return guid;
    }
	
    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
