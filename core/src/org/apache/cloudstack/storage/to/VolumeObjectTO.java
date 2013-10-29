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
package org.apache.cloudstack.storage.to;

import com.cloud.hypervisor.Hypervisor;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;

public class VolumeObjectTO implements DataTO {
    private String uuid;
    private Volume.Type volumeType;
    private DataStoreTO dataStore;
    private String name;
    private Long size;
    private String path;
    private Long volumeId;
    private String vmName;
    private long accountId;
    private String chainInfo;
    private Storage.ImageFormat format;
    private long id;

    private Long deviceId;
    private Long bytesReadRate;
    private Long bytesWriteRate;
    private Long iopsReadRate;
    private Long iopsWriteRate;
    private Hypervisor.HypervisorType hypervisorType;

    public VolumeObjectTO() {

    }

    public VolumeObjectTO(VolumeInfo volume) {
        this.uuid = volume.getUuid();
        this.path = volume.getPath();
        this.accountId = volume.getAccountId();
        if (volume.getDataStore() != null) {
            this.dataStore = volume.getDataStore().getTO();
        } else {
            this.dataStore = null;
        }
        this.vmName = volume.getAttachedVmName();
        this.size = volume.getSize();
        this.setVolumeId(volume.getId());
        this.chainInfo = volume.getChainInfo();
        this.volumeType = volume.getVolumeType();
        this.name = volume.getName();
        this.setId(volume.getId());
        this.format = volume.getFormat();
        this.bytesReadRate = volume.getBytesReadRate();
        this.bytesWriteRate = volume.getBytesWriteRate();
        this.iopsReadRate = volume.getIopsReadRate();
        this.iopsWriteRate = volume.getIopsWriteRate();
        this.hypervisorType = volume.getHypervisorType();
        setDeviceId(volume.getDeviceId());
    }

    public String getUuid() {
        return this.uuid;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    public Volume.Type getVolumeType() {
        return this.volumeType;
    }

    @Override
    public DataStoreTO getDataStore() {
        return this.dataStore;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return this.hypervisorType;
    }


    public void setDataStore(DataStoreTO store){
        this.dataStore = store;
    }

    public void setDataStore(PrimaryDataStoreTO dataStore) {
        this.dataStore = dataStore;
    }

    public String getName() {
        return this.name;
    }

    public Long getSize() {
        return this.size;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.VOLUME;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getChainInfo() {
        return chainInfo;
    }

    public void setChainInfo(String chainInfo) {
        this.chainInfo = chainInfo;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Storage.ImageFormat getFormat() {
        return format;
    }

    public void setFormat(Storage.ImageFormat format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return new StringBuilder("volumeTO[uuid=").append(uuid).append("|path=").append(path)
                .append("|datastore=").append(dataStore).append("]").toString();
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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }


}
