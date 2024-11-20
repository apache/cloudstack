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
package org.apache.cloudstack.network;

import java.util.Map;

public class BgpPeerTO {
    Long peerId;
    Long peerAsNumber;
    String ip4Address;
    String ip6Address;
    String peerPassword;
    Long networkId;
    Long networkAsNumber;
    String guestIp4Cidr;
    String guestIp6Cidr;

    Map<BgpPeer.Detail, String> details;

    public BgpPeerTO(Long peerId, String ip4Address, String ip6Address, Long peerAsNumber, String peerPassword,
                     Long networkId, Long networkAsNumber, String guestIp4Cidr, String guestIp6Cidr, Map<BgpPeer.Detail, String> details) {
        this.peerId = peerId;
        this.ip4Address = ip4Address;
        this.ip6Address = ip6Address;
        this.peerAsNumber = peerAsNumber;
        this.peerPassword = peerPassword;
        this.networkId = networkId;
        this.networkAsNumber = networkAsNumber;
        this.guestIp4Cidr = guestIp4Cidr;
        this.guestIp6Cidr = guestIp6Cidr;
        this.details = details;
    }

    public BgpPeerTO(Long networkId) {
        this.networkId = networkId;
    }

    public Long getPeerId() {
        return peerId;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public Long getPeerAsNumber() {
        return peerAsNumber;
    }

    public String getPeerPassword() {
        return peerPassword;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getNetworkAsNumber() {
        return networkAsNumber;
    }

    public String getGuestIp4Cidr() {
        return guestIp4Cidr;
    }

    public String getGuestIp6Cidr() {
        return guestIp6Cidr;
    }

    public Map<BgpPeer.Detail, String> getDetails() {
        return details;
    }
}
