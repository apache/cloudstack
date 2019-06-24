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
package com.cloud.network.security;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Allows JMX access
 *
 */
public interface SecurityGroupManagerMBean {
    void enableUpdateMonitor(boolean enable);

    void disableSchedulerForVm(Long vmId);

    void enableSchedulerForVm(Long vmId);

    Long[] getDisabledVmsForScheduler();

    void enableSchedulerForAllVms();

    Map<Long, Date> getScheduledTimestamps();

    Map<Long, Date> getLastUpdateSentTimestamps();

    int getQueueSize();

    List<Long> getVmsInQueue();

    void scheduleRulesetUpdateForVm(Long vmId);

    void tryRulesetUpdateForVmBypassSchedulerVeryDangerous(Long vmId, Long seqno);

    void simulateVmStart(Long vmId);

    void disableSchedulerEntirelyVeryDangerous(boolean disable);

    boolean isSchedulerDisabledEntirely();

    void clearSchedulerQueueVeryDangerous();
}
