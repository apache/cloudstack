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
 *  VirtualMachineRouter is a small VM instance that is started to 
 *  bridge internal and external traffic.
 */
public interface DomainRouter extends VirtualMachine {
	public enum Role {
		DHCP_FIREWALL_LB_PASSWD_USERDATA,
		DHCP_USERDATA
	}
    /**
     * @return the mac address for the router.
     */
    public String getGuestMacAddress();
    
    public String getGuestIpAddress();
    
    public String getPublicMacAddress();
    
    public String getPublicNetmask();
    
    public String getPrivateNetmask();
    
    public String getVnet();
    
    public String getVlanId();
    
    public String getZoneVlan();
    
    public String getGuestZoneMacAddress();
    
    /**
     * @return the gateway address for the router to use.
     */
    public String getGateway();
    
    /**
     * @return the ram size for this machine.
     */
    public int getRamSize();
    
    public String getGuestNetmask();
    
    /**
     * @return the public ip address used for source nat.
     */
    String getPublicIpAddress();
    
    String getDns1();
    String getDns2();
    String getDomain();
    
    /**
     * @return account id that the domain router belongs to.
     */
    long getAccountId();

    /**
     * @return domain id that the domain router belongs to.
     */
    long getDomainId();
    
    Role getRole();
    
    /**
     * @return the range of dhcp addresses served (start and end)
     */
    String[] getDhcpRange();
}
