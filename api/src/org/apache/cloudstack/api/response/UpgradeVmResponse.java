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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class UpgradeVmResponse extends BaseResponse {
    @SerializedName("id")
    private String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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

    public boolean isHaEnable() {
        return haEnable;
    }

    public void setHaEnable(boolean haEnable) {
        this.haEnable = haEnable;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateDisplayText() {
        return templateDisplayText;
    }

    public void setTemplateDisplayText(String templateDisplayText) {
        this.templateDisplayText = templateDisplayText;
    }

    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public String getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public long getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    public long getCpuUsed() {
        return cpuUsed;
    }

    public void setCpuUsed(long cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public long getNetworkKbsRead() {
        return networkKbsRead;
    }

    public void setNetworkKbsRead(long networkKbsRead) {
        this.networkKbsRead = networkKbsRead;
    }

    public long getNetworkKbsWrite() {
        return networkKbsWrite;
    }

    public void setNetworkKbsWrite(long networkKbsWrite) {
        this.networkKbsWrite = networkKbsWrite;
    }

    public String isId() {
        return id;
    }

    @SerializedName("name")
    @Param(description = "the ID of the virtual machine")
    private String name;

    @SerializedName("created")
    @Param(description = "the date when this virtual machine was created")
    private Date created;

    @SerializedName("ipaddress")
    @Param(description = "the ip address of the virtual machine")
    private String ipAddress;

    @SerializedName("state")
    @Param(description = "the state of the virtual machine")
    private String state;

    @SerializedName("account")
    @Param(description = "the account associated with the virtual machine")
    private String account;

    @SerializedName("domainid")
    @Param(description = "the ID of the domain in which the virtual machine exists")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "the name of the domain in which the virtual machine exists")
    private String domain;

    @SerializedName("haenable")
    @Param(description = "true if high-availability is enabled, false otherwise")
    private boolean haEnable;

    @SerializedName("zoneid")
    @Param(description = "the ID of the availablility zone for the virtual machine")
    private String zoneId;

    @SerializedName("displayname")
    @Param(description = "user generated name. The name of the virtual machine is returned if no displayname exists.")
    private String displayName;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the name of the availability zone for the virtual machine")
    private String zoneName;

    @SerializedName("hostid")
    @Param(description = "the ID of the host for the virtual machine")
    private String hostId;

    @SerializedName("hostname")
    @Param(description = "the name of the host for the virtual machine")
    private String hostName;

    @SerializedName("templateid")
    @Param(description = "the ID of the template for the virtual machine. A -1 is returned if the virtual machine was created from an ISO file.")
    private String templateId;

    @SerializedName("templatename")
    @Param(description = "the name of the template for the virtual machine")
    private String templateName;

    @SerializedName("templatedisplaytext")
    @Param(description = " an alternate display text of the template for the virtual machine")
    private String templateDisplayText;

    @SerializedName("passwordenabled")
    @Param(description = "true if the password rest feature is enabled, false otherwise")
    private boolean passwordEnabled;

    @SerializedName("serviceofferingid")
    @Param(description = "the ID of the service offering of the virtual machine")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname")
    @Param(description = "the name of the service offering of the virtual machine")
    private String serviceOfferingName;

    @SerializedName("cpunumber")
    @Param(description = "the number of cpu this virtual machine is running with")
    private long cpuSpeed;

    @SerializedName("memory")
    @Param(description = "the memory allocated for the virtual machine")
    private long memory;

    @SerializedName("cpuused")
    @Param(description = "the amount of the vm's CPU currently used")
    private long cpuUsed;

    @SerializedName("networkkbsread")
    @Param(description = "the incoming network traffic on the vm")
    private long networkKbsRead;

    @SerializedName("networkkbswrite")
    @Param(description = "the outgoing network traffic on the host")
    private long networkKbsWrite;
}
