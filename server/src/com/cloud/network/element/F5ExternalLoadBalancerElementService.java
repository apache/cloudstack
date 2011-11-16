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

package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddExternalLoadBalancerCmd;
import com.cloud.api.commands.AddF5LoadBalancerCmd;
import com.cloud.api.commands.ConfigureF5LoadBalancerCmd;
import com.cloud.api.commands.DeleteExternalLoadBalancerCmd;
import com.cloud.api.commands.DeleteF5LoadBalancerCmd;
import com.cloud.api.commands.ListExternalLoadBalancersCmd;
import com.cloud.api.commands.ListF5LoadBalancerNetworksCmd;
import com.cloud.api.commands.ListF5LoadBalancersCmd;
import com.cloud.api.response.F5LoadBalancerResponse;
import com.cloud.host.Host;
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.Network;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.utils.component.PluggableService;

@SuppressWarnings("deprecation")
public interface F5ExternalLoadBalancerElementService extends PluggableService {

    /**
     * adds a F5 load balancer device in to a physical network
     * @param AddF5LoadBalancerCmd 
     * @return ExternalLoadBalancerDeviceVO object for the device added
     */
    public ExternalLoadBalancerDeviceVO addF5LoadBalancer(AddF5LoadBalancerCmd cmd);

    /**
     * removes a F5 load balancer device from a physical network
     * @param DeleteF5LoadBalancerCmd 
     * @return true if F5 load balancer device is successfully deleted
     */
    public boolean deleteF5LoadBalancer(DeleteF5LoadBalancerCmd cmd);

    /**
     * configures a F5 load balancer device added in a physical network
     * @param ConfigureF5LoadBalancerCmd
     * @return ExternalLoadBalancerDeviceVO for the device configured
     */
    public ExternalLoadBalancerDeviceVO configureF5LoadBalancer(ConfigureF5LoadBalancerCmd cmd);

    /**
     * lists all the load balancer devices added in to a physical network
     * @param ListF5LoadBalancersCmd
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network.
     */
    public List<ExternalLoadBalancerDeviceVO> listF5LoadBalancers(ListF5LoadBalancersCmd cmd);

    /**
     * lists all the guest networks using a F5 load balancer device
     * @param ListF5LoadBalancerNetworksCmd
     * @return list of the guest networks that are using this F5 load balancer
     */
    public List<? extends Network> listNetworks(ListF5LoadBalancerNetworksCmd cmd);

    public F5LoadBalancerResponse createF5LoadBalancerResponse(ExternalLoadBalancerDeviceVO lbDeviceVO);

    /* Deprecated API helper function */
    @Deprecated  // API helper function supported for backward compatibility
    public Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd);

    @Deprecated //  API helper function supported for backward compatibility
    public boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd);

    @Deprecated //  API helper function supported for backward compatibility
    public List<Host> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd);

    @Deprecated //  API helper function supported for backward compatibility
    public ExternalLoadBalancerResponse createExternalLoadBalancerResponse(Host externalLb);
}
