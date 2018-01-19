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

    public SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId, String accountName);

    public SecurityGroupVO createDefaultSecurityGroup(Long accountId);

    public boolean addInstanceToGroups(Long userVmId, List<Long> groups);

    public void removeInstanceFromGroups(long userVmId);

    public void fullSync(long agentId, HashMap<String, Pair<Long, Long>> newGroupStates);

    public String getSecurityGroupsNamesForVm(long vmId);

    public List<SecurityGroupVO> getSecurityGroupsForVm(long vmId);

    public boolean isVmSecurityGroupEnabled(Long vmId);

    SecurityGroup getDefaultSecurityGroup(long accountId);

    SecurityGroup getSecurityGroup(String name, long accountId);

    boolean isVmMappedToDefaultSecurityGroup(long vmId);
}
