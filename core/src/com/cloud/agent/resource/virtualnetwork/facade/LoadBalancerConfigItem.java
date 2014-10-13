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

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.FileConfigItem;
import com.cloud.agent.resource.virtualnetwork.ScriptConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.network.HAProxyConfigurator;
import com.cloud.network.LoadBalancerConfigurator;

public class LoadBalancerConfigItem extends AbstractConfigItemFacade{

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final LoadBalancerConfigCommand command = (LoadBalancerConfigCommand) cmd;

        final LinkedList<ConfigItem> cfg = new LinkedList<>();

        final String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        final LoadBalancerConfigurator cfgtr = new HAProxyConfigurator();

        final String[] config = cfgtr.generateConfiguration(command);
        final StringBuffer buff = new StringBuffer();
        for (int i = 0; i < config.length; i++) {
            buff.append(config[i]);
            buff.append("\n");
        }
        final String tmpCfgFilePath = "/etc/haproxy/";
        final String tmpCfgFileName = "haproxy.cfg.new." + String.valueOf(System.currentTimeMillis());
        cfg.add(new FileConfigItem(tmpCfgFilePath, tmpCfgFileName, buff.toString()));

        final String[][] rules = cfgtr.generateFwRules(command);

        final String[] addRules = rules[LoadBalancerConfigurator.ADD];
        final String[] removeRules = rules[LoadBalancerConfigurator.REMOVE];
        final String[] statRules = rules[LoadBalancerConfigurator.STATS];

        String args = " -f " + tmpCfgFilePath + tmpCfgFileName;
        StringBuilder sb = new StringBuilder();
        if (addRules.length > 0) {
            for (int i = 0; i < addRules.length; i++) {
                sb.append(addRules[i]).append(',');
            }
            args += " -a " + sb.toString();
        }

        sb = new StringBuilder();
        if (removeRules.length > 0) {
            for (int i = 0; i < removeRules.length; i++) {
                sb.append(removeRules[i]).append(',');
            }

            args += " -d " + sb.toString();
        }

        sb = new StringBuilder();
        if (statRules.length > 0) {
            for (int i = 0; i < statRules.length; i++) {
                sb.append(statRules[i]).append(',');
            }

            args += " -s " + sb.toString();
        }

        if (command.getVpcId() == null) {
            args = " -i " + routerIp + args;
            cfg.add(new ScriptConfigItem(VRScripts.LB, args));
        } else {
            args = " -i " + command.getNic().getIp() + args;
            cfg.add(new ScriptConfigItem(VRScripts.VPC_LB, args));
        }

        return cfg;
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        return null;
    }
}