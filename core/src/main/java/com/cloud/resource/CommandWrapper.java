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

package com.cloud.resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public abstract class CommandWrapper<T extends Command, A extends Answer, R extends ServerResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    /**
     * @param T is the command to be used.
     * @param R is the resource base to be used.
     * @return A and the Answer from the command.
     */
    public abstract A execute(T command, R serverResource);

    protected String sanitizeBashCommandArgument(String input) {
        StringBuilder sanitized = new StringBuilder();
        for (char c : input.toCharArray()) {
            if ("\\\"'`$|&;()<>*?![]{}~".indexOf(c) != -1) {
                sanitized.append('\\');
            }
            sanitized.append(c);
        }
        return sanitized.toString();
    }

    public void removeDpdkPort(String portToRemove) {
        logger.debug("Removing DPDK port: " + portToRemove);
        int port;
        try {
            port = Integer.valueOf(portToRemove);
        } catch (NumberFormatException nfe) {
            throw new CloudRuntimeException(String.format("Invalid DPDK port specified: '%s'", portToRemove));
        }
        Script.executeCommand("ovs-vsctl", "del-port", String.valueOf(port));
    }
}
