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

package com.cloud.hypervisor.xenserver.resource.wrapper.xen56;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.ExecutionResult;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  NetworkUsageCommand.class)
public final class XenServer56NetworkUsageCommandWrapper extends CommandWrapper<NetworkUsageCommand, Answer, XenServer56Resource> {

    private static final Logger s_logger = Logger.getLogger(XenServer56NetworkUsageCommandWrapper.class);

    @Override
    public Answer execute(final NetworkUsageCommand command, final XenServer56Resource xenServer56) {
        if (command.isForVpc()) {
            return executeNetworkUsage(command, xenServer56);
        }
        try {
            final Connection conn = xenServer56.getConnection();
            if (command.getOption() != null && command.getOption().equals("create")) {
                final String result = xenServer56.networkUsage(conn, command.getPrivateIP(), "create", null);
                final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, result, 0L, 0L);
                return answer;
            }
            final long[] stats = xenServer56.getNetworkStats(conn, command.getPrivateIP(), null);
            final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, "", stats[0], stats[1]);
            return answer;
        } catch (final Exception ex) {
            s_logger.warn("Failed to get network usage stats due to ", ex);
            return new NetworkUsageAnswer(command, ex);
        }
    }

    protected NetworkUsageAnswer executeNetworkUsage(final NetworkUsageCommand command, final XenServer56Resource xenServer56) {
        try {
            final String option = command.getOption();
            final String publicIp = command.getGatewayIP();

            String args = " -l " + publicIp + " ";
            if (option.equals("get")) {
                args += "-g";
            } else if (option.equals("create")) {
                args += "-c";
                final String vpcCIDR = command.getVpcCIDR();
                args += " -v " + vpcCIDR;
            } else if (option.equals("reset")) {
                args += "-r";
            } else if (option.equals("vpn")) {
                args += "-n";
            } else if (option.equals("remove")) {
                args += "-d";
            } else {
                return new NetworkUsageAnswer(command, "success", 0L, 0L);
            }

            final ExecutionResult result = xenServer56.executeInVR(command.getPrivateIP(), "vpc_netusage.sh", args);
            final String detail = result.getDetails();
            if (!result.isSuccess()) {
                throw new Exception(" vpc network usage plugin call failed ");
            }
            if (option.equals("get") || option.equals("vpn")) {
                final long[] stats = new long[2];
                if (detail != null) {
                    final String[] splitResult = detail.split(":");
                    int i = 0;
                    while (i < splitResult.length - 1) {
                        stats[0] += Long.parseLong(splitResult[i++]);
                        stats[1] += Long.parseLong(splitResult[i++]);
                    }
                    return new NetworkUsageAnswer(command, "success", stats[0], stats[1]);
                }
            }
            return new NetworkUsageAnswer(command, "success", 0L, 0L);
        } catch (final Exception ex) {
            s_logger.warn("Failed to get network usage stats due to ", ex);
            return new NetworkUsageAnswer(command, ex);
        }
    }
}
