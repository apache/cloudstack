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

package com.cloud.agent.resource.virtualnetwork.model;

import java.util.Map;

public class MonitorService extends ConfigBase {
    public String config, disableMonitoring;
    public Boolean healthChecksEnabled;
    public Integer healthChecksBasicRunInterval;
    public Integer healthChecksAdvancedRunInterval;
    public String excludedHealthChecks;
    public Map<String, String> healthChecksConfig;

    public MonitorService() {
        super(ConfigBase.MONITORSERVICE);
    }

    public MonitorService(String config, String disableMonitoring, String healthChecksEnabled) {
        super(ConfigBase.MONITORSERVICE);
        this.config = config;
        this.disableMonitoring = disableMonitoring;
        this.healthChecksEnabled = Boolean.parseBoolean(healthChecksEnabled);
    }

    public String getConfig() {
        return config;
    }

    public String getDisableMonitoring() {
        return disableMonitoring;
    }

    public Boolean getHealthChecksEnabled() {
        return healthChecksEnabled;
    }

    public Integer getHealthChecksBasicRunInterval() {
        return healthChecksBasicRunInterval;
    }

    public Integer getHealthChecksAdvancedRunInterval() {
        return healthChecksAdvancedRunInterval;
    }

    public String getExcludedHealthChecks() {
        return excludedHealthChecks;
    }

    public Map<String, String> getHealthChecksConfig() {
        return healthChecksConfig;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public void setDisableMonitoring(String disableMonitoring) {
        this.disableMonitoring = disableMonitoring;
    }

    public void setHealthChecksEnabled(Boolean healthChecksEnabled) {
        this.healthChecksEnabled = healthChecksEnabled;
    }

    public void setHealthChecksBasicRunInterval(Integer healthChecksBasicRunInterval) {
        this.healthChecksBasicRunInterval = healthChecksBasicRunInterval;
    }

    public void setHealthChecksAdvancedRunInterval(Integer healthChecksAdvancedRunInterval) {
        this.healthChecksAdvancedRunInterval = healthChecksAdvancedRunInterval;
    }

    public void setExcludedHealthChecks(String excludedHealthChecks) {
        this.excludedHealthChecks = excludedHealthChecks;
    }

    public void setHealthChecksConfig(Map<String, String> healthChecksConfig) {
        this.healthChecksConfig = healthChecksConfig;
    }

    public void setDeleteFromProcessedCache(boolean deleteFromProcessedCache) {
        this.deleteFromProcessedCache = deleteFromProcessedCache;
    }
}
