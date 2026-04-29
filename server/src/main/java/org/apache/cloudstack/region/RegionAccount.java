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
package org.apache.cloudstack.region;

import com.cloud.user.AccountVO;

public class RegionAccount extends AccountVO {
    String accountUuid;
    String domainUuid;
    String domain;
    String receivedbytes;
    String sentbytes;
    String vmlimit;
    String vmtotal;
    String vmavailable;
    String iplimit;
    String iptotal;
    String ipavailable;
    String volumelimit;
    String volumetotal;
    String volumeavailable;
    String snapshotlimit;
    String snapshottotal;
    String snapshotavailable;
    String templatelimit;
    String templatetotal;
    String templateavailable;
    String vmstopped;
    String vmrunning;
    String projectlimit;
    String projecttotal;
    String projectavailable;
    String networklimit;
    String networktotal;
    String networkavailable;
    RegionUser user;

    public RegionAccount() {
    }

    public String getAccountuuid() {
        return accountUuid;
    }

    public void setAccountuuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getReceivedbytes() {
        return receivedbytes;
    }

    public void setReceivedbytes(String receivedbytes) {
        this.receivedbytes = receivedbytes;
    }

    public String getSentbytes() {
        return sentbytes;
    }

    public void setSentbytes(String sentbytes) {
        this.sentbytes = sentbytes;
    }

    public String getVmlimit() {
        return vmlimit;
    }

    public void setVmlimit(String vmlimit) {
        this.vmlimit = vmlimit;
    }

    public String getVmtotal() {
        return vmtotal;
    }

    public void setVmtotal(String vmtotal) {
        this.vmtotal = vmtotal;
    }

    public String getVmavailable() {
        return vmavailable;
    }

    public void setVmavailable(String vmavailable) {
        this.vmavailable = vmavailable;
    }

    public String getIplimit() {
        return iplimit;
    }

    public void setIplimit(String iplimit) {
        this.iplimit = iplimit;
    }

    public String getIptotal() {
        return iptotal;
    }

    public void setIptotal(String iptotal) {
        this.iptotal = iptotal;
    }

    public String getIpavailable() {
        return ipavailable;
    }

    public void setIpavailable(String ipavailable) {
        this.ipavailable = ipavailable;
    }

    public String getVolumelimit() {
        return volumelimit;
    }

    public void setVolumelimit(String volumelimit) {
        this.volumelimit = volumelimit;
    }

    public String getVolumetotal() {
        return volumetotal;
    }

    public void setVolumetotal(String volumetotal) {
        this.volumetotal = volumetotal;
    }

    public String getVolumeavailable() {
        return volumeavailable;
    }

    public void setVolumeavailable(String volumeavailable) {
        this.volumeavailable = volumeavailable;
    }

    public String getSnapshotlimit() {
        return snapshotlimit;
    }

    public void setSnapshotlimit(String snapshotlimit) {
        this.snapshotlimit = snapshotlimit;
    }

    public String getSnapshottotal() {
        return snapshottotal;
    }

    public void setSnapshottotal(String snapshottotal) {
        this.snapshottotal = snapshottotal;
    }

    public String getSnapshotavailable() {
        return snapshotavailable;
    }

    public void setSnapshotavailable(String snapshotavailable) {
        this.snapshotavailable = snapshotavailable;
    }

    public String getTemplatelimit() {
        return templatelimit;
    }

    public void setTemplatelimit(String templatelimit) {
        this.templatelimit = templatelimit;
    }

    public String getTemplatetotal() {
        return templatetotal;
    }

    public void setTemplatetotal(String templatetotal) {
        this.templatetotal = templatetotal;
    }

    public String getTemplateavailable() {
        return templateavailable;
    }

    public void setTemplateavailable(String templateavailable) {
        this.templateavailable = templateavailable;
    }

    public String getVmstopped() {
        return vmstopped;
    }

    public void setVmstopped(String vmstopped) {
        this.vmstopped = vmstopped;
    }

    public String getVmrunning() {
        return vmrunning;
    }

    public void setVmrunning(String vmrunning) {
        this.vmrunning = vmrunning;
    }

    public String getProjectlimit() {
        return projectlimit;
    }

    public void setProjectlimit(String projectlimit) {
        this.projectlimit = projectlimit;
    }

    public String getProjecttotal() {
        return projecttotal;
    }

    public void setProjecttotal(String projecttotal) {
        this.projecttotal = projecttotal;
    }

    public String getProjectavailable() {
        return projectavailable;
    }

    public void setProjectavailable(String projectavailable) {
        this.projectavailable = projectavailable;
    }

    public String getNetworklimit() {
        return networklimit;
    }

    public void setNetworklimit(String networklimit) {
        this.networklimit = networklimit;
    }

    public String getNetworktotal() {
        return networktotal;
    }

    public void setNetworktotal(String networktotal) {
        this.networktotal = networktotal;
    }

    public String getNetworkavailable() {
        return networkavailable;
    }

    public void setNetworkavailable(String networkavailable) {
        this.networkavailable = networkavailable;
    }

    public RegionUser getUser() {
        return user;
    }

    public void setUser(RegionUser user) {
        this.user = user;
    }

}
