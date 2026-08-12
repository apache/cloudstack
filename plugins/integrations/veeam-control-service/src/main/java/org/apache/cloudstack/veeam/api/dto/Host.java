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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Host extends BaseDto {

    private String address;
    private String autoNumaStatus;
    private Certificate certificate;
    private Cpu cpu;
    private String externalStatus;
    private HardwareInformation hardwareInformation;
    private String kdumpStatus;
    private Version libvirtVersion;
    private String maxSchedulingMemory;
    private String memory;
    private String numaSupported;
    private Os os;
    private String port;
    private String protocol;
    private String reinstallationRequired;
    private String status;
    private ApiSummary summary;
    private String type;
    private String updateAvailable;
    private Version version;
    private String vgpuPlacement;
    private Ref cluster;
    private NamedList<Link> actions;
    private String name;
    private String comment;
    private List<Link> link;

    // getters/setters (generate via IDE)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAutoNumaStatus() {
        return autoNumaStatus;
    }

    public void setAutoNumaStatus(String autoNumaStatus) {
        this.autoNumaStatus = autoNumaStatus;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public HardwareInformation getHardwareInformation() {
        return hardwareInformation;
    }

    public void setHardwareInformation(HardwareInformation hardwareInformation) {
        this.hardwareInformation = hardwareInformation;
    }

    public String getKdumpStatus() {
        return kdumpStatus;
    }

    public void setKdumpStatus(String kdumpStatus) {
        this.kdumpStatus = kdumpStatus;
    }

    public Version getLibvirtVersion() {
        return libvirtVersion;
    }

    public void setLibvirtVersion(Version libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }

    public String getMaxSchedulingMemory() {
        return maxSchedulingMemory;
    }

    public void setMaxSchedulingMemory(String maxSchedulingMemory) {
        this.maxSchedulingMemory = maxSchedulingMemory;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getNumaSupported() {
        return numaSupported;
    }

    public void setNumaSupported(String numaSupported) {
        this.numaSupported = numaSupported;
    }

    public Os getOs() {
        return os;
    }

    public void setOs(Os os) {
        this.os = os;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getReinstallationRequired() {
        return reinstallationRequired;
    }

    public void setReinstallationRequired(String reinstallationRequired) {
        this.reinstallationRequired = reinstallationRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ApiSummary getSummary() {
        return summary;
    }

    public void setSummary(ApiSummary summary) {
        this.summary = summary;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpdateAvailable() {
        return updateAvailable;
    }

    public void setUpdateAvailable(String updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String getVgpuPlacement() {
        return vgpuPlacement;
    }

    public void setVgpuPlacement(String vgpuPlacement) {
        this.vgpuPlacement = vgpuPlacement;
    }

    public Ref getCluster() {
        return cluster;
    }

    public void setCluster(Ref cluster) {
        this.cluster = cluster;
    }

    public NamedList<Link> getActions() {
        return actions;
    }

    public void setActions(NamedList<Link> actions) {
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HardwareInformation {
        private String manufacturer;
        private String productName;
        private String serialNumber;
        private String uuid;
        private String version;

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static Host of(String href, String id) {
        return withHrefAndId(new Host(), href, id);
    }
}
