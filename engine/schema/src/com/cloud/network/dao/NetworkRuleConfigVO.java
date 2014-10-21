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
package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = ("network_rule_config"))
public class NetworkRuleConfigVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "security_group_id")
    private long securityGroupId;

    @Column(name = "public_port")
    private String publicPort;

    @Column(name = "private_port")
    private String privatePort;

    @Column(name = "protocol")
    private String protocol;

    public NetworkRuleConfigVO() {
    }

    public NetworkRuleConfigVO(long securityGroupId, String publicPort, String privatePort, String protocol) {
        this.securityGroupId = securityGroupId;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.protocol = protocol;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getSecurityGroupId() {
        return securityGroupId;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
    }
}
