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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  CleanupNetworkRulesCmd.class)
public final class CitrixCleanupNetworkRulesCmdWrapper extends CommandWrapper<CleanupNetworkRulesCmd, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCleanupNetworkRulesCmdWrapper.class);

    @Override
    public Answer execute(final CleanupNetworkRulesCmd command, final CitrixResourceBase citrixResourceBase) {
        if (!citrixResourceBase.canBridgeFirewall()) {
            return new Answer(command, true, null);
        }
        final Connection conn = citrixResourceBase.getConnection();

        final String result = citrixResourceBase.callHostPlugin(conn, "vmops", "cleanup_rules", "instance", citrixResourceBase.getVMInstanceName());
        final int numCleaned = Integer.parseInt(result);

        if (result == null || result.isEmpty() || numCleaned < 0) {
            s_logger.warn("Failed to cleanup rules for host " + citrixResourceBase.getHost().getIp());
            return new Answer(command, false, result);
        }

        if (numCleaned > 0) {
            s_logger.info("Cleaned up rules for " + result + " vms on host " + citrixResourceBase.getHost().getIp());
        }
        return new Answer(command, true, result);
    }
}
