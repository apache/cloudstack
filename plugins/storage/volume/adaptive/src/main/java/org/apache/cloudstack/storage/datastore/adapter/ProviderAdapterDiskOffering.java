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
package org.apache.cloudstack.storage.datastore.adapter;

import java.util.Date;
import org.apache.commons.lang.NotImplementedException;
import com.cloud.offering.DiskOffering;

/**
 * Wrapper Disk Offering that masks the cloudstack-dependent classes from the storage provider code
 */
public class ProviderAdapterDiskOffering {
    private ProvisioningType type;
    private DiskCacheMode diskCacheMode;
    private DiskOffering hiddenDiskOffering;
    private State state;
    public ProviderAdapterDiskOffering(DiskOffering hiddenDiskOffering) {
        this.hiddenDiskOffering = hiddenDiskOffering;
        if (hiddenDiskOffering.getProvisioningType() != null) {
            this.type = ProvisioningType.getProvisioningType(hiddenDiskOffering.getProvisioningType().toString());
        }
        if (hiddenDiskOffering.getCacheMode() != null) {
            this.diskCacheMode = DiskCacheMode.getDiskCasehMode(hiddenDiskOffering.getCacheMode().toString());
        }
        if (hiddenDiskOffering.getState() != null) {
            this.state = State.valueOf(hiddenDiskOffering.getState().toString());
        }
    }
    public Long getBytesReadRate() {
        return hiddenDiskOffering.getBytesReadRate();
    }
    public Long getBytesReadRateMax() {
        return hiddenDiskOffering.getBytesReadRateMax();
    }
    public Long getBytesReadRateMaxLength() {
        return hiddenDiskOffering.getBytesReadRateMaxLength();
    }
    public Long getBytesWriteRate() {
        return hiddenDiskOffering.getBytesWriteRate();
    }
    public Long getBytesWriteRateMax() {
        return hiddenDiskOffering.getBytesWriteRateMax();
    }
    public Long getBytesWriteRateMaxLength() {
        return hiddenDiskOffering.getBytesWriteRateMaxLength();
    }
    public DiskCacheMode getCacheMode() {
        return diskCacheMode;
    }
    public Date getCreated() {
        return hiddenDiskOffering.getCreated();
    }
    public long getDiskSize() {
        return hiddenDiskOffering.getDiskSize();
    }
    public boolean getDiskSizeStrictness() {
        return hiddenDiskOffering.getDiskSizeStrictness();
    }
    public String getDisplayText() {
        return hiddenDiskOffering.getDisplayText();
    }
    public boolean getEncrypt() {
        return hiddenDiskOffering.getEncrypt();
    }
    public Integer getHypervisorSnapshotReserve() {
        return hiddenDiskOffering.getHypervisorSnapshotReserve();
    }
    public long getId() {
        return hiddenDiskOffering.getId();
    }
    public Long getIopsReadRate() {
        return hiddenDiskOffering.getIopsReadRate();
    }
    public Long getIopsReadRateMax() {
        return hiddenDiskOffering.getIopsReadRateMax();
    }
    public Long getIopsReadRateMaxLength() {
        return hiddenDiskOffering.getIopsReadRateMaxLength();
    }
    public Long getIopsWriteRate() {
        return hiddenDiskOffering.getIopsWriteRate();
    }
    public Long getIopsWriteRateMax() {
        return hiddenDiskOffering.getIopsWriteRateMax();
    }
    public Long getIopsWriteRateMaxLength() {
        return hiddenDiskOffering.getIopsWriteRateMaxLength();
    }
    public Long getMaxIops() {
        return hiddenDiskOffering.getMaxIops();
    }
    public Long getMinIops() {
        return hiddenDiskOffering.getMinIops();
    }
    public String getName() {
        return hiddenDiskOffering.getName();
    }
    public State getState() {
        return state;
    }
    public String getTags() {
        return hiddenDiskOffering.getTags();
    }
    public String[] getTagsArray() {
        return hiddenDiskOffering.getTagsArray();
    }
    public String getUniqueName() {
        return hiddenDiskOffering.getUniqueName();
    }
    public String getUuid() {
        return hiddenDiskOffering.getUuid();
    }
    public ProvisioningType getType() {
        return type;
    }
    public void setType(ProvisioningType type) {
        this.type = type;
    }

    public static enum ProvisioningType {
        THIN("thin"),
        SPARSE("sparse"),
        FAT("fat");

        private final String provisionType;

        private ProvisioningType(String provisionType){
            this.provisionType = provisionType;
        }

        public String toString(){
            return this.provisionType;
        }

        public static ProvisioningType getProvisioningType(String provisioningType){

            if(provisioningType.equals(THIN.provisionType)){
                return ProvisioningType.THIN;
            } else if(provisioningType.equals(SPARSE.provisionType)){
                return ProvisioningType.SPARSE;
            } else if (provisioningType.equals(FAT.provisionType)){
                return ProvisioningType.FAT;
            } else {
                throw new NotImplementedException("Invalid provisioning type specified: " + provisioningType);
            }
        }
    }


    enum State {
        Inactive, Active,
    }

    enum DiskCacheMode {
        NONE("none"), WRITEBACK("writeback"), WRITETHROUGH("writethrough");

        private final String _diskCacheMode;

        DiskCacheMode(String cacheMode) {
            _diskCacheMode = cacheMode;
        }

        @Override
        public String toString() {
            return _diskCacheMode;
        }

        public static DiskCacheMode getDiskCasehMode(String cacheMode) {
            if (cacheMode.equals(NONE._diskCacheMode)) {
                return NONE;
            } else if (cacheMode.equals(WRITEBACK._diskCacheMode)) {
                return WRITEBACK;
            } else if (cacheMode.equals(WRITETHROUGH._diskCacheMode)) {
                return WRITETHROUGH;
            } else {
                throw new NotImplementedException("Invalid cache mode specified: " + cacheMode);
            }
        }
    };
}
