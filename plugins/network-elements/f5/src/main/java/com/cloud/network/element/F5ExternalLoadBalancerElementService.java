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

import org.apache.cloudstack.api.response.ExternalLoadBalancerResponse;

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
import com.cloud.network.Network;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
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
    @Deprecated
    // API helper function supported for backward compatibility
        public
        Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd);

    @Deprecated
    //  API helper function supported for backward compatibility
        public
        boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd);

    @Deprecated
    //  API helper function supported for backward compatibility
        public
        List<Host> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd);

    @Deprecated
    //  API helper function supported for backward compatibility
        public
        ExternalLoadBalancerResponse createExternalLoadBalancerResponse(Host externalLb);
}
