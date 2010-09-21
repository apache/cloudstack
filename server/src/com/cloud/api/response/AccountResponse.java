/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AccountResponse extends BaseResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("accounttype")
    private Short accountType;

    @SerializedName("domainid")
    private Long domainId;

    @SerializedName("domain")
    private String domainName;

    @SerializedName("receivedbytes")
    private Long bytesReceived;

    @SerializedName("sentbytes")
    private Long bytesSent;

    @SerializedName("vmlimit")
    private String vmLimit;

    @SerializedName("vmtotal")
    private Long vmTotal;

    @SerializedName("vmavailable")
    private String vmAvailable;

    @SerializedName("iplimit")
    private String ipLimit;

    @SerializedName("iptotal")
    private Long ipTotal;

    @SerializedName("ipavailable")
    private String ipAvailable;

    @SerializedName("volumelimit")
    private String volumeLimit;

    @SerializedName("volumetotal")
    private Long volumeTotal;

    @SerializedName("volumeavailable")
    private String volumeAvailable;

    @SerializedName("snapshotlimit")
    private String snapshotLimit;

    @SerializedName("snapshottotal")
    private Long snapshotTotal;

    @SerializedName("snapshotavailable")
    private String snapshotAvailable;

    @SerializedName("templatelimit")
    private String templateLimit;

    @SerializedName("templatetotal")
    private Long templateTotal;

    @SerializedName("templateavailable")
    private String templateAvailable;

    @SerializedName("vmstopped")
    private Integer vmStopped;

    @SerializedName("vmrunning")
    private Integer vmRunning;

    @SerializedName("state")
    private String state;

    @SerializedName("iscleanuprequired")
    private Boolean cleanupRequired;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getAccountType() {
        return accountType;
    }

    public void setAccountType(Short accountType) {
        this.accountType = accountType;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public Long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public String getVmLimit() {
        return vmLimit;
    }

    public void setVmLimit(String vmLimit) {
        this.vmLimit = vmLimit;
    }

    public Long getVmTotal() {
        return vmTotal;
    }

    public void setVmTotal(Long vmTotal) {
        this.vmTotal = vmTotal;
    }

    public String getVmAvailable() {
        return vmAvailable;
    }

    public void setVmAvailable(String vmAvailable) {
        this.vmAvailable = vmAvailable;
    }

    public String getIpLimit() {
        return ipLimit;
    }

    public void setIpLimit(String ipLimit) {
        this.ipLimit = ipLimit;
    }

    public Long getIpTotal() {
        return ipTotal;
    }

    public void setIpTotal(Long ipTotal) {
        this.ipTotal = ipTotal;
    }

    public String getIpAvailable() {
        return ipAvailable;
    }

    public void setIpAvailable(String ipAvailable) {
        this.ipAvailable = ipAvailable;
    }

    public String getVolumeLimit() {
        return volumeLimit;
    }

    public void setVolumeLimit(String volumeLimit) {
        this.volumeLimit = volumeLimit;
    }

    public Long getVolumeTotal() {
        return volumeTotal;
    }

    public void setVolumeTotal(Long volumeTotal) {
        this.volumeTotal = volumeTotal;
    }

    public String getVolumeAvailable() {
        return volumeAvailable;
    }

    public void setVolumeAvailable(String volumeAvailable) {
        this.volumeAvailable = volumeAvailable;
    }

    public String getSnapshotLimit() {
        return snapshotLimit;
    }

    public void setSnapshotLimit(String snapshotLimit) {
        this.snapshotLimit = snapshotLimit;
    }

    public Long getSnapshotTotal() {
        return snapshotTotal;
    }

    public void setSnapshotTotal(Long snapshotTotal) {
        this.snapshotTotal = snapshotTotal;
    }

    public String getSnapshotAvailable() {
        return snapshotAvailable;
    }

    public void setSnapshotAvailable(String snapshotAvailable) {
        this.snapshotAvailable = snapshotAvailable;
    }

    public String getTemplateLimit() {
        return templateLimit;
    }

    public void setTemplateLimit(String templateLimit) {
        this.templateLimit = templateLimit;
    }

    public Long getTemplateTotal() {
        return templateTotal;
    }

    public void setTemplateTotal(Long templateTotal) {
        this.templateTotal = templateTotal;
    }

    public String getTemplateAvailable() {
        return templateAvailable;
    }

    public void setTemplateAvailable(String templateAvailable) {
        this.templateAvailable = templateAvailable;
    }

    public Integer getVmStopped() {
        return vmStopped;
    }

    public void setVmStopped(Integer vmStopped) {
        this.vmStopped = vmStopped;
    }

    public Integer getVmRunning() {
        return vmRunning;
    }

    public void setVmRunning(Integer vmRunning) {
        this.vmRunning = vmRunning;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getCleanupRequired() {
        return cleanupRequired;
    }

    public void setCleanupRequired(Boolean cleanupRequired) {
        this.cleanupRequired = cleanupRequired;
    }
}
