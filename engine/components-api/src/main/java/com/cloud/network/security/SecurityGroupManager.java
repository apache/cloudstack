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

import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

/**
 * Ensures that network firewall rules stay updated as VMs go up and down
 *
 */
public interface SecurityGroupManager {

    public static final String DEFAULT_GROUP_NAME = "default";
    public static final String DEFAULT_GROUP_DESCRIPTION = "Default Security Group";
    public static final int TIME_BETWEEN_CLEANUPS = 60;
    public static final int WORKER_THREAD_COUNT = 10;

    ConfigKey<Integer> SecurityGroupWorkCleanupInterval = new ConfigKey<>("Network", Integer.class,
            "network.securitygroups.work.cleanup.interval", "120",
            "Time interval (seconds) in which finished work is cleaned up from the work table", true);

    ConfigKey<Integer> SecurityGroupWorkerThreads = new ConfigKey<>("Network", Integer.class,
            "network.securitygroups.workers.pool.size", "50",
            "Number of worker threads processing the security group update work queue", true);

    ConfigKey<Integer> SecurityGroupWorkGlobalLockTimeout = new ConfigKey<>("Network", Integer.class,
            "network.securitygroups.work.lock.timeout", "300",
            "Lock wait timeout (seconds) while updating the security group work queue", true);

    ConfigKey<Integer> SecurityGroupWorkPerAgentMaxQueueSize = new ConfigKey<>("Network", Integer.class,
            "network.securitygroups.work.per.agent.queue.size", "100",
            "The number of outstanding security group work items that can be queued to a host. If exceeded, work items will get dropped to conserve memory. "
                    + "Security Group Sync will take care of ensuring that the host gets updated eventually", true);

    public SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId, String accountName);

    public SecurityGroupVO createDefaultSecurityGroup(Long accountId);

    public boolean addInstanceToGroups(UserVm userVm, List<Long> groups);

    public void removeInstanceFromGroups(UserVm userVm);

    public void fullSync(long agentId, HashMap<String, Pair<Long, Long>> newGroupStates);

    public String getSecurityGroupsNamesForVm(long vmId);

    public List<SecurityGroupVO> getSecurityGroupsForVm(long vmId);

    public boolean isVmSecurityGroupEnabled(Long vmId);

    SecurityGroup getDefaultSecurityGroup(long accountId);

    SecurityGroup getSecurityGroup(String name, long accountId);

    boolean isVmMappedToDefaultSecurityGroup(long vmId);

    void scheduleRulesetUpdateToHosts(List<Long> affectedVms, boolean updateSeqno, Long delayMs);
}
