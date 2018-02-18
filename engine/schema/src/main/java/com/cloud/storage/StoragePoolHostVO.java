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
package com.cloud.storage;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDaoBase;

/**
 * Join table for storage pools and hosts
 *
 */
@Entity
@Table(name = "storage_pool_host_ref")
public class StoragePoolHostVO implements StoragePoolHostAssoc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pool_id")
    private long poolId;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "local_path")
    private String localPath;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdated = null;

    public StoragePoolHostVO() {
        super();
    }

    public StoragePoolHostVO(long poolId, long hostId, String localPath) {
        this.poolId = poolId;
        this.hostId = hostId;
        this.localPath = localPath;
    }

    @Override
    public long getHostId() {
        return hostId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public long getPoolId() {
        return poolId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

}
