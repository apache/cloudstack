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

import java.util.LinkedList;
import java.util.List;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.VpnUser;
import com.cloud.agent.resource.virtualnetwork.model.VpnUserList;

public class VpnUsersConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final VpnUsersCfgCommand command = (VpnUsersCfgCommand) cmd;

        final List<VpnUser> vpnUsers = new LinkedList<VpnUser>();
        for (final VpnUsersCfgCommand.UsernamePassword userpwd : command.getUserpwds()) {
            vpnUsers.add(new VpnUser(userpwd.getUsername(), userpwd.getPassword(), userpwd.isAdd()));
        }

        final VpnUserList vpnUserList = new VpnUserList(vpnUsers);
        return generateConfigItems(vpnUserList);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.VPN_USER_LIST_CONFIG;

        return super.generateConfigItems(configuration);
    }
}
