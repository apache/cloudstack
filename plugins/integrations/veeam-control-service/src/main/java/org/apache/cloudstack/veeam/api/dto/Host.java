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

package org.apache.cloudstack.veeam.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Host {

    @JsonProperty("address")
    private String address;

    @JsonProperty("auto_numa_status")
    private String autoNumaStatus;

    @JsonProperty("certificate")
    private Certificate certificate;

    @JsonProperty("cpu")
    private Cpu cpu;

    @JsonProperty("external_status")
    private String externalStatus;

    @JsonProperty("hardware_information")
    private HardwareInformation hardwareInformation;

    @JsonProperty("kdump_status")
    private String kdumpStatus;

    @JsonProperty("libvirt_version")
    private Version libvirtVersion;

    @JsonProperty("max_scheduling_memory")
    private String maxSchedulingMemory;

    @JsonProperty("memory")
    private String memory;

    @JsonProperty("numa_supported")
    private String numaSupported;

    @JsonProperty("os")
    private Os os;

    @JsonProperty("port")
    private String port;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("reinstallation_required")
    private String reinstallationRequired;

    @JsonProperty("status")
    private String status;

    @JsonProperty("summary")
    private ApiSummary summary;

    @JsonProperty("type")
    private String type;

    @JsonProperty("update_available")
    private String updateAvailable;

    @JsonProperty("version")
    private Version version;

    @JsonProperty("vgpu_placement")
    private String vgpuPlacement;

    @JsonProperty("cluster")
    private Ref cluster;

    @JsonProperty("actions")
    private Actions actions;

    @JsonProperty("name")
    private String name;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("link")
    private List<Link> link;

    @JsonProperty("href")
    private String href;

    @JsonProperty("id")
    private String id;

    // getters/setters (generate via IDE)
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAutoNumaStatus() { return autoNumaStatus; }
    public void setAutoNumaStatus(String autoNumaStatus) { this.autoNumaStatus = autoNumaStatus; }
    public Certificate getCertificate() { return certificate; }
    public void setCertificate(Certificate certificate) { this.certificate = certificate; }
    public Cpu getCpu() { return cpu; }
    public void setCpu(Cpu cpu) { this.cpu = cpu; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }
    public HardwareInformation getHardwareInformation() { return hardwareInformation; }
    public void setHardwareInformation(HardwareInformation hardwareInformation) { this.hardwareInformation = hardwareInformation; }
    public String getKdumpStatus() { return kdumpStatus; }
    public void setKdumpStatus(String kdumpStatus) { this.kdumpStatus = kdumpStatus; }
    public Version getLibvirtVersion() { return libvirtVersion; }
    public void setLibvirtVersion(Version libvirtVersion) { this.libvirtVersion = libvirtVersion; }
    public String getMaxSchedulingMemory() { return maxSchedulingMemory; }
    public void setMaxSchedulingMemory(String maxSchedulingMemory) { this.maxSchedulingMemory = maxSchedulingMemory; }
    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }
    public String getNumaSupported() { return numaSupported; }
    public void setNumaSupported(String numaSupported) { this.numaSupported = numaSupported; }
    public Os getOs() { return os; }
    public void setOs(Os os) { this.os = os; }
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getReinstallationRequired() { return reinstallationRequired; }
    public void setReinstallationRequired(String reinstallationRequired) { this.reinstallationRequired = reinstallationRequired; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ApiSummary getSummary() { return summary; }
    public void setSummary(ApiSummary summary) { this.summary = summary; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUpdateAvailable() { return updateAvailable; }
    public void setUpdateAvailable(String updateAvailable) { this.updateAvailable = updateAvailable; }
    public Version getVersion() { return version; }
    public void setVersion(Version version) { this.version = version; }
    public String getVgpuPlacement() { return vgpuPlacement; }
    public void setVgpuPlacement(String vgpuPlacement) { this.vgpuPlacement = vgpuPlacement; }
    public Ref getCluster() { return cluster; }
    public void setCluster(Ref cluster) { this.cluster = cluster; }
    public Actions getActions() { return actions; }
    public void setActions(Actions actions) { this.actions = actions; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public List<Link> getLink() { return link; }
    public void setLink(List<Link> link) { this.link = link; }
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
