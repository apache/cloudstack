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

import com.google.gson.annotations.SerializedName;

public class CloudStackServiceOffering {
    @SerializedName(ApiConstants.ID)
    private String id;
    @SerializedName(ApiConstants.CPU_NUMBER)
    private Long cpuNumber;
    @SerializedName(ApiConstants.CPU_SPEED)
    private Long cpuSpeed;
    @SerializedName(ApiConstants.CREATED)
    private String created;
    @SerializedName(ApiConstants.DEFAULT_USE)
    private Boolean defaultUse;
    @SerializedName(ApiConstants.DISPLAY_TEXT)
    private String displayText;
    @SerializedName(ApiConstants.DOMAIN)
    private String domain;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private String domainId;
    @SerializedName(ApiConstants.HOST_TAGS)
    private String hostTags;
    @SerializedName(ApiConstants.IS_SYSTEM)
    private Boolean isSystem;
    @SerializedName(ApiConstants.LIMIT_CPU_USE)
    private Boolean limitCpuUse;
    @SerializedName(ApiConstants.MEMORY)
    private Long memory;
    @SerializedName(ApiConstants.NAME)
    private String name;
    @SerializedName(ApiConstants.OFFER_HA)
    private Boolean offerHa;
    @SerializedName(ApiConstants.STORAGE_TYPE)
    private String storageType;
    @SerializedName(ApiConstants.SYSTEM_VM_TYPE)
    private String systemVmType;
    @SerializedName(ApiConstants.TAGS)
    private String tags;

    /**
     *
     */
    public CloudStackServiceOffering() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the cpuNumber
     */
    public Long getCpuNumber() {
        return cpuNumber;
    }

    /**
     * @return the cpuSpeed
     */
    public Long getCpuSpeed() {
        return cpuSpeed;
    }

    /**
     * @return the created
     */
    public String getCreated() {
        return created;
    }

    /**
     * @return the defaultUse
     */
    public Boolean getDefaultUse() {
        return defaultUse;
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

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    /**
     * @return the hostTags
     */
    public String getHostTags() {
        return hostTags;
    }

    /**
     * @return the isSystem
     */
    public Boolean getIsSystem() {
        return isSystem;
    }

    /**
     * @return the limitCpuUse
     */
    public Boolean getLimitCpuUse() {
        return limitCpuUse;
    }

    /**
     * @return the memory
     */
    public Long getMemory() {
        return memory;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the offerHa
     */
    public Boolean getOfferHa() {
        return offerHa;
    }

    /**
     * @return the storageType
     */
    public String getStorageType() {
        return storageType;
    }

    /**
     * @return the systemVmType
     */
    public String getSystemVmType() {
        return systemVmType;
    }

    /**
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

}
