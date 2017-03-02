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
package com.cloud.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;


/**
 * NetScalerPodVO contains information about a EIP deployment where on datacenter L3 router a PBR (policy
 * based routing) is setup between a POD's subnet IP range to a NetScaler device. This VO object
 * represents a mapping between a POD and NetScaler device where PBR is setup.
 *
 */
@Entity
@Table(name = "external_netscaler_controlcenter")
public class NetScalerControlCenterVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "ncc_ip")
    private String nccip;

    @Column(name = "num_retries")
    private int numRetries;

    public NetScalerControlCenterVO() {
    }

    public NetScalerControlCenterVO(long hostId, String username, String password, String nccip, int retries) {
        this.username = username;
        this.password = password;
        this.uuid = UUID.randomUUID().toString();
        this.nccip = nccip;
        this.numRetries = retries;
    }
    public NetScalerControlCenterVO(String username, String password, String nccip, int retries) {
        this.username = username;
        this.password = password;
        this.uuid = UUID.randomUUID().toString();
        this.nccip = nccip;
        this.numRetries = retries;
    }

    public String getUuid() {
        return uuid;
    }

    public String getNccip() {
        return nccip;
    }

    public void setNccip(String nccip) {
        this.nccip = nccip;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getNumRetries() {
        return numRetries;
    }

    public void setNumRetries(int numRetries) {
        this.numRetries = numRetries;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return id;
    }

}
