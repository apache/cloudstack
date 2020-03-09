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

import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.AddSrxFirewallCmd;
import com.cloud.api.commands.ConfigureSrxFirewallCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.DeleteSrxFirewallCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.api.commands.ListSrxFirewallNetworksCmd;
import com.cloud.api.commands.ListSrxFirewallsCmd;
import com.cloud.api.response.SrxFirewallResponse;
import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface JuniperSRXFirewallElementService extends PluggableService {

    /**
     * adds a SRX firewall device in to a physical network
     * @param AddSrxFirewallCmd
     * @return ExternalFirewallDeviceVO object for the firewall added
     */
    public ExternalFirewallDeviceVO addSrxFirewall(AddSrxFirewallCmd cmd);

    /**
     * removes SRX firewall device from a physical network
     * @param DeleteSrxFirewallCmd
     * @return true if firewall device successfully deleted
     */
    public boolean deleteSrxFirewall(DeleteSrxFirewallCmd cmd);

    /**
     * configures a SRX firewal device added in a physical network
     * @param ConfigureSrxFirewallCmd
     * @return ExternalFirewallDeviceVO for the device configured
     */
    public ExternalFirewallDeviceVO configureSrxFirewall(ConfigureSrxFirewallCmd cmd);

    /**
     * lists all the SRX firewall devices added in to a physical network
     * @param ListSrxFirewallsCmd
     * @return list of ExternalFirewallDeviceVO for the devices in the physical network.
     */
    public List<ExternalFirewallDeviceVO> listSrxFirewalls(ListSrxFirewallsCmd cmd);

    /**
     * lists all the guest networks using a SRX firewall device
     * @param ListSrxFirewallNetworksCmd
     * @return list of the guest networks that are using this F5 load balancer
     */
    public List<? extends Network> listNetworks(ListSrxFirewallNetworksCmd cmd);

    public SrxFirewallResponse createSrxFirewallResponse(ExternalFirewallDeviceVO fwDeviceVO);

    @Deprecated
    // API helper function supported for backward compatibility
        public
        Host addExternalFirewall(AddExternalFirewallCmd cmd);

    @Deprecated
    // API helper function supported for backward compatibility
        public
        boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd);

    @Deprecated
    // API helper function supported for backward compatibility
        public
        List<Host> listExternalFirewalls(ListExternalFirewallsCmd cmd);

    @Deprecated
    // API helper function supported for backward compatibility
        public
        ExternalFirewallResponse createExternalFirewallResponse(Host externalFirewall);
}
