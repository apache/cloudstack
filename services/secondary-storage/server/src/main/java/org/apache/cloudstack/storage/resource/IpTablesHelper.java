//
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
//

package org.apache.cloudstack.storage.resource;

import com.cloud.utils.script.Script;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class IpTablesHelper {
    public static final Logger LOGGER = LogManager.getLogger(IpTablesHelper.class);

    public static final String OUTPUT_CHAIN = "OUTPUT";
    public static final String INPUT_CHAIN = "INPUT";
    public static final String INSERT = " -I ";
    public static final String APPEND = " -A ";

    public static boolean needsAdding(String chain, String rule) {
        Script command = new Script("/bin/bash", LOGGER);
        command.add("-c");
        command.add("iptables -C " + chain + " " + rule);

        String commandOutput = command.execute();
        boolean needsAdding = (commandOutput != null && commandOutput.contains("iptables: Bad rule (does a matching rule exist in that chain?)."));
        LOGGER.debug(String.format("Rule [%s], %s need adding to [%s] : %s",
                rule,
                needsAdding ? "does indeed" : "doesn't",
                chain,
                commandOutput
        ));
        return needsAdding;
    }

    public static String addConditionally(String chain, boolean insert, String rule, String errMsg) {
        LOGGER.info(String.format("Adding rule [%s] to [%s] if required.", rule, chain));
        if (needsAdding(chain, rule)) {
            Script command = new Script("/bin/bash", LOGGER);
            command.add("-c");
            command.add("iptables" + (insert ? INSERT : APPEND) + chain + " " + rule);
            String result = command.execute();
            LOGGER.debug(String.format("Executed [%s] with result [%s]", command, result));
            if (result != null) {
                LOGGER.warn(String.format("%s , err = %s", errMsg, result));
                return errMsg + result;
            }
        } else {
            LOGGER.warn("Rule already defined in SVM: " + rule);
        }
        return null;
    }
}
