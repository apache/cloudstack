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

package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalRouterAnswer;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupNiciraNvpCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.DestinationNatRule;
import com.cloud.network.nicira.L3GatewayAttachment;
import com.cloud.network.nicira.LogicalRouter;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.Match;
import com.cloud.network.nicira.NatRule;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpList;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.PatchAttachment;
import com.cloud.network.nicira.RouterNextHop;
import com.cloud.network.nicira.SingleDefaultRouteImplicitRoutingConfig;
import com.cloud.network.nicira.SourceNatRule;
import com.cloud.network.nicira.TransportZoneBinding;
import com.cloud.network.nicira.VifAttachment;
import com.cloud.resource.ServerResource;

public class NiciraNvpResource implements ServerResource {
    private static final int NAME_MAX_LEN = 40;

    private static final Logger s_logger = Logger.getLogger(NiciraNvpResource.class);

    private String name;
    private String guid;
    private String zoneId;
    private int numRetries;

    private NiciraNvpApi niciraNvpApi;

    protected NiciraNvpApi createNiciraNvpApi() {
        return new NiciraNvpApi();
    }

    @Override
    public boolean configure(final String ignoredName, final Map<String, Object> params) throws ConfigurationException {

        name = (String)params.get("name");
        if (name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        guid = (String)params.get("guid");
        if (guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        zoneId = (String)params.get("zoneId");
        if (zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        numRetries = 2;

        final String ip = (String)params.get("ip");
        if (ip == null) {
            throw new ConfigurationException("Unable to find IP");
        }

        final String adminuser = (String)params.get("adminuser");
        if (adminuser == null) {
            throw new ConfigurationException("Unable to find admin username");
        }

        final String adminpass = (String)params.get("adminpass");
        if (adminpass == null) {
            throw new ConfigurationException("Unable to find admin password");
        }

        niciraNvpApi = createNiciraNvpApi();
        niciraNvpApi.setControllerAddress(ip);
        niciraNvpApi.setAdminCredentials(adminuser, adminpass);

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        // Think up a better name for this Type?
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        final StartupNiciraNvpCommand sc = new StartupNiciraNvpCommand();
        sc.setGuid(guid);
        sc.setName(name);
        sc.setDataCenter(zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion(NiciraNvpResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        try {
            final ControlClusterStatus ccs = niciraNvpApi.getControlClusterStatus();
            if (!"stable".equals(ccs.getClusterStatus())) {
                s_logger.error("ControlCluster state is not stable: " + ccs.getClusterStatus());
                return null;
            }
        } catch (final NiciraNvpApiException e) {
            s_logger.error("getControlClusterStatus failed", e);
            return null;
        }
        return new PingCommand(Host.Type.L2Networking, id);
    }

    @Override
    public Answer executeRequest(final Command cmd) {
        return executeRequest(cmd, numRetries);
    }

    public Answer executeRequest(final Command cmd, final int numRetries) {

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        try {
            return wrapper.execute(cmd, this);
        } catch (final Exception e) {
            //return Answer.createUnsupportedCommandAnswer(cmd);
            // [TODO] Remove when all the commands are refactored.
        }

        if (cmd instanceof CreateLogicalSwitchCommand) {
            return executeRequest((CreateLogicalSwitchCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteLogicalSwitchCommand) {
            return executeRequest((DeleteLogicalSwitchCommand)cmd, numRetries);
        } else if (cmd instanceof CreateLogicalSwitchPortCommand) {
            return executeRequest((CreateLogicalSwitchPortCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteLogicalSwitchPortCommand) {
            return executeRequest((DeleteLogicalSwitchPortCommand)cmd, numRetries);
        } else if (cmd instanceof UpdateLogicalSwitchPortCommand) {
            return executeRequest((UpdateLogicalSwitchPortCommand)cmd, numRetries);
        } else if (cmd instanceof FindLogicalSwitchPortCommand) {
            return executeRequest((FindLogicalSwitchPortCommand)cmd, numRetries);
        } else if (cmd instanceof CreateLogicalRouterCommand) {
            return executeRequest((CreateLogicalRouterCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteLogicalRouterCommand) {
            return executeRequest((DeleteLogicalRouterCommand)cmd, numRetries);
        } else if (cmd instanceof ConfigureStaticNatRulesOnLogicalRouterCommand) {
            return executeRequest((ConfigureStaticNatRulesOnLogicalRouterCommand)cmd, numRetries);
        } else if (cmd instanceof ConfigurePortForwardingRulesOnLogicalRouterCommand) {
            return executeRequest((ConfigurePortForwardingRulesOnLogicalRouterCommand)cmd, numRetries);
        } else if (cmd instanceof ConfigurePublicIpsOnLogicalRouterCommand) {
            return executeRequest((ConfigurePublicIpsOnLogicalRouterCommand)cmd, numRetries);
        }
        s_logger.debug("Received unsupported command " + cmd.toString());
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(final IAgentControl agentControl) {
    }

    private Answer executeRequest(final CreateLogicalSwitchCommand cmd, int numRetries) {
        LogicalSwitch logicalSwitch = new LogicalSwitch();
        logicalSwitch.setDisplayName(truncate("lswitch-" + cmd.getName(), NAME_MAX_LEN));
        logicalSwitch.setPortIsolationEnabled(false);

        // Set transport binding
        final List<TransportZoneBinding> ltzb = new ArrayList<TransportZoneBinding>();
        ltzb.add(new TransportZoneBinding(cmd.getTransportUuid(), cmd.getTransportType()));
        logicalSwitch.setTransportZones(ltzb);

        // Tags set to scope cs_account and account name
        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", cmd.getOwnerName()));
        logicalSwitch.setTags(tags);

        try {
            logicalSwitch = niciraNvpApi.createLogicalSwitch(logicalSwitch);
            return new CreateLogicalSwitchAnswer(cmd, true, "Logicalswitch " + logicalSwitch.getUuid() + " created", logicalSwitch.getUuid());
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new CreateLogicalSwitchAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(final DeleteLogicalSwitchCommand cmd, int numRetries) {
        try {
            niciraNvpApi.deleteLogicalSwitch(cmd.getLogicalSwitchUuid());
            return new DeleteLogicalSwitchAnswer(cmd, true, "Logicalswitch " + cmd.getLogicalSwitchUuid() + " deleted");
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new DeleteLogicalSwitchAnswer(cmd, e);
            }
        }
    }

    private Answer executeRequest(final CreateLogicalSwitchPortCommand cmd, int numRetries) {
        final String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        final String attachmentUuid = cmd.getAttachmentUuid();

        try {
            // Tags set to scope cs_account and account name
            final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account", cmd.getOwnerName()));

            final LogicalSwitchPort logicalSwitchPort = new LogicalSwitchPort(attachmentUuid, tags, true);
            final LogicalSwitchPort newPort = niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, logicalSwitchPort);
            try {
                niciraNvpApi.updateLogicalSwitchPortAttachment(cmd.getLogicalSwitchUuid(), newPort.getUuid(), new VifAttachment(attachmentUuid));
            } catch (final NiciraNvpApiException ex) {
                s_logger.warn("modifyLogicalSwitchPort failed after switchport was created, removing switchport");
                niciraNvpApi.deleteLogicalSwitchPort(cmd.getLogicalSwitchUuid(), newPort.getUuid());
                throw ex; // Rethrow the original exception
            }
            return new CreateLogicalSwitchPortAnswer(cmd, true, "Logical switch port " + newPort.getUuid() + " created", newPort.getUuid());
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new CreateLogicalSwitchPortAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(final DeleteLogicalSwitchPortCommand cmd, int numRetries) {
        try {
            niciraNvpApi.deleteLogicalSwitchPort(cmd.getLogicalSwitchUuid(), cmd.getLogicalSwitchPortUuid());
            return new DeleteLogicalSwitchPortAnswer(cmd, true, "Logical switch port " + cmd.getLogicalSwitchPortUuid() + " deleted");
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new DeleteLogicalSwitchPortAnswer(cmd, e);
            }
        }
    }

    private Answer executeRequest(final UpdateLogicalSwitchPortCommand cmd, int numRetries) {
        final String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        final String logicalSwitchPortUuid = cmd.getLogicalSwitchPortUuid();
        final String attachmentUuid = cmd.getAttachmentUuid();

        try {
            // Tags set to scope cs_account and account name
            final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account", cmd.getOwnerName()));

            niciraNvpApi.updateLogicalSwitchPortAttachment(logicalSwitchUuid, logicalSwitchPortUuid, new VifAttachment(attachmentUuid));
            return new UpdateLogicalSwitchPortAnswer(cmd, true, "Attachment for  " + logicalSwitchPortUuid + " updated", logicalSwitchPortUuid);
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new UpdateLogicalSwitchPortAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(final FindLogicalSwitchPortCommand cmd, int numRetries) {
        final String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        final String logicalSwitchPortUuid = cmd.getLogicalSwitchPortUuid();

        try {
            final NiciraNvpList<LogicalSwitchPort> ports = niciraNvpApi.findLogicalSwitchPortsByUuid(logicalSwitchUuid, logicalSwitchPortUuid);
            if (ports.getResultCount() == 0) {
                return new FindLogicalSwitchPortAnswer(cmd, false, "Logical switchport " + logicalSwitchPortUuid + " not found", null);
            } else {
                return new FindLogicalSwitchPortAnswer(cmd, true, "Logical switchport " + logicalSwitchPortUuid + " found", logicalSwitchPortUuid);
            }
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new FindLogicalSwitchPortAnswer(cmd, e);
            }
        }
    }

    private Answer executeRequest(final CreateLogicalRouterCommand cmd, int numRetries) {
        final String routerName = cmd.getName();
        final String gatewayServiceUuid = cmd.getGatewayServiceUuid();
        final String logicalSwitchUuid = cmd.getLogicalSwitchUuid();

        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", cmd.getOwnerName()));

        final String publicNetworkNextHopIp = cmd.getPublicNextHop();
        final String publicNetworkIpAddress = cmd.getPublicIpCidr();
        final String internalNetworkAddress = cmd.getInternalIpCidr();

        s_logger.debug("Creating a logical router with external ip " + publicNetworkIpAddress + " and internal ip " + internalNetworkAddress + "on gateway service " +
                gatewayServiceUuid);

        try {
            // Create the Router
            LogicalRouter lrc = new LogicalRouter();
            lrc.setDisplayName(truncate(routerName, NAME_MAX_LEN));
            lrc.setTags(tags);
            lrc.setRoutingConfig(new SingleDefaultRouteImplicitRoutingConfig(new RouterNextHop(publicNetworkNextHopIp)));
            lrc = niciraNvpApi.createLogicalRouter(lrc);

            // store the switchport for rollback
            LogicalSwitchPort lsp = null;

            try {
                // Create the outside port for the router
                LogicalRouterPort lrpo = new LogicalRouterPort();
                lrpo.setAdminStatusEnabled(true);
                lrpo.setDisplayName(truncate(routerName + "-outside-port", NAME_MAX_LEN));
                lrpo.setTags(tags);
                final List<String> outsideIpAddresses = new ArrayList<String>();
                outsideIpAddresses.add(publicNetworkIpAddress);
                lrpo.setIpAddresses(outsideIpAddresses);
                lrpo = niciraNvpApi.createLogicalRouterPort(lrc.getUuid(), lrpo);

                // Attach the outside port to the gateway service on the correct VLAN
                final L3GatewayAttachment attachment = new L3GatewayAttachment(gatewayServiceUuid);
                if (cmd.getVlanId() != 0) {
                    attachment.setVlanId(cmd.getVlanId());
                }
                niciraNvpApi.updateLogicalRouterPortAttachment(lrc.getUuid(), lrpo.getUuid(), attachment);

                // Create the inside port for the router
                LogicalRouterPort lrpi = new LogicalRouterPort();
                lrpi.setAdminStatusEnabled(true);
                lrpi.setDisplayName(truncate(routerName + "-inside-port", NAME_MAX_LEN));
                lrpi.setTags(tags);
                final List<String> insideIpAddresses = new ArrayList<String>();
                insideIpAddresses.add(internalNetworkAddress);
                lrpi.setIpAddresses(insideIpAddresses);
                lrpi = niciraNvpApi.createLogicalRouterPort(lrc.getUuid(), lrpi);

                // Create the inside port on the lswitch
                lsp = new LogicalSwitchPort(truncate(routerName + "-inside-port", NAME_MAX_LEN), tags, true);
                lsp = niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, lsp);

                // Attach the inside router port to the lswitch port with a PatchAttachment
                niciraNvpApi.updateLogicalRouterPortAttachment(lrc.getUuid(), lrpi.getUuid(), new PatchAttachment(lsp.getUuid()));

                // Attach the inside lswitch port to the router with a PatchAttachment
                niciraNvpApi.updateLogicalSwitchPortAttachment(logicalSwitchUuid, lsp.getUuid(), new PatchAttachment(lrpi.getUuid()));

                // Setup the source nat rule
                final SourceNatRule snr = new SourceNatRule();
                snr.setToSourceIpAddressMin(publicNetworkIpAddress.split("/")[0]);
                snr.setToSourceIpAddressMax(publicNetworkIpAddress.split("/")[0]);
                final Match match = new Match();
                match.setSourceIpAddresses(internalNetworkAddress);
                snr.setMatch(match);
                snr.setOrder(200);
                niciraNvpApi.createLogicalRouterNatRule(lrc.getUuid(), snr);
            } catch (final NiciraNvpApiException e) {
                // We need to destroy the router if we already created it
                // this will also take care of any router ports and rules
                try {
                    niciraNvpApi.deleteLogicalRouter(lrc.getUuid());
                    if (lsp != null) {
                        niciraNvpApi.deleteLogicalSwitchPort(logicalSwitchUuid, lsp.getUuid());
                    }
                } catch (final NiciraNvpApiException ex) {
                }

                throw e;
            }

            return new CreateLogicalRouterAnswer(cmd, true, "Logical Router created (uuid " + lrc.getUuid() + ")", lrc.getUuid());
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new CreateLogicalRouterAnswer(cmd, e);
            }
        }
    }

    private Answer executeRequest(final DeleteLogicalRouterCommand cmd, int numRetries) {
        try {
            niciraNvpApi.deleteLogicalRouter(cmd.getLogicalRouterUuid());
            return new DeleteLogicalRouterAnswer(cmd, true, "Logical Router deleted (uuid " + cmd.getLogicalRouterUuid() + ")");
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new DeleteLogicalRouterAnswer(cmd, e);
            }
        }
    }

    private Answer executeRequest(final ConfigurePublicIpsOnLogicalRouterCommand cmd, int numRetries) {
        try {
            final NiciraNvpList<LogicalRouterPort> ports = niciraNvpApi.findLogicalRouterPortByGatewayServiceUuid(cmd.getLogicalRouterUuid(), cmd.getL3GatewayServiceUuid());
            if (ports.getResultCount() != 1) {
                return new ConfigurePublicIpsOnLogicalRouterAnswer(cmd, false, "No logical router ports found, unable to set ip addresses");
            }
            final LogicalRouterPort lrp = ports.getResults().get(0);
            lrp.setIpAddresses(cmd.getPublicCidrs());
            niciraNvpApi.updateLogicalRouterPort(cmd.getLogicalRouterUuid(), lrp);

            return new ConfigurePublicIpsOnLogicalRouterAnswer(cmd, true, "Configured " + cmd.getPublicCidrs().size() + " ip addresses on logical router uuid " +
                    cmd.getLogicalRouterUuid());
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new ConfigurePublicIpsOnLogicalRouterAnswer(cmd, e);
            }
        }

    }

    private Answer executeRequest(final ConfigureStaticNatRulesOnLogicalRouterCommand cmd, int numRetries) {
        try {
            final NiciraNvpList<NatRule> existingRules = niciraNvpApi.findNatRulesByLogicalRouterUuid(cmd.getLogicalRouterUuid());
            // Rules of the game (also known as assumptions-that-will-make-stuff-break-later-on)
            // A SourceNat rule with a match other than a /32 cidr is assumed to be the "main" SourceNat rule
            // Any other SourceNat rule should have a corresponding DestinationNat rule

            for (final StaticNatRuleTO rule : cmd.getRules()) {

                final NatRule[] rulepair = generateStaticNatRulePair(rule.getDstIp(), rule.getSrcIp());

                NatRule incoming = null;
                NatRule outgoing = null;

                for (final NatRule storedRule : existingRules.getResults()) {
                    if (storedRule.equalsIgnoreUuid(rulepair[1])) {
                        // The outgoing rule exists
                        outgoing = storedRule;
                        s_logger.debug("Found matching outgoing rule " + outgoing.getUuid());
                        if (incoming != null) {
                            break;
                        }
                    } else if (storedRule.equalsIgnoreUuid(rulepair[0])) {
                        // The incoming rule exists
                        incoming = storedRule;
                        s_logger.debug("Found matching incoming rule " + incoming.getUuid());
                        if (outgoing != null) {
                            break;
                        }
                    }
                }
                if (incoming != null && outgoing != null) {
                    if (rule.revoked()) {
                        s_logger.debug("Deleting incoming rule " + incoming.getUuid());
                        niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), incoming.getUuid());

                        s_logger.debug("Deleting outgoing rule " + outgoing.getUuid());
                        niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), outgoing.getUuid());
                    }
                } else {
                    if (rule.revoked()) {
                        s_logger.warn("Tried deleting a rule that does not exist, " + rule.getSrcIp() + " -> " + rule.getDstIp());
                        break;
                    }

                    rulepair[0] = niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0]);
                    s_logger.debug("Created " + natRuleToString(rulepair[0]));

                    try {
                        rulepair[1] = niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[1]);
                        s_logger.debug("Created " + natRuleToString(rulepair[1]));
                    } catch (final NiciraNvpApiException ex) {
                        s_logger.debug("Failed to create SourceNatRule, rolling back DestinationNatRule");
                        niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0].getUuid());
                        throw ex; // Rethrow original exception
                    }

                }
            }
            return new ConfigureStaticNatRulesOnLogicalRouterAnswer(cmd, true, cmd.getRules().size() + " StaticNat rules applied");
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new ConfigureStaticNatRulesOnLogicalRouterAnswer(cmd, e);
            }
        }
    }

    private Answer executeRequest(final ConfigurePortForwardingRulesOnLogicalRouterCommand cmd, int numRetries) {
        try {
            final NiciraNvpList<NatRule> existingRules = niciraNvpApi.findNatRulesByLogicalRouterUuid(cmd.getLogicalRouterUuid());
            // Rules of the game (also known as assumptions-that-will-make-stuff-break-later-on)
            // A SourceNat rule with a match other than a /32 cidr is assumed to be the "main" SourceNat rule
            // Any other SourceNat rule should have a corresponding DestinationNat rule

            for (final PortForwardingRuleTO rule : cmd.getRules()) {
                if (rule.isAlreadyAdded() && !rule.revoked()) {
                    // Don't need to do anything
                    continue;
                }

                if (rule.getDstPortRange()[0] != rule.getDstPortRange()[1] || rule.getSrcPortRange()[0] != rule.getSrcPortRange()[1]) {
                    return new ConfigurePortForwardingRulesOnLogicalRouterAnswer(cmd, false, "Nicira NVP doesn't support port ranges for port forwarding");
                }

                final NatRule[] rulepair = generatePortForwardingRulePair(rule.getDstIp(), rule.getDstPortRange(), rule.getSrcIp(), rule.getSrcPortRange(), rule.getProtocol());

                NatRule incoming = null;
                NatRule outgoing = null;

                for (final NatRule storedRule : existingRules.getResults()) {
                    if (storedRule.equalsIgnoreUuid(rulepair[1])) {
                        // The outgoing rule exists
                        outgoing = storedRule;
                        s_logger.debug("Found matching outgoing rule " + outgoing.getUuid());
                        if (incoming != null) {
                            break;
                        }
                    } else if (storedRule.equalsIgnoreUuid(rulepair[0])) {
                        // The incoming rule exists
                        incoming = storedRule;
                        s_logger.debug("Found matching incoming rule " + incoming.getUuid());
                        if (outgoing != null) {
                            break;
                        }
                    }
                }
                if (incoming != null && outgoing != null) {
                    if (rule.revoked()) {
                        s_logger.debug("Deleting incoming rule " + incoming.getUuid());
                        niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), incoming.getUuid());

                        s_logger.debug("Deleting outgoing rule " + outgoing.getUuid());
                        niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), outgoing.getUuid());
                    }
                } else {
                    if (rule.revoked()) {
                        s_logger.warn("Tried deleting a rule that does not exist, " + rule.getSrcIp() + " -> " + rule.getDstIp());
                        break;
                    }

                    rulepair[0] = niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0]);
                    s_logger.debug("Created " + natRuleToString(rulepair[0]));

                    try {
                        rulepair[1] = niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[1]);
                        s_logger.debug("Created " + natRuleToString(rulepair[1]));
                    } catch (final NiciraNvpApiException ex) {
                        s_logger.warn("NiciraNvpApiException during create call, rolling back previous create");
                        niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0].getUuid());
                        throw ex; // Rethrow the original exception
                    }

                }
            }
            return new ConfigurePortForwardingRulesOnLogicalRouterAnswer(cmd, true, cmd.getRules().size() + " PortForwarding rules applied");
        } catch (final NiciraNvpApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new ConfigurePortForwardingRulesOnLogicalRouterAnswer(cmd, e);
            }
        }

    }

    private Answer retry(final Command cmd, final int numRetries) {
        s_logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetries);
        return executeRequest(cmd, numRetries);
    }

    private String natRuleToString(final NatRule rule) {

        final StringBuilder natRuleStr = new StringBuilder();
        natRuleStr.append("Rule ");
        natRuleStr.append(rule.getUuid());
        natRuleStr.append(" (");
        natRuleStr.append(rule.getType());
        natRuleStr.append(") :");
        final Match m = rule.getMatch();
        natRuleStr.append("match (");
        natRuleStr.append(m.getProtocol());
        natRuleStr.append(" ");
        natRuleStr.append(m.getSourceIpAddresses());
        natRuleStr.append(" [");
        natRuleStr.append(m.getSourcePort());
        natRuleStr.append(" ] -> ");
        natRuleStr.append(m.getDestinationIpAddresses());
        natRuleStr.append(" [");
        natRuleStr.append(m.getDestinationPort());
        natRuleStr.append(" ]) -->");
        if ("SourceNatRule".equals(rule.getType())) {
            natRuleStr.append(((SourceNatRule)rule).getToSourceIpAddressMin());
            natRuleStr.append("-");
            natRuleStr.append(((SourceNatRule)rule).getToSourceIpAddressMax());
            natRuleStr.append(" [");
            natRuleStr.append(((SourceNatRule)rule).getToSourcePort());
            natRuleStr.append(" ])");
        } else {
            natRuleStr.append(((DestinationNatRule)rule).getToDestinationIpAddress());
            natRuleStr.append(" [");
            natRuleStr.append(((DestinationNatRule)rule).getToDestinationPort());
            natRuleStr.append(" ])");
        }
        return natRuleStr.toString();
    }

    private String truncate(final String string, final int length) {
        if (string.length() <= length) {
            return string;
        } else {
            return string.substring(0, length);
        }
    }

    protected NatRule[] generateStaticNatRulePair(final String insideIp, final String outsideIp) {
        final NatRule[] rulepair = new NatRule[2];
        rulepair[0] = new DestinationNatRule();
        rulepair[0].setType("DestinationNatRule");
        rulepair[0].setOrder(100);
        rulepair[1] = new SourceNatRule();
        rulepair[1].setType("SourceNatRule");
        rulepair[1].setOrder(100);

        Match m = new Match();
        m.setDestinationIpAddresses(outsideIp);
        rulepair[0].setMatch(m);
        ((DestinationNatRule)rulepair[0]).setToDestinationIpAddress(insideIp);

        // create matching snat rule
        m = new Match();
        m.setSourceIpAddresses(insideIp);
        rulepair[1].setMatch(m);
        ((SourceNatRule)rulepair[1]).setToSourceIpAddressMin(outsideIp);
        ((SourceNatRule)rulepair[1]).setToSourceIpAddressMax(outsideIp);

        return rulepair;

    }

    protected NatRule[] generatePortForwardingRulePair(final String insideIp, final int[] insidePorts, final String outsideIp, final int[] outsidePorts,
            final String protocol) {
        // Start with a basic static nat rule, then add port and protocol details
        final NatRule[] rulepair = generateStaticNatRulePair(insideIp, outsideIp);

        ((DestinationNatRule)rulepair[0]).setToDestinationPort(insidePorts[0]);
        rulepair[0].getMatch().setDestinationPort(outsidePorts[0]);
        rulepair[0].setOrder(50);
        rulepair[0].getMatch().setEthertype("IPv4");
        if ("tcp".equals(protocol)) {
            rulepair[0].getMatch().setProtocol(6);
        } else if ("udp".equals(protocol)) {
            rulepair[0].getMatch().setProtocol(17);
        }

        ((SourceNatRule)rulepair[1]).setToSourcePort(outsidePorts[0]);
        rulepair[1].getMatch().setSourcePort(insidePorts[0]);
        rulepair[1].setOrder(50);
        rulepair[1].getMatch().setEthertype("IPv4");
        if ("tcp".equals(protocol)) {
            rulepair[1].getMatch().setProtocol(6);
        } else if ("udp".equals(protocol)) {
            rulepair[1].getMatch().setProtocol(17);
        }

        return rulepair;

    }

    @Override
    public void setName(final String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
        // TODO Auto-generated method stub
    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(final int level) {
        // TODO Auto-generated method stub
    }

}
