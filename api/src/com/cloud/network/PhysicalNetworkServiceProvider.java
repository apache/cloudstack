/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

/**
 * 
 */
package com.cloud.network;

import java.util.List;

import com.cloud.network.Network.Service;



/**
 * This defines the specifics of a physical network service provider
 *  
 */
public interface PhysicalNetworkServiceProvider {

    public enum State {
        Disabled,
        Enabled,
        Shutdown;
    }
    
    long getId();

    State getState();

    long getPhysicalNetworkId();
    
    String getProviderName();

    long getDestinationPhysicalNetworkId();

    void setState(State state);

    boolean isLbServiceProvided();

    boolean isVpnServiceProvided();

    boolean isDhcpServiceProvided();

    boolean isDnsServiceProvided();

    boolean isGatewayServiceProvided();

    boolean isFirewallServiceProvided();

    boolean isSourcenatServiceProvided();

    boolean isUserdataServiceProvided();

    boolean isSecuritygroupServiceProvided();

    List<Service> getEnabledServices();

    String getUuid();
}
