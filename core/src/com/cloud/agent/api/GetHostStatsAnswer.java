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

package com.cloud.agent.api;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.host.HostStats;

@LogLevel(Log4jLevel.Trace)
public class GetHostStatsAnswer extends Answer implements HostStats {

    HostStatsEntry hostStats;

    protected GetHostStatsAnswer() {
        hostStats = new HostStatsEntry();
    }

    public GetHostStatsAnswer(GetHostStatsCommand cmd, HostStatsEntry hostStatistics) {
        super(cmd);
        this.hostStats = hostStatistics;
    }

    public GetHostStatsAnswer(GetHostStatsCommand cmd, double cpuUtilization, double freeMemoryKBs, double totalMemoryKBs, double networkReadKBs, double networkWriteKBs,
            String entityType) {
        super(cmd);
        hostStats = new HostStatsEntry();

        hostStats.setCpuUtilization(cpuUtilization);
        hostStats.setFreeMemoryKBs(freeMemoryKBs);
        hostStats.setTotalMemoryKBs(totalMemoryKBs);
        hostStats.setNetworkReadKBs(networkReadKBs);
        hostStats.setNetworkWriteKBs(networkWriteKBs);
        hostStats.setEntityType(entityType);
    }

    @Override
    public double getUsedMemory() {
        return hostStats.getUsedMemory();
    }

    @Override
    public double getFreeMemoryKBs() {
        return hostStats.getFreeMemoryKBs();
    }

    @Override
    public double getTotalMemoryKBs() {
        return hostStats.getTotalMemoryKBs();
    }

    @Override
    public double getCpuUtilization() {
        return hostStats.getCpuUtilization();
    }

    @Override
    public double getNetworkReadKBs() {
        return hostStats.getNetworkReadKBs();
    }

    @Override
    public double getNetworkWriteKBs() {
        return hostStats.getNetworkWriteKBs();
    }

    @Override
    public String getEntityType() {
        return hostStats.getEntityType();
    }

    @Override
    public HostStats getHostStats() {
        return hostStats;
    }
}
