/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.kms.KMSKey;

import java.util.Date;

@EntityReference(value = KMSKey.class)
public class KMSKeyResponse extends BaseResponse implements ControlledViewEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the UUID of the key")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the key")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the key")
    private String description;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account owning the key")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the account ID owning the key")
    private String accountId;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the key")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the key")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "the domain path of the key")
    private String domainPath;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID where the key is valid")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the zone name where the key is valid")
    private String zoneName;

    @SerializedName(ApiConstants.HSM_PROFILE_ID)
    @Param(description = "the zone ID where the key is valid")
    private String hsmProfileId;

    @SerializedName(ApiConstants.HSM_PROFILE)
    @Param(description = "the zone name where the key is valid")
    private String hsmProfileName;

    @SerializedName(ApiConstants.ALGORITHM)
    @Param(description = "the encryption algorithm")
    private String algorithm;

    @SerializedName(ApiConstants.KEY_BITS)
    @Param(description = "the key size in bits")
    private Integer keyBits;

    @SerializedName(ApiConstants.VERSION)
    @Param(description = "the key size in bits")
    private Integer version;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "whether the key is enabled")
    private Boolean enabled;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the creation timestamp")
    private Date created;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project ID of the key")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the key")
    private String projectName;

    @SerializedName(ApiConstants.KEK_LABEL)
    @Param(description = "the provider-specific KEK label (admin only)", authorized = { RoleType.Admin })
    private String kekLabel;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getDomainId() {
        return domainId;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getHsmProfileId() {
        return hsmProfileId;
    }

    public void setHsmProfileId(String hsmProfileId) {
        this.hsmProfileId = hsmProfileId;
    }

    public String getHsmProfileName() {
        return hsmProfileName;
    }

    public void setHsmProfileName(String hsmProfileName) {
        this.hsmProfileName = hsmProfileName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getKeyBits() {
        return keyBits;
    }

    public void setKeyBits(Integer keyBits) {
        this.keyBits = keyBits;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getKekLabel() {
        return kekLabel;
    }

    public void setKekLabel(String kekLabel) {
        this.kekLabel = kekLabel;
    }
}
