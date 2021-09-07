//
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
//

package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering.DiskCacheMode;
import com.cloud.storage.MigrationOptions;
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
    private Storage.ProvisioningType provisioningType;
    private Long poolId;
    private long id;

    private Long deviceId;
    private Long bytesReadRate;
    private Long bytesReadRateMax;
    private Long bytesReadRateMaxLength;
    private Long bytesWriteRate;
    private Long bytesWriteRateMax;
    private Long bytesWriteRateMaxLength;
    private Long iopsReadRate;
    private Long iopsReadRateMax;
    private Long iopsReadRateMaxLength;
    private Long iopsWriteRate;
    private Long iopsWriteRateMax;
    private Long iopsWriteRateMaxLength;
    private DiskCacheMode cacheMode;
    private Hypervisor.HypervisorType hypervisorType;
    private MigrationOptions migrationOptions;
    private boolean directDownload;
    private String dataStoreUuid;
    private boolean deployAsIs;
    private String updatedDataStoreUUID;
    private String vSphereStoragePolicyId;

    public VolumeObjectTO() {

    }

    public VolumeObjectTO(VolumeInfo volume) {
        uuid = volume.getUuid();
        path = volume.getPath();
        accountId = volume.getAccountId();
        if (volume.getDataStore() != null) {
            dataStore = volume.getDataStore().getTO();
        } else {
            dataStore = null;
        }
        vmName = volume.getAttachedVmName();
        size = volume.getSize();
        setVolumeId(volume.getId());
        chainInfo = volume.getChainInfo();
        volumeType = volume.getVolumeType();
        name = volume.getName();
        setId(volume.getId());
        format = volume.getFormat();
        provisioningType = volume.getProvisioningType();
        poolId = volume.getPoolId();
        bytesReadRate = volume.getBytesReadRate();
        bytesReadRateMax = volume.getBytesReadRateMax();
        bytesReadRateMaxLength = volume.getBytesReadRateMaxLength();
        bytesWriteRate = volume.getBytesWriteRate();
        bytesWriteRateMax = volume.getBytesWriteRateMax();
        bytesWriteRateMaxLength = volume.getBytesWriteRateMaxLength();
        iopsReadRate = volume.getIopsReadRate();
        iopsReadRateMax = volume.getIopsReadRateMax();
        iopsReadRateMaxLength = volume.getIopsReadRateMaxLength();
        iopsWriteRate = volume.getIopsWriteRate();
        iopsWriteRateMax = volume.getIopsWriteRateMax();
        iopsWriteRateMaxLength = volume.getIopsWriteRateMaxLength();
        cacheMode = volume.getCacheMode();
        hypervisorType = volume.getHypervisorType();
        setDeviceId(volume.getDeviceId());
        this.migrationOptions = volume.getMigrationOptions();
        this.directDownload = volume.isDirectDownload();
        this.deployAsIs = volume.isDeployAsIs();
        this.vSphereStoragePolicyId = volume.getvSphereStoragePolicyId();
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getPath() {
        return path;
    }

    public Volume.Type getVolumeType() {
        return volumeType;
    }

    @Override
    public DataStoreTO getDataStore() {
        return dataStore;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setDataStore(DataStoreTO store) {
        dataStore = store;
    }

    public void setDataStore(PrimaryDataStoreTO dataStore) {
        this.dataStore = dataStore;
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
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

    public Storage.ProvisioningType getProvisioningType(){
        return provisioningType;
    }

    public void setProvisioningType(Storage.ProvisioningType provisioningType){
        this.provisioningType = provisioningType;
    }

    public Long getPoolId(){
        return poolId;
    }

    public void setPoolId(Long poolId){
        this.poolId = poolId;
    }

    @Override
    public String toString() {
        return new StringBuilder("volumeTO[uuid=").append(uuid).append("|path=").append(path).append("|datastore=").append(dataStore).append("]").toString();
    }

    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public Long getBytesReadRateMax() { return bytesReadRateMax; }

    public void setBytesReadRateMax(Long bytesReadRateMax) { this.bytesReadRateMax = bytesReadRateMax; }

    public Long getBytesReadRateMaxLength() { return bytesReadRateMaxLength; }

    public void setBytesReadRateMaxLength(Long bytesReadRateMaxLength) { this.bytesReadRateMaxLength = bytesReadRateMaxLength; }

    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public Long getBytesWriteRateMax() { return bytesWriteRateMax; }

    public void setBytesWriteRateMax(Long bytesWriteRateMax) { this.bytesWriteRateMax = bytesWriteRateMax; }

    public Long getBytesWriteRateMaxLength() { return bytesWriteRateMaxLength; }

    public void setBytesWriteRateMaxLength(Long bytesWriteRateMaxLength) { this.bytesWriteRateMaxLength = bytesWriteRateMaxLength; }

    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public Long getIopsReadRateMax() { return iopsReadRateMax; }

    public void setIopsReadRateMax(Long iopsReadRateMax) { this.iopsReadRateMax = iopsReadRateMax; }

    public Long getIopsReadRateMaxLength() { return iopsReadRateMaxLength; }

    public void setIopsReadRateMaxLength(Long iopsReadRateMaxLength) { this.iopsReadRateMaxLength = iopsReadRateMaxLength; }

    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public Long getIopsWriteRateMax() { return iopsWriteRateMax; }

    public void setIopsWriteRateMax(Long iopsWriteRateMax) { this.iopsWriteRateMax = iopsWriteRateMax; }

    public Long getIopsWriteRateMaxLength() { return iopsWriteRateMaxLength; }

    public void setIopsWriteRateMaxLength(Long iopsWriteRateMaxLength) { this.iopsWriteRateMaxLength = iopsWriteRateMaxLength; }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setCacheMode(DiskCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public DiskCacheMode getCacheMode() {
        return cacheMode;
    }

    public MigrationOptions getMigrationOptions() {
        return migrationOptions;
    }

    public boolean isDirectDownload() {
        return directDownload;
    }

    public String getDataStoreUuid() {
        return dataStoreUuid;
    }

    public void setDataStoreUuid(String dataStoreUuid) {
        this.dataStoreUuid = dataStoreUuid;
    }

    public boolean isDeployAsIs() {
        return deployAsIs;
    }

    public String getUpdatedDataStoreUUID() {
        return updatedDataStoreUUID;
    }

    public void setUpdatedDataStoreUUID(String updatedDataStoreUUID) {
        this.updatedDataStoreUUID = updatedDataStoreUUID;
    }

    public String getvSphereStoragePolicyId() {
        return vSphereStoragePolicyId;
    }

    public void setvSphereStoragePolicyId(String vSphereStoragePolicyId) {
        this.vSphereStoragePolicyId = vSphereStoragePolicyId;
    }
}
