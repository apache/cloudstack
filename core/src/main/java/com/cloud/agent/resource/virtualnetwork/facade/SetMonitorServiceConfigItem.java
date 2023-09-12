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

import org.apache.log4j.Logger;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.ScriptConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.MonitorService;

public class SetMonitorServiceConfigItem extends AbstractConfigItemFacade {
    private static final Logger s_logger = Logger.getLogger(SetMonitorServiceConfigItem.class);

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final SetMonitorServiceCommand command = (SetMonitorServiceCommand) cmd;

        final MonitorService monitorService = new MonitorService(
                command.getConfiguration(),
                cmd.getAccessDetail(SetMonitorServiceCommand.ROUTER_MONITORING_ENABLED),
                cmd.getAccessDetail(SetMonitorServiceCommand.ROUTER_HEALTH_CHECKS_ENABLED));

        setupHealthChecksRelatedInfo(monitorService, command);

        monitorService.setDeleteFromProcessedCache(command.shouldDeleteFromProcessedCache());

        List<ConfigItem> configItems = generateConfigItems(monitorService);
        if (configItems != null && command.shouldReconfigureAfterUpdate()) {
            configItems.add(new ScriptConfigItem(VRScripts.CONFIGURE, "monitor_service.json"));
        }
        return configItems;
    }

    private void setupHealthChecksRelatedInfo(MonitorService monitorService, SetMonitorServiceCommand command) {
        try {
            monitorService.setHealthChecksBasicRunInterval(Integer.parseInt(command.getAccessDetail(SetMonitorServiceCommand.ROUTER_HEALTH_CHECKS_BASIC_INTERVAL)));
        } catch (NumberFormatException exception) {
            s_logger.error("Unexpected health check basic interval set" + command.getAccessDetail(SetMonitorServiceCommand.ROUTER_HEALTH_CHECKS_BASIC_INTERVAL) +
                    ". Exception: " + exception + "Will use default value");
        }

        try {
            monitorService.setHealthChecksAdvancedRunInterval(Integer.parseInt(command.getAccessDetail(SetMonitorServiceCommand.ROUTER_HEALTH_CHECKS_ADVANCED_INTERVAL)));
        } catch (NumberFormatException exception) {
            s_logger.error("Unexpected health check advanced interval set" + command.getAccessDetail(SetMonitorServiceCommand.ROUTER_HEALTH_CHECKS_ADVANCED_INTERVAL) +
                    ". Exception: " + exception + "Will use default value");
        }

        monitorService.setExcludedHealthChecks(command.getAccessDetail(SetMonitorServiceCommand.ROUTER_HEALTH_CHECKS_EXCLUDED));
        monitorService.setHealthChecksConfig(command.getHealthChecksConfig());
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.MONITOR_SERVICE_CONFIG;

        return super.generateConfigItems(configuration);
    }
}
