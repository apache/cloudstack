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
package com.cloud.network;

/**
 * Network includes all of the enums used within networking.
 *
 */
public class Network {
    /**
     * Different ways to assign ip address to this network.
     */
    public enum Mode {
        None,
        Static,
        Dhcp,
        ExternalDhcp;
    };
    
    public enum AddressFormat {
        Ip4,
        Ip6
    }

    /**
     * Different types of broadcast domains. 
     */
    public enum BroadcastDomainType {
        Native,
        Vlan,
        Vswitch,
        LinkLocal,
        Vnet;
    };
    
    /**
     * Different types of network traffic in the data center. 
     */
    public enum TrafficType {
        Public,
        Guest,
        Storage,
        Control,
        Vpn,
        Management
    };
    
    public enum IsolationType {
        None,
        Ec2,
        Vlan,
        Vswitch,
        Vnet;
    }
}
