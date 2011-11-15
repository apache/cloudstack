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

import java.util.List;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.rules.FirewallRule;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.Manager;

/* ExternalLoadBalancerDeviceManager provides a abstract implementation for managing a external load balancer in device agnostic manner.
 * Device specific managers for external load balancers (like F5 and Netscaler) should be implemented as pluggable service extending 
 * ExternalLoadBalancerDeviceManager implementation. An implementation of device specific manager can override default behaviour if needed.
 */

public interface ExternalLoadBalancerDeviceManager extends Manager{

    /**
     * adds a load balancer device in to a physical network
     * @param physicalNetworkId physical network id of the network in to which device to be added
     * @param url url encoding device IP and device configuration parameter
     * @param username username
     * @param password password
     * @param deviceName device name
     * @param server resource that will handle the commands specific to this device 
     * @return Host object for the device added
     */
    public ExternalLoadBalancerDeviceVO addExternalLoadBalancer(long physicalNetworkId, String url, String username, String password, String deviceName, ServerResource resource);

    /**
     * deletes load balancer device added in to a physical network
     * @param hostId
     * @return true if device successfully deleted
     */
    public boolean deleteExternalLoadBalancer(long hostId);

    /**
     * list external load balancers of given device name type added in to a physical network 
     * @param physicalNetworkId
     * @param deviceName
     * @return list of host objects for the external load balancers added in to the physical network
     */
    public List<Host> listExternalLoadBalancers(long physicalNetworkId, String deviceName);

    /**
     * finds a suitable load balancer device which can be used by this network
     * @param network guest network
     * @param dedicatedLb true if a dedicated load balancer is needed for this guest network 
     * @return ExternalLoadBalancerDeviceVO corresponding to the suitable device
     * @throws InsufficientCapacityException
     */
    public ExternalLoadBalancerDeviceVO findSuitableLoadBalancerForNetwork(Network network, boolean dedicatedLb) throws InsufficientCapacityException;

    /**
     * returns the load balancer device allocated for the guest network
     * @param network guest network id
     * @return ExternalLoadBalancerDeviceVO object corresponding the load balancer device assigned for this guest network 
     */
    public ExternalLoadBalancerDeviceVO getExternalLoadBalancerForNetwork(Network network);

    /**
     * applies load balancer rules
     * @param network guest network if
     * @param rules load balancer rules
     * @return true if successfully applied rules
     * @throws ResourceUnavailableException
     */
    public boolean applyLoadBalancerRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;

    /**
     * implements or shutdowns guest network on the load balancer device assigned to the guest network
     * @param add
     * @param guestConfig
     * @return
     * @throws ResourceUnavailableException
     * @throws InsufficientCapacityException
     */
    public boolean manageGuestNetworkWithExternalLoadBalancer(boolean add, Network guestConfig) throws ResourceUnavailableException, InsufficientCapacityException;

}
