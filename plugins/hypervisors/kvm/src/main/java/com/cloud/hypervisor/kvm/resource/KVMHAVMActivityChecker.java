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

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.storage.Storage.StoragePoolType;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import java.util.concurrent.Callable;

public class KVMHAVMActivityChecker extends KVMHABase implements Callable<Boolean> {
    private static final Logger LOG = Logger.getLogger(KVMHAVMActivityChecker.class);

    final private NfsStoragePool nfsStoragePool;
    final private RbdStoragePool rbdStoragePool;
    final private String hostIP;
    final private String volumeUuidList;
    final private String vmActivityCheckPath;
    final private Duration activityScriptTimeout = Duration.standardSeconds(3600L);
    final private long suspectTimeInSeconds;
    final private StoragePoolType poolType;

    public KVMHAVMActivityChecker(final NfsStoragePool pool, final RbdStoragePool rbdpool, final String host, final String volumeUUIDListString, String vmActivityCheckPath, final long suspectTime, StoragePoolType poolType) {
        this.nfsStoragePool = pool;
        this.rbdStoragePool = rbdpool;
        this.hostIP = host;
        this.volumeUuidList = volumeUUIDListString;
        this.vmActivityCheckPath = vmActivityCheckPath;
        this.suspectTimeInSeconds = suspectTime;
        this.poolType = poolType;
    }

    @Override
    public Boolean checkingHeartBeat() {
        Script cmd = new Script(vmActivityCheckPath, activityScriptTimeout.getStandardSeconds(), LOG);
        String poolIp = "";
        if (poolType == StoragePoolType.NetworkFilesystem) {
            cmd.add("-i", nfsStoragePool._poolIp);
            cmd.add("-p", nfsStoragePool._poolMountSourcePath);
            cmd.add("-m", nfsStoragePool._mountDestPath);
            cmd.add("-t", String.valueOf(String.valueOf(System.currentTimeMillis() / 1000)));
            cmd.add("-d", String.valueOf(suspectTimeInSeconds));
            poolIp = nfsStoragePool._poolIp;
        } else if (poolType == StoragePoolType.RBD) {
            cmd.add("-i", rbdStoragePool._poolSourceHost);
            cmd.add("-p", rbdStoragePool._poolMountSourcePath);
            cmd.add("-n", rbdStoragePool._poolAuthUserName);
            cmd.add("-s", rbdStoragePool._poolAuthSecret);
            poolIp = rbdStoragePool._poolIp;
        }
        cmd.add("-h", hostIP);
        cmd.add("-u", volumeUuidList);
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

        String result = cmd.execute(parser);
        String parsedLine = parser.getLine();

        LOG.debug(String.format("Checking heart beat with KVMHAVMActivityChecker [{command=\"%s\", result: \"%s\", log: \"%s\", pool: \"%s\"}].", cmd.toString(), result, parsedLine, poolIp));

        if (result == null && parsedLine.contains("DEAD")) {
            LOG.warn(String.format("Checking heart beat with KVMHAVMActivityChecker command [%s] returned [%s]. It is [%s]. It may cause a shutdown of host IP [%s].", cmd.toString(), result, parsedLine, hostIP));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Boolean call() throws Exception {
        return checkingHeartBeat();
    }
}
