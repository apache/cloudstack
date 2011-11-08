/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

import java.util.ArrayList;
import java.util.List;
import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.AddExternalLoadBalancerCmd;
import com.cloud.api.commands.AddNetworkDeviceCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.DeleteExternalLoadBalancerCmd;
import com.cloud.api.commands.DeleteNetworkDeviceCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.api.commands.ListExternalLoadBalancersCmd;
import com.cloud.api.commands.ListNetworkDeviceCmd;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.rules.FirewallRule;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.server.api.response.NetworkDeviceResponse;
import com.cloud.utils.component.Manager;

public interface ExternalNetworkDeviceManager extends Manager {
    
    public static class NetworkDevice {
        private String _name;
        private String _provider;
        private static List<NetworkDevice> supportedNetworkDevices = new ArrayList<NetworkDevice>();

        public static final NetworkDevice ExternalDhcp = new NetworkDevice("ExternalDhcp", null);
        public static final NetworkDevice PxeServer = new NetworkDevice("PxeServer", null);
        public static final NetworkDevice NetscalerLoadBalancer = new NetworkDevice("NetscalerLoadBalancer", Network.Provider.Netscaler.getName());
        public static final NetworkDevice F5BigIpLoadBalancer = new NetworkDevice("F5BigIpLoadBalancer", Network.Provider.F5BigIp.getName());
        public static final NetworkDevice JuniperSRXFirewall = new NetworkDevice("JuniperSRXFirewall", Network.Provider.JuniperSRX.getName());
        
        public NetworkDevice(String deviceName, String ntwkServiceprovider) {
            _name = deviceName;
            _provider = ntwkServiceprovider;
            supportedNetworkDevices.add(this);
        }
        
        public String getName() {
            return _name;
        }

        public String getNetworkServiceProvder() {
            return _provider;
        }

        public static NetworkDevice getNetworkDevice(String devicerName) {
            for (NetworkDevice device : supportedNetworkDevices) {
                if (device.getName().equalsIgnoreCase(devicerName)) {
                    return device;
                }
            }
            return null;
        }
    }

    public static class LoadBalancerCapacityType {
        private String _capacityType;
        public static final LoadBalancerCapacityType Throughput = new LoadBalancerCapacityType("Throughput");
        public static final LoadBalancerCapacityType publicIPOwned = new LoadBalancerCapacityType("publicIPOwned");

        public LoadBalancerCapacityType(String capacityType) {
            _capacityType = capacityType;
        }

        public String getCapacityType() {
            return _capacityType;
        }
    }

    public Host addNetworkDevice(AddNetworkDeviceCmd cmd);
    
    public NetworkDeviceResponse getApiResponse(Host device);
    
    public List<Host> listNetworkDevice(ListNetworkDeviceCmd cmd);
    
    public boolean deleteNetworkDevice(DeleteNetworkDeviceCmd cmd);
    
    // External Firewall methods

    public Host addExternalFirewall(AddExternalFirewallCmd cmd);

    public boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd);
    
    public List<Host> listExternalFirewalls(ListExternalFirewallsCmd cmd);

    public ExternalFirewallResponse createExternalFirewallResponse(Host externalFirewall);
        
    public boolean manageGuestNetworkWithExternalFirewall(boolean add, Network network) throws ResourceUnavailableException, InsufficientCapacityException;
    
    public boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;

    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddresses) throws ResourceUnavailableException;

    public boolean manageRemoteAccessVpn(boolean create, Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException;
    
    public boolean manageRemoteAccessVpnUsers(Network network, RemoteAccessVpn vpn, List<? extends VpnUser> users) throws ResourceUnavailableException;

    // External Load balancer methods    
    
    public Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd);

    public boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd);

    public List<Host> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd);
    
    public ExternalLoadBalancerResponse createExternalLoadBalancerResponse(Host externalLoadBalancer);
    
    public boolean manageGuestNetworkWithExternalLoadBalancer(boolean add, Network guestConfig) throws ResourceUnavailableException, InsufficientCapacityException;
    
    public boolean applyLoadBalancerRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;
    
    // General methods
    
    public int getVlanOffset(long physicalNetworkId, int vlanTag);
    
    public int getGloballyConfiguredCidrSize();
}
