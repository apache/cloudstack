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

package org.apache.cloudstack.veeam.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "disk")
public final class Disk extends BaseDto {

    private String bootable;
    private String actualSize;
    private String alias;
    private String backup;
    private String contentType;
    private String format;
    private String imageId;
    private String propagateErrors;
    private String initialSize;
    private String provisionedSize;
    private String qcowVersion;
    private String shareable;
    private String sparse;
    private String status;
    private String storageType;
    private String totalSize;
    private String wipeAfterDelete;
    private Ref diskProfile;
    private Ref quota;
    private StorageDomains storageDomains;
    private Actions actions;
    private String name;
    private String description;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;

    public String getBootable() {
        return bootable;
    }

    public void setBootable(String bootable) {
        this.bootable = bootable;
    }

    public String getActualSize() {
        return actualSize;
    }

    public void setActualSize(String actualSize) {
        this.actualSize = actualSize;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getBackup() {
        return backup;
    }

    public void setBackup(String backup) {
        this.backup = backup;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getPropagateErrors() {
        return propagateErrors;
    }

    public void setPropagateErrors(String propagateErrors) {
        this.propagateErrors = propagateErrors;
    }

    public String getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(String initialSize) {
        this.initialSize = initialSize;
    }

    public String getProvisionedSize() {
        return provisionedSize;
    }

    public void setProvisionedSize(String provisionedSize) {
        this.provisionedSize = provisionedSize;
    }

    public String getQcowVersion() {
        return qcowVersion;
    }

    public void setQcowVersion(String qcowVersion) {
        this.qcowVersion = qcowVersion;
    }

    public String getShareable() {
        return shareable;
    }

    public void setShareable(String shareable) {
        this.shareable = shareable;
    }

    public String getSparse() {
        return sparse;
    }

    public void setSparse(String sparse) {
        this.sparse = sparse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }

    public String getWipeAfterDelete() {
        return wipeAfterDelete;
    }

    public void setWipeAfterDelete(String wipeAfterDelete) {
        this.wipeAfterDelete = wipeAfterDelete;
    }

    public Ref getDiskProfile() {
        return diskProfile;
    }

    public void setDiskProfile(Ref diskProfile) {
        this.diskProfile = diskProfile;
    }

    public Ref getQuota() {
        return quota;
    }

    public void setQuota(Ref quota) {
        this.quota = quota;
    }

    public StorageDomains getStorageDomains() {
        return storageDomains;
    }

    public void setStorageDomains(StorageDomains storageDomains) {
        this.storageDomains = storageDomains;
    }

    public Actions getActions() {
        return actions;
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }
}
