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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vpc_gateways")
public class VpcGatewayVO implements VpcGateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "ip4_address")
    String ip4Address;

    @Column(name = "gateway")
    String gateway;

    @Column(name = "netmask")
    String netmask;

    @Column(name = "vlan_tag")
    String broadcastUri;

    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    VpcGateway.Type type;

    @Column(name = "vpc_id")
    Long vpcId;

    @Column(name = "zone_id")
    long zoneId;

    @Column(name = "network_id")
    long networkId;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    State state;

    @Column(name = "source_nat")
    boolean sourceNat;

    @Column(name = "network_acl_id")
    long networkACLId;

    protected VpcGatewayVO() {
        uuid = UUID.randomUUID().toString();
    }

    /**
     * @param ip4Address
     * @param type
     * @param vpcId
     * @param zoneId
     * @param networkId
     * @param broadcastUri TODO
     * @param gateway TODO
     * @param netmask TODO
     * @param accountId TODO
     * @param domainId TODO
     * @param account_id
     * @param sourceNat
     */
    public VpcGatewayVO(String ip4Address, Type type, long vpcId, long zoneId, long networkId, String broadcastUri, String gateway, String netmask, long accountId,
            long domainId, boolean sourceNat, long networkACLId) {
        this.ip4Address = ip4Address;
        this.type = type;
        this.vpcId = vpcId;
        this.zoneId = zoneId;
        this.networkId = networkId;
        this.broadcastUri = broadcastUri;
        this.gateway = gateway;
        this.netmask = netmask;
        uuid = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.domainId = domainId;
        state = State.Creating;
        this.sourceNat = sourceNat;
        this.networkACLId = networkACLId;

    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getIp4Address() {
        return ip4Address;
    }

    @Override
    public VpcGateway.Type getType() {
        return type;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("VpcGateway[");
        buf.append(id).append("|").append(ip4Address.toString()).append("|").append(vpcId).append("]");
        return buf.toString();
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
    public String getBroadcastUri() {
        return broadcastUri;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean getSourceNat() {
        return sourceNat;
    }

    public void setNetworkACLId(long networkACLId) {
        this.networkACLId = networkACLId;
    }

    @Override
    public long getNetworkACLId() {
        return networkACLId;
    }

    @Override
    public Class<?> getEntityType() {
        return VpcGateway.class;
    }
}
