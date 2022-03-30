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

/**
 * management server related stats
 */
public interface ManagementServerHostStats {
    Date getCollectionTime();

    Date getSystemBootTime();

    long getManagementServerHostId();

    String getManagementServerHostUuid();

    long getSessions();

    double getCpuUtilization();

    long getTotalJvmMemoryBytes();

    double getFreeJvmMemoryBytes();

    long getProcessJvmMemoryBytes();

    long getJvmUptime();

    long getJvmStartTime();

    int getAvailableProcessors();

    double getLoadAverage();

    long getTotalInit();

    long getTotalUsed();

    long getMaxJvmMemoryBytes();

    long getTotalCommitted();

    long getPid();

    String getJvmName();

    String getJvmVendor();

    String getJvmVersion();

    String getOsDistribution();

    int getAgentCount();

    long getHeapMemoryUsed();

    long getHeapMemoryTotal();

    int getThreadsBlockedCount();

    int getThreadsTotalCount();

    int getThreadsDaemonCount();

    int getThreadsRunnableCount();

    int getThreadsTerminatedCount();

    int getThreadsWaitingCount();

    long getSystemMemoryTotal();

    long getSystemMemoryFree();

    long getSystemMemoryUsed();

    long getSystemMemoryVirtualSize();

    String getLogInfo();

    /**
     * @return in mega hertz
     */
    double getSystemTotalCpuCycles();

    double[] getSystemLoadAverages();

    long[] getSystemCyclesUsage();

    boolean isDbLocal();

    boolean isUsageLocal();

    String getKernelVersion();
}
