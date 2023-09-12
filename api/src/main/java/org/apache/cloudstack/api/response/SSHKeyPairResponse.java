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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.serializer.Param;
import com.cloud.user.SSHKeyPair;

import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = SSHKeyPair.class)
public class SSHKeyPairResponse extends BaseResponseWithAnnotations {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the ssh keypair")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the keypair")
    private String name;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the owner of the keypair")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the keypair owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the keypair owner")
    private String domain;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the keypair owner")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the keypair owner")
    private String projectName;

    @SerializedName("fingerprint")
    @Param(description = "Fingerprint of the public key")
    private String fingerprint;

    public SSHKeyPairResponse() {
    }

    public SSHKeyPairResponse(String uuid, String name, String fingerprint) {
        this.id = uuid;
        this.name = name;
        this.fingerprint = fingerprint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domain;
    }

    public void setDomainName(String domain) {
        this.domain = domain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
