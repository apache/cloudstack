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
import com.cloud.storage.Bucket;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@EntityReference(value = Bucket.class)
@SuppressWarnings("unused")
public class BucketResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse {
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
    @SerializedName(ApiConstants.OBJECT_STORAGE_ID)
    @Param(description = "id of the object storage hosting the Bucket; returned to admin user only")
    private String objectStoragePoolId;

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
}