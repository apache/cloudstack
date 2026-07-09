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
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.host.Status;
import com.cloud.serializer.Param;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VirtualMachine.class)
public class SystemVmResponse extends BaseResponseWithAnnotations {
    @SerializedName("id")
    @Param(description = "The ID of the System VM")
    private String id;

    @SerializedName("systemvmtype")
    @Param(description = "The System VM type")
    private String systemVmType;

    @SerializedName("zoneid")
    @Param(description = "The Zone ID for the System VM")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "The Zone name for the System VM")
    private String zoneName;

    @SerializedName("dns1")
    @Param(description = "The first DNS for the System VM")
    private String dns1;

    @SerializedName("dns2")
    @Param(description = "The second DNS for the System VM")
    private String dns2;

    @SerializedName("networkdomain")
    @Param(description = "The Network domain for the System VM")
    private String networkDomain;

    @SerializedName("gateway")
    @Param(description = "The gateway for the System VM")
    private String gateway;

    @SerializedName("name")
    @Param(description = "The name of the System VM")
    private String name;

    @SerializedName("podid")
    @Param(description = "The Pod ID for the System VM")
    private String podId;

    @SerializedName("podname")
    @Param(description = "The Pod name for the System VM", since = "4.13.2")
    private String podName;

    @SerializedName("hostid")
    @Param(description = "The host ID for the System VM")
    private String hostId;

    @SerializedName("hostname")
    @Param(description = "The hostname for the System VM")
    private String hostName;

    @SerializedName(ApiConstants.HOST_CONTROL_STATE)
    @Param(description = "The control state of the host for the System VM")
    private String hostControlState;

    @SerializedName("hypervisor")
    @Param(description = "The hypervisor on which the Template runs")
    private String hypervisor;

    @SerializedName(ApiConstants.PRIVATE_IP)
    @Param(description = "The private IP address for the System VM")
    private String privateIp;

    @SerializedName(ApiConstants.PRIVATE_MAC_ADDRESS)
    @Param(description = "The private MAC address for the System VM")
    private String privateMacAddress;

    @SerializedName(ApiConstants.PRIVATE_NETMASK)
    @Param(description = "The private netmask for the System VM")
    private String privateNetmask;

    @SerializedName(ApiConstants.LINK_LOCAL_IP)
    @Param(description = "The Control IP address for the System VM")
    private String linkLocalIp;

    @SerializedName(ApiConstants.LINK_LOCAL_MAC_ADDRESS)
    @Param(description = "The link local MAC address for the System VM")
    private String linkLocalMacAddress;

    @SerializedName(ApiConstants.LINK_LOCAL_MAC_NETMASK)
    @Param(description = "The link local netmask for the System VM")
    private String linkLocalNetmask;

    @SerializedName("publicip")
    @Param(description = "The public IP address for the System VM")
    private String publicIp;

    @SerializedName("publicmacaddress")
    @Param(description = "The public MAC address for the System VM")
    private String publicMacAddress;

    @SerializedName("publicnetmask")
    @Param(description = "The public netmask for the System VM")
    private String publicNetmask;

    @SerializedName("storageip")
    @Param(description = "the ip address for the system VM on the storage network")
    private String storageIp;

    @SerializedName("templateid")
    @Param(description = "The Template ID for the System VM")
    private String templateId;

    @SerializedName("templatename")
    @Param(description = "The Template name for the System VM", since = "4.13.2")
    private String templateName;

    @SerializedName("created")
    @Param(description = "The date and time the System VM was created")
    private Date created;

    @SerializedName("state")
    @Param(description = "The state of the System VM")
    private String state;

    @SerializedName("agentstate")
    @Param(description = "The agent state of the System VM", since = "4.13.1")
    private String agentState;

    @SerializedName("activeviewersessions")
    @Param(description = "The number of active console sessions for the console proxy System VM")
    private Integer activeViewerSessions;

    @SerializedName("guestvlan")
    @Param(description = "Guest VLAN range")
    private String guestVlan;

    @SerializedName("publicvlan")
    @Param(description = "Public VLAN range")
    private List<String> publicVlan;

    @SerializedName("disconnected")
    @Param(description = "The last disconnected date of host", since = "4.13.1")
    private Date disconnectedOn;

    @SerializedName("version")
    @Param(description = "The systemvm agent version", since = "4.13.1")
    private String version;

    @SerializedName(ApiConstants.IS_DYNAMICALLY_SCALABLE)
    @Param(description = "True if the Instance contains XS/VMWare tools in order to support dynamic scaling of Instance CPU/memory.")
    private Boolean isDynamicallyScalable;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "the ID of the service offering of the system virtual machine.")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname")
    @Param(description = "the name of the service offering of the system virtual machine.")
    private String serviceOfferingName;

    @SerializedName(ApiConstants.ARCH)
    @Param(description = "CPU arch of the system VM", since = "4.20.1")
    private String arch;

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

    public String getHostControlState() {
        return hostControlState;
    }

    public void setHostControlState(String hostControlState) {
        this.hostControlState = hostControlState;
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

    public String getStorageIp() {
        return storageIp;
    }

    public void setStorageIp(String storageIp) {
        this.storageIp = storageIp;
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

    public void setArch(String arch) {
        this.arch = arch;
    }
}
