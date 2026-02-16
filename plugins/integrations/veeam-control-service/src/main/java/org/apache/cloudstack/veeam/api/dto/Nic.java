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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Nic extends BaseDto {

    private String name;
    private String description;
    @JacksonXmlProperty(localName = "interface")
    @JsonProperty("interface")
    private String interfaceType;
    private String linked;
    private Mac mac;
    private String plugged;
    public String synced;
    private Ref vnicProfile;
    private Vm vm;
    private ReportedDevices reportedDevices;

    public Nic() {
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getLinked() {
        return linked;
    }

    public void setLinked(String linked) {
        this.linked = linked;
    }

    public Mac getMac() {
        return mac;
    }

    public void setMac(Mac mac) {
        this.mac = mac;
    }

    public String getPlugged() {
        return plugged;
    }

    public void setPlugged(String plugged) {
        this.plugged = plugged;
    }

    public String getSynced() {
        return synced;
    }

    public void setSynced(String synced) {
        this.synced = synced;
    }

    public Ref getVnicProfile() {
        return vnicProfile;
    }

    public void setVnicProfile(Ref vnicProfile) {
        this.vnicProfile = vnicProfile;
    }

    public Vm getVm() {
        return vm;
    }

    public void setVm(Vm vm) {
        this.vm = vm;
    }

    public ReportedDevices getReportedDevices() {
        return reportedDevices;
    }

    public void setReportedDevices(ReportedDevices reportedDevices) {
        this.reportedDevices = reportedDevices;
    }
}
