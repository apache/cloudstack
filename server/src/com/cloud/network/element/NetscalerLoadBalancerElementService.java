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
import com.cloud.api.commands.AddNetscalerLoadBalancerCmd;
import com.cloud.api.commands.ConfigureNetscalerLoadBalancerCmd;
import com.cloud.api.commands.DeleteNetscalerLoadBalancerCmd;
import com.cloud.api.commands.ListNetscalerLoadBalancerNetworksCmd;
import com.cloud.api.commands.ListNetscalerLoadBalancersCmd;
import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.Network;
import com.cloud.utils.component.PluggableService;

public interface NetscalerLoadBalancerElementService extends PluggableService {

    /**
     * adds a Netscaler load balancer device in to a physical network
     * @param AddNetscalerLoadBalancerCmd 
     * @return ExternalLoadBalancerDeviceVO object for the device added
     */
    public ExternalLoadBalancerDeviceVO addNetscalerLoadBalancer(AddNetscalerLoadBalancerCmd cmd);

    /**
     * removes a Netscaler load balancer device from a physical network
     * @param DeleteNetscalerLoadBalancerCmd 
     * @return ExternalLoadBalancerDeviceVO object for the device deleted
     */
    public boolean deleteNetscalerLoadBalancer(DeleteNetscalerLoadBalancerCmd cmd);

    /**
     * configures a Netscaler load balancer device added in a physical network
     * @param ConfigureNetscalerLoadBalancerCmd
     * @return ExternalLoadBalancerDeviceVO for the device configured
     */
    public ExternalLoadBalancerDeviceVO configureNetscalerLoadBalancer(ConfigureNetscalerLoadBalancerCmd cmd);

    /**
     * lists all the load balancer devices added in to a physical network
     * @param physicalNetworkId physical Network Id
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network.
     */
    public List<ExternalLoadBalancerDeviceVO> listNetscalerLoadBalancers(ListNetscalerLoadBalancersCmd cmd);

    /**
     * lists all the guest networks using a Netscaler load balancer device
     * @param lbDeviceId external load balancer device Id
     * @return list of the guest networks that are using this Netscaler load balancer
     */
    public List<? extends Network> listNetworks(ListNetscalerLoadBalancerNetworksCmd cmd);

    /**
     * creates API response object for netscaler load balancers
     * @param lbDeviceVO external load balancer VO object
     * @return NetscalerLoadBalancerResponse
     */
    public NetscalerLoadBalancerResponse createNetscalerLoadBalancerResponse(ExternalLoadBalancerDeviceVO lbDeviceVO);
}
