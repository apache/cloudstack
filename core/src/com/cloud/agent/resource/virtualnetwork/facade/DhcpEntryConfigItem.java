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

import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.VmDhcpConfig;

public class DhcpEntryConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final DhcpEntryCommand command = (DhcpEntryCommand) cmd;

        final VmDhcpConfig vmDhcpConfig = new VmDhcpConfig(command.getVmName(), command.getVmMac(), command.getVmIpAddress(), command.getVmIp6Address(), command.getDuid(), command.getDefaultDns(),
                command.getDefaultRouter(), command.getStaticRoutes(), command.isDefault());

        return generateConfigItems(vmDhcpConfig);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.VM_DHCP_CONFIG;

        return super.generateConfigItems(configuration);
    }
}