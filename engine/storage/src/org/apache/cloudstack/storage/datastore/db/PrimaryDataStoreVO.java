/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.storage.datastore.DataStoreStatus;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "storage_pool")
public class PrimaryDataStoreVO implements Identity {
    @Id
    @TableGenerator(name = "storage_pool_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "storage_pool_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "name", updatable = false, nullable = false, length = 255)
    private String name = null;

    @Column(name = "uuid", length = 255)
    private String uuid = null;

    @Column(name = "pool_type", updatable = false, nullable = false, length = 32)
    private String poolType;

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

    @Column(name = "available_bytes", updatable = true, nullable = true)
    private long availableBytes;

    @Column(name = "capacity_bytes", updatable = true, nullable = true)
    private long capacityBytes;

    @Column(name = "status", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private DataStoreStatus status;

    @Column(name = "storage_provider_id", updatable = true, nullable = false)
    private Long storageProviderId;

    @Column(name = "host_address")
    private String hostAddress;

    @Column(name = "path")
    private String path;

    @Column(name = "port")
    private int port;

    @Column(name = "user_info")
    private String userInfo;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;

    public long getId() {
        return id;
    }

    public DataStoreStatus getStatus() {
        return status;
    }

    public PrimaryDataStoreVO() {
        this.status = DataStoreStatus.Initial;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getPoolType() {
        return poolType;
    }

    public void setPoolType(String protocol) {
        this.poolType = protocol;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public Long getStorageProviderId() {
        return storageProviderId;
    }

    public void setStorageProviderId(Long provider) {
        storageProviderId = provider;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public void setAvailableBytes(long available) {
        availableBytes = available;
    }

    public void setCapacityBytes(long capacity) {
        capacityBytes = capacity;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String host) {
        this.hostAddress = host;
    }

    public String getPath() {
        return path;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setStatus(DataStoreStatus status) {
        this.status = status;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDataCenterId(long dcId) {
        this.dataCenterId = dcId;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

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
        return this.scope;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PrimaryDataStoreVO) || obj == null) {
            return false;
        }
        PrimaryDataStoreVO that = (PrimaryDataStoreVO) obj;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return new Long(id).hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("Pool[").append(id).append("|").append(poolType).append("]").toString();
    }
}