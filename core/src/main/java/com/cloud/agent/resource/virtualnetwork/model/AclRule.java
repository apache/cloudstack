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

public abstract class AclRule {
    private String cidr;
    private boolean allowed;

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    protected AclRule() {
        // Empty constructor for (de)serialization
    }

    protected AclRule(String cidr, boolean allowed) {
        this.cidr = cidr;
        this.allowed = allowed;
    }

}

/*
{"device":"eth2","mac_address":"02:00:56:36:00:02","private_gateway_acl":false,"nic_ip":"172.16.1.1","nic_netmask":"24",
    "rule":"Ingress:41:0:0:192.168.5.0/24:DROP:,"
            + "Ingress:all:0:0:192.168.4.0/24:ACCEPT:,"
            + "Ingress:icmp:8:-1:192.168.3.0/24:ACCEPT:,"
            + "Ingress:udp:8080:8081:192.168.2.0/24:ACCEPT:,"
            + "Ingress:tcp:22:22:192.168.1.0/24:ACCEPT:,","type":"networkacl"}
 */