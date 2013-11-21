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
package com.cloud.vm;

import java.net.URI;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.Network;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.Nic.ReservationStrategy;

public class NicProfile implements InternalIdentity {
    long id;
    long networkId;
    BroadcastDomainType broadcastType;
    Mode mode;
    long vmId;
    String gateway;
    AddressFormat format;
    TrafficType trafficType;
    String ip4Address;
    String ip6Address;
    String ip6Gateway;
    String ip6Cidr;
    String macAddress;
    URI isolationUri;
    String netmask;
    URI broadcastUri;
    ReservationStrategy strategy;
    String reservationId;
    boolean defaultNic;
    Integer deviceId;
    String dns1;
    String dns2;
    String ip6Dns1;
    String ip6Dns2;
    Integer networkRate;
    boolean isSecurityGroupEnabled;
    String name;
    String requestedIpv4;
    String requestedIpv6;

    public String getDns1() {
        return dns1;
    }

    public String getName() {
        return name;
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

    public boolean isDefaultNic() {
        return defaultNic;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public URI getBroadCastUri() {
        return broadcastUri;
    }

    public void setIsolationUri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }

    public URI getIsolationUri() {
        return isolationUri;
    }

    public void setStrategy(ReservationStrategy strategy) {
        this.strategy = strategy;
    }

    public BroadcastDomainType getType() {
        return broadcastType;
    }

    public void setBroadcastType(BroadcastDomainType broadcastType) {
        this.broadcastType = broadcastType;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public void setDefaultNic(boolean defaultNic) {
        this.defaultNic = defaultNic;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setFormat(AddressFormat format) {
        this.format = format;
    }

    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    public Mode getMode() {
        return mode;
    }

    public long getNetworkId() {
        return networkId;
    }

    public long getVirtualMachineId() {
        return vmId;
    }

    @Override
    public long getId() {
        return id;
    }

    public BroadcastDomainType getBroadcastType() {
        return broadcastType;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public long getVmId() {
        return vmId;
    }

    public String getGateway() {
        return gateway;
    }

    public AddressFormat getFormat() {
        return format;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setIp4Address(String ip4Address) {
        this.ip4Address = ip4Address;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    public ReservationStrategy getStrategy() {
        return strategy;
    }

    public NicProfile(Nic nic, Network network, URI broadcastUri, URI isolationUri, Integer networkRate, boolean isSecurityGroupEnabled, String name) {
        this.id = nic.getId();
        this.networkId = network.getId();
        this.gateway = nic.getGateway();
        this.mode = network.getMode();
        this.broadcastType = network.getBroadcastDomainType();
        this.trafficType = network.getTrafficType();
        this.ip4Address = nic.getIp4Address();
        this.format = nic.getAddressFormat();
        this.ip6Address = nic.getIp6Address();
        this.macAddress = nic.getMacAddress();
        this.reservationId = nic.getReservationId();
        this.strategy = nic.getReservationStrategy();
        this.deviceId = nic.getDeviceId();
        this.defaultNic = nic.isDefaultNic();
        this.broadcastUri = broadcastUri;
        this.isolationUri = isolationUri;
        this.netmask = nic.getNetmask();
        this.isSecurityGroupEnabled = isSecurityGroupEnabled;
        this.vmId = nic.getInstanceId();
        this.name = name;
        this.ip6Cidr = nic.getIp6Cidr();
        this.ip6Gateway = nic.getIp6Gateway();

        if (networkRate != null) {
            this.networkRate = networkRate;
        }
    }

    public NicProfile(ReservationStrategy strategy, String ip4Address, String macAddress, String gateway, String netmask) {
        this.format = AddressFormat.Ip4;
        this.ip4Address = ip4Address;
        this.macAddress = macAddress;
        this.gateway = gateway;
        this.netmask = netmask;
        this.strategy = strategy;
    }

    public NicProfile(String requestedIpv4, String requestedIpv6) {
        this.requestedIpv4 = requestedIpv4;
        this.requestedIpv6 = requestedIpv6;
    }

    public NicProfile() {
    }

    public ReservationStrategy getReservationStrategy() {
        return strategy;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public boolean isSecurityGroupEnabled() {
        return this.isSecurityGroupEnabled;
    }

    public void setSecurityGroupEnabled(boolean enabled) {
        this.isSecurityGroupEnabled = enabled;
    }

    public String getRequestedIpv4() {
        return requestedIpv4;
    }

    public void deallocate() {
        this.gateway = null;
        this.mode = null;
        this.format = null;
        this.broadcastType = null;
        this.trafficType = null;
        this.ip4Address = null;
        this.ip6Address = null;
        this.macAddress = null;
        this.reservationId = null;
        this.strategy = null;
        this.deviceId = null;
        this.broadcastUri = null;
        this.isolationUri = null;
        this.netmask = null;
        this.dns1 = null;
        this.dns2 = null;
    }

    @Override
    public String toString() {
        return new StringBuilder("NicProfile[").append(id)
            .append("-")
            .append(vmId)
            .append("-")
            .append(reservationId)
            .append("-")
            .append(ip4Address)
            .append("-")
            .append(broadcastUri)
            .toString();
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

    public String getRequestedIpv6() {
        return requestedIpv6;
    }

    public void setRequestedIpv6(String requestedIpv6) {
        this.requestedIpv6 = requestedIpv6;
    }

    public String getIp6Dns1() {
        return ip6Dns1;
    }

    public void setIp6Dns1(String ip6Dns1) {
        this.ip6Dns1 = ip6Dns1;
    }

    public String getIp6Dns2() {
        return ip6Dns2;
    }

    public void setIp6Dns2(String ip6Dns2) {
        this.ip6Dns2 = ip6Dns2;
    }

}
