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
     */
    @Override
    public Boolean hasHeartBeat() {
        String hostAndPools = String.format("host IP [%s] in pools [%s]", host.getPrivateNetwork().getIp(), storagePools.stream().map(pool -> pool.getPoolUUID()).collect(Collectors.joining(", ")));
        logger.debug("Checking heart beat with KVMHAChecker for {}", hostAndPools);

        boolean heartBeatCheckResult = false;
        for (HAStoragePool pool : storagePools) {
            heartBeatCheckResult = pool.getPool().hasHeartBeat(pool, host);
            if (reportIfHeartBeatFailedForOneStoragePool && !heartBeatCheckResult) {
                break;
            }
        }

        if (!heartBeatCheckResult) {
            logger.warn("All checks with KVMHAChecker for {} considered it as dead. It may cause a shutdown of the host.", hostAndPools);
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
