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
package com.cloud.hypervisor.xenserver.resource;

import com.xensource.xenapi.Network;

/**
 * A list of UUIDs that are gathered from the XenServer when the resource first
 * connects to XenServer. These UUIDs do not change over time.
 */
public class XsHost {

    private String systemvmisouuid;
    private String uuid;
    private String ip;
    private String publicNetwork;
    private String privateNetwork;
    private String linkLocalNetwork;
    private Network vswitchNetwork;
    private String storageNetwork1;
    private String guestNetwork;
    private String guestPif;
    private String publicPif;
    private String privatePif;
    private String storagePif1;
    private String storagePif2;
    private String pool;
    private int speed;
    private Integer cpuSockets;
    private int cpus;
    private String productVersion;
    private String localSRuuid;

    public String getSystemvmisouuid() {
        return systemvmisouuid;
    }

    public void setSystemvmisouuid(final String systemvmisouuid) {
        this.systemvmisouuid = systemvmisouuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public String getPublicNetwork() {
        return publicNetwork;
    }

    public void setPublicNetwork(final String publicNetwork) {
        this.publicNetwork = publicNetwork;
    }

    public String getPrivateNetwork() {
        return privateNetwork;
    }

    public void setPrivateNetwork(final String privateNetwork) {
        this.privateNetwork = privateNetwork;
    }

    public String getLinkLocalNetwork() {
        return linkLocalNetwork;
    }

    public void setLinkLocalNetwork(final String linkLocalNetwork) {
        this.linkLocalNetwork = linkLocalNetwork;
    }

    public Network getVswitchNetwork() {
        return vswitchNetwork;
    }

    public void setVswitchNetwork(final Network vswitchNetwork) {
        this.vswitchNetwork = vswitchNetwork;
    }

    public String getStorageNetwork1() {
        return storageNetwork1;
    }

    public void setStorageNetwork1(final String storageNetwork1) {
        this.storageNetwork1 = storageNetwork1;
    }

    public String getGuestNetwork() {
        return guestNetwork;
    }

    public void setGuestNetwork(final String guestNetwork) {
        this.guestNetwork = guestNetwork;
    }

    public String getGuestPif() {
        return guestPif;
    }

    public void setGuestPif(final String guestPif) {
        this.guestPif = guestPif;
    }

    public String getPublicPif() {
        return publicPif;
    }

    public void setPublicPif(final String publicPif) {
        this.publicPif = publicPif;
    }

    public String getPrivatePif() {
        return privatePif;
    }

    public void setPrivatePif(final String privatePif) {
        this.privatePif = privatePif;
    }

    public String getStoragePif1() {
        return storagePif1;
    }

    public void setStoragePif1(final String storagePif1) {
        this.storagePif1 = storagePif1;
    }

    public String getStoragePif2() {
        return storagePif2;
    }

    public void setStoragePif2(final String storagePif2) {
        this.storagePif2 = storagePif2;
    }

    public String getPool() {
        return pool;
    }

    public void setPool(final String pool) {
        this.pool = pool;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(final int speed) {
        this.speed = speed;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(final Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public int getCpus() {
        return cpus;
    }

    public void setCpus(final int cpus) {
        this.cpus = cpus;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(final String productVersion) {
        this.productVersion = productVersion;
    }

    public String getLocalSRuuid() {
        return localSRuuid;
    }

    public void setLocalSRuuid(final String localSRuuid) {
        this.localSRuuid = localSRuuid;
    }

    @Override
    public String toString() {
        return new StringBuilder("XS[").append(uuid).append("-").append(ip).append("]").toString();
    }
}