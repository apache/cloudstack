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

import com.cloud.network.vpn.RemoteAccessVpnL2TPProtocol;
import com.cloud.utils.db.Encrypt;

import javax.persistence.*;

@Entity
@Table(name = "remote_access_vpn")
@SecondaryTable(name = "remote_access_vpn_l2tp", pkJoinColumns = @PrimaryKeyJoinColumn(name = "vpn_id"))
public class RemoteAccessVpnL2TPVO extends RemoteAccessVpnVO {

    @Encrypt
    @Column(name = "preshared_key", table = "remote_access_vpn_l2tp")
    private String presharedKey;

    public RemoteAccessVpnL2TPVO() {
        super();
        this.setProviderName(RemoteAccessVpnL2TPProtocol.PROTOCOL_NAME);
    }

    public RemoteAccessVpnL2TPVO(long accountId, long domainId, Long networkId, long publicIpId, Long vpcId, String localIp, String ipRange,  String presharedKey) {
        super(
                accountId,
                domainId,
                networkId,
                publicIpId,
                vpcId,
                localIp,
                ipRange
        );

        this.presharedKey = presharedKey;
        this.setProviderName(RemoteAccessVpnL2TPProtocol.PROTOCOL_NAME);
    }

    public String getIpsecPresharedKey() {
        return presharedKey;
    }

    public void setIpsecPresharedKey(String ipsecPresharedKey) {
        this.presharedKey = ipsecPresharedKey;
    }
}
