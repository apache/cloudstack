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

package com.cloud.hypervisor.ovm3.resources.helpers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.resources.Ovm3VirtualRoutingResource;
import com.cloud.utils.ExecutionResult;

public class Ovm3VirtualRoutingSupport {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final String CREATE = "create";
    private static final String SUCCESS = "success";
    private final Connection c;
    private final Ovm3VirtualRoutingResource vrr;
    private final Ovm3Configuration config;
    public Ovm3VirtualRoutingSupport(Connection conn, Ovm3Configuration ovm3config, Ovm3VirtualRoutingResource ovm3vrr) {
        c = conn;
        vrr = ovm3vrr;
        config = ovm3config;
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    public Answer execute(NetworkUsageCommand cmd) {
        if (cmd.isForVpc()) {
            return vpcNetworkUsage(cmd);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Executing resource NetworkUsageCommand " + cmd);
        }
        if (cmd.getOption() != null && CREATE.equals(cmd.getOption())) {
            String result = networkUsage(cmd.getPrivateIP(), CREATE, null);
            return new NetworkUsageAnswer(cmd, result, 0L, 0L);
        }
        long[] stats = getNetworkStats(cmd.getPrivateIP());

        return new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    private String networkUsage(final String privateIpAddress,
            final String option, final String ethName) {
        String args = null;
        if ("get".equals(option)) {
            args = "-g";
        } else if (CREATE.equals(option)) {
            args = "-c";
        } else if ("reset".equals(option)) {
            args = "-r";
        } else if ("addVif".equals(option)) {
            args = "-a";
            args += ethName;
        } else if ("deleteVif".equals(option)) {
            args = "-d";
            args += ethName;
        }
        ExecutionResult result = vrr.executeInVR(privateIpAddress, "netusage.sh",
                args);

        if (result == null || !result.isSuccess()) {
            return null;
        }

        return result.getDetails();
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    private long[] getNetworkStats(String privateIP) {
        String result = networkUsage(privateIP, "get", null);
        long[] stats = new long[2];
        if (result != null) {
            try {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += (Long.parseLong(splitResult[i++]));
                    stats[1] += (Long.parseLong(splitResult[i++]));
                }
            } catch (Exception e) {
                logger.warn(
                        "Unable to parse return from script return of network usage command: "
                                + e.toString(), e);
            }
        }
        return stats;
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    private NetworkUsageAnswer vpcNetworkUsage(NetworkUsageCommand cmd) {
        String privateIp = cmd.getPrivateIP();
        String option = cmd.getOption();
        String publicIp = cmd.getGatewayIP();

        String args = "-l " + publicIp + " ";
        if ("get".equals(option)) {
            args += "-g";
        } else if (CREATE.equals(option)) {
            args += "-c";
            String vpcCIDR = cmd.getVpcCIDR();
            args += " -v " + vpcCIDR;
        } else if ("reset".equals(option)) {
            args += "-r";
        } else if ("vpn".equals(option)) {
            args += "-n";
        } else if ("remove".equals(option)) {
            args += "-d";
        } else {
            return new NetworkUsageAnswer(cmd, SUCCESS, 0L, 0L);
        }

        ExecutionResult callResult = vrr.executeInVR(privateIp, "vpc_netusage.sh",
                args);

        if (!callResult.isSuccess()) {
            logger.error("Unable to execute NetworkUsage command on DomR ("
                    + privateIp
                    + "), domR may not be ready yet. failure due to "
                    + callResult.getDetails());
        }

        if ("get".equals(option) || "vpn".equals(option)) {
            String result = callResult.getDetails();
            if (result == null || result.isEmpty()) {
                logger.error(" vpc network usage get returns empty ");
            }
            long[] stats = new long[2];
            if (result != null) {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += (Long.parseLong(splitResult[i++]));
                    stats[1] += (Long.parseLong(splitResult[i++]));
                }
                return new NetworkUsageAnswer(cmd, SUCCESS, stats[0],
                        stats[1]);
            }
        }
        return new NetworkUsageAnswer(cmd, SUCCESS, 0L, 0L);
    }

    /*
     * we don't for now, gave an error on migration though....
     */
    public Answer execute(NetworkRulesSystemVmCommand cmd) {
        boolean success = true;
        return new Answer(cmd, success, "");
    }

    public CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();
        int interval = cmd.getInterval();
        int retries = cmd.getRetries();

        try {
            CloudstackPlugin cSp = new CloudstackPlugin(c);
            if (!cSp.dom0CheckPort(privateIp, cmdPort, retries, interval)) {
                String msg = "Port " + cmdPort + " not reachable for " + vmName
                        + ": " + config.getAgentHostname();
                logger.info(msg);
                return new CheckSshAnswer(cmd, msg);
            }
        } catch (Exception e) {
            String msg = "Can not reach port " + cmdPort + " on System vm "
                    + vmName + ": " + config.getAgentHostname()
                    + " due to exception: " + e;
            logger.error(msg);
            return new CheckSshAnswer(cmd, msg);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Ping " + cmdPort + " succeeded for vm " + vmName
                    + ": " + config.getAgentHostname() + " " + cmd);
        }
        return new CheckSshAnswer(cmd);
    }
}
