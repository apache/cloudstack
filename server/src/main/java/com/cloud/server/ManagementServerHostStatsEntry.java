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
package com.cloud.server;

import java.util.Date;

public class ManagementServerHostStatsEntry implements ManagementServerHostStats {

    private long managementServerHostId;
    private String managementServerHostUuid;

    private Date collectionTime;
    private long sessions;
    private double cpuUtilization;
    private long totalJvmMemoryBytes;
    private long freeJvmMemoryBytes;
    private long maxJvmMemoryBytes;
    private long processJvmMemoryBytes;
    private long jvmUptime;
    private long jvmStartTime;
    private int availableProcessors;
    private double loadAverage;
    long totalInit;
    long totalUsed;
    long totalCommitted;
    private long pid;
    private String jvmName;
    private String jvmVendor;
    private String jvmVersion;
    private String osDistribution;
    private int agentCount;

    private long heapMemoryUsed;
    private long heapMemoryTotal;
    private int threadsBlockedCount;
    private int threadsDaemonCount;
    private int threadsRunnableCount;
    private int threadsTerminatedCount;
    private int threadsTotalCount;
    private int threadsWaitingCount;
    private long systemMemoryTotal;
    private long systemMemoryFree;
    private long systemMemoryUsed;
    private long systemMemoryVirtualSize;
    private String logInfo;
    private double systemTotalCpuCycles;
    private double[] systemLoadAverages;
    private long[] systemCyclesUsage;
    private boolean dbLocal;
    private boolean usageLocal;

    private Date systemBootTime;
    private String kernelVersion;

    public ManagementServerHostStatsEntry() {
        this(new Date());
    }

    public ManagementServerHostStatsEntry(Date date) {
        collectionTime = date;
    }

    @Override
    public long getManagementServerHostId() {
        return managementServerHostId;
    }

    public void setManagementServerHostId(long managementServerHostId) {
        this.managementServerHostId = managementServerHostId;
    }

    @Override
    public String getManagementServerHostUuid() {
        return managementServerHostUuid;
    }

    public void setManagementServerHostUuid(String managementServerHostUuid) {
        this.managementServerHostUuid = managementServerHostUuid;
    }

    @Override
    public Date getCollectionTime(){
        return collectionTime;
    }

    @Override
    public long getSessions() {
        return sessions;
    }

    @Override
    public double getCpuUtilization() {
        return cpuUtilization;
    }

    @Override
    public long getTotalJvmMemoryBytes() {
        return totalJvmMemoryBytes;
    }

    @Override
    public double getFreeJvmMemoryBytes() {
        return freeJvmMemoryBytes;
    }

    @Override
    public long getProcessJvmMemoryBytes() {
        return processJvmMemoryBytes;
    }

    @Override
    public long getJvmUptime() {
        return jvmUptime;
    }

    @Override
    public long getJvmStartTime() {
        return jvmStartTime;
    }

    @Override
    public int getAvailableProcessors() {
        return availableProcessors;
    }

    @Override
    public double getLoadAverage() {
        return loadAverage;
    }

    @Override
    public long getTotalInit() {
        return totalInit;
    }

    @Override
    public long getTotalUsed() {
        return totalUsed;
    }

    @Override
    public long getMaxJvmMemoryBytes() {
        return maxJvmMemoryBytes;
    }

    @Override
    public long getTotalCommitted() {
        return totalCommitted;
    }

    @Override
    public long getPid() {
        return pid;
    }

    @Override
    public String getJvmName() {
        return jvmName;
    }

    @Override
    public String getJvmVendor() {
        return jvmVendor;
    }

    @Override
    public String getJvmVersion() {
        return jvmVersion;
    }

    @Override
    public String getOsDistribution() {
        return osDistribution;
    }

    @Override
    public int getAgentCount() {
        return agentCount;
    }

    @Override
    public long getHeapMemoryUsed() {
        return heapMemoryUsed;
    }

    @Override
    public long getHeapMemoryTotal() {
        return heapMemoryTotal;
    }

    public void setSessions(long sessions) {
        this.sessions = sessions;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public void setTotalJvmMemoryBytes(long totalJvmMemoryBytes) {
        this.totalJvmMemoryBytes = totalJvmMemoryBytes;
    }

    public void setFreeJvmMemoryBytes(long freeJvmMemoryBytes) {
        this.freeJvmMemoryBytes = freeJvmMemoryBytes;
    }

    public void setProcessJvmMemoryBytes(long processJvmMemoryBytes) {
        this.processJvmMemoryBytes = processJvmMemoryBytes;
    }

    protected void validateSome() {
        assert totalJvmMemoryBytes - processJvmMemoryBytes > freeJvmMemoryBytes;
    }

    public void setJvmUptime(long jvmUptime) {
        this.jvmUptime = jvmUptime;
    }

    public void setJvmStartTime(long jvmStartTime) {
        this.jvmStartTime = jvmStartTime;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public void setLoadAverage(double loadAverage) {
        this.loadAverage = loadAverage;
    }

    public void setTotalInit(long totalInit) {
        this.totalInit = totalInit;
    }

    public void setTotalUsed(long totalUsed) {
        this.totalUsed = totalUsed;
    }

    public void setMaxJvmMemoryBytes(long maxJvmMemoryBytes) {
        this.maxJvmMemoryBytes = maxJvmMemoryBytes;
    }

    public void setTotalCommitted(long totalCommitted) {
        this.totalCommitted = totalCommitted;
    }

    public void setProcessId(long pid) {
        this.pid = pid;
    }

    public void setJvmName(String name) {
        this.jvmName = name;
    }

    public void setJvmVendor(String vmVendor) {
        this.jvmVendor = vmVendor;
    }

    public void setJvmVersion(String vmVersion) {
        this.jvmVersion = vmVersion;
    }

    public void setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
    }

    public void setAgentCount(int agentCount) {
        this.agentCount = agentCount;
    }

    public void setHeapMemoryUsed(long heapMemoryUsed) {
        this.heapMemoryUsed = heapMemoryUsed;
    }

    public void setHeapMemoryTotal(long heapMemoryTotal) {
        this.heapMemoryTotal = heapMemoryTotal;
    }

    public void setThreadsBlockedCount(int threadsBlockedCount) {
        this.threadsBlockedCount = threadsBlockedCount;
    }

    @Override
    public int getThreadsBlockedCount() {
        return threadsBlockedCount;
    }

    public void setThreadsTotalCount(int threadsTotalCount) {
        this.threadsTotalCount = threadsTotalCount;
    }

    @Override
    public int getThreadsTotalCount() {
        return threadsTotalCount;
    }

    public void setThreadsDaemonCount(int threadsDaemonCount) {
        this.threadsDaemonCount = threadsDaemonCount;
    }

    @Override
    public int getThreadsDaemonCount() {
        return threadsDaemonCount;
    }

    public void setThreadsRunnableCount(int threadsRunnableCount) {
        this.threadsRunnableCount = threadsRunnableCount;
    }

    @Override
    public int getThreadsRunnableCount() {
        return threadsRunnableCount;
    }

    public void setThreadsTerminatedCount(int threadsTerminatedCount) {
        this.threadsTerminatedCount = threadsTerminatedCount;
    }

    @Override
    public int getThreadsTerminatedCount() {
        return threadsTerminatedCount;
    }

    public void setThreadsWaitingCount(int threadsWaitingCount) {
        this.threadsWaitingCount = threadsWaitingCount;
    }

    @Override
    public int getThreadsWaitingCount() {
        return threadsWaitingCount;
    }

    public void setSystemMemoryTotal(long systemMemoryTotal) {
        this.systemMemoryTotal = systemMemoryTotal;
    }

    @Override
    public long getSystemMemoryTotal() {
        return systemMemoryTotal;
    }

    public void setSystemMemoryFree(long systemMemoryFree) {
        this.systemMemoryFree = systemMemoryFree;
    }

    @Override
    public long getSystemMemoryFree() {
        return systemMemoryFree;
    }

    public void setSystemMemoryUsed(long systemMemoryUsed) {
        this.systemMemoryUsed = systemMemoryUsed;
    }

    @Override
    public long getSystemMemoryUsed() {
        return systemMemoryUsed;
    }

    public void setSystemMemoryVirtualSize(long systemMemoryVirtualSize) {
        this.systemMemoryVirtualSize = systemMemoryVirtualSize;
    }

    @Override
    public long getSystemMemoryVirtualSize() {
        return systemMemoryVirtualSize;
    }

    public void setLogInfo(String logInfo) {
        this.logInfo = logInfo;
    }

    @Override
    public String getLogInfo() {
        return logInfo;
    }

    public void setSystemTotalCpuCycles(double systemTotalCpuCycles) {
        this.systemTotalCpuCycles = systemTotalCpuCycles;
    }

    @Override
    public double getSystemTotalCpuCycles() {
        return systemTotalCpuCycles;
    }

    public void setSystemLoadAverages(double[] systemLoadAverages) {
        this.systemLoadAverages = systemLoadAverages;
    }

    @Override
    public double[] getSystemLoadAverages() {
        return systemLoadAverages;
    }

    public void setSystemCyclesUsage(long[] systemCyclesUsage) {
        this.systemCyclesUsage = systemCyclesUsage;
    }

    @Override
    public long[] getSystemCyclesUsage() {
        return systemCyclesUsage;
    }

    public void setDbLocal(boolean dbLocal) {
        this.dbLocal = dbLocal;
    }

    @Override
    public boolean isDbLocal() {
        return dbLocal;
    }

    public void setUsageLocal(boolean usageLocal) {
        this.usageLocal = usageLocal;
    }

    @Override
    public boolean isUsageLocal() {
        return usageLocal;
    }

    @Override
    public Date getSystemBootTime() {
        return systemBootTime;
    }

    public void setSystemBootTime(Date systemBootTime) {
        this.systemBootTime = systemBootTime;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    @Override
    public String getKernelVersion() {
        return kernelVersion;
    }
}
