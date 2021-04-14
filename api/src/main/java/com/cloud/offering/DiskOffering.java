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
package com.cloud.offering;

import java.util.Date;

import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.storage.Storage.ProvisioningType;

/**
 * Represents a disk offering that specifies what the end user needs in
 * the disk offering.
 *
 */
public interface DiskOffering extends InfrastructureEntity, Identity, InternalIdentity {
    enum State {
        Inactive, Active,
    }

    State getState();

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
    };

    String getUniqueName();

    boolean isUseLocalStorage();

    String getName();

    boolean isSystemUse();

    String getDisplayText();

    ProvisioningType getProvisioningType();

    String getTags();

    String[] getTagsArray();

    Date getCreated();

    boolean isCustomized();

    void setDiskSize(long diskSize);

    long getDiskSize();

    void setCustomizedIops(Boolean customizedIops);

    Boolean isCustomizedIops();

    void setMinIops(Long minIops);

    Long getMinIops();

    void setMaxIops(Long maxIops);

    Long getMaxIops();

    boolean isRecreatable();

    void setBytesReadRate(Long bytesReadRate);

    Long getBytesReadRate();

    void setBytesReadRateMax(Long bytesReadRateMax);

    Long getBytesReadRateMax();

    void setBytesReadRateMaxLength(Long bytesReadRateMaxLength);

    Long getBytesReadRateMaxLength();


    void setBytesWriteRate(Long bytesWriteRate);

    Long getBytesWriteRate();

    void setBytesWriteRateMax(Long bytesWriteMax);

    Long getBytesWriteRateMax();

    void setBytesWriteRateMaxLength(Long bytesWriteMaxLength);

    Long getBytesWriteRateMaxLength();


    void setIopsReadRate(Long iopsReadRate);

    Long getIopsReadRate();

    void setIopsReadRateMax(Long iopsReadRateMax);

    Long getIopsReadRateMax();

    void setIopsReadRateMaxLength(Long iopsReadRateMaxLength);

    Long getIopsReadRateMaxLength();

    void setIopsWriteRate(Long iopsWriteRate);

    Long getIopsWriteRate();

    void setIopsWriteRateMax(Long iopsWriteRateMax);

    Long getIopsWriteRateMax();


    void setIopsWriteRateMaxLength(Long iopsWriteRateMaxLength);

    Long getIopsWriteRateMaxLength();

    void setHypervisorSnapshotReserve(Integer hypervisorSnapshotReserve);

    Integer getHypervisorSnapshotReserve();

    DiskCacheMode getCacheMode();

    void setCacheMode(DiskCacheMode cacheMode);

    boolean isComputeOnly();
}
