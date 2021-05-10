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

package org.apache.cloudstack.storage.datastore.api;

public class Sdc {
    String id;
    String name;
    String mdmConnectionState;
    Boolean sdcApproved;
    String perfProfile;
    String sdcGuid;
    String sdcIp;
    String[] sdcIps;
    String systemId;
    String osType;
    String kernelVersion;
    String softwareVersionInfo;
    String versionInfo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMdmConnectionState() {
        return mdmConnectionState;
    }

    public void setMdmConnectionState(String mdmConnectionState) {
        this.mdmConnectionState = mdmConnectionState;
    }

    public Boolean getSdcApproved() {
        return sdcApproved;
    }

    public void setSdcApproved(Boolean sdcApproved) {
        this.sdcApproved = sdcApproved;
    }

    public String getPerfProfile() {
        return perfProfile;
    }

    public void setPerfProfile(String perfProfile) {
        this.perfProfile = perfProfile;
    }

    public String getSdcGuid() {
        return sdcGuid;
    }

    public void setSdcGuid(String sdcGuid) {
        this.sdcGuid = sdcGuid;
    }

    public String getSdcIp() {
        return sdcIp;
    }

    public void setSdcIp(String sdcIp) {
        this.sdcIp = sdcIp;
    }

    public String[] getSdcIps() {
        return sdcIps;
    }

    public void setSdcIps(String[] sdcIps) {
        this.sdcIps = sdcIps;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public String getSoftwareVersionInfo() {
        return softwareVersionInfo;
    }

    public void setSoftwareVersionInfo(String softwareVersionInfo) {
        this.softwareVersionInfo = softwareVersionInfo;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }
}
