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

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.offering.DiskOffering;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DiskOffering.class)
public class DiskOfferingResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "unique ID of the disk offering")
    private String id;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID(s) this disk offering belongs to. Ignore this information as it is not currently applicable.")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name(s) this disk offering belongs to. Ignore this information as it is not currently applicable.")
    private String domain;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID(s) this disk offering belongs to. Ignore this information as it is not currently applicable.", since = "4.13.0")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE)
    @Param(description = "the zone name(s) this disk offering belongs to. Ignore this information as it is not currently applicable.", since = "4.13.0")
    private String zone;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the disk offering")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "an alternate display text of the disk offering.")
    private String displayText;

    @SerializedName(ApiConstants.DISK_SIZE)
    @Param(description = "the size of the disk offering in GB")
    private Long diskSize;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this disk offering was created")
    private Date created;

    @SerializedName("iscustomized")
    @Param(description = "true if disk offering uses custom size, false otherwise")
    private Boolean customized;

    @SerializedName("iscustomizediops")
    @Param(description = "true if disk offering uses custom iops, false otherwise")
    private Boolean customizedIops;

    @SerializedName(ApiConstants.MIN_IOPS)
    @Param(description = "the min iops of the disk offering")
    private Long minIops;

    @SerializedName(ApiConstants.MAX_IOPS)
    @Param(description = "the max iops of the disk offering")
    private Long maxIops;

    @SerializedName(ApiConstants.HYPERVISOR_SNAPSHOT_RESERVE)
    @Param(description = "Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)", since = "4.4")
    private Integer hypervisorSnapshotReserve;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the tags for the disk offering")
    private String tags;

    @SerializedName("storagetype")
    @Param(description = "the storage type for this disk offering")
    private String storageType;

    @SerializedName("provisioningtype") @Param(description="provisioning type used to create volumes. Valid values are thin, sparse, fat.", since = "4.4.0")
    private String provisioningType;

    @SerializedName("diskBytesReadRate")
    @Param(description = "bytes read rate of the disk offering")
    private Long bytesReadRate;

    @SerializedName("diskBytesReadRateMax")
    @Param(description = "burst bytes read rate of the disk offering")
    private Long bytesReadRateMax;

    @SerializedName("diskBytesReadRateMaxLength")
    @Param(description = "length (in seconds) of the burst")
    private Long bytesReadRateMaxLength;

    @SerializedName("diskBytesWriteRate")
    @Param(description = "bytes write rate of the disk offering")
    private Long bytesWriteRate;

    @SerializedName("diskBytesWriteRateMax")
    @Param(description = "burst bytes write rate of the disk offering")
    private Long bytesWriteRateMax;

    @SerializedName("diskBytesWriteRateMaxLength")
    @Param(description = "length (in seconds) of the burst")
    private Long bytesWriteRateMaxLength;

    @SerializedName("diskIopsReadRate")
    @Param(description = "io requests read rate of the disk offering")
    private Long iopsReadRate;

    @SerializedName("diskIopsReadRateMax")
    @Param(description = "burst io requests read rate of the disk offering")
    private Long iopsReadRateMax;

    @SerializedName("diskIopsReadRateMaxLength")
    @Param(description = "length (in second) of the burst")
    private Long iopsReadRateMaxLength;

    @SerializedName("diskIopsWriteRate")
    @Param(description = "io requests write rate of the disk offering")
    private Long iopsWriteRate;

    @SerializedName("diskIopsWriteRateMax")
    @Param(description = "burst io requests write rate of the disk offering")
    private Long iopsWriteRateMax;

    @SerializedName("diskIopsWriteRateMaxLength")
    @Param(description = "length (in seconds) of the burst")
    private Long iopsWriteRateMaxLength;

    @SerializedName("cacheMode")
    @Param(description = "the cache mode to use for this disk offering. none, writeback or writethrough", since = "4.4")
    private String cacheMode;

    @SerializedName("displayoffering")
    @Param(description = "whether to display the offering to the end user or not.")
    private Boolean displayOffering;

    @SerializedName("vspherestoragepolicy")
    @Param(description = "the vsphere storage policy tagged to the disk offering in case of VMware", since = "4.15")
    private String vsphereStoragePolicy;

    public Boolean getDisplayOffering() {
        return displayOffering;
    }

    public void setDisplayOffering(Boolean displayOffering) {
        this.displayOffering = displayOffering;
    }

    public String getId() {
        return id;

    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public Long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(Long diskSize) {
        this.diskSize = diskSize;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean isCustomized() {
        return customized;
    }

    public void setCustomized(Boolean customized) {
        this.customized = customized;
    }

    public Boolean isCustomizedIops() {
        return customizedIops;
    }

    public void setCustomizedIops(Boolean customizedIops) {
        this.customizedIops = customizedIops;
    }

    public Long getMinIops() {
        return minIops;
    }

    public void setMinIops(Long minIops) {
        this.minIops = minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public void setMaxIops(Long maxIops) {
        this.maxIops = maxIops;
    }

    public Integer getHypervisorSnapshotReserve() {
        return hypervisorSnapshotReserve;
    }

    public void setHypervisorSnapshotReserve(Integer hypervisorSnapshotReserve) {
        this.hypervisorSnapshotReserve = hypervisorSnapshotReserve;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getProvisioningType(){
        return provisioningType;
    }

    public void setProvisioningType(String provisioningType){
        this.provisioningType = provisioningType;
    }

    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
    }

    public void setBytesReadRateMax(Long bytesReadRateMax) {
        this.bytesReadRateMax = bytesReadRateMax;
    }

    public void setBytesReadRateMaxLength(Long bytesReadRateMaxLength) {
        this.bytesReadRateMaxLength = bytesReadRateMaxLength;
    }

    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
    }

    public void setBytesWriteRateMax(Long bytesWriteRateMax) {
        this.bytesWriteRateMax = bytesWriteRateMax;
    }

    public void setBytesWriteRateMaxLength(Long bytesWriteRateMaxLength) {
        this.bytesWriteRateMaxLength = bytesWriteRateMaxLength;
    }

    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
    }

    public void setIopsReadRateMax(Long iopsReadRateMax) {
        this.iopsReadRateMax = iopsReadRateMax;
    }

    public void setIopsReadRateMaxLength(Long iopsReadRateMaxLength) {
        this.iopsReadRateMaxLength = iopsReadRateMaxLength;
    }

    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
    }

    public void setIopsWriteRateMax(Long iopsWriteRateMax) {
        this.iopsWriteRateMax = iopsWriteRateMax;
    }

    public void setIopsWriteRateMaxLength(Long iopsWriteRateMaxLength) {
        this.iopsWriteRateMaxLength = iopsWriteRateMaxLength;
    }

    public String getVsphereStoragePolicy() {
        return vsphereStoragePolicy;
    }

    public void setVsphereStoragePolicy(String vsphereStoragePolicy) {
        this.vsphereStoragePolicy = vsphereStoragePolicy;
    }
}
