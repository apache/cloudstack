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

import java.net.URI;

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;

public class NetworkProfile implements Network {
    private final long id;
    private final String uuid;
    private final long dataCenterId;
    private final long ownerId;
    private final long domainId;
    private String dns1;
    private String dns2;
    private URI broadcastUri;
    private final State state;
    private boolean isRedundant;
    private boolean isRollingRestart = false;
    private final String name;
    private final Mode mode;
    private final BroadcastDomainType broadcastDomainType;
    private TrafficType trafficType;
    private final String gateway;
    private final String cidr;
    private final String networkCidr;
    private final String ip6Gateway;
    private final String ip6Cidr;
    private final long networkOfferingId;
    private final long related;
    private final String displayText;
    private final String reservationId;
    private final String networkDomain;
    private final Network.GuestType guestType;
    private Long physicalNetworkId;
    private final ACLType aclType;
    private final boolean restartRequired;
    private final boolean specifyIpRanges;
    private final Long vpcId;
    private final boolean displayNetwork;
    private Long networkAclId;
    private final String guruName;
    private boolean strechedL2Subnet;
    private String externalId;

    public NetworkProfile(Network network) {
        id = network.getId();
        uuid = network.getUuid();
        broadcastUri = network.getBroadcastUri();
        dataCenterId = network.getDataCenterId();
        ownerId = network.getAccountId();
        state = network.getState();
        name = network.getName();
        mode = network.getMode();
        broadcastDomainType = network.getBroadcastDomainType();
        trafficType = network.getTrafficType();
        gateway = network.getGateway();
        cidr = network.getCidr();
        networkCidr = network.getNetworkCidr();
        ip6Gateway = network.getIp6Gateway();
        ip6Cidr = network.getIp6Cidr();
        networkOfferingId = network.getNetworkOfferingId();
        related = network.getRelated();
        displayText = network.getDisplayText();
        reservationId = network.getReservationId();
        networkDomain = network.getNetworkDomain();
        domainId = network.getDomainId();
        guestType = network.getGuestType();
        physicalNetworkId = network.getPhysicalNetworkId();
        aclType = network.getAclType();
        restartRequired = network.isRestartRequired();
        specifyIpRanges = network.getSpecifyIpRanges();
        vpcId = network.getVpcId();
        displayNetwork = network.getDisplayNetwork();
        networkAclId = network.getNetworkACLId();
        guruName = network.getGuruName();
        strechedL2Subnet = network.isStrechedL2Network();
        isRedundant = network.isRedundant();
        isRollingRestart = network.isRollingRestart();
        externalId = network.getExternalId();
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    @Override
    public String getGuruName() {
        return guruName;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    @Override
    public URI getBroadcastUri() {
        return broadcastUri;
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
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getAccountId() {
        return ownerId;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isRedundant() {
        return this.isRedundant;
    }

    @Override
    public boolean isRollingRestart() {
        return isRollingRestart;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public BroadcastDomainType getBroadcastDomainType() {
        return broadcastDomainType;
    }

    @Override
    public TrafficType getTrafficType() {
        return trafficType;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    @Override
    public String getCidr() {
        return cidr;
    }

    @Override
    public String getNetworkCidr() {
        return networkCidr;
    }

    @Override
    public long getNetworkOfferingId() {
        return networkOfferingId;
    }

    @Override
    public long getRelated() {
        return related;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public String getReservationId() {
        return reservationId;
    }

    @Override
    public String getNetworkDomain() {
        return networkDomain;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public Network.GuestType getGuestType() {
        return guestType;
    }

    @Override
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    @Override
    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    @Override
    public ACLType getAclType() {
        return aclType;
    }

    @Override
    public boolean isRestartRequired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getSpecifyIpRanges() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getDisplayNetwork() {
        return displayNetwork;
    }

    @Override
    public boolean isDisplay(){
        return displayNetwork;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    @Override
    public Long getNetworkACLId() {
        return networkAclId;
    }

    @Override
    public void setNetworkACLId(Long networkACLId) {
        networkAclId = networkACLId;
    }

    @Override
    public void setTrafficType(TrafficType type) {
        trafficType = type;
    }

    @Override
    public String getIp6Gateway() {
        return ip6Gateway;
    }

    @Override
    public String getIp6Cidr() {
        return ip6Cidr;
    }

    @Override
    public Class<?> getEntityType() {
        return Network.class;
    }

    @Override
    public boolean isStrechedL2Network() {
        return false;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public PVlanType getPvlanType() {
        return null;
    }

    @Override
    public String getRouterIp() {
        return null;
    }

    @Override
    public String getRouterIpv6() {
        return null;
    }

}
