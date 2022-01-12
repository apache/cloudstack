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
