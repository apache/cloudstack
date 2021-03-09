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

import com.cloud.network.RemoteAccessVpn;
import com.cloud.utils.db.Encrypt;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "remote_access_vpn")
public class RemoteAccessVpnVO implements RemoteAccessVpn {
    @Column(name = "account_id")
    private long accountId;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "vpn_server_addr_id")
    private long serverAddressId;

    @Column(name = "local_ip")
    private String localIp;

    @Column(name = "ip_range")
    private String ipRange;

    @Encrypt
    @Column(name = "ipsec_psk")
    private String ipsecPresharedKey;

    @Column(name = "state")
    private State state;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    public RemoteAccessVpnVO() {
        uuid = UUID.randomUUID().toString();
    }

    public RemoteAccessVpnVO(long accountId, long domainId, Long networkId, long publicIpId, Long vpcId, String localIp, String ipRange,  String presharedKey) {
        this.accountId = accountId;
        serverAddressId = publicIpId;
        this.ipRange = ipRange;
        ipsecPresharedKey = presharedKey;
        this.localIp = localIp;
        this.domainId = domainId;
        this.networkId = networkId;
        state = State.Added;
        uuid = UUID.randomUUID().toString();
        this.vpcId = vpcId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getServerAddressId() {
        return serverAddressId;
    }

    @Override
    public String getIpRange() {
        return ipRange;
    }

    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    @Override
    public String getIpsecPresharedKey() {
        return ipsecPresharedKey;
    }

    public void setIpsecPresharedKey(String ipsecPresharedKey) {
        this.ipsecPresharedKey = ipsecPresharedKey;
    }

    @Override
    public String getLocalIp() {
        return localIp;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    @Override
    public Class<?> getEntityType() {
        return RemoteAccessVpn.class;
    }
}
