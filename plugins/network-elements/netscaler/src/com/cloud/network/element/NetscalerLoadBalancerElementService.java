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
package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddNetscalerLoadBalancerCmd;
import com.cloud.api.commands.ConfigureNetscalerLoadBalancerCmd;
import com.cloud.api.commands.DeleteNetscalerLoadBalancerCmd;
import com.cloud.api.commands.ListNetscalerLoadBalancerNetworksCmd;
import com.cloud.api.commands.ListNetscalerLoadBalancersCmd;
import com.cloud.api.commands.RegisterNetscalerControlCenterCmd;
import com.cloud.api.commands.RegisterServicePackageCmd;
import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.api.response.NetScalerServicePackageResponse;
import com.cloud.network.NetScalerControlCenterVO;
import com.cloud.network.NetScalerServicePackageVO;
import com.cloud.network.Network;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
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
     * @return true if Netscaler device is deleted successfully
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
     * @param ListNetscalerLoadBalancersCmd
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network.
     */
    public List<ExternalLoadBalancerDeviceVO> listNetscalerLoadBalancers(ListNetscalerLoadBalancersCmd cmd);

    /**
     * lists all the guest networks using a Netscaler load balancer device
     * @param ListNetscalerLoadBalancerNetworksCmd
     * @return list of the guest networks that are using this Netscaler load balancer
     */
    public List<? extends Network> listNetworks(ListNetscalerLoadBalancerNetworksCmd cmd);

    /**
     * creates API response object for netscaler load balancers
     * @param lbDeviceVO external load balancer VO object
     * @return NetscalerLoadBalancerResponse
     */
    public NetscalerLoadBalancerResponse createNetscalerLoadBalancerResponse(ExternalLoadBalancerDeviceVO lbDeviceVO);

    /**
     * creates API response object for netscaler load balancers
     * @param lbDeviceVO external load balancer VO object
     * @return NetscalerLoadBalancerResponse
     */
    public NetScalerServicePackageResponse registerNetscalerServicePackage(RegisterServicePackageCmd cmd);

    public NetScalerServicePackageResponse deleteNetscalerServicePackage(RegisterServicePackageCmd cmd);

    public NetScalerServicePackageResponse listNetscalerServicePackage(RegisterServicePackageCmd cmd);

    public NetScalerServicePackageResponse createNetscalerServicePackageResponse(NetScalerServicePackageVO servicePackageVO);

    public NetScalerControlCenterVO registerNetscalerControlCenter(RegisterNetscalerControlCenterCmd registerNetscalerControlCenterCmd);
}
