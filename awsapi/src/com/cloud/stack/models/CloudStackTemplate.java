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

package com.cloud.stack.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CloudStackTemplate {
    @SerializedName(ApiConstants.ID)
    private String id;
    @SerializedName(ApiConstants.ACCOUNT)
    private String account;
    @SerializedName(ApiConstants.ACCOUNT_ID)
    private String accountId;
    @SerializedName(ApiConstants.BOOTABLE)
    private Boolean bootable;
    @SerializedName(ApiConstants.CHECKSUM)
    private String checksum;
    @SerializedName(ApiConstants.CREATED)
    private String created;
    @SerializedName(ApiConstants.CROSS_ZONES)
    private Boolean crossZones;
    @SerializedName(ApiConstants.DISPLAY_TEXT)
    private String displayText;
    @SerializedName(ApiConstants.DOMAIN)
    private String domain;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private String domainId;
    @SerializedName(ApiConstants.FORMAT)
    private String format;
    @SerializedName(ApiConstants.HOST_ID)
    private String hostId;
    @SerializedName(ApiConstants.HOST_NAME)
    private String hostName;
    @SerializedName(ApiConstants.HYPERVISOR)
    private String hyperVisor;
    @SerializedName(ApiConstants.IS_EXTRACTABLE)
    private Boolean isExtractable;
    @SerializedName(ApiConstants.IS_FEATURED)
    private Boolean isFeatured;
    @SerializedName(ApiConstants.IS_PUBLIC)
    private Boolean isPublic;
    @SerializedName(ApiConstants.IS_READY)
    private Boolean isReady;
    @SerializedName(ApiConstants.JOB_ID)
    private String jobId;
    @SerializedName(ApiConstants.JOB_STATUS)
    private String jobStatus;
    @SerializedName(ApiConstants.NAME)
    private String name;
    @SerializedName(ApiConstants.OS_TYPE_ID)
    private String osTypeId;
    @SerializedName(ApiConstants.OS_TYPE_NAME)
    private String osTypeName;
    @SerializedName(ApiConstants.PASSWORD_ENABLED)
    private Boolean passwordEnabled;
    @SerializedName(ApiConstants.REMOVED)
    private String removedDate;
    @SerializedName(ApiConstants.SIZE)
    private Long size;
    @SerializedName(ApiConstants.SOURCE_TEMPLATE_ID)
    private String sourceTemplateId;
    @SerializedName(ApiConstants.STATUS)
    private String status;
    @SerializedName(ApiConstants.TEMPLATE_TYPE)
    private String templateType;
    @SerializedName(ApiConstants.ZONE_ID)
    private String zoneId;
    @SerializedName(ApiConstants.ZONE_NAME)
    private String zoneName;
    @SerializedName(ApiConstants.TAGS)
    private List<CloudStackKeyValue> tags;

    /**
     *
     */
    public CloudStackTemplate() {
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @return the accountId
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * @return the bootable
     */
    public Boolean getBootable() {
        return bootable;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @return the created
     */
    public String getCreated() {
        return created;
    }

    /**
     * @return the crossZones
     */
    public Boolean getCrossZones() {
        return crossZones;
    }

    /**
     * @return the displayText
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return the domainId
     */
    public String getDomainId() {
        return domainId;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @return the hostId
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @return the hyperVisor
     */
    public String getHyperVisor() {
        return hyperVisor;
    }

    /**
     * @return the isExtractable
     */
    public Boolean getIsExtractable() {
        return isExtractable;
    }

    /**
     * @return the isFeatured
     */
    public Boolean getIsFeatured() {
        return isFeatured;
    }

    /**
     * @return the isPublic
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * @return the isReady
     */
    public Boolean getIsReady() {
        return isReady;
    }

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return the jobStatus
     */
    public String getJobStatus() {
        return jobStatus;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the osTypeId
     */
    public String getOsTypeId() {
        return osTypeId;
    }

    /**
     * @return the osTypeName
     */
    public String getOsTypeName() {
        return osTypeName;
    }

    /**
     * @return the passwordEnabled
     */
    public Boolean getPasswordEnabled() {
        return passwordEnabled;
    }

    /**
     * @return the removedDate
     */
    public String getRemovedDate() {
        return removedDate;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @return the sourceTemplateId
     */
    public String getSourceTemplateId() {
        return sourceTemplateId;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return the templateType
     */
    public String getTemplateType() {
        return templateType;
    }

    /**
     * @return the zoneId
     */
    public String getZoneId() {
        return zoneId;
    }

    /**
     * @return the zoneName
     */
    public String getZoneName() {
        return zoneName;
    }

    /**
     * @return all tags
     */
    public List<CloudStackKeyValue> getTags() {
        return tags;
    }
}
