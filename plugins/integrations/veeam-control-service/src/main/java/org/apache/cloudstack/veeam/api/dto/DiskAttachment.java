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

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DiskAttachment extends BaseDto {

    private String active;
    private String bootable;
    @JsonProperty("interface")
    private String iface; // virtio_scsi etc
    private String logicalName;
    private String passDiscard;
    private String readOnly;
    private String usesScsiReservation;
    private Disk disk;
    private Vm vm;

    public DiskAttachment() {
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getBootable() {
        return bootable;
    }

    public void setBootable(String bootable) {
        this.bootable = bootable;
    }

    public String getIface() {
        return iface;
    }

    public void setIface(String iface) {
        this.iface = iface;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public String getPassDiscard() {
        return passDiscard;
    }

    public void setPassDiscard(String passDiscard) {
        this.passDiscard = passDiscard;
    }

    public String getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

    public String getUsesScsiReservation() {
        return usesScsiReservation;
    }

    public void setUsesScsiReservation(String usesScsiReservation) {
        this.usesScsiReservation = usesScsiReservation;
    }

    public Disk getDisk() {
        return disk;
    }

    public void setDisk(Disk disk) {
        this.disk = disk;
    }

    public Vm getVm() {
        return vm;
    }

    public void setVm(Vm vm) {
        this.vm = vm;
    }
}
