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

/**
 * Transfer object to transfer network settings.
 */
public class NetworkTO {
    private String ip;
    private String netmask;
    private String gateway;
    private String mac;
    private String dns1;
    private String dns2;
    private String vlan;
    
    protected NetworkTO() {
    }
    
    /**
     * This constructor is usually for hosts where the other information are not important.
     * 
     * @param ip ip address
     * @param netmask netmask
     * @param mac mac address
     */
    public NetworkTO(String ip, String netmask, String mac) {
        this(ip, null, netmask, mac, null, null, null);
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
    public NetworkTO(String ip, String vlan, String netmask, String mac, String gateway, String dns1, String dns2) {
        this.ip = ip;
        this.netmask = netmask;
        this.mac = mac;
        this.gateway = gateway;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.vlan = vlan;
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
}
