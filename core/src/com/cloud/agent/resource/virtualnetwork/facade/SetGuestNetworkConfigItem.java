//
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
//

package com.cloud.agent.resource.virtualnetwork.facade;

import java.util.List;

import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.GuestNetwork;
import com.cloud.utils.net.NetUtils;

public class SetGuestNetworkConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final SetupGuestNetworkCommand command = (SetupGuestNetworkCommand) cmd;

        final NicTO nic = command.getNic();
        final String routerGIP = command.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        final String gateway = command.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
        final String cidr = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));
        final String netmask = nic.getNetmask();
        final String domainName = command.getNetworkDomain();
        String dns = command.getDefaultDns1();

        if (dns == null || dns.isEmpty()) {
            dns = command.getDefaultDns2();
        } else {
            final String dns2 = command.getDefaultDns2();
            if (dns2 != null && !dns2.isEmpty()) {
                dns += "," + dns2;
            }
        }

        final GuestNetwork guestNetwork = new GuestNetwork(command.isAdd(), nic.getMac(), "eth" + nic.getDeviceId(), routerGIP, netmask, gateway,
                cidr, dns, domainName);

        return generateConfigItems(guestNetwork);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.GUEST_NETWORK_CONFIG;

        return super.generateConfigItems(configuration);
    }
}