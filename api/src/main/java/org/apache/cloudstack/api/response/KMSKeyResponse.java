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
public class KMSKeyResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the UUID of the key")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the key")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the key")
    private String description;

    @SerializedName(ApiConstants.PURPOSE)
    @Param(description = "the purpose of the key (VOLUME_ENCRYPTION, TLS_CERT)")
    private String purpose;

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

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "the KMS provider (database, pkcs11, etc.)")
    private String provider;

    @SerializedName(ApiConstants.ALGORITHM)
    @Param(description = "the encryption algorithm")
    private String algorithm;

    @SerializedName(ApiConstants.KEY_BITS)
    @Param(description = "the key size in bits")
    private Integer keyBits;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the key (Enabled, Disabled, Deleted)")
    private String state;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the creation timestamp")
    private Date created;

    // KEK label is admin-only for security
    @SerializedName(ApiConstants.KEK_LABEL)
    @Param(description = "the provider-specific KEK label (admin only)", authorized = {RoleType.Admin})
    private String kekLabel;

    // Getters and Setters

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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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
        // KMS keys are not project-scoped
    }

    @Override
    public void setProjectName(String projectName) {
        // KMS keys are not project-scoped
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
