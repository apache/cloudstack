// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network;

import java.util.List;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.Manager;

/* ExternalLoadBalancerDeviceManager provides a abstract implementation for managing a external load balancer in device agnostic manner.
 * Device specific managers for external load balancers (like F5 and Netscaler) should be implemented as pluggable service extending 
 * ExternalLoadBalancerDeviceManager implementation. An implementation of device specific manager can override default behaviour if needed.
 */

public interface ExternalLoadBalancerDeviceManager extends Manager{


    public static final int DEFAULT_LOAD_BALANCER_CAPACITY = 50;

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
    public boolean manageGuestNetworkWithExternalLoadBalancer(boolean add, Network guestConfig) throws ResourceUnavailableException,
            InsufficientCapacityException;

    public List<LoadBalancerTO> getLBHealthChecks(Network network, List<? extends FirewallRule> rules)
            throws ResourceUnavailableException;
}
