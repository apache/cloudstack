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
package com.cloud.hypervisor.kvm.resource;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


import com.cloud.agent.api.to.HostTO;

public class KVMHAChecker extends KVMHABase implements Callable<Boolean> {
    private List<HAStoragePool> storagePools;
    private HostTO host;
    private boolean reportIfHeartBeatFailedForOneStoragePool;

    public KVMHAChecker(List<HAStoragePool> pools, HostTO host, boolean reportIfHeartBeatFailedForOneStoragePool) {
        this.storagePools = pools;
        this.host = host;
        this.reportIfHeartBeatFailedForOneStoragePool = reportIfHeartBeatFailedForOneStoragePool;
    }

    /*
     * True means heart beating is on going, or we can't get it's status.
     * False means heart beating is stopped definitely.
     *
     * The answer comes from the heartbeats only: it is what VM HA uses (KVMInvestigator, CheckOnHostCommand), which needs no out-of-band management.
     * The check of the activity of the VMs on their volumes (KVMHAVMActivityChecker) is only done by Host HA, which needs it.
     *
     * If the host is checked on more than one storage pool, it is dead if its heartbeat is stopped on one of them when
     * reportIfHeartBeatFailedForOneStoragePool is set, or else only if its heartbeat is stopped on all of them.
     */
    @Override
    public Boolean hasHeartBeat() {
        String hostAndPools = String.format("host IP [%s] in pools [%s]", host.getPrivateNetwork().getIp(),
                storagePools.stream().map(pool -> pool.getPoolUUID()).collect(Collectors.joining(", ")));
        logger.debug("Checking heart beat with KVMHAChecker for {}", hostAndPools);

        if (storagePools.isEmpty()) {
            // this host monitors no storage pool, e.g. its agent is not upgraded yet and does not monitor the pools of the type
            // which a newer version does: it has nothing to tell about the host, which is not a dead host
            logger.debug("KVMHAChecker has no storage pool to check the heart beat for {}, so it can not tell whether the host is alive.", hostAndPools);
            return true;
        }

        boolean heartBeatCheckResult = false;
        for (HAStoragePool pool : storagePools) {
            if (pool.getPool().hasHeartBeat(pool, host)) {
                heartBeatCheckResult = true;
            } else if (reportIfHeartBeatFailedForOneStoragePool) {
                heartBeatCheckResult = false;
                break;
            }
        }

        if (!heartBeatCheckResult) {
            logger.warn("All checks with KVMHAChecker for {} considered it as dead, which may trigger HA actions, such as fencing it and restarting its VMs on other hosts.", hostAndPools);
        }

        return heartBeatCheckResult;
    }

    @Override
    public Boolean call() throws Exception {
        // logger.addAppender(new org.apache.log4j.ConsoleAppender(new
        // org.apache.log4j.PatternLayout(), "System.out"));
        return hasHeartBeat();
    }
}
