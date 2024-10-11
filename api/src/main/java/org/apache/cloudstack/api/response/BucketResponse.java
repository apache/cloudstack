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
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.storage.object.Bucket;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@EntityReference(value = Bucket.class)
@SuppressWarnings("unused")
public class BucketResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse, ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the Bucket")
    private String id;
    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the Bucket")
    private String name;
    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the Bucket was created")
    private Date created;
    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the Bucket")
    private String accountName;
    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the bucket")
    private String projectId;
    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the bucket")
    private String projectName;
    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the bucket")
    private String domainId;
    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the bucket")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the bucket belongs", since = "4.19.2.0")
    private String domainPath;
    @SerializedName(ApiConstants.OBJECT_STORAGE_ID)
    @Param(description = "id of the object storage hosting the Bucket; returned to admin user only")
    private String objectStoragePoolId;

    @SerializedName(ApiConstants.OBJECT_STORAGE)
    @Param(description = "Name of the object storage hosting the Bucket; returned to admin user only")
    private String objectStoragePool;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "Total size of objects in Bucket")
    private Long size;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "State of the Bucket")
    private String state;

    @SerializedName(ApiConstants.QUOTA)
    @Param(description = "Bucket Quota in GB")
    private Integer quota;

    @SerializedName(ApiConstants.ENCRYPTION)
    @Param(description = "Bucket Encryption")
    private Boolean encryption;

    @SerializedName(ApiConstants.VERSIONING)
    @Param(description = "Bucket Versioning")
    private Boolean versioning;

    @SerializedName(ApiConstants.OBJECT_LOCKING)
    @Param(description = "Bucket Object Locking")
    private Boolean objectLock;

    @SerializedName(ApiConstants.POLICY)
    @Param(description = "Bucket Access Policy")
    private String policy;

    @SerializedName(ApiConstants.URL)
    @Param(description = "Bucket URL")
    private String bucketURL;

    @SerializedName(ApiConstants.ACCESS_KEY)
    @Param(description = "Bucket Access Key")
    private String accessKey;

    @SerializedName(ApiConstants.USER_SECRET_KEY)
    @Param(description = "Bucket Secret Key")
    private String secretKey;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "Object storage provider")
    private String provider;

    public BucketResponse() {
        tags = new LinkedHashSet<ResourceTagResponse>();
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

    public void setName(String name) {
        this.name = name;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void setObjectStoragePoolId(String objectStoragePoolId) {
        this.objectStoragePoolId = objectStoragePoolId;
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getObjectStoragePoolId() {
        return objectStoragePoolId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
    public long getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public boolean isVersioning() {
        return versioning;
    }

    public void setVersioning(boolean versioning) {
        this.versioning = versioning;
    }

    public boolean isEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    public boolean isObjectLock() {
        return objectLock;
    }

    public void setObjectLock(boolean objectLock) {
        this.objectLock = objectLock;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getBucketURL() {
        return bucketURL;
    }

    public void setBucketURL(String bucketURL) {
        this.bucketURL = bucketURL;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setState(Bucket.State state) {
        this.state = state.toString();
    }

    public String getState() {
        return state;
    }

    public void setObjectStoragePool(String objectStoragePool) {
        this.objectStoragePool = objectStoragePool;
    }

    public String getObjectStoragePool() {
        return objectStoragePool;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
