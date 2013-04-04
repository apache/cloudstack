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
package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import com.cloud.org.Cluster;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;

/**
 * Storage Pool DB view.
 *
 */
@Entity
@Table(name="storage_pool_view")
public class StoragePoolJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="name")
    private String name;

    @Column(name="path")
    private String path;

    @Column(name="host_address")
    private String hostAddress;


    @Column(name="status")
    @Enumerated(value=EnumType.STRING)
    private StoragePoolStatus status;

    @Column(name="pool_type")
    @Enumerated(value=EnumType.STRING)
    private StoragePoolType poolType;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="capacity_bytes")
    private long capacityBytes;

    @Column(name="cluster_id")
    private long clusterId;

    @Column(name="cluster_uuid")
    private String clusterUuid;

    @Column(name="cluster_name")
    private String clusterName;

    @Column(name="cluster_type")
    @Enumerated(value=EnumType.STRING)
    private Cluster.ClusterType clusterType;

    @Column(name="data_center_id")
    private long zoneId;

    @Column(name="data_center_uuid")
    private String zoneUuid;

    @Column(name="data_center_name")
    private String zoneName;

    @Column(name="pod_id")
    private long podId;

    @Column(name="pod_uuid")
    private String podUuid;

    @Column(name="pod_name")
    private String podName;


    @Column(name="tag")
    private String tag;

    @Column(name="disk_used_capacity")
    private long usedCapacity;

    @Column(name="disk_reserved_capacity")
    private long reservedCapacity;


    @Column(name="job_id")
    private long jobId;

    @Column(name="job_uuid")
    private String jobUuid;

    @Column(name="job_status")
    private int jobStatus;
    
    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;

    /**
     * @return the scope
     */
    public ScopeType getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     */
    public void setScope(ScopeType scope) {
        this.scope = scope;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public StoragePoolStatus getStatus() {
        return status;
    }

    public void setStatus(StoragePoolStatus status) {
        this.status = status;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public void setPoolType(StoragePoolType poolType) {
        this.poolType = poolType;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public void setCapacityBytes(long capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Cluster.ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(Cluster.ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public void setPodUuid(String podUuid) {
        this.podUuid = podUuid;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public long getReservedCapacity() {
        return reservedCapacity;
    }

    public void setReservedCapacity(long reservedCapacity) {
        this.reservedCapacity = reservedCapacity;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(int jobStatus) {
        this.jobStatus = jobStatus;
    }


}
