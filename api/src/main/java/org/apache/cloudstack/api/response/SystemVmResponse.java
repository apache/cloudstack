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
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.host.Status;
import com.cloud.serializer.Param;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VirtualMachine.class)
public class SystemVmResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the ID of the system VM")
    private String id;

    @SerializedName("systemvmtype")
    @Param(description = "the system VM type")
    private String systemVmType;

    @SerializedName("jobid")
    @Param(description = "the job ID associated with the system VM. This is only displayed if the router listed is part of a currently running asynchronous job.")
    private String jobId;

    @SerializedName("jobstatus")
    @Param(description = "the job status associated with the system VM.  This is only displayed if the router listed is part of a currently running asynchronous job.")
    private Integer jobStatus;

    @SerializedName("zoneid")
    @Param(description = "the Zone ID for the system VM")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name for the system VM")
    private String zoneName;

    @SerializedName("dns1")
    @Param(description = "the first DNS for the system VM")
    private String dns1;

    @SerializedName("dns2")
    @Param(description = "the second DNS for the system VM")
    private String dns2;

    @SerializedName("networkdomain")
    @Param(description = "the network domain for the system VM")
    private String networkDomain;

    @SerializedName("gateway")
    @Param(description = "the gateway for the system VM")
    private String gateway;

    @SerializedName("name")
    @Param(description = "the name of the system VM")
    private String name;

    @SerializedName("podid")
    @Param(description = "the Pod ID for the system VM")
    private String podId;

    @SerializedName("podname")
    @Param(description = "the Pod name for the system VM", since = "4.13.2")
    private String podName;

    @SerializedName("hostid")
    @Param(description = "the host ID for the system VM")
    private String hostId;

    @SerializedName("hostname")
    @Param(description = "the hostname for the system VM")
    private String hostName;

    @SerializedName("hypervisor")
    @Param(description = "the hypervisor on which the template runs")
    private String hypervisor;

    @SerializedName(ApiConstants.PRIVATE_IP)
    @Param(description = "the private IP address for the system VM")
    private String privateIp;

    @SerializedName(ApiConstants.PRIVATE_MAC_ADDRESS)
    @Param(description = "the private MAC address for the system VM")
    private String privateMacAddress;

    @SerializedName(ApiConstants.PRIVATE_NETMASK)
    @Param(description = "the private netmask for the system VM")
    private String privateNetmask;

    @SerializedName(ApiConstants.LINK_LOCAL_IP)
    @Param(description = "the link local IP address for the system vm")
    private String linkLocalIp;

    @SerializedName(ApiConstants.LINK_LOCAL_MAC_ADDRESS)
    @Param(description = "the link local MAC address for the system vm")
    private String linkLocalMacAddress;

    @SerializedName(ApiConstants.LINK_LOCAL_MAC_NETMASK)
    @Param(description = "the link local netmask for the system vm")
    private String linkLocalNetmask;

    @SerializedName("publicip")
    @Param(description = "the public IP address for the system VM")
    private String publicIp;

    @SerializedName("publicmacaddress")
    @Param(description = "the public MAC address for the system VM")
    private String publicMacAddress;

    @SerializedName("publicnetmask")
    @Param(description = "the public netmask for the system VM")
    private String publicNetmask;

    @SerializedName("templateid")
    @Param(description = "the template ID for the system VM")
    private String templateId;

    @SerializedName("templatename")
    @Param(description = "the template name for the system VM", since = "4.13.2")
    private String templateName;

    @SerializedName("created")
    @Param(description = "the date and time the system VM was created")
    private Date created;

    @SerializedName("state")
    @Param(description = "the state of the system VM")
    private String state;

    @SerializedName("agentstate")
    @Param(description = "the agent state of the system VM", since = "4.13.1")
    private String agentState;

    @SerializedName("activeviewersessions")
    @Param(description = "the number of active console sessions for the console proxy system vm")
    private Integer activeViewerSessions;

    @SerializedName("guestvlan")
    @Param(description = "guest vlan range")
    private String guestVlan;

    @SerializedName("publicvlan")
    @Param(description = "public vlan range")
    private List<String> publicVlan;

    @SerializedName("disconnected")
    @Param(description = "the last disconnected date of host", since = "4.13.1")
    private Date disconnectedOn;

    @SerializedName("version")
    @Param(description = "the systemvm agent version", since = "4.13.1")
    private String version;

    @SerializedName(ApiConstants.IS_DYNAMICALLY_SCALABLE)
    @Param(description = "true if vm contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory.")
    private Boolean isDynamicallyScalable;

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

    public String getSystemVmType() {
        return systemVmType;
    }

    public void setSystemVmType(String systemVmType) {
        this.systemVmType = systemVmType;
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

    public String getDns1() {
        return dns1;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPodId() {
        return podId;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public void setPodName(String podName) {
        this.podName = podName;
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

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public String getPrivateNetmask() {
        return privateNetmask;
    }

    public void setPrivateNetmask(String privateNetmask) {
        this.privateNetmask = privateNetmask;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public String getPublicNetmask() {
        return publicNetmask;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAgentState() {
        return agentState;
    }

    public void setAgentState(Status agentState) {
        if (agentState != null) {
            this.agentState = agentState.toString();
        } else {
            this.agentState = Status.Unknown.toString();
        }
    }

    public Integer getActiveViewerSessions() {
        return activeViewerSessions;
    }

    public void setActiveViewerSessions(Integer activeViewerSessions) {
        this.activeViewerSessions = activeViewerSessions;
    }

    public String getLinkLocalIp() {
        return linkLocalIp;
    }

    public void setLinkLocalIp(String linkLocalIp) {
        this.linkLocalIp = linkLocalIp;
    }

    public String getLinkLocalMacAddress() {
        return linkLocalMacAddress;
    }

    public void setLinkLocalMacAddress(String linkLocalMacAddress) {
        this.linkLocalMacAddress = linkLocalMacAddress;
    }

    public String getLinkLocalNetmask() {
        return linkLocalNetmask;
    }

    public void setLinkLocalNetmask(String linkLocalNetmask) {
        this.linkLocalNetmask = linkLocalNetmask;
    }

    public String getGuestVlan() {
        return guestVlan;
    }

    public void setGuestVlan(String guestVlan) {
        this.guestVlan = guestVlan;
    }

    public List<String> getPublicVlan() {
        return publicVlan;
    }

    public void setPublicVlan(List<String> publicVlan) {
        this.publicVlan = publicVlan;
    }

    public Date getDisconnectedOn() {
        return disconnectedOn;
    }

    public void setDisconnectedOn(Date disconnectedOn) {
        this.disconnectedOn = disconnectedOn;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    public void setDynamicallyScalable(Boolean dynamicallyScalable) {
        isDynamicallyScalable = dynamicallyScalable;
    }
}
