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

import com.cloud.agent.api.to.HostTO;
import org.joda.time.Duration;

import java.util.concurrent.Callable;

public class KVMHAVMActivityChecker extends KVMHABase implements Callable<Boolean> {

    private final HAStoragePool storagePool;
    private final String volumeUuidList;
    private final String vmActivityCheckPath;
    private final Duration activityScriptTimeout = Duration.standardSeconds(3600L);
    private final long suspectTimeInSeconds;
    private final HostTO host;

    public KVMHAVMActivityChecker(final HAStoragePool pool, final HostTO host, final String volumeUUIDListString, String vmActivityCheckPath, final long suspectTime) {
        this.storagePool = pool;
        this.volumeUuidList = volumeUUIDListString;
        this.vmActivityCheckPath = vmActivityCheckPath;
        this.suspectTimeInSeconds = suspectTime;
        this.host = host;
    }

    @Override
    public Boolean checkingHeartBeat() {
        return this.storagePool.getPool().vmActivityCheck(storagePool, host, activityScriptTimeout, volumeUuidList, vmActivityCheckPath, suspectTimeInSeconds);
    }

    @Override
    public Boolean call() throws Exception {
        return checkingHeartBeat();
    }
}
