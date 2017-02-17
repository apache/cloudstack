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

import com.cloud.api.commands.AddVyosRouterFirewallCmd;
import com.cloud.api.commands.ConfigureVyosRouterFirewallCmd;
import com.cloud.api.commands.DeleteVyosRouterFirewallCmd;
import com.cloud.api.commands.ListVyosRouterFirewallNetworksCmd;
import com.cloud.api.commands.ListVyosRouterFirewallsCmd;
import com.cloud.api.response.VyosRouterFirewallResponse;
import com.cloud.network.Network;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface VyosRouterFirewallElementService extends PluggableService {

    /**
     * adds a Vyos Router device in to a physical network
     * @param AddVyosRouterFirewallCmd
     * @return ExternalFirewallDeviceVO object for the firewall added
     */
    public ExternalFirewallDeviceVO addVyosRouterFirewall(AddVyosRouterFirewallCmd cmd);

    /**
     * removes Vyos Router device from a physical network
     * @param DeleteVyosRouterFirewallCmd
     * @return true if firewall device successfully deleted
     */
    public boolean deleteVyosRouterFirewall(DeleteVyosRouterFirewallCmd cmd);

    /**
     * configures a Vyos Router device added in a physical network
     * @param ConfigureVyosRouterFirewallCmd
     * @return ExternalFirewallDeviceVO for the device configured
     */
    public ExternalFirewallDeviceVO configureVyosRouterFirewall(ConfigureVyosRouterFirewallCmd cmd);

    /**
     * lists all the Vyos Router devices added in to a physical network
     * @param ListVyosRouterFirewallsCmd
     * @return list of ExternalFirewallDeviceVO for the devices in the physical network.
     */
    public List<ExternalFirewallDeviceVO> listVyosRouterFirewalls(ListVyosRouterFirewallsCmd cmd);

    /**
     * lists all the guest networks using a VyosRouter firewall device
     * @param ListVyosRouterFirewallNetworksCmd
     * @return list of the guest networks that are using this F5 load balancer
     */
    public List<? extends Network> listNetworks(ListVyosRouterFirewallNetworksCmd cmd);

    public VyosRouterFirewallResponse createVyosRouterFirewallResponse(ExternalFirewallDeviceVO fwDeviceVO);
}