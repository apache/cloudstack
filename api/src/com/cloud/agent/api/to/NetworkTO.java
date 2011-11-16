/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api.to;

import java.net.URI;

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;

/**
 * Transfer object to transfer network settings.
 */
public class NetworkTO {
    protected String uuid;
    protected String ip;
    protected String netmask;
    protected String gateway;
    protected String mac;
    protected String dns1;
    protected String dns2;
    protected BroadcastDomainType broadcastType;
    protected TrafficType type;
    protected URI broadcastUri;
    protected URI isolationUri;
    protected boolean isSecurityGroupEnabled;
    protected String name;

    public NetworkTO() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public BroadcastDomainType getBroadcastType() {
        return broadcastType;
    }

    public void setBroadcastType(BroadcastDomainType broadcastType) {
        this.broadcastType = broadcastType;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setType(TrafficType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSecurityGroupEnabled(boolean enabled) {
        this.isSecurityGroupEnabled = enabled;
    }

    /**
     * This constructor is usually for hosts where the other information are not important.
     * 
     * @param ip ip address
     * @param netmask netmask
     * @param mac mac address
     */
    public NetworkTO(String ip, String netmask, String mac) {
        this(ip, netmask, mac, null, null, null);
    }

    /**
     * This is the full constructor and should be used for VM's network as it contains
     * the full information about what is needed.
     * 
     * @param ip
     * @param vlan
     * @param netmask
     * @param mac
     * @param gateway
     * @param dns1
     * @param dns2
     */
    public NetworkTO(String ip, String netmask, String mac, String gateway, String dns1, String dns2) {
        this.ip = ip;
        this.netmask = netmask;
        this.mac = mac;
        this.gateway = gateway;
        this.dns1 = dns1;
        this.dns2 = dns2;
    }

    public String getIp() {
        return ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public String getMac() {
        return mac;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public TrafficType getType() {
        return type;
    }

    public URI getBroadcastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public URI getIsolationUri() {
        return isolationUri;
    }

    public void setIsolationuri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }

    public boolean isSecurityGroupEnabled() {
        return this.isSecurityGroupEnabled;
    }

}
