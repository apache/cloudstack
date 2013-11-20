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
package com.cloud.bridge.service.core.s3;

import java.util.Calendar;

import com.cloud.bridge.util.XElement;

public class S3ListBucketObjectEntry {

    @XElement(name = "Key")
    protected String key;

    protected String version;
    protected boolean isLatest;
    protected boolean isDeletionMarker;

    @XElement(name = "LastModified")
    protected Calendar lastModified;

    @XElement(name = "ETag")
    protected String ETag;

    @XElement(name = "Size")
    protected long size;

    @XElement(name = "Owner")
    protected S3CanonicalUser owner = new S3CanonicalUser();

    @XElement(name = "StorageClass")
    protected String storageClass;

    public S3ListBucketObjectEntry() {
        isLatest = false;
        isDeletionMarker = false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(boolean isLatest) {
        this.isLatest = isLatest;
    }

    public boolean getIsDeletionMarker() {
        return isDeletionMarker;
    }

    public void setIsDeletionMarker(boolean isDeletionMarker) {
        this.isDeletionMarker = isDeletionMarker;
    }

    public String getETag() {
        return ETag;
    }

    public void setETag(String eTag) {
        ETag = eTag;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(Calendar lastModified) {
        this.lastModified = lastModified;
    }

    public String getOwnerCanonicalId() {
        return owner.getID();
    }

    public void setOwnerCanonicalId(String ownerCanonicalId) {
        owner.setID(ownerCanonicalId);
    }

    public String getOwnerDisplayName() {
        return owner.getDisplayName();
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        owner.setDisplayName(ownerDisplayName);
    }

    public S3CanonicalUser getOwner() {
        return owner;
    }

    public void setOwner(S3CanonicalUser owner) {
        this.owner = owner;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }
}
