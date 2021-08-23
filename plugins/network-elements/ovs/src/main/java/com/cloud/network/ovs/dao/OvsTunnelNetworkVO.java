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

package com.cloud.network.ovs.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "ovs_tunnel_network")
public class OvsTunnelNetworkVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "from")
    private long from;

    @Column(name = "to")
    private long to;

    @Column(name = "key")
    private int key;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "port_name")
    private String portName;

    @Column(name = "state")
    private String state;

    public OvsTunnelNetworkVO() {

    }

    public OvsTunnelNetworkVO(long from, long to, int key, long networkId) {
        this.from = from;
        this.to = to;
        this.key = key;
        this.networkId = networkId;
        this.portName = "[]";
        this.state = OvsTunnel.State.Created.name();
    }

    public void setKey(int key) {
        this.key = key;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public int getKey() {
        return key;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPortName(String name) {
        this.portName = name;
    }

    public String getPortName() {
        return portName;
    }
}
