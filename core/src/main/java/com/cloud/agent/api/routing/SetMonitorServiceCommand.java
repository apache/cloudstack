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

package com.cloud.agent.api.routing;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.api.to.MonitorServiceTO;

/**
 *
 * AccessDetails allow different components to put in information about
 * how to access the components inside the command.
 */
public class SetMonitorServiceCommand extends NetworkElementCommand {
    public static final String ROUTER_MONITORING_ENABLED = "router.monitor.enabled";
    public static final String ROUTER_HEALTH_CHECKS_ENABLED = "router.health.checks.enabled";
    public static final String ROUTER_HEALTH_CHECKS_BASIC_INTERVAL = "router.health.checks.basic.interval";
    public static final String ROUTER_HEALTH_CHECKS_ADVANCED_INTERVAL = "router.health.checks.advanced.interval";
    public static final String ROUTER_HEALTH_CHECKS_EXCLUDED = "router.health.checks.excluded";

    private MonitorServiceTO[] services;
    private Map<String, String> healthChecksConfig;
    private boolean reconfigureAfterUpdate;
    private boolean deleteFromProcessedCache;

    protected SetMonitorServiceCommand() {
    }

    public SetMonitorServiceCommand(List<MonitorServiceTO> services) {
        if (CollectionUtils.isNotEmpty(services)) {
            this.services = services.toArray(new MonitorServiceTO[services.size()]);
        }
    }

    public MonitorServiceTO[] getRules() {
        return services;
    }

    public String getConfiguration() {
        if (services == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (MonitorServiceTO service : services) {
            sb.append("[").append(service.getService()).append("]").append(":");
            sb.append("processname=").append(service.getProcessname()).append(":");
            sb.append("servicename=").append(service.getServiceName()).append(":");
            sb.append("pidfile=").append(service.getPidFile()).append(":");
            sb.append(",");
        }

        return sb.toString();
    }

    public Map<String, String> getHealthChecksConfig() {
        return healthChecksConfig;
    }

    public void setHealthChecksConfig(Map<String, String> healthChecksConfig) {
        this.healthChecksConfig = healthChecksConfig;
    }

    public boolean shouldReconfigureAfterUpdate() {
        return reconfigureAfterUpdate;
    }

    public void setReconfigureAfterUpdate(boolean reconfigureAfterUpdate) {
        this.reconfigureAfterUpdate = reconfigureAfterUpdate;
    }

    public boolean shouldDeleteFromProcessedCache() {
        return deleteFromProcessedCache;
    }

    public void setDeleteFromProcessedCache(boolean deleteFromProcessedCache) {
        this.deleteFromProcessedCache = deleteFromProcessedCache;
    }

    @Override
    public int getAnswersCount() {
        return 2 + (reconfigureAfterUpdate ? 1 : 0);
    }
}
