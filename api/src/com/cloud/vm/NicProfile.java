/**
 * 
 */
package com.cloud.vm;

import com.cloud.network.Network.AddressFormat;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;

public class NicProfile {
    long id;
    BroadcastDomainType broadcastType;
    String cidr;
    Mode mode;
    long vmId;
    String gateway;
    int deviceId;
    AddressFormat format;
    TrafficType trafficType;
    String ip4Address;
    String ip6Address;
    String macAddress;
    
    public BroadcastDomainType getType() {
        return broadcastType;
    }

    public void setBroadcastType(BroadcastDomainType broadcastType) {
        this.broadcastType = broadcastType;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
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
        return id;
    }
    
    public long getVirtualMachineId() {
        return vmId;
    }
    
    public long getId() {
        return id;
    }

    public BroadcastDomainType getBroadcastType() {
        return broadcastType;
    }

    public String getCidr() {
        return cidr;
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

    public int getDeviceId() {
        return deviceId;
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

    public NicProfile(Nic nic, NetworkConfiguration network) {
        this.id = nic.getId();
        this.deviceId = nic.getDeviceId();
        this.cidr = network.getCidr();
        this.gateway = network.getGateway();
        this.mode = network.getMode();
        this.format = null;
        this.broadcastType = network.getBroadcastDomainType();
        this.trafficType = network.getTrafficType();
        this.ip4Address = nic.getIp4Address();
        this.ip6Address = null;
        this.macAddress = nic.getMacAddress();
    }

    public NicProfile(long id, BroadcastDomainType type, String cidr, Mode mode, long vmId) {
        this.id = id;
        this.broadcastType = type;
        this.cidr = cidr;
        this.mode = mode;
        this.vmId = vmId;
    }
    
    public NicProfile(String ip4Address, String macAddress, String gateway) {
        this.ip4Address = ip4Address;
        this.macAddress = macAddress;
        this.gateway = gateway;
    }
}
