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

package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;

import java.util.Map;

@EntityReference(value = VolumeOnStorageTO.class)
public class VolumeForImportResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the volume")
    private String name;

    @SerializedName(ApiConstants.PATH)
    @Param(description = "the path of the volume")
    private String path;

    @SerializedName(ApiConstants.FULL_PATH)
    @Param(description = "the full path of the volume")
    private String fullPath;

    @SerializedName(ApiConstants.FORMAT)
    @Param(description = "the format of the volume")
    private String format;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "the size of the volume")
    private long size;

    @SerializedName(ApiConstants.VIRTUAL_SIZE)
    @Param(description = "the virtual size of the volume")
    private long virtualSize;

    @SerializedName(ApiConstants.ENCRYPT_FORMAT)
    @Param(description = "the encrypt format of the volume")
    private String qemuEncryptFormat;

    @SerializedName(ApiConstants.STORAGE_ID)
    @Param(description = "id of the primary storage hosting the volume")
    private String storagePoolId;

    @SerializedName(ApiConstants.STORAGE)
    @Param(description = "name of the primary storage hosting the volume")
    private String storagePoolName;

    @SerializedName(ApiConstants.STORAGE_TYPE)
    @Param(description = "type of the primary storage hosting the volume")
    private String storagePoolType;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "volume details in key/value pairs.")
    private Map details;

    @SerializedName(ApiConstants.CHAIN_INFO)
    @Param(description = "the chain info of the volume")
    String chainInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public String getQemuEncryptFormat() {
        return qemuEncryptFormat;
    }

    public void setQemuEncryptFormat(String qemuEncryptFormat) {
        this.qemuEncryptFormat = qemuEncryptFormat;
    }

    public String getStoragePoolId() {
        return storagePoolId;
    }

    public void setStoragePoolId(String storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public String getStoragePoolName() {
        return storagePoolName;
    }

    public void setStoragePoolName(String storagePoolName) {
        this.storagePoolName = storagePoolName;
    }

    public String getStoragePoolType() {
        return storagePoolType;
    }

    public void setStoragePoolType(String storagePoolType) {
        this.storagePoolType = storagePoolType;
    }

    public Map getDetails() {
        return details;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    public String getChainInfo() {
        return chainInfo;
    }

    public void setChainInfo(String chainInfo) {
        this.chainInfo = chainInfo;
    }
}
