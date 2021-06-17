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

package org.apache.cloudstack.storage.datastore.api;

import java.util.Arrays;
import java.util.List;

public class Volume {
    public enum VolumeType {
        ThickProvisioned,
        ThinProvisioned,
        Snapshot
    }
    String id;
    String name;
    String ancestorVolumeId;
    String consistencyGroupId;
    Long creationTime;
    Long sizeInKb;
    String sizeInGB;
    String storagePoolId;
    VolumeType volumeType;
    String volumeSizeInGb;
    String vtreeId;
    SdcMappingInfo[] mappedSdcInfo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAncestorVolumeId() {
        return ancestorVolumeId;
    }

    public void setAncestorVolumeId(String ancestorVolumeId) {
        this.ancestorVolumeId = ancestorVolumeId;
    }

    public String getConsistencyGroupId() {
        return consistencyGroupId;
    }

    public void setConsistencyGroupId(String consistencyGroupId) {
        this.consistencyGroupId = consistencyGroupId;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Long getSizeInKb() {
        return sizeInKb;
    }

    public void setSizeInKb(Long sizeInKb) {
        this.sizeInKb = sizeInKb;
    }

    public String getSizeInGB() {
        return sizeInGB;
    }

    public void setSizeInGB(Integer sizeInGB) {
        this.sizeInGB = sizeInGB.toString();
    }

    public void setVolumeSizeInGb(String volumeSizeInGb) {
        this.volumeSizeInGb = volumeSizeInGb;
    }

    public String getStoragePoolId() {
        return storagePoolId;
    }

    public void setStoragePoolId(String storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public String getVolumeSizeInGb() {
        return volumeSizeInGb;
    }

    public void setVolumeSizeInGb(Integer volumeSizeInGb) {
        this.volumeSizeInGb = volumeSizeInGb.toString();
    }

    public VolumeType getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = Enum.valueOf(VolumeType.class, volumeType);
    }

    public void setVolumeType(VolumeType volumeType) {
        this.volumeType = volumeType;
    }

    public String getVtreeId() {
        return vtreeId;
    }

    public void setVtreeId(String vtreeId) {
        this.vtreeId = vtreeId;
    }

    public List<SdcMappingInfo> getMappedSdcList() {
        if (mappedSdcInfo != null) {
            return Arrays.asList(mappedSdcInfo);
        }
        return null;
    }

    public SdcMappingInfo[] getMappedSdcInfo() {
        return mappedSdcInfo;
    }

    public void setMappedSdcInfo(SdcMappingInfo[] mappedSdcInfo) {
        this.mappedSdcInfo = mappedSdcInfo;
    }
}
