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

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.RemoteAccessVpn;

public class RemoteAccessVpnConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final RemoteAccessVpnCfgCommand command = (RemoteAccessVpnCfgCommand)cmd;

        final RemoteAccessVpn remoteAccessVpn = new RemoteAccessVpn(
                command.isCreate(),
                command.getIpRange(),
                command.getPresharedKey(),
                command.getVpnServerIp(),
                command.getLocalIp(),
                command.getLocalCidr(),
                command.getPublicInterface(),
                command.getVpnType(),
                command.getCaCert(),
                command.getServerCert(),
                command.getServerKey());

        return generateConfigItems(remoteAccessVpn);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.REMOTE_ACCESS_VPN_CONFIG;

        return super.generateConfigItems(configuration);
    }
}