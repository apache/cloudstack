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
package com.cloud.agent.api.to;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;

public class VolumeTO implements InternalIdentity {
    protected VolumeTO() {
    }

    private long id;
    private String name;
    private String mountPoint;
    private String path;
    private long size;
    private Volume.Type type;
    private StoragePoolType storagePoolType;
    private String storagePoolUuid;
    private long deviceId;
    private String chainInfo;
    private String guestOsType;
    private Long bytesReadRate;
    private Long bytesWriteRate;
    private Long iopsReadRate;
    private Long iopsWriteRate;
    private Long chainSize;

    public VolumeTO(long id, Volume.Type type, StoragePoolType poolType, String poolUuid, String name, String mountPoint, String path, long size, String chainInfo) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.size = size;
        this.type = type;
        this.storagePoolType = poolType;
        this.storagePoolUuid = poolUuid;
        this.mountPoint = mountPoint;
        this.chainInfo = chainInfo;
    }

    public VolumeTO(long id, Volume.Type type, StoragePoolType poolType, String poolUuid, String name, String mountPoint, String path, long size, String chainInfo, String guestOsType) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.size = size;
        this.type = type;
        this.storagePoolType = poolType;
        this.storagePoolUuid = poolUuid;
        this.mountPoint = mountPoint;
        this.chainInfo = chainInfo;
        this.guestOsType = guestOsType;
    }

    public VolumeTO(Volume volume, StoragePool pool) {
        this.id = volume.getId();
        this.name = volume.getName();
        this.path = volume.getPath();
        this.size = volume.getSize();
        this.type = volume.getVolumeType();
        this.storagePoolType = pool.getPoolType();
        this.storagePoolUuid = pool.getUuid();
        this.mountPoint = volume.getFolder();
        this.chainInfo = volume.getChainInfo();
        this.chainSize = volume.getVmSnapshotChainSize();
        if (volume.getDeviceId() != null)
            this.deviceId = volume.getDeviceId();
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long id) {
        this.deviceId = id;
    }

    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public Volume.Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public StoragePoolType getPoolType() {
        return storagePoolType;
    }

    public String getPoolUuid() {
        return storagePoolUuid;
    }

    public String getChainInfo() {
        return chainInfo;
    }
    
    public void setChainInfo(String chainInfo) {
    	this.chainInfo = chainInfo;
    }

    public String getOsType() {
        return guestOsType;
    }
    
    public void setPath(String path){
        this.path = path;
    }

    @Override
    public String toString() {
        return new StringBuilder("Vol[").append(id).append("|").append(type).append("|").append(path).append("|").append(size).append("]").toString();
    }

    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public Long getChainSize() {
        return chainSize;
    }

    public void setChainSize(Long chainSize) {
        this.chainSize = chainSize;
    }
}
