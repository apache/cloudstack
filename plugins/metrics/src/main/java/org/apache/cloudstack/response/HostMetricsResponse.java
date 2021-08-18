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

import java.text.DecimalFormat;
import java.text.ParseException;

import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;

import com.cloud.serializer.Param;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.annotations.SerializedName;

public class HostMetricsResponse extends HostResponse {
    @SerializedName("powerstate")
    @Param(description = "out-of-band management power state")
    private OutOfBandManagement.PowerState powerState;

    @SerializedName("instances")
    @Param(description = "instances on the host")
    private String instances;

    @SerializedName("cputotalghz")
    @Param(description = "the total cpu capacity in Ghz")
    private String cpuTotal;

    @SerializedName("cpuusedghz")
    @Param(description = "the total cpu used in Ghz")
    private String cpuUsed;

    @SerializedName("cpuallocatedghz")
    @Param(description = "the total cpu allocated in Ghz")
    private String cpuAllocated;

    @SerializedName("cpuloadaverage")
    @Param(description = "the average cpu load the last minute")
    private Double loadAverage;

    @SerializedName("memorytotalgb")
    @Param(description = "the total memory capacity in GiB")
    private String memTotal;

    @SerializedName("memoryusedgb")
    @Param(description = "the total memory used in GiB")
    private String memUsed;

    @SerializedName("memoryallocatedgb")
    @Param(description = "the total memory allocated in GiB")
    private String memAllocated;

    @SerializedName("networkread")
    @Param(description = "network read in GiB")
    private String networkRead;

    @SerializedName("networkwrite")
    @Param(description = "network write in GiB")
    private String networkWrite;

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

    public void setPowerState(final OutOfBandManagement.PowerState powerState) {
        this.powerState = powerState;
    }

    public void setInstances(final Long running, final Long total) {
        if (running != null && total != null) {
            this.instances = String.format("%d / %d", running, total);
        }
    }

    public void setCpuTotal(final Integer cpuNumber, final Long cpuSpeed) {
        if (cpuNumber != null && cpuSpeed != null) {
            this.cpuTotal = String.format("%.2f Ghz", cpuNumber * cpuSpeed / 1000.0);
        }
    }

    public void setCpuUsed(final String cpuUsed, final Integer cpuNumber, final Long cpuSpeed) {
        if (cpuUsed != null && cpuNumber != null && cpuSpeed != null) {
            this.cpuUsed = String.format("%.2f Ghz", parseCPU(cpuUsed) * cpuNumber * cpuSpeed / (100.0 * 1000.0));
        }
    }

    public void setLoadAverage(final Double loadAverage) {
        if (loadAverage != null) {
            this.loadAverage = loadAverage;
        }
    }

    public void setCpuAllocated(final String cpuAllocated, final Integer cpuNumber, final Long cpuSpeed) {
        if (cpuAllocated != null && cpuNumber != null && cpuSpeed != null) {
            this.cpuAllocated = String.format("%.2f Ghz", parseCPU(cpuAllocated) * cpuNumber * cpuSpeed / (100.0 * 1000.0));
        }
    }

    public String getCpuAllocatedGhz() {
        return cpuAllocated;
    }

    public void setMemTotal(final Long memTotal) {
        if (memTotal != null) {
            this.memTotal = String.format("%.2f GB", memTotal / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setMemUsed(final Long memUsed) {
        if (memUsed != null) {
            this.memUsed = String.format("%.2f GB", memUsed / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setMemAllocated(final Long memAllocated) {
        if (memAllocated != null) {
            this.memAllocated = String.format("%.2f GB", memAllocated / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setNetworkRead(final Long networkReadKbs) {
        if (networkReadKbs != null) {
            this.networkRead = String.format("%.2f GB", networkReadKbs / (1024.0 * 1024.0));
        }
    }

    public void setNetworkWrite(final Long networkWriteKbs) {
        if (networkWriteKbs != null) {
            this.networkWrite = String.format("%.2f GB", networkWriteKbs / (1024.0 * 1024.0));
        }
    }

    public void setCpuUsageThreshold(final String cpuUsed, final Double threshold) {
        if (cpuUsed != null && threshold != null) {
            this.cpuThresholdExceeded = parseCPU(cpuUsed) > (100.0 * threshold);
        }
    }

    public void setCpuUsageDisableThreshold(final String cpuUsed, final Float threshold) {
        if (cpuUsed != null && threshold != null) {
            this.cpuDisableThresholdExceeded = parseCPU(cpuUsed) > (100.0 * threshold);
        }
    }

    public void setCpuAllocatedThreshold(final String cpuAllocated, final Double threshold) {
        if (cpuAllocated != null && threshold != null) {
            this.cpuAllocatedThresholdExceeded = parseCPU(cpuAllocated) > (100.0 * threshold );
        }
    }

    public void setCpuAllocatedDisableThreshold(final String cpuAllocated, final Float threshold) {
        if (cpuAllocated != null && threshold != null) {
            this.cpuAllocatedDisableThresholdExceeded = parseCPU(cpuAllocated) > (100.0 * threshold);
        }
    }

    public void setMemoryUsageThreshold(final Long memUsed, final Long memTotal, final Double threshold) {
        if (memUsed != null && memTotal != null && threshold != null) {
            this.memoryThresholdExceeded = memUsed > (memTotal * threshold);
        }
    }

    public void setMemoryUsageDisableThreshold(final Long memUsed, final Long memTotal, final Float threshold) {
        if (memUsed != null && memTotal != null && threshold != null) {
            this.memoryDisableThresholdExceeded = memUsed > (memTotal * threshold);
        }
    }

    public void setMemoryAllocatedThreshold(final Long memAllocated, final Long memTotal, final Double threshold) {
        if (memAllocated != null && memTotal != null && threshold != null) {
            this.memoryAllocatedThresholdExceeded = memAllocated > (memTotal * threshold);
        }
    }

    public void setMemoryAllocatedDisableThreshold(final Long memAllocated, final Long memTotal, final Float threshold) {
        if (memAllocated != null && memTotal != null && threshold != null) {
            this.memoryAllocatedDisableThresholdExceeded = memAllocated > (memTotal * threshold);
        }
    }

    private Double parseCPU(String cpu) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        try {
            return decimalFormat.parse(cpu).doubleValue();
        } catch (ParseException e) {
            throw new CloudRuntimeException(e);
        }
    }

}
