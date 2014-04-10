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
package com.cloud.vm;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Volume;

/**
 * DiskProfile describes a disk and what functionality is required from it.
 * and resources to allocate and create disks. There object is immutable once
 */
public class DiskProfile {
    private long size;
    private String[] tags;
    private Volume.Type type;
    private String name;
    private boolean useLocalStorage;
    private boolean recreatable;
    private long diskOfferingId;
    private Long templateId;
    private long volumeId;
    private String path;
    private ProvisioningType provisioningType;
    private Long bytesReadRate;
    private Long bytesWriteRate;
    private Long iopsReadRate;
    private Long iopsWriteRate;
    private String cacheMode;

    private HypervisorType hyperType;

    protected DiskProfile() {
    }

    public DiskProfile(long volumeId, Volume.Type type, String name, long diskOfferingId, long size, String[] tags, boolean useLocalStorage, boolean recreatable,
            Long templateId) {
        this.type = type;
        this.name = name;
        this.size = size;
        this.tags = tags;
        this.useLocalStorage = useLocalStorage;
        this.recreatable = recreatable;
        this.diskOfferingId = diskOfferingId;
        this.templateId = templateId;
        this.volumeId = volumeId;
    }

    public DiskProfile(Volume vol, DiskOffering offering, HypervisorType hyperType) {
        this(vol.getId(),
            vol.getVolumeType(),
            vol.getName(),
            offering.getId(),
            vol.getSize(),
            offering.getTagsArray(),
            offering.getUseLocalStorage(),
            offering.isCustomized(),
            null);
        this.hyperType = hyperType;
    }

    public DiskProfile(DiskProfile dp) {

    }

    /**
     * @return size of the disk requested in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * @return id of the volume backing up this disk characteristics
     */
    public long getVolumeId() {
        return volumeId;
    }

    /**
     * @return Unique name for the disk.
     */
    public String getName() {
        return name;
    }

    /**
     * @return tags for the disk. This can be used to match it to different storage pools.
     */
    public String[] getTags() {
        return tags;
    }

    /**
     * @return type of volume.
     */
    public Volume.Type getType() {
        return type;
    }

    /**
     * @return Does this volume require local storage?
     */
    public boolean useLocalStorage() {
        return useLocalStorage;
    }

    public void setUseLocalStorage(boolean useLocalStorage) {
        this.useLocalStorage = useLocalStorage;
    }

    /**
     * @return Is this volume recreatable? A volume is recreatable if the disk's content can be
     *         reconstructed from the template.
     */
    public boolean isRecreatable() {
        return recreatable;
    }

    /**
     * @return template id the disk is based on. Can be null if it is not based on any templates.
     */
    public Long getTemplateId() {
        return templateId;
    }

    /**
     * @return disk offering id that the disk is based on.
     */
    public long getDiskOfferingId() {
        return diskOfferingId;
    }

    @Override
    public String toString() {
        return new StringBuilder("DskChr[").append(type).append("|").append(size).append("|").append("]").toString();
    }

    public void setHyperType(HypervisorType hyperType) {
        this.hyperType = hyperType;
    }

    public HypervisorType getHypervisorType() {
        return this.hyperType;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setProvisioningType(ProvisioningType provisioningType){
        this.provisioningType = provisioningType;
    }

    public ProvisioningType getProvisioningType(){
        return this.provisioningType;
    }

    public void setSize(long size) {
        this.size = size;
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

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    public String getCacheMode() {
        return cacheMode;
    }
}
