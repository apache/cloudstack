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
package com.cloud.vm;

/**
 * VirtualNetwork describes from a management level the
 * things needed to provide the network to the virtual
 * machine.
 */
public class VirtualNetwork {
    public enum Mode {
        None,
        Local,
        Static,
        Dhcp;
    }
    
    public enum Isolation {
        VNET,
        VLAN,
        OSWITCH,
    }
    
    /**
     * The gateway for this network.
     */
    public String gateway;
    
    /**
     * Netmask
     */
    public String netmask;
    
    /**
     * ip address.  null if mode is DHCP.
     */
    public String ip;
    
    /**
     * Mac Address.
     */
    public String mac;
    
    /**
     * rate limit on this network.  -1 if no limit.
     */
    public long rate;
    
    /**
     * tag for virtualization.
     */
    public String tag;
    
    /**
     * mode to acquire ip address.
     */
    public Mode mode;
    
    /**
     * Isolation method for networking.
     */
    public Isolation method;
    
    public boolean firewalled;
    
    public int[] openPorts;
    
    public int[] closedPorts;
}
