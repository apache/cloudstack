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


import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class KVMHAChecker extends KVMHABase implements Callable<Boolean> {
    private List<NfsStoragePool> nfsStoragePools;
    private String hostIp;
    private long heartBeatCheckerTimeout = 360000; // 6 minutes

    public KVMHAChecker(List<NfsStoragePool> pools, String host) {
        this.nfsStoragePools = pools;
        this.hostIp = host;
    }

    /*
     * True means heartbeaing is on going, or we can't get it's status. False
     * means heartbeating is stopped definitely
     */
    @Override
    public Boolean checkingHeartBeat() {
        boolean validResult = false;

        String hostAndPools = String.format("host IP [%s] in pools [%s]", hostIp, nfsStoragePools.stream().map(pool -> pool._poolIp).collect(Collectors.joining(", ")));

        logger.debug(String.format("Checking heart beat with KVMHAChecker for %s", hostAndPools));

        for (NfsStoragePool pool : nfsStoragePools) {
            Script cmd = new Script(s_heartBeatPath, heartBeatCheckerTimeout, logger);
            cmd.add("-i", pool._poolIp);
            cmd.add("-p", pool._poolMountSourcePath);
            cmd.add("-m", pool._mountDestPath);
            cmd.add("-h", hostIp);
            cmd.add("-r");
            cmd.add("-t", String.valueOf(_heartBeatUpdateFreq / 1000));
            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
            String result = cmd.execute(parser);
            String parsedLine = parser.getLine();

            logger.debug(String.format("Checking heart beat with KVMHAChecker [{command=\"%s\", result: \"%s\", log: \"%s\", pool: \"%s\"}].", cmd.toString(), result, parsedLine,
                    pool._poolIp));

            if (result == null && parsedLine.contains("DEAD")) {
                logger.warn(String.format("Checking heart beat with KVMHAChecker command [%s] returned [%s]. [%s]. It may cause a shutdown of host IP [%s].", cmd.toString(),
                        result, parsedLine, hostIp));
            } else {
                validResult = true;
            }
        }

        if (!validResult) {
            logger.warn(String.format("All checks with KVMHAChecker for %s considered it as dead. It may cause a shutdown of the host.", hostAndPools));
        }

        return validResult;
    }

    @Override
    public Boolean call() throws Exception {
        // logger.addAppender(new org.apache.log4j.ConsoleAppender(new
        // org.apache.log4j.PatternLayout(), "System.out"));
        return checkingHeartBeat();
    }
}
