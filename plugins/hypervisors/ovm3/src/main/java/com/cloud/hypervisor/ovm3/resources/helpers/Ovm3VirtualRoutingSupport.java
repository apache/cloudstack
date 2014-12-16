package com.cloud.hypervisor.ovm3.resources.helpers;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.resources.Ovm3VirtualRoutingResource;
import com.cloud.utils.ExecutionResult;

public class Ovm3VirtualRoutingSupport {
    private final Logger LOGGER = Logger
            .getLogger(Ovm3VirtualRoutingSupport.class);
    private Connection c;
    private Ovm3VirtualRoutingResource vrr;
    private Ovm3Configuration config;
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Executing resource NetworkUsageCommand " + cmd);
        }
        if (cmd.getOption() != null && "create".equals(cmd.getOption())) {
            String result = networkUsage(cmd.getPrivateIP(), "create", null);
            return new NetworkUsageAnswer(cmd, result, 0L, 0L);
        }
        long[] stats = getNetworkStats(cmd.getPrivateIP());

        return new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    public String networkUsage(final String privateIpAddress,
            final String option, final String ethName) {
        String args = null;
        if ("get".equals(option)) {
            args = "-g";
        } else if ("create".equals(option)) {
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
    public long[] getNetworkStats(String privateIP) {
        String result = networkUsage(privateIP, "get", null);
        long[] stats = new long[2];
        if (result != null) {
            try {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += (Long.valueOf(splitResult[i++])).longValue();
                    stats[1] += (Long.valueOf(splitResult[i++])).longValue();
                }
            } catch (Exception e) {
                LOGGER.warn(
                        "Unable to parse return from script return of network usage command: "
                                + e.toString(), e);
            }
        }
        return stats;
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    public NetworkUsageAnswer vpcNetworkUsage(NetworkUsageCommand cmd) {
        String privateIp = cmd.getPrivateIP();
        String option = cmd.getOption();
        String publicIp = cmd.getGatewayIP();

        String args = "-l " + publicIp + " ";
        if ("get".equals(option)) {
            args += "-g";
        } else if ("create".equals(option)) {
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
            return new NetworkUsageAnswer(cmd, "success", 0L, 0L);
        }

        ExecutionResult callResult = vrr.executeInVR(privateIp, "vpc_netusage.sh",
                args);

        if (!callResult.isSuccess()) {
            LOGGER.error("Unable to execute NetworkUsage command on DomR ("
                    + privateIp
                    + "), domR may not be ready yet. failure due to "
                    + callResult.getDetails());
        }

        if ("get".equals(option) || "vpn".equals(option)) {
            String result = callResult.getDetails();
            if (result == null || result.isEmpty()) {
                LOGGER.error(" vpc network usage get returns empty ");
            }
            long[] stats = new long[2];
            if (result != null) {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += (Long.valueOf(splitResult[i++])).longValue();
                    stats[1] += (Long.valueOf(splitResult[i++])).longValue();
                }
                return new NetworkUsageAnswer(cmd, "success", stats[0],
                        stats[1]);
            }
        }
        return new NetworkUsageAnswer(cmd, "success", 0L, 0L);
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
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            if (!cSp.domrCheckPort(privateIp, cmdPort, retries, interval)) {
                String msg = "Port " + cmdPort + " not reachable for " + vmName
                        + " via " + config.getAgentHostname();
                LOGGER.info(msg);
                return new CheckSshAnswer(cmd, msg);
            }
        } catch (Exception e) {
            String msg = "Can not reach port " + cmdPort + " on System vm "
                    + vmName + " via " + config.getAgentHostname()
                    + " due to exception: " + e;
            LOGGER.error(msg);
            return new CheckSshAnswer(cmd, msg);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ping " + cmdPort + " succeeded for vm " + vmName
                    + " via " + config.getAgentHostname() + cmd);
        }
        return new CheckSshAnswer(cmd);
    }
}
