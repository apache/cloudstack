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
package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.util.StoragePoolTypeConverter;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.storage.Storage.StoragePoolType;

@Entity
@Table(name = "mockstoragepool")
public class MockStoragePoolVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "guid")
    private String uuid;

    @Column(name = "mount_point")
    private String mountPoint;

    @Column(name = "capacity")
    private long capacity;

    @Column(name = "hostguid")
    private String hostGuid;

    @Column(name = "pool_type")
    @Convert(converter = StoragePoolTypeConverter.class)
    private StoragePoolType poolType;

    public MockStoragePoolVO() {

    }

    public String getHostGuid() {
        return this.hostGuid;
    }

    public void setHostGuid(String hostGuid) {
        this.hostGuid = hostGuid;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public StoragePoolType getPoolType() {
        return this.poolType;
    }

    public void setStorageType(StoragePoolType poolType) {
        this.poolType = poolType;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMountPoint() {
        return this.mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint.replace('\\', '/');
    }

    public long getCapacity() {
        return this.capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
}
