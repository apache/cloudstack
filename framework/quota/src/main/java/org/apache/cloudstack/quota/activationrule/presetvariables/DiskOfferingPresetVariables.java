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

package org.apache.cloudstack.quota.activationrule.presetvariables;

public class DiskOfferingPresetVariables extends GenericPresetVariable {

    @PresetVariableDefinition(description = "A long informing the bytes read rate of the disk offering.")
    private Long bytesReadRate;

    @PresetVariableDefinition(description = "A long informing the burst bytes read rate of the disk offering.")
    private Long bytesReadBurst;

    @PresetVariableDefinition(description = "The length (in seconds) of the bytes read burst.")
    private Long bytesReadBurstLength;

    @PresetVariableDefinition(description = "A long informing the bytes write rate of the disk offering.")
    private Long bytesWriteRate;

    @PresetVariableDefinition(description = "A long informing the burst bytes write rate of the disk offering.")
    private Long bytesWriteBurst;

    @PresetVariableDefinition(description = "The length (in seconds) of the bytes write burst.")
    private Long bytesWriteBurstLength;

    @PresetVariableDefinition(description = "A long informing the I/O requests read rate of the disk offering.")
    private Long iopsReadRate;

    @PresetVariableDefinition(description = "A long informing the burst I/O requests read rate of the disk offering.")
    private Long iopsReadBurst;

    @PresetVariableDefinition(description = "The length (in seconds) of the IOPS read burst.")
    private Long iopsReadBurstLength;

    @PresetVariableDefinition(description = "A long informing the I/O requests write rate of the disk offering.")
    private Long iopsWriteRate;

    @PresetVariableDefinition(description = "A long informing the burst I/O requests write rate of the disk offering.")
    private Long iopsWriteBurst;

    @PresetVariableDefinition(description = "The length (in seconds) of the IOPS write burst.")
    private Long iopsWriteBurstLength;

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
        fieldNamesToIncludeInToString.add("bytesReadRate");
    }

    public Long getBytesReadBurst() {
        return bytesReadBurst;
    }

    public void setBytesReadBurst(Long bytesReadBurst) {
        this.bytesReadBurst = bytesReadBurst;
        fieldNamesToIncludeInToString.add("bytesReadBurst");
    }

    public Long getBytesReadBurstLength() {
        return bytesReadBurstLength;
    }

    public void setBytesReadBurstLength(Long bytesReadBurstLength) {
        this.bytesReadBurstLength = bytesReadBurstLength;
        fieldNamesToIncludeInToString.add("bytesReadBurstLength");
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
        fieldNamesToIncludeInToString.add("bytesWriteRate");
    }

    public Long getBytesWriteBurst() {
        return bytesWriteBurst;
    }

    public void setBytesWriteBurst(Long bytesWriteBurst) {
        this.bytesWriteBurst = bytesWriteBurst;
        fieldNamesToIncludeInToString.add("bytesWriteBurst");
    }

    public Long getBytesWriteBurstLength() {
        return bytesWriteBurstLength;
    }

    public void setBytesWriteBurstLength(Long bytesWriteBurstLength) {
        this.bytesWriteBurstLength = bytesWriteBurstLength;
        fieldNamesToIncludeInToString.add("bytesWriteBurstLength");
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
        fieldNamesToIncludeInToString.add("iopsReadRate");
    }

    public Long getIopsReadBurst() {
        return iopsReadBurst;
    }

    public void setIopsReadBurst(Long iopsReadBurst) {
        this.iopsReadBurst = iopsReadBurst;
        fieldNamesToIncludeInToString.add("iopsReadBurst");
    }

    public Long getIopsReadBurstLength() {
        return iopsReadBurstLength;
    }

    public void setIopsReadBurstLength(Long iopsReadBurstLength) {
        this.iopsReadBurstLength = iopsReadBurstLength;
        fieldNamesToIncludeInToString.add("iopsReadBurstLength");
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
        fieldNamesToIncludeInToString.add("iopsWriteRate");
    }

    public Long getIopsWriteBurst() {
        return iopsWriteBurst;
    }

    public void setIopsWriteBurst(Long iopsWriteBurst) {
        this.iopsWriteBurst = iopsWriteBurst;
        fieldNamesToIncludeInToString.add("iopsWriteBurst");
    }

    public Long getIopsWriteBurstLength() {
        return iopsWriteBurstLength;
    }

    public void setIopsWriteBurstLength(Long iopsWriteBurstLength) {
        this.iopsWriteBurstLength = iopsWriteBurstLength;
        fieldNamesToIncludeInToString.add("iopsWriteBurstLength");
    }
}
