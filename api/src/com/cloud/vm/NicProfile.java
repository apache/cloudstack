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

import java.io.Serializable;
import java.net.URI;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.Network;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.Nic.ReservationStrategy;

public class NicProfile implements InternalIdentity, Serializable {
    private static final long serialVersionUID = 4997005771736090304L;

    long id;
    long networkId;
    long vmId;
    String reservationId;
    Integer deviceId;

    String name;
    String uuid;

    String macAddress;
    BroadcastDomainType broadcastType;
    Mode mode;
    AddressFormat format;
    TrafficType trafficType;
    URI isolationUri;
    URI broadcastUri;
    ReservationStrategy strategy;
    boolean defaultNic;
    Integer networkRate;
    boolean isSecurityGroupEnabled;

    // IPv4
    String ipv4Address;
    String ipv4Netmask;
    String ipv4Gateway;
    String ipv4Dns1;
    String ipv4Dns2;
    String requestedIPv4;

    // IPv6
    String ipv6Address;
    String ipv6Gateway;
    String ipv6Cidr;
    String ipv6Dns1;
    String ipv6Dns2;
    String requestedIPv6;

    //
    // CONSTRUCTORS
    //

    public NicProfile() {
    }

    public NicProfile(Nic nic, Network network, URI broadcastUri, URI isolationUri, Integer networkRate, boolean isSecurityGroupEnabled, String name) {
        id = nic.getId();
        networkId = network.getId();
        mode = network.getMode();
        broadcastType = network.getBroadcastDomainType();
        trafficType = network.getTrafficType();
        format = nic.getAddressFormat();

        ipv4Address = nic.getIPv4Address();
        ipv4Netmask = nic.getIPv4Netmask();
        ipv4Gateway = nic.getIPv4Gateway();

        ipv6Address = nic.getIPv6Address();
        ipv6Gateway = nic.getIPv6Gateway();
        ipv6Cidr = nic.getIPv6Cidr();

        macAddress = nic.getMacAddress();
        reservationId = nic.getReservationId();
        strategy = nic.getReservationStrategy();
        deviceId = nic.getDeviceId();
        defaultNic = nic.isDefaultNic();
        this.broadcastUri = broadcastUri;
        this.isolationUri = isolationUri;

        this.isSecurityGroupEnabled = isSecurityGroupEnabled;
        vmId = nic.getInstanceId();
        this.name = name;
        uuid = nic.getUuid();

        if (networkRate != null) {
            this.networkRate = networkRate;
        }
    }

    public NicProfile(String requestedIPv4, String requestedIPv6) {
        this.requestedIPv4 = requestedIPv4;
        this.requestedIPv6 = requestedIPv6;
    }

    public NicProfile(ReservationStrategy strategy, String ipv4Address, String macAddress, String ipv4gateway, String ipv4netmask) {
        format = AddressFormat.Ip4;
        this.ipv4Address = ipv4Address;
        this.ipv4Gateway = ipv4gateway;
        this.ipv4Netmask = ipv4netmask;
        this.macAddress = macAddress;
        this.strategy = strategy;
    }

    //
    // GET & SET GENERAL
    //

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworId(long networkId){
        this.networkId = networkId;
    }

    public long getVirtualMachineId() {
        return vmId;
    }

    public void setVirtualMachineId(long virtualMachineId) {
        this.vmId = virtualMachineId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public BroadcastDomainType getBroadcastType() {
        return broadcastType;
    }

    public void setBroadcastType(BroadcastDomainType broadcastType) {
        this.broadcastType = broadcastType;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public AddressFormat getFormat() {
        return format;
    }

    public void setFormat(AddressFormat format) {
        this.format = format;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    public URI getIsolationUri() {
        return isolationUri;
    }

    public void setIsolationUri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }

    public URI getBroadCastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public ReservationStrategy getReservationStrategy() {
        return strategy;
    }

    public void setReservationStrategy(ReservationStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean isDefaultNic() {
        return defaultNic;
    }

    public void setDefaultNic(boolean defaultNic) {
        this.defaultNic = defaultNic;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    public void setNetworkRate(Integer networkRate) {
        this.networkRate = networkRate;
    }

    public boolean isSecurityGroupEnabled() {
        return isSecurityGroupEnabled;
    }

    public void setSecurityGroupEnabled(boolean enabled) {
        isSecurityGroupEnabled = enabled;
    }

    //
    // GET & SET IPv4
    //

    public String getIPv4Address() {
        return ipv4Address;
    }

    public void setIPv4Address(String ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    public String getIPv4Netmask() {
        return ipv4Netmask;
    }

    public void setIPv4Netmask(String ipv4Netmask) {
        this.ipv4Netmask = ipv4Netmask;
    }

    public String getIPv4Gateway() {
        return ipv4Gateway;
    }

    public void setIPv4Gateway(String ipv4Gateway) {
        this.ipv4Gateway = ipv4Gateway;
    }

    public String getIPv4Dns1() {
        return ipv4Dns1;
    }

    public void setIPv4Dns1(String ipv4Dns1) {
        this.ipv4Dns1 = ipv4Dns1;
    }

    public String getIPv4Dns2() {
        return ipv4Dns2;
    }

    public void setIPv4Dns2(String ipv4Dns2) {
        this.ipv4Dns2 = ipv4Dns2;
    }

    public String getRequestedIPv4() {
        return requestedIPv4;
    }

    public void setRequestedIPv4(String requestedIPv4) {
        this.requestedIPv4 = requestedIPv4;
    }

    //
    // GET & SET IPv6
    //

    public String getIPv6Address() {
        return ipv6Address;
    }

    public void setIPv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public String getIPv6Gateway() {
        return ipv6Gateway;
    }

    public void setIPv6Gateway(String ipv6Gateway) {
        this.ipv6Gateway = ipv6Gateway;
    }

    public String getIPv6Cidr() {
        return ipv6Cidr;
    }

    public void setIPv6Cidr(String ipv6Cidr) {
        this.ipv6Cidr = ipv6Cidr;
    }

    public String getIPv6Dns1() {
        return ipv6Dns1;
    }

    public void setIPv6Dns1(String ipv6Dns1) {
        this.ipv6Dns1 = ipv6Dns1;
    }

    public String getIPv6Dns2() {
        return ipv6Dns2;
    }

    public void setIPv6Dns2(String ipv6Dns2) {
        this.ipv6Dns2 = ipv6Dns2;
    }

    public String getRequestedIPv6() {
        return requestedIPv6;
    }

    public void setRequestedIPv6(String requestedIPv6) {
        this.requestedIPv6 = requestedIPv6;
    }

    //
    // OTHER METHODS
    //

    public void deallocate() {
        mode = null;
        format = null;
        broadcastType = null;
        trafficType = null;

        ipv4Address = null;
        ipv4Netmask = null;
        ipv4Gateway = null;
        ipv4Dns1 = null;
        ipv4Dns2 = null;

        ipv6Address = null;
        ipv6Gateway = null;
        ipv6Cidr = null;
        ipv6Dns1 = null;
        ipv6Dns2 = null;

        macAddress = null;
        reservationId = null;
        strategy = null;
        deviceId = null;
        broadcastUri = null;
        isolationUri = null;

    }

    @Override
    public String toString() {
        return new StringBuilder("NicProfile[").append(id)
                .append("-")
                .append(vmId)
                .append("-")
                .append(reservationId)
                .append("-")
                .append(ipv4Address)
                .append("-")
                .append(broadcastUri)
                .toString();
    }
}