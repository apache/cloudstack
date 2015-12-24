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
import com.cloud.agent.api.routing.QuaggaConfigCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.QuaggaRule;
import com.cloud.network.QuaggaOSPFConfigurator;

public class QuaggaConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final QuaggaConfigCommand command = (QuaggaConfigCommand) cmd;

        final QuaggaOSPFConfigurator cfgtr = new QuaggaOSPFConfigurator();
        final String[] zebraConfig = cfgtr.generateZebraConfiguration(command);
        final String[] ospfConfig = cfgtr.generateOSPFConfiguration(command);

        String routerIp = command.getRouterAccessIp();

        final String tmpCfgFilePath = "/etc/quagga/";
        final String tmpCfgFileName = "ospfd.conf.new." + String.valueOf(System.currentTimeMillis());

        final QuaggaRule quaggaRule = new QuaggaRule(zebraConfig, ospfConfig, tmpCfgFilePath, tmpCfgFileName, routerIp);

        return generateConfigItems(quaggaRule);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.QUAGGA_CONFIG;

        return super.generateConfigItems(configuration);
    }

}
