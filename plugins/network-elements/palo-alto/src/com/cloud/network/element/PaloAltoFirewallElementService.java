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

import org.apache.cloudstack.api.response.ExternalFirewallResponse;

import com.cloud.api.commands.AddPaloAltoFirewallCmd;
import com.cloud.api.commands.ConfigurePaloAltoFirewallCmd;
import com.cloud.api.commands.DeletePaloAltoFirewallCmd;
import com.cloud.api.commands.ListPaloAltoFirewallNetworksCmd;
import com.cloud.api.commands.ListPaloAltoFirewallsCmd;
import com.cloud.api.response.PaloAltoFirewallResponse;
import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface PaloAltoFirewallElementService extends PluggableService {

    /**
     * adds a Palo Alto firewall device in to a physical network
     * @param AddPaloAltoFirewallCmd
     * @return ExternalFirewallDeviceVO object for the firewall added
     */
    public ExternalFirewallDeviceVO addPaloAltoFirewall(AddPaloAltoFirewallCmd cmd);

    /**
     * removes Palo Alto firewall device from a physical network
     * @param DeletePaloAltoFirewallCmd
     * @return true if firewall device successfully deleted
     */
    public boolean deletePaloAltoFirewall(DeletePaloAltoFirewallCmd cmd);

    /**
     * configures a Palo Alto firewal device added in a physical network
     * @param ConfigurePaloAltoFirewallCmd
     * @return ExternalFirewallDeviceVO for the device configured
     */
    public ExternalFirewallDeviceVO configurePaloAltoFirewall(ConfigurePaloAltoFirewallCmd cmd);

    /**
     * lists all the Palo Alto firewall devices added in to a physical network
     * @param ListPaloAltoFirewallsCmd
     * @return list of ExternalFirewallDeviceVO for the devices in the physical network.
     */
    public List<ExternalFirewallDeviceVO> listPaloAltoFirewalls(ListPaloAltoFirewallsCmd cmd);

    /**
     * lists all the guest networks using a PaloAlto firewall device
     * @param ListPaloAltoFirewallNetworksCmd
     * @return list of the guest networks that are using this F5 load balancer
     */
    public List<? extends Network> listNetworks(ListPaloAltoFirewallNetworksCmd cmd);

    public PaloAltoFirewallResponse createPaloAltoFirewallResponse(ExternalFirewallDeviceVO fwDeviceVO);
}
