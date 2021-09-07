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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.utils.db.GenericDao;

/**
 * Storage Pool DB view.
 *
 */
@Entity
@Table(name = "storage_pool_view")
public class StoragePoolJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "path")
    private String path;

    @Column(name = "host_address")
    private String hostAddress;

    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private StoragePoolStatus status;

    @Column(name = "pool_type")
    @Enumerated(value = EnumType.STRING)
    private StoragePoolType poolType;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "capacity_bytes")
    private long capacityBytes;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "cluster_uuid")
    private String clusterUuid;

    @Column(name = "cluster_name")
    private String clusterName;

    @Column(name = "cluster_type")
    @Enumerated(value = EnumType.STRING)
    private Cluster.ClusterType clusterType;

    @Column(name = "data_center_id")
    private long zoneId;

    @Column(name = "data_center_uuid")
    private String zoneUuid;

    @Column(name = "data_center_name")
    private String zoneName;

    @Column(name = "pod_id")
    private long podId;

    @Column(name = "pod_uuid")
    private String podUuid;

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "tag")
    private String tag;

    @Column(name = "disk_used_capacity")
    private long usedCapacity;

    @Column(name = "disk_reserved_capacity")
    private long reservedCapacity;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_uuid")
    private String jobUuid;

    @Column(name = "job_status")
    private int jobStatus;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;

    @Column(name = "capacity_iops")
    private Long capacityIops;

    @Column(name = "hypervisor")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisor;

    @Column(name = "storage_provider_name")
    private String storageProviderName;

    @Column(name = "parent")
    private Long parent;

    /**
     * @return the scope
     */
    public ScopeType getScope() {
        return scope;
    }

    public HypervisorType getHypervisor() {
        return hypervisor;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public StoragePoolStatus getStatus() {
        return status;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public Long getCapacityIops() {
        return capacityIops;
    }

    public long getClusterId() {
        return clusterId;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public String getClusterName() {
        return clusterName;
    }

    public Cluster.ClusterType getClusterType() {
        return clusterType;
    }

    public long getZoneId() {
        return zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public long getPodId() {
        return podId;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public String getPodName() {
        return podName;
    }

    public String getTag() {
        return tag;
    }

    public long getUsedCapacity() {
        return usedCapacity;
    }

    public long getReservedCapacity() {
        return reservedCapacity;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public String getStorageProviderName() {
        return storageProviderName;
    }

    public Long getParent() {
        return parent;
    }
}
