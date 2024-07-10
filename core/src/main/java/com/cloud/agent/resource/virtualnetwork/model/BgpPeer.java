//
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
//

package com.cloud.agent.resource.virtualnetwork.model;

public class BgpPeer {
    private Long peerId;
    private String ip4Address;
    private String ip6Address;
    private Long asNumber;
    private String password;
    private Long networkId;
    private String ip4Cidr;
    private String ip6Cidr;

    public BgpPeer() {
        // Empty constructor for (de)serialization
    }

    public BgpPeer(Long peerId, String ip4Address, String ip6Address, Long asNumber, String password, Long networkId, String ip4Cidr, String ip6Cidr) {
        this.peerId = peerId;
        this.ip4Address = ip4Address;
        this.ip6Address = ip6Address;
        this.asNumber = asNumber;
        this.password = password;
        this.networkId = networkId;
        this.ip4Cidr = ip4Cidr;
        this.ip6Cidr = ip6Cidr;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public void setIp4Address(String ip4Address) {
        this.ip4Address = ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    public Long getAsNumber() {
        return asNumber;
    }

    public void setAsNumber(Long asNumber) {
        this.asNumber = asNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIp4Cidr() {
        return ip4Cidr;
    }

    public void setIp4Cidr(String ip4Cidr) {
        this.ip4Cidr = ip4Cidr;
    }

    public String getIp6Cidr() {
        return ip6Cidr;
    }

    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }
}
