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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.response.ZoneResponse;

public class ZoneMetricsResponse extends ZoneResponse implements HostMetricsSummary {
    @SerializedName("state")
    @Param(description = "state of the cluster")
    private String state;

    @SerializedName("clusters")
    @Param(description = "healthy / total clusters in the zone")
    private String resources;

    @SerializedName("cputotal")
    @Param(description = "the total cpu capacity in Ghz")
    private String cpuTotal;

    @SerializedName("cpuused")
    @Param(description = "the total cpu used in Ghz")
    private String cpuUsed;

    @SerializedName("cpuallocated")
    @Param(description = "the total cpu allocated in Ghz")
    private String cpuAllocated;

    @SerializedName("cpumaxdeviation")
    @Param(description = "the maximum cpu deviation")
    private String cpuMaxDeviation;

    @SerializedName("memorytotal")
    @Param(description = "the total cpu capacity in GiB")
    private String memTotal;

    @SerializedName("memoryused")
    @Param(description = "the total cpu used in GiB")
    private String memUsed;

    @SerializedName("memoryallocated")
    @Param(description = "the total cpu allocated in GiB")
    private String memAllocated;

    @SerializedName("memorymaxdeviation")
    @Param(description = "the maximum memory deviation")
    private String memMaxDeviation;

    @SerializedName("cputhreshold")
    @Param(description = "cpu usage notification threshold exceeded")
    private Boolean cpuThresholdExceeded;

    @SerializedName("cpudisablethreshold")
    @Param(description = "cpu usage disable threshold exceeded")
    private Boolean cpuDisableThresholdExceeded;

    @SerializedName("cpuallocatedthreshold")
    @Param(description = "cpu allocated notification threshold exceeded")
    private Boolean cpuAllocatedThresholdExceeded;

    @SerializedName("cpuallocateddisablethreshold")
    @Param(description = "cpu allocated disable threshold exceeded")
    private Boolean cpuAllocatedDisableThresholdExceeded;

    @SerializedName("memorythreshold")
    @Param(description = "memory usage notification threshold exceeded")
    private Boolean memoryThresholdExceeded;

    @SerializedName("memorydisablethreshold")
    @Param(description = "memory usage disable threshold exceeded")
    private Boolean memoryDisableThresholdExceeded;

    @SerializedName("memoryallocatedthreshold")
    @Param(description = "memory allocated notification threshold exceeded")
    private Boolean memoryAllocatedThresholdExceeded;

    @SerializedName("memoryallocateddisablethreshold")
    @Param(description = "memory allocated disable threshold exceeded")
    private Boolean memoryAllocatedDisableThresholdExceeded;


    public void setState(final String allocationState) {
        this.state = allocationState;
    }

    public void setResource(final Long upResources, final Long totalResources) {
        if (upResources != null && totalResources != null) {
            this.resources = String.format("%d / %d", upResources, totalResources);
        }
    }

    public void setCpuTotal(final Long cpuTotal) {
        if (cpuTotal != null) {
            this.cpuTotal = String.format("%.2f Ghz", cpuTotal / 1000.0);
        }
    }

    public void setCpuUsed(final Double cpuUsedPercentage, final Long totalHosts) {
        if (cpuUsedPercentage != null && totalHosts != null && totalHosts != 0) {
            this.cpuUsed = String.format("%.2f%%", 1.0 * cpuUsedPercentage / totalHosts);
        }
    }

    public void setCpuAllocated(final Long cpuAllocated, final Long cpuTotal) {
        if (cpuAllocated != null && cpuTotal != null && cpuTotal != 0) {
            this.cpuAllocated = String.format("%.2f%%", cpuAllocated * 100.0 / cpuTotal);
        }
    }

    public void setCpuMaxDeviation(final Double maxCpuUsagePercentage, final Double totalCpuUsedPercentage, final Long totalHosts) {
        if (maxCpuUsagePercentage != null && totalCpuUsedPercentage != null && totalHosts != null && totalHosts != 0) {
            final Double averageCpuUsagePercentage = totalCpuUsedPercentage / totalHosts;
            this.cpuMaxDeviation = String.format("%.2f%%", (maxCpuUsagePercentage - averageCpuUsagePercentage) / averageCpuUsagePercentage);
        }
    }

    public void setMemTotal(final Long memTotal) {
        if (memTotal != null) {
            this.memTotal = String.format("%.2f GB", memTotal / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setMemUsed( final Long memUsed, final Long memTotal) {
        if (memUsed != null && memTotal != null) {
            this.memUsed = String.format("%.2f%%", memUsed * 100.0 / memTotal);
        }
    }

    public void setMemAllocated(final Long memAllocated, final Long memTotal) {
        if (memAllocated != null && memTotal != null && memTotal != 0) {
            this.memAllocated = String.format("%.2f%%", memAllocated * 100.0 / memTotal);
        }
    }

    public void setMemMaxDeviation(final Long maxMemoryUsage, final Long totalMemory, final Long totalHosts) {
        if (maxMemoryUsage != null && totalMemory != null && totalHosts != null && totalHosts != 0) {
            final Long averageMemoryUsage = totalMemory / totalHosts;
            this.memMaxDeviation = String.format("%.2f%%", (maxMemoryUsage - averageMemoryUsage) * 100.0 / averageMemoryUsage);
        }
    }

    public void setCpuUsageThreshold(final Double cpuUsed, final Long totalHosts, final Double threshold) {
        if (cpuUsed != null && totalHosts != null && threshold != null && totalHosts != 0) {
            this.cpuThresholdExceeded = (cpuUsed / (100.0 * totalHosts)) > threshold;
        }
    }

    public void setCpuUsageDisableThreshold(final Double cpuUsed, final Long totalHosts, final Float threshold) {
        if (cpuUsed != null && totalHosts != null && threshold != null && totalHosts != 0) {
            this.cpuDisableThresholdExceeded = (cpuUsed / (100.0 * totalHosts)) > threshold;
        }
    }

    public void setCpuAllocatedThreshold(final Long cpuAllocated, final Long cpuUsed, final Double threshold) {
        if (cpuAllocated != null && cpuUsed != null && threshold != null && cpuUsed != 0) {
            this.cpuAllocatedThresholdExceeded = (1.0 * cpuAllocated / cpuUsed) > threshold;
        }
    }

    public void setCpuAllocatedDisableThreshold(final Long cpuAllocated, final Long cpuUsed, final Float threshold) {
        if (cpuAllocated != null && cpuUsed != null && threshold != null && cpuUsed != 0) {
            this.cpuAllocatedDisableThresholdExceeded = (1.0 * cpuAllocated / cpuUsed) > threshold;
        }
    }

    public void setMemoryUsageThreshold(final Long memUsed, final Long memTotal, final Double threshold) {
        if (memUsed != null && memTotal != null && threshold != null && memTotal != 0) {
            this.memoryThresholdExceeded = (1.0 * memUsed / memTotal) > threshold;
        }
    }

    public void setMemoryUsageDisableThreshold(final Long memUsed, final Long memTotal, final Float threshold) {
        if (memUsed != null && memTotal != null && threshold != null && memTotal != 0) {
            this.memoryDisableThresholdExceeded = (1.0 * memUsed / memTotal) > threshold;
        }
    }


    public void setMemoryAllocatedThreshold(final Long memAllocated, final Long memTotal, final Double threshold) {
        if (memAllocated != null && memTotal != null && threshold != null && memTotal != 0) {
            this.memoryAllocatedThresholdExceeded = (1.0 * memAllocated / memTotal) > threshold;
        }
    }

    public void setMemoryAllocatedDisableThreshold(final Long memAllocated, final Long memTotal, final Float threshold) {
        if (memAllocated != null && memTotal != null && threshold != null && memTotal != 0) {
            this.memoryAllocatedDisableThresholdExceeded = (1.0 * memAllocated / memTotal) > threshold;
        }
    }
}
