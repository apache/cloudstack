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

import com.cloud.network.Network;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.Nic.ReservationStrategy;
import org.apache.cloudstack.api.InternalIdentity;

import java.io.Serializable;
import java.net.URI;

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
    int mtu;
    Integer networkRate;
    boolean isSecurityGroupEnabled;

    // IPv4
    String iPv4Address;
    String iPv4Netmask;
    String iPv4Gateway;
    String iPv4Dns1;
    String iPv4Dns2;
    String requestedIPv4;

    // IPv6
    String iPv6Address;
    String iPv6Gateway;
    String iPv6Cidr;
    String iPv6Dns1;
    String iPv6Dns2;
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

        iPv4Address = nic.getIPv4Address();
        iPv4Netmask = nic.getIPv4Netmask();
        iPv4Gateway = nic.getIPv4Gateway();

        iPv6Address = nic.getIPv6Address();
        iPv6Gateway = nic.getIPv6Gateway();
        iPv6Cidr = nic.getIPv6Cidr();

        macAddress = nic.getMacAddress();
        reservationId = nic.getReservationId();
        strategy = nic.getReservationStrategy();
        deviceId = nic.getDeviceId();
        defaultNic = nic.isDefaultNic();
        mtu = nic.getMtu();
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

    public NicProfile(String requestedIPv4, String requestedIPv6, String requestedMacAddress) {
        this(requestedIPv4, requestedIPv6);
        this.macAddress = requestedMacAddress;
    }

    public NicProfile(String requestedIPv4, String requestedIPv6, String requestedMacAddress, int mtu) {
        this(requestedIPv4, requestedIPv6, requestedMacAddress);
        this.mtu = mtu;
    }

    public NicProfile(ReservationStrategy strategy, String iPv4Address, String macAddress, String iPv4gateway, String iPv4netmask) {
        format = AddressFormat.Ip4;
        this.iPv4Address = iPv4Address;
        this.iPv4Gateway = iPv4gateway;
        this.iPv4Netmask = iPv4netmask;
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

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
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
        return iPv4Address;
    }

    public void setIPv4Address(String ipv4Address) {
        this.iPv4Address = ipv4Address;
    }

    public String getIPv4Netmask() {
        return iPv4Netmask;
    }

    public void setIPv4Netmask(String ipv4Netmask) {
        this.iPv4Netmask = ipv4Netmask;
    }

    public String getIPv4Gateway() {
        return iPv4Gateway;
    }

    public void setIPv4Gateway(String ipv4Gateway) {
        this.iPv4Gateway = ipv4Gateway;
    }

    public String getIPv4Dns1() {
        return iPv4Dns1;
    }

    public void setIPv4Dns1(String ipv4Dns1) {
        this.iPv4Dns1 = ipv4Dns1;
    }

    public String getIPv4Dns2() {
        return iPv4Dns2;
    }

    public void setIPv4Dns2(String ipv4Dns2) {
        this.iPv4Dns2 = ipv4Dns2;
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
        return iPv6Address;
    }

    public void setIPv6Address(String ipv6Address) {
        this.iPv6Address = ipv6Address;
    }

    public String getIPv6Gateway() {
        return iPv6Gateway;
    }

    public void setIPv6Gateway(String ipv6Gateway) {
        this.iPv6Gateway = ipv6Gateway;
    }

    public String getIPv6Cidr() {
        return iPv6Cidr;
    }

    public void setIPv6Cidr(String ipv6Cidr) {
        this.iPv6Cidr = ipv6Cidr;
    }

    public String getIPv6Dns1() {
        return iPv6Dns1;
    }

    public void setIPv6Dns1(String ipv6Dns1) {
        this.iPv6Dns1 = ipv6Dns1;
    }

    public String getIPv6Dns2() {
        return iPv6Dns2;
    }

    public void setIPv6Dns2(String ipv6Dns2) {
        this.iPv6Dns2 = ipv6Dns2;
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

        iPv4Address = null;
        iPv4Netmask = null;
        iPv4Gateway = null;
        iPv4Dns1 = null;
        iPv4Dns2 = null;

        iPv6Address = null;
        iPv6Gateway = null;
        iPv6Cidr = null;
        iPv6Dns1 = null;
        iPv6Dns2 = null;

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
                .append(iPv4Address)
                .append("-")
                .append(broadcastUri)
                .append("-")
                .append(mtu)
                .toString();
    }
}
