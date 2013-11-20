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
package com.cloud.network.vpc;

public class PrivateIpAddress implements PrivateIp {
    String broadcastUri;
    String gateway;
    String netmask;
    String ipAddress;
    String macAddress;
    long networkId;
    boolean sourceNat;

    /**
     * @param privateIp
     * @param broadcastUri
     * @param gateway
     * @param netmask
     * @param macAddress TODO
     * @param physicalNetworkId TODO
     */
    public PrivateIpAddress(PrivateIpVO privateIp, String broadcastUri, String gateway, String netmask, String macAddress) {
        super();
        this.ipAddress = privateIp.getIpAddress();
        this.broadcastUri = broadcastUri;
        this.gateway = gateway;
        this.netmask = netmask;
        this.macAddress = macAddress;
        this.networkId = privateIp.getNetworkId();
        this.sourceNat = privateIp.getSourceNat();
    }

    @Override
    public String getBroadcastUri() {
        return broadcastUri;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public boolean getSourceNat() {
        return sourceNat;
    }
}
