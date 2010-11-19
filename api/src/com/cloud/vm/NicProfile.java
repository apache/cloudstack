/**
 * 
 */
package com.cloud.vm;

import java.net.URI;

import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.Network;
import com.cloud.resource.Resource;
import com.cloud.resource.Resource.ReservationStrategy;

public class NicProfile {
    long id;
    BroadcastDomainType broadcastType;
    Mode mode;
    long vmId;
    String gateway;
    AddressFormat format;
    TrafficType trafficType;
    String ip4Address;
    String ip6Address;
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

    public void setVmId(long vmId) {
        this.vmId = vmId;
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

    public NicProfile(Nic nic, Network network, URI broadcastUri, URI isolationUri) {
        this.id = nic.getId();
        this.gateway = network.getGateway();
        this.mode = network.getMode();
        this.format = null;
        this.broadcastType = network.getBroadcastDomainType();
        this.trafficType = network.getTrafficType();
        this.ip4Address = nic.getIp4Address();
        this.ip6Address = null;
        this.macAddress = nic.getMacAddress();
        this.reservationId = nic.getReservationId();
        this.strategy = nic.getReservationStrategy();
        this.deviceId = nic.getDeviceId();
        this.defaultNic = nic.isDefaultNic();
        this.broadcastUri = broadcastUri;
        this.isolationUri = isolationUri;
    }

    public NicProfile(long id, BroadcastDomainType type, Mode mode, long vmId) {
        this.id = id;
        this.broadcastType = type;
        this.mode = mode;
        this.vmId = vmId;
    }
    
    public NicProfile(Resource.ReservationStrategy strategy, String ip4Address, String macAddress, String gateway, String netmask) {
        this.format = AddressFormat.Ip4;
        this.ip4Address = ip4Address;
        this.macAddress = macAddress;
        this.gateway = gateway;
        this.netmask = netmask;
        this.strategy = strategy;
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
}
