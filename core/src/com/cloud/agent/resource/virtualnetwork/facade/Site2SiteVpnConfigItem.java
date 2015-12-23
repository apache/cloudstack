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
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.Site2SiteVpn;

public class Site2SiteVpnConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final Site2SiteVpnCfgCommand command = (Site2SiteVpnCfgCommand) cmd;

        final Site2SiteVpn site2siteVpn = new Site2SiteVpn(command.getLocalPublicIp(), command.getLocalGuestCidr(), command.getLocalPublicGateway(), command.getPeerGatewayIp(),
                command.getPeerGuestCidrList(), command.getEspPolicy(), command.getIkePolicy(), command.getIpsecPsk(), command.getIkeLifetime(), command.getEspLifetime(), command.isCreate(), command.getDpd(),
                command.isPassive(), command.getEncap());
        return generateConfigItems(site2siteVpn);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.SITE_2_SITE_VPN_CONFIG;

        return super.generateConfigItems(configuration);
    }
}