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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.storage.object.ObjectStore;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = ObjectStore.class)
public class ObjectStoreResponse extends BaseResponseWithAnnotations {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the object store")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the object store")
    private String name;

    @SerializedName(ApiConstants.URL)
    @Param(description = "the url of the object store")
    private String url;

    @SerializedName("providername")
    @Param(description = "the provider name of the object store")
    private String providerName;

    @SerializedName("storagetotal")
    @Param(description = "the total size of the object store")
    private Long storageTotal;

    @SerializedName("storageallocated")
    @Param(description = "the allocated size of the object store")
    private Long storageAllocated;

    @SerializedName("perbucketcredentialsready")
    @Param(description = "whether this store can provide per-bucket credentials right now; root admin only", since = "24.0.0")
    private Boolean perBucketCredentialsReady;

    @SerializedName("perbucketcredentialsissue")
    @Param(description = "what has to be resolved before this store can provide per-bucket credentials, absent when it can; root admin only", since = "24.0.0")
    private String perBucketCredentialsIssue;

    @SerializedName("perbucketcredentialssupported")
    @Param(description = "only with accountid: whether this store supports per-bucket credentials at all; when false the account cannot be migrated on it", since = "24.0.0")
    private Boolean perBucketCredentialsSupported;

    @SerializedName("accountcredentialscope")
    @Param(description = "only with accountid: 'bucket' when the account has been migrated for per-bucket credentials on this store, 'account' otherwise", since = "24.0.0")
    private String accountCredentialScope;

    @SerializedName("accountkeyrotationpending")
    @Param(description = "only with accountid: true when the account was migrated from a pre-existing identity and its original key has not been rotated yet", since = "24.0.0")
    private Boolean accountKeyRotationPending;

    @SerializedName("legacybuckets")
    @Param(description = "only with accountid: number of the account's buckets on this store that still use the account-level key", since = "24.0.0")
    private Long legacyBuckets;

    @SerializedName("storageused")
    @Param(description = "the object store currently used size")
    private Long storageUsed;

    public ObjectStoreResponse() {
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Long getStorageTotal() {
        return storageTotal;
    }

    public void setStorageTotal(Long storageTotal) {
        this.storageTotal = storageTotal;
    }

    public Long getStorageAllocated() {
        return storageAllocated;
    }

    public void setStorageAllocated(Long storageAllocated) {
        this.storageAllocated = storageAllocated;
    }

    public Long getStorageUsed() {
        return storageUsed;
    }

    public void setStorageUsed(Long storageUsed) {
        this.storageUsed = storageUsed;
    }

    public void setPerBucketCredentialsReady(Boolean perBucketCredentialsReady) {
        this.perBucketCredentialsReady = perBucketCredentialsReady;
    }

    public void setPerBucketCredentialsIssue(String perBucketCredentialsIssue) {
        this.perBucketCredentialsIssue = perBucketCredentialsIssue;
    }

    public void setPerBucketCredentialsSupported(Boolean perBucketCredentialsSupported) {
        this.perBucketCredentialsSupported = perBucketCredentialsSupported;
    }

    public void setAccountCredentialScope(String accountCredentialScope) {
        this.accountCredentialScope = accountCredentialScope;
    }

    public void setLegacyBuckets(Long legacyBuckets) {
        this.legacyBuckets = legacyBuckets;
    }

    public void setAccountKeyRotationPending(Boolean accountKeyRotationPending) {
        this.accountKeyRotationPending = accountKeyRotationPending;
    }
}
