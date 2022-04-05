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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class KVMHAChecker extends KVMHABase implements Callable<Boolean> {
    private static final Logger s_logger = Logger.getLogger(KVMHAChecker.class);
    private List<NfsStoragePool> nfsStoragePools;
    private List<RbdStoragePool> rbdStoragePools;
    private String hostIp;
    private long heartBeatCheckerTimeout = 360000; // 6 minutes

    public KVMHAChecker(List<NfsStoragePool> nfspools, List<RbdStoragePool> rbdpools, String host) {
        this.nfsStoragePools = nfspools;
        this.rbdStoragePools = rbdpools;
        this.hostIp = host;
    }

    /*
     * True means heartbeaing is on going, or we can't get it's status. False
     * means heartbeating is stopped definitely
     */
    @Override
    public Boolean checkingHeartBeat() {
        boolean validResult = false;

        String hostAndPools = "";

        s_logger.debug(String.format("Checking heart beat with KVMHAChecker for %s", hostAndPools));

        for (NfsStoragePool nfspools : nfsStoragePools) {
            hostAndPools = String.format("host IP [%s] in pools [%s]", hostIp, nfsStoragePools.stream().map(pool -> pool._poolIp).collect(Collectors.joining(", ")));
            s_logger.debug(String.format("Checking heart beat with KVMHAChecker for %s", hostAndPools));

            Script cmd = new Script(s_heartBeatPath, heartBeatCheckerTimeout, s_logger);
            cmd.add("-i", nfspools._poolIp);
            cmd.add("-p", nfspools._poolMountSourcePath);
            cmd.add("-m", nfspools._mountDestPath);
            cmd.add("-h", hostIp);
            cmd.add("-r");
            cmd.add("-t", String.valueOf(_heartBeatUpdateFreq / 1000));
            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
            String result = cmd.execute(parser);
            String parsedLine = parser.getLine();

            s_logger.debug(String.format("Checking heart beat with KVMHAChecker [{command=\"%s\", result: \"%s\", log: \"%s\", pool: \"%s\"}].", cmd.toString(), result, parsedLine,
            nfspools._poolIp));

            if (result == null && parsedLine.contains("DEAD")) {
                s_logger.warn(String.format("Checking heart beat with KVMHAChecker command [%s] returned [%s]. [%s]. It may cause a shutdown of host IP [%s].", cmd.toString(),
                        result, parsedLine, hostIp));
            } else {
                validResult = true;
            }
        }

        for (RbdStoragePool rbdpools : rbdStoragePools) {
            hostAndPools = String.format("host IP [%s] in pools [%s]", hostIp, rbdStoragePools.stream().map(pool -> pool._monHost).collect(Collectors.joining(", ")));
            s_logger.debug(String.format("Checking heart beat with KVMHAChecker for %s", hostAndPools));

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command().add("python3");
            processBuilder.command().add(s_heartBeatPathRbd);
            processBuilder.command().add("-i");
            processBuilder.command().add(rbdpools._poolSourceHost);
            processBuilder.command().add("-p");
            processBuilder.command().add(rbdpools._poolMountSourcePath);
            processBuilder.command().add("-n");
            processBuilder.command().add(rbdpools._poolAuthUserName);
            processBuilder.command().add("-s");
            processBuilder.command().add(rbdpools._poolAuthSecret);
            processBuilder.command().add("-v");
            processBuilder.command().add(hostIp);
            processBuilder.command().add("-r");
            processBuilder.command().add("r");
            Process process = null;
            String parsedLine = "";
            try {
                process = processBuilder.start();
                BufferedReader bfr = new BufferedReader(new InputStreamReader(process.getInputStream()));
                parsedLine = bfr.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            s_logger.debug(String.format("Checking heart beat with KVMHAChecker [{command=\"%s\", log: \"%s\", pool: \"%s\"}].", processBuilder.command().toString(), parsedLine,
            rbdpools._monHost));

            if (process != null && parsedLine.contains("DEAD")) {
                s_logger.warn(String.format("Checking heart beat with KVMHAChecker command [%s] returned. [%s]. It may cause a shutdown of host IP [%s].", processBuilder.command().toString(),
                        parsedLine, hostIp));
            } else {
                validResult = true;
            }
        }

        if (!validResult) {
            s_logger.warn(String.format("All checks with KVMHAChecker for %s considered it as dead. It may cause a shutdown of the host.", hostAndPools));
        }

        return validResult;
    }

    @Override
    public Boolean call() throws Exception {
        // s_logger.addAppender(new org.apache.log4j.ConsoleAppender(new
        // org.apache.log4j.PatternLayout(), "System.out"));
        return checkingHeartBeat();
    }
}
