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

package com.cloud.hypervisor.xenserver.resource.wrapper.xcp;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.hypervisor.xenserver.resource.XcpServerResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  NetworkUsageCommand.class)
public final class XcpServerNetworkUsageCommandWrapper extends CommandWrapper<NetworkUsageCommand, Answer, XcpServerResource> {

    private static final Logger s_logger = Logger.getLogger(XcpServerNetworkUsageCommandWrapper.class);

    @Override
    public Answer execute(final NetworkUsageCommand command, final XcpServerResource xcpServerResource) {
        try {
            final Connection conn = xcpServerResource.getConnection();
            if (command.getOption() != null && command.getOption().equals("create")) {
                final String result = xcpServerResource.networkUsage(conn, command.getPrivateIP(), "create", null);
                final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, result, 0L, 0L);
                return answer;
            }
            final long[] stats = xcpServerResource.getNetworkStats(conn, command.getPrivateIP(), null);
            final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, "", stats[0], stats[1]);
            return answer;
        } catch (final Exception ex) {
            s_logger.warn("Failed to get network usage stats due to ", ex);
            return new NetworkUsageAnswer(command, ex);
        }
    }
}
