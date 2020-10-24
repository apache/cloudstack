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
package org.apache.cloudstack.storage.datastore.db;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "storage_pool")
public class StoragePoolVO implements StoragePool {
    @Id
    @TableGenerator(name = "storage_pool_sq",
                    table = "sequence",
                    pkColumnName = "name",
                    valueColumnName = "value",
                    pkColumnValue = "storage_pool_seq",
                    allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "name", updatable = false, nullable = false, length = 255)
    private String name = null;

    @Column(name = "uuid", length = 255)
    private String uuid = null;

    @Column(name = "pool_type", updatable = false, nullable = false, length = 32)
    @Enumerated(value = EnumType.STRING)
    private StoragePoolType poolType;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "update_time", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updateTime;

    @Column(name = "data_center_id", updatable = true, nullable = false)
    private long dataCenterId;

    @Column(name = "pod_id", updatable = true)
    private Long podId;

    @Column(name = "used_bytes", updatable = true, nullable = true)
    private long usedBytes;

    @Column(name = "capacity_bytes", updatable = true, nullable = true)
    private long capacityBytes;

    @Column(name = "status", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private StoragePoolStatus status;

    @Column(name = "storage_provider_name", updatable = true, nullable = false)
    private String storageProviderName;

    @Column(name = "host_address")
    private String hostAddress;

    @Column(name = "path")
    private String path;

    @Column(name = "port")
    private int port;

    @Encrypt
    @Column(name = "user_info")
    private String userInfo;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;

    @Column(name = "managed")
    private boolean managed;

    @Column(name = "capacity_iops", updatable = true, nullable = true)
    private Long capacityIops;

    @Column(name = "hypervisor")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisor;

    @Column(name = "parent")
    private Long parent = 0L;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public StoragePoolStatus getStatus() {
        return status;
    }

    public StoragePoolVO() {
        status = StoragePoolStatus.Initial;
    }

    public StoragePoolVO(long poolId, String name, String uuid, StoragePoolType type, long dataCenterId, Long podId, long availableBytes, long capacityBytes,
            String hostAddress, int port, String hostPath) {
        this.name = name;
        id = poolId;
        this.uuid = uuid;
        poolType = type;
        this.dataCenterId = dataCenterId;
        usedBytes = availableBytes;
        this.capacityBytes = capacityBytes;
        this.hostAddress = hostAddress;
        this.port = port;
        this.podId = podId;
        setStatus(StoragePoolStatus.Initial);
        setPath(hostPath);
    }

    public StoragePoolVO(StoragePoolVO that) {
        this(that.id, that.name, that.uuid, that.poolType, that.dataCenterId, that.podId, that.usedBytes, that.capacityBytes, that.hostAddress, that.port, that.path);
    }

    public StoragePoolVO(StoragePoolType type, String hostAddress, int port, String path) {
        poolType = type;
        this.hostAddress = hostAddress;
        this.port = port;
        setStatus(StoragePoolStatus.Initial);
        uuid = UUID.randomUUID().toString();
        setPath(path);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public StoragePoolType getPoolType() {
        return poolType;
    }

    public void setPoolType(StoragePoolType protocol) {
        poolType = protocol;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getUsedBytes() {
        return usedBytes;
    }

    @Override
    public String getStorageProviderName() {
        return storageProviderName;
    }

    public void setStorageProviderName(String providerName) {
        storageProviderName = providerName;
    }

    @Override
    public long getCapacityBytes() {
        return capacityBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public void setCapacityBytes(long capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    @Override
    public boolean isManaged() {
        return managed;
    }

    public void setCapacityIops(Long capacityIops) {
        this.capacityIops = capacityIops;
    }

    @Override
    public Long getCapacityIops() {
        return capacityIops;
    }

    @Override
    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String host) {
        hostAddress = host;
    }

    @Override
    public String getPath() {
        String updatedPath = path;
        if (poolType == StoragePoolType.SMB) {
            updatedPath = UriUtils.getUpdateUri(updatedPath, false);
            if (updatedPath.contains("password") && updatedPath.contains("?")) {
                updatedPath = updatedPath.substring(0, updatedPath.indexOf('?'));
            }
        }

        return updatedPath;
    }

    @Override
    public String getUserInfo() {
        return userInfo;
    }

    public void setStatus(StoragePoolStatus status) {
        this.status = status;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDataCenterId(long dcId) {
        dataCenterId = dcId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public Long getPodId() {
        return podId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScope(ScopeType scope) {
        this.scope = scope;
    }

    public ScopeType getScope() {
        return scope;
    }

    @Override
    public HypervisorType getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(HypervisorType hypervisor) {
        this.hypervisor = hypervisor;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StoragePoolVO) || obj == null) {
            return false;
        }
        StoragePoolVO that = (StoragePoolVO)obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return new Long(id).hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("Pool[").append(id).append("|").append(poolType).append("]").toString();
    }

    @Override
    public boolean isShared() {
        return scope == ScopeType.HOST ? false : true;
    }

    @Override
    public boolean isLocal() {
        return !isShared();
    }

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    @Override
    public boolean isInMaintenance() {
        return status == StoragePoolStatus.PrepareForMaintenance || status == StoragePoolStatus.Maintenance || status == StoragePoolStatus.ErrorInMaintenance ||
            removed != null;
    }
}
