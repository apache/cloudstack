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

import java.util.List;
import java.util.Map;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AccountResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the account")
    private IdentityProxy id = new IdentityProxy("account");

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the account")
    private String name;

    @SerializedName(ApiConstants.ACCOUNT_TYPE) @Param(description="account type (admin, domain-admin, user)")
    private Short accountType;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="id of the Domain the account belongs too")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN) @Param(description="name of the Domain the account belongs too")
    private String domainName;

    @SerializedName(ApiConstants.RECEIVED_BYTES) @Param(description="the total number of network traffic bytes received")
    private Long bytesReceived;

    @SerializedName(ApiConstants.SENT_BYTES) @Param(description="the total number of network traffic bytes sent")
    private Long bytesSent;

    @SerializedName(ApiConstants.VM_LIMIT) @Param(description="the total number of virtual machines that can be deployed by this account")
    private String vmLimit;

    @SerializedName(ApiConstants.VM_TOTAL) @Param(description="the total number of virtual machines deployed by this account")
    private Long vmTotal;

    @SerializedName(ApiConstants.VM_AVAILABLE) @Param(description="the total number of virtual machines available for this account to acquire")
    private String vmAvailable;

    @SerializedName(ApiConstants.IP_LIMIT) @Param(description="the total number of public ip addresses this account can acquire")
    private String ipLimit;

    @SerializedName(ApiConstants.IP_TOTAL) @Param(description="the total number of public ip addresses allocated for this account")
    private Long ipTotal;

    @SerializedName(ApiConstants.IP_AVAILABLE) @Param(description="the total number of public ip addresses available for this account to acquire")
    private String ipAvailable;

    @SerializedName("volumelimit") @Param(description="the total volume which can be used by this account")
    private String volumeLimit;

    @SerializedName("volumetotal") @Param(description="the total volume being used by this account")
    private Long volumeTotal;

    @SerializedName("volumeavailable") @Param(description="the total volume available for this account")
    private String volumeAvailable;

    @SerializedName("snapshotlimit") @Param(description="the total number of snapshots which can be stored by this account")
    private String snapshotLimit;

    @SerializedName("snapshottotal") @Param(description="the total number of snapshots stored by this account")
    private Long snapshotTotal;

    @SerializedName("snapshotavailable") @Param(description="the total number of snapshots available for this account")
    private String snapshotAvailable;

    @SerializedName("templatelimit") @Param(description="the total number of templates which can be created by this account")
    private String templateLimit;

    @SerializedName("templatetotal") @Param(description="the total number of templates which have been created by this account")
    private Long templateTotal;

    @SerializedName("templateavailable") @Param(description="the total number of templates available to be created by this account")
    private String templateAvailable;

    @SerializedName("vmstopped") @Param(description="the total number of virtual machines stopped for this account")
    private Integer vmStopped;

    @SerializedName("vmrunning") @Param(description="the total number of virtual machines running for this account")
    private Integer vmRunning;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the account")
    private String state;

    @SerializedName(ApiConstants.IS_CLEANUP_REQUIRED) @Param(description="true if the account requires cleanup")
    private Boolean cleanupRequired;
    
    @SerializedName("user")  @Param(description="the list of users associated with account", responseObject = UserResponse.class)
    private List<UserResponse> users;
    
    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain")
    private String networkDomain;
    
    @SerializedName(ApiConstants.ACCOUNT_DETAILS) @Param(description="details for the account")
    private Map<String, String> details;

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
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
        return domainId.getValue();
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
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
    
    public  List<UserResponse> getUsers() {
        return this.users;
    }
    
    public void setUsers(List<UserResponse> users) {
        this.users = users;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }
    
    public void setDetails(Map<String, String> details) {
    	this.details = details;
    }
    
    public Map getDetails() {
    	return details;
    }
}
