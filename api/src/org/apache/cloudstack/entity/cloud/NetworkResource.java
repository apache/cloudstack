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
package org.apache.cloudstack.entity.cloud;

import java.net.URI;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.entity.CloudResource;
import org.apache.cloudstack.entity.identity.AccountResource;
import org.apache.cloudstack.entity.infrastructure.DataCenterResource;

import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;

/**
 * Network resource entity
 */
public class NetworkResource extends CloudResource {

    // attributes
    //TODO: need to understand what is the relationship between gateway, cidr and those similar information stored in
    // NicResource??
    private String name;
    private String displayText;
    private Mode mode;
    private BroadcastDomainType broadcastDomainType;
    private TrafficType trafficType;
    private URI broadcastUri;
    private State state;
    private String dns1;
    private String dns2;
    private Network.GuestType guestType;
    private ControlledEntity.ACLType aclType;
    private String ip4Gateway;
    private String ip4Cidr;
    private String ip6Gateway;
    private String ip6Cidr;
    // "cidr" is the Cloudstack managed address space, all CloudStack managed vms get IP address from "cidr",
    // In general "cidr" also serves as the network CIDR
    // But in case IP reservation is configured for a Guest network, "networkcidr" is the Effective network CIDR for that network,
    // "cidr" will still continue to be the effective address space for CloudStack managed vms in that Guest network

    // "networkcidr" is the network CIDR of the guest network which uses IP reservation.
    // It is the summation of "cidr" and the reservedIPrange(the address space used for non CloudStack purposes).
    // For networks not configured with IP reservation, "networkcidr" is always null
    private String networkCidr;
    private boolean specifyIpRanges; // is this flag for IP reservation?
    private String networkDomain;

    // relationship
    private PhysicalNetworkResource physicalNetwork;
    private NetworkOfferingResource networkOffering;
    private DataCenterResource dataCenter;
    private AccountResource account;



    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDisplayText() {
        return displayText;
    }
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }
    public Mode getMode() {
        return mode;
    }
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    public BroadcastDomainType getBroadcastDomainType() {
        return broadcastDomainType;
    }
    public void setBroadcastDomainType(BroadcastDomainType broadcastDomainType) {
        this.broadcastDomainType = broadcastDomainType;
    }
    public TrafficType getTrafficType() {
        return trafficType;
    }
    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }
    public URI getBroadcastUri() {
        return broadcastUri;
    }
    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }
    public String getDns1() {
        return dns1;
    }
    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }
    public String getDns2() {
        return dns2;
    }
    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }
    public Network.GuestType getGuestType() {
        return guestType;
    }
    public void setGuestType(Network.GuestType guestType) {
        this.guestType = guestType;
    }
    public ControlledEntity.ACLType getAclType() {
        return aclType;
    }
    public void setAclType(ControlledEntity.ACLType aclType) {
        this.aclType = aclType;
    }
    public String getIp4Gateway() {
        return ip4Gateway;
    }
    public void setIp4Gateway(String ip4Gateway) {
        this.ip4Gateway = ip4Gateway;
    }
    public String getIp4Cidr() {
        return ip4Cidr;
    }
    public void setIp4Cidr(String ip4Cidr) {
        this.ip4Cidr = ip4Cidr;
    }
    public String getIp6Gateway() {
        return ip6Gateway;
    }
    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }
    public String getIp6Cidr() {
        return ip6Cidr;
    }
    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }
    public String getNetworkCidr() {
        return networkCidr;
    }
    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }
    public boolean isSpecifyIpRanges() {
        return specifyIpRanges;
    }
    public void setSpecifyIpRanges(boolean specifyIpRanges) {
        this.specifyIpRanges = specifyIpRanges;
    }
    public String getNetworkDomain() {
        return networkDomain;
    }
    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }
    public PhysicalNetworkResource getPhysicalNetwork() {
        return physicalNetwork;
    }
    public void setPhysicalNetwork(PhysicalNetworkResource physicalNetwork) {
        this.physicalNetwork = physicalNetwork;
    }
    public NetworkOfferingResource getNetworkOffering() {
        return networkOffering;
    }
    public void setNetworkOffering(NetworkOfferingResource networkOffering) {
        this.networkOffering = networkOffering;
    }
    public DataCenterResource getDataCenter() {
        return dataCenter;
    }
    public void setDataCenter(DataCenterResource dataCenter) {
        this.dataCenter = dataCenter;
    }
    public AccountResource getAccount() {
        return account;
    }
    public void setAccount(AccountResource account) {
        this.account = account;
    }


}
