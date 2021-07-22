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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class KVMHAChecker extends KVMHABase implements Callable<Boolean> {
    private static final Logger s_logger = Logger.getLogger(KVMHAChecker.class);
    private List<NfsStoragePool> _pools;
    private String _hostIP;
    private long _heartBeatCheckerTimeout = 360000; /* 6 minutes */

    public KVMHAChecker(List<NfsStoragePool> pools, String host) {
        this._pools = pools;
        this._hostIP = host;
    }

    /*
     * True means heartbeaing is on going, or we can't get it's status. False
     * means heartbeating is stopped definitely
     */
    @Override
    public Boolean checkingHeartBeat() {
        List<Boolean> results = new ArrayList<Boolean>();
        for (NfsStoragePool pool : _pools) {
            Script cmd = new Script(s_heartBeatPath, _heartBeatCheckerTimeout, s_logger);
            cmd.add("-i", pool._poolIp);
            cmd.add("-p", pool._poolMountSourcePath);
            cmd.add("-m", pool._mountDestPath);
            cmd.add("-h", _hostIP);
            cmd.add("-r");
            cmd.add("-t", String.valueOf(_heartBeatUpdateFreq / 1000));
            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
            String result = cmd.execute(parser);
            s_logger.debug("KVMHAChecker pool: " + pool._poolIp);
            s_logger.debug("KVMHAChecker result: " + result);
            s_logger.debug("KVMHAChecker parser: " + parser.getLine());
            if (result == null && parser.getLine().contains("> DEAD <")) {
                s_logger.debug("read heartbeat failed: ");
                results.add(false);
            } else {
                results.add(true);
            }
        }

        for (Boolean r : results) {
            if (r) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Boolean call() throws Exception {
        // s_logger.addAppender(new org.apache.log4j.ConsoleAppender(new
        // org.apache.log4j.PatternLayout(), "System.out"));
        return checkingHeartBeat();
    }
}
