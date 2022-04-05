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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.concurrent.Callable;

public class KVMHAVMActivityChecker extends KVMHABase implements Callable<Boolean> {
    private static final Logger LOG = Logger.getLogger(KVMHAVMActivityChecker.class);

    final private NfsStoragePool nfsStoragePool;
    final private RbdStoragePool rbdStoragePool;
    final private String hostIp;
    final private String volumeUuidList;
    final private String vmActivityCheckPath;
    final private Duration activityScriptTimeout = Duration.standardSeconds(3600L);
    final private long suspectTimeInSeconds;
    final private StoragePoolType poolType;

    public KVMHAVMActivityChecker(final NfsStoragePool pool, final RbdStoragePool rbdpool, final String host, final String volumeUUIDListString, String vmActivityCheckPath, final long suspectTime, StoragePoolType poolType) {
        this.nfsStoragePool = pool;
        this.rbdStoragePool = rbdpool;
        this.hostIp = host;
        this.volumeUuidList = volumeUUIDListString;
        this.vmActivityCheckPath = vmActivityCheckPath;
        this.suspectTimeInSeconds = suspectTime;
        this.poolType = poolType;
    }

    @Override
    public Boolean checkingHeartBeat() {
        String parsedLine = "";
        String command = "";
        if (poolType == StoragePoolType.NetworkFilesystem) {
            Script cmd = new Script(vmActivityCheckPath, activityScriptTimeout.getStandardSeconds(), LOG);
            cmd.add("-i", nfsStoragePool._poolIp);
            cmd.add("-p", nfsStoragePool._poolMountSourcePath);
            cmd.add("-m", nfsStoragePool._mountDestPath);
            cmd.add("-t", String.valueOf(String.valueOf(System.currentTimeMillis() / 1000)));
            cmd.add("-d", String.valueOf(suspectTimeInSeconds));
            cmd.add("-h", hostIp);
            cmd.add("-u", volumeUuidList);

            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

            String result = cmd.execute(parser);
            parsedLine = parser.getLine();
            command = cmd.toString();

            LOG.debug(String.format("Checking heart beat with KVMHAVMActivityChecker [{command=\"%s\", result: \"%s\", log: \"%s\", pool: \"%s\"}].", cmd.toString(), result, parsedLine, nfsStoragePool._poolIp));

        } else if (poolType == StoragePoolType.RBD) {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command().add("python3");
            processBuilder.command().add(vmActivityCheckPath);
            processBuilder.command().add("-i");
            processBuilder.command().add(rbdStoragePool._poolSourceHost);
            processBuilder.command().add("-p");
            processBuilder.command().add(rbdStoragePool._poolMountSourcePath);
            processBuilder.command().add("-n");
            processBuilder.command().add(rbdStoragePool._poolAuthUserName);
            processBuilder.command().add("-s");
            processBuilder.command().add(rbdStoragePool._poolAuthSecret);
            processBuilder.command().add("-v");
            processBuilder.command().add(hostIp);
            processBuilder.command().add("-u");
            processBuilder.command().add(volumeUuidList);
            command = processBuilder.command().toString();
            Process process = null;

            try {
                process = processBuilder.start();
                BufferedReader bfr = new BufferedReader(new InputStreamReader(process.getInputStream()));
                parsedLine = bfr.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            LOG.debug(String.format("Checking heart beat with KVMHAVMActivityChecker [{command=\"%s\", log: \"%s\", pool: \"%s\"}].", command, parsedLine, rbdStoragePool._monHost));

        }
        if (parsedLine.contains("DEAD")) {
            LOG.warn(String.format("Checking heart beat with KVMHAVMActivityChecker command [%s]. It is [%s]. It may cause a shutdown of host IP [%s].", command, parsedLine, hostIp));
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
