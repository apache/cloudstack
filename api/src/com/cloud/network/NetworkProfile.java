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
import org.apache.cloudstack.api.InternalIdentity;

public class NetworkProfile implements Network {
    private long id;
    private String uuid;
    private long dataCenterId;
    private long ownerId;
    private long domainId;
    private String dns1;
    private String dns2;
    private URI broadcastUri;
    private State state;
    private String name;
    private Mode mode;
    private BroadcastDomainType broadcastDomainType;
    private TrafficType trafficType;
    private String gateway;
    private String cidr;
    private long networkOfferingId;
    private long related;
    private String displayText;
    private String reservationId;
    private String networkDomain;
    private Network.GuestType guestType;
    private Long physicalNetworkId;
    private ACLType aclType;
    private boolean restartRequired;
    private boolean specifyIpRanges;
    private Long vpcId;

    public NetworkProfile(Network network) {
        this.id = network.getId();
        this.uuid = network.getUuid();
        this.broadcastUri = network.getBroadcastUri();
        this.dataCenterId = network.getDataCenterId();
        this.ownerId = network.getAccountId();
        this.state = network.getState();
        this.name = network.getName();
        this.mode = network.getMode();
        this.broadcastDomainType = network.getBroadcastDomainType();
        this.trafficType = network.getTrafficType();
        this.gateway = network.getGateway();
        this.cidr = network.getCidr();
        this.networkOfferingId = network.getNetworkOfferingId();
        this.related = network.getRelated();
        this.displayText = network.getDisplayText();
        this.reservationId = network.getReservationId();
        this.networkDomain = network.getNetworkDomain();
        this.domainId = network.getDomainId();
        this.guestType = network.getGuestType();
        this.physicalNetworkId = network.getPhysicalNetworkId();
        this.aclType = network.getAclType();
        this.restartRequired = network.isRestartRequired();
        this.specifyIpRanges = network.getSpecifyIpRanges();
        this.vpcId = network.getVpcId();
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
    public Long getVpcId() {
        return vpcId;
    }

}
