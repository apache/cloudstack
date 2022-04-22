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
package org.apache.cloudstack.response;

public interface HostMetricsSummary {
    void setCpuTotal(Long totalCpu);

    void setCpuAllocated(Long cpuAllocated, Long totalCpu);

    void setCpuUsed(Double cpuUsedPercentage, Long totalHosts);

    void setCpuMaxDeviation(Double maximumCpuUsage, Double cpuUsedPercentage, Long totalHosts);

    void setCpuUsageThreshold(Double cpuUsedPercentage, Long totalHosts, Double cpuThreshold);

    void setCpuUsageDisableThreshold(Double cpuUsedPercentage, Long totalHosts, Float cpuDisableThreshold);

    void setCpuAllocatedThreshold(Long cpuAllocated, Long totalCpu, Double cpuThreshold);

    void setCpuAllocatedDisableThreshold(Long cpuAllocated, Long totalCpu, Float cpuDisableThreshold);

    void setMemTotal(Long totalMemory);

    void setMemAllocated(Long memoryAllocated, Long totalMemory);

    void setMemUsed(Long memoryUsed, Long totalMemory);

    void setMemMaxDeviation(Long maximumMemoryUsage, Long memoryUsed, Long totalHosts);

    void setMemoryUsageThreshold(Long memoryUsed, Long totalMemory, Double memoryThreshold);

    void setMemoryUsageDisableThreshold(Long memoryUsed, Long totalMemory, Float memoryDisableThreshold);

    void setMemoryAllocatedThreshold(Long memoryAllocated, Long totalMemory, Double memoryThreshold);

    void setMemoryAllocatedDisableThreshold(Long memoryAllocated, Long totalMemory, Float memoryDisableThreshold);
}
