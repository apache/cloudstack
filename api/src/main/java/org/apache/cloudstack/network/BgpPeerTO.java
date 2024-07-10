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

public class BgpPeerTO {
    Long peerId;
    String ip4Address;
    String ip6Address;
    Long asNumber;
    String password;
    Long networkId;
    String ip4Cidr;
    String ip6Cidr;

    public BgpPeerTO(Long peerId, String ip4Address, String ip6Address, Long asNumber, String password, Long networkId, String ip4Cidr, String ip6Cidr) {
        this.peerId = peerId;
        this.ip4Address = ip4Address;
        this.ip6Address = ip6Address;
        this.asNumber = asNumber;
        this.password = password;
        this.networkId = networkId;
        this.ip4Cidr = ip4Cidr;
        this.ip6Cidr = ip6Cidr;
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

    public Long getAsNumber() {
        return asNumber;
    }

    public String getPassword() {
        return password;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public String getIp4Cidr() {
        return ip4Cidr;
    }

    public String getIp6Cidr() {
        return ip6Cidr;
    }
}
