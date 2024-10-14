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
package org.apache.cloudstack.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;

import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.model.Segment;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.StartupNsxCommand;
import org.apache.cloudstack.agent.api.CreateNsxDhcpRelayConfigCommand;
import org.apache.cloudstack.agent.api.CreateNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.CreateNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxPortForwardRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.DeleteNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.service.NsxApiClient;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NsxResource implements ServerResource {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final String DHCP_RELAY_CONFIGS_PATH_PREFIX = "/infra/dhcp-relay-configs";

    private String name;
    protected String hostname;
    protected String username;
    protected String password;
    protected String guid;
    protected String port;
    protected String tier0Gateway;
    protected String edgeCluster;
    protected String transportZone;
    protected String zoneId;

    protected NsxApiClient nsxApiClient;

    @Override
    public Host.Type getType() {
        return Host.Type.Routing;
    }
    @Override
    public StartupCommand[] initialize() {
        StartupNsxCommand sc = new StartupNsxCommand();
        sc.setGuid(guid);
        sc.setName(name);
        sc.setDataCenter(zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion("");
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return null;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand) cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return executeRequest((CheckHealthCommand) cmd);
        } else if (cmd instanceof DeleteNsxTier1GatewayCommand) {
            return executeRequest((DeleteNsxTier1GatewayCommand) cmd);
        } else if (cmd instanceof DeleteNsxSegmentCommand) {
            return executeRequest((DeleteNsxSegmentCommand) cmd);
        } else if (cmd instanceof CreateNsxSegmentCommand) {
            return executeRequest((CreateNsxSegmentCommand) cmd);
        }  else if (cmd instanceof CreateNsxTier1GatewayCommand) {
            return executeRequest((CreateNsxTier1GatewayCommand) cmd);
        } else if (cmd instanceof CreateNsxDhcpRelayConfigCommand) {
            return executeRequest((CreateNsxDhcpRelayConfigCommand) cmd);
        } else if (cmd instanceof CreateOrUpdateNsxTier1NatRuleCommand) {
            return executeRequest((CreateOrUpdateNsxTier1NatRuleCommand) cmd);
        } else if (cmd instanceof CreateNsxStaticNatCommand) {
            return executeRequest((CreateNsxStaticNatCommand) cmd);
        } else if (cmd instanceof DeleteNsxNatRuleCommand) {
            return executeRequest((DeleteNsxNatRuleCommand) cmd);
        } else if (cmd instanceof CreateNsxPortForwardRuleCommand) {
          return executeRequest((CreateNsxPortForwardRuleCommand) cmd);
        } else if (cmd instanceof CreateNsxLoadBalancerRuleCommand) {
            return executeRequest((CreateNsxLoadBalancerRuleCommand) cmd);
        } else if (cmd instanceof DeleteNsxLoadBalancerRuleCommand) {
            return executeRequest((DeleteNsxLoadBalancerRuleCommand) cmd);
        }  else  if (cmd instanceof DeleteNsxDistributedFirewallRulesCommand) {
            return executeRequest((DeleteNsxDistributedFirewallRulesCommand) cmd);
        } else if (cmd instanceof CreateNsxDistributedFirewallRulesCommand) {
            return executeRequest((CreateNsxDistributedFirewallRulesCommand) cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public void disconnected() {
        // Do nothing
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        // Do nothing
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // Do nothing
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return new HashMap<>();
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // Do nothing
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        hostname = (String) params.get("hostname");
        if (hostname == null) {
            throw new ConfigurationException("Missing NSX hostname from params: " + params);
        }

        port = (String) params.get("port");
        if (port == null) {
            throw new ConfigurationException("Missing NSX port from params: " + params);
        }

        username = (String) params.get("username");
        if (username == null) {
            throw new ConfigurationException("Missing NSX username from params: " + params);
        }

        password = (String) params.get("password");
        if (password == null) {
            throw new ConfigurationException("Missing NSX password from params: " + params);
        }

        this.name = (String) params.get("name");
        if (this.name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        guid = (String) params.get("guid");
        if (guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        zoneId = (String) params.get("zoneId");
        if (zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        tier0Gateway = (String) params.get("tier0Gateway");
        if (tier0Gateway == null) {
            throw new ConfigurationException("Missing NSX tier0 gateway");
        }

        edgeCluster = (String) params.get("edgeCluster");
        if (edgeCluster == null) {
            throw new ConfigurationException("Missing NSX edgeCluster");
        }

        transportZone = (String) params.get("transportZone");
        if (transportZone == null) {
            throw new ConfigurationException("Missing NSX transportZone");
        }

        nsxApiClient = new NsxApiClient(hostname, port, username, password.toCharArray());
        return true;
    }

    private Answer executeRequest(CreateOrUpdateNsxTier1NatRuleCommand cmd) {
        String tier1GatewayName = cmd.getTier1GatewayName();
        String action = cmd.getAction();
        String translatedIpAddress = cmd.getTranslatedIpAddress();
        String natRuleId = cmd.getNatRuleId();
        String natId = "USER";
        try {
            nsxApiClient.createTier1NatRule(tier1GatewayName, natId, natRuleId, action, translatedIpAddress);
        } catch (CloudRuntimeException e) {
            String msg = String.format("Error creating the NAT rule with ID %s on Tier1 Gateway %s: %s", natRuleId, tier1GatewayName, e.getMessage());
            logger.error(msg, e);
            return new NsxAnswer(cmd, e);
        }
        return new NsxAnswer(cmd, true, "");
    }

    private Answer executeRequest(CreateNsxDhcpRelayConfigCommand cmd) {
        long datacenterId = cmd.getZoneId();
        long domainId = cmd.getDomainId();
        long accountId = cmd.getAccountId();
        Long vpcId = cmd.getVpcId();
        long networkId = cmd.getNetworkId();
        String vpcName = cmd.getVpcName();
        String networkName = cmd.getNetworkName();
        List<String> addresses = cmd.getAddresses();

        String dhcpRelayConfigName = NsxControllerUtils.getNsxDhcpRelayConfigId(datacenterId, domainId, accountId, vpcId, networkId);

        String msg = String.format("Creating DHCP relay config with name %s on network %s of VPC %s",
                dhcpRelayConfigName, networkName, vpcName);
        logger.debug(msg);

        try {
            nsxApiClient.createDhcpRelayConfig(dhcpRelayConfigName, addresses);
        } catch (CloudRuntimeException e) {
            msg = String.format("Error creating the DHCP relay config with name %s: %s", dhcpRelayConfigName, e.getMessage());
            logger.error(msg, e);
            return new NsxAnswer(cmd, e);
        }

        String segmentName = NsxControllerUtils.getNsxSegmentId(domainId, accountId, datacenterId, vpcId, networkId);
        String dhcpConfigPath = String.format("%s/%s", DHCP_RELAY_CONFIGS_PATH_PREFIX, dhcpRelayConfigName);
        try {
            Segment segment = nsxApiClient.getSegmentById(segmentName);
            segment.setDhcpConfigPath(dhcpConfigPath);
            nsxApiClient.updateSegment(segmentName, segment);
        } catch (CloudRuntimeException e) {
            msg = String.format("Error adding the DHCP relay config with name %s to the segment %s: %s", dhcpRelayConfigName, segmentName, e.getMessage());
            logger.error(msg);
            return new NsxAnswer(cmd, e);
        }

        return new NsxAnswer(cmd, true, "");
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(CheckHealthCommand cmd) {
        return new CheckHealthAnswer(cmd, nsxApiClient.isNsxControllerActive());
    }

    private Answer executeRequest(CreateNsxTier1GatewayCommand cmd) {
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(), cmd.getNetworkResourceId(), cmd.isResourceVpc());
        boolean sourceNatEnabled = cmd.isSourceNatEnabled();
        try {
            nsxApiClient.createTier1Gateway(tier1GatewayName, tier0Gateway, edgeCluster, sourceNatEnabled);
            return new NsxAnswer(cmd, true, "");
        } catch (CloudRuntimeException e) {
            String msg = String.format("Cannot create tier 1 gateway %s (%s: %s): %s", tier1GatewayName,
                    (cmd.isResourceVpc() ? "VPC" : "NETWORK"), cmd.getNetworkResourceName(), e.getMessage());
            logger.error(msg);
            return new NsxAnswer(cmd, e);
        }
    }

    private Answer executeRequest(DeleteNsxTier1GatewayCommand cmd) {
        String tier1Id = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(), cmd.getNetworkResourceId(), cmd.isResourceVpc());
        String lbName = NsxControllerUtils.getLoadBalancerName(tier1Id);
        try {
            nsxApiClient.deleteLoadBalancer(lbName);
            nsxApiClient.deleteTier1Gateway(tier1Id);
        } catch (Exception e) {
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private Answer executeRequest(CreateNsxSegmentCommand cmd) {
        try {
            String siteId = nsxApiClient.getDefaultSiteId();
            String enforcementPointPath = nsxApiClient.getDefaultEnforcementPointPath(siteId);
            TransportZoneListResult transportZoneListResult = nsxApiClient.getTransportZones();
            if (CollectionUtils.isEmpty(transportZoneListResult.getResults())) {
                String errorMsg = String.format("Failed to create network: %s as no transport zones were found in the linked NSX infrastructure", cmd.getNetworkName());
                logger.error(errorMsg);
                return new NsxAnswer(cmd, new CloudRuntimeException(errorMsg));
            }
            List<TransportZone> transportZones = transportZoneListResult.getResults().stream().filter(tz -> tz.getDisplayName().equals(transportZone)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(transportZones)) {
                String errorMsg = String.format("Failed to create network: %s as no transport zone of name %s was found in the linked NSX infrastructure", cmd.getNetworkName(), transportZone);
                logger.error(errorMsg);
                return new NsxAnswer(cmd, new CloudRuntimeException(errorMsg));
            }

            String segmentName = NsxControllerUtils.getNsxSegmentId(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(), cmd.getVpcId(), cmd.getNetworkId());
            String gatewayAddress = cmd.getNetworkGateway() + "/" + cmd.getNetworkCidr().split("/")[1];

            Long networkResourceId = Objects.isNull(cmd.getVpcId()) ? cmd.getNetworkId() : cmd.getVpcId();
            boolean isResourceVpc = !Objects.isNull(cmd.getVpcId());
            String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(),
                    cmd.getZoneId(), networkResourceId, isResourceVpc);
            nsxApiClient.createSegment(segmentName, tier1GatewayName, gatewayAddress, enforcementPointPath, transportZones);
            nsxApiClient.createGroupForSegment(segmentName);
        } catch (Exception e) {
            logger.error(String.format("Failed to create network: %s", cmd.getNetworkName()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(DeleteNsxSegmentCommand cmd) {
        String segmentName = NsxControllerUtils.getNsxSegmentId(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getVpcId(), cmd.getNetworkId());
        try {
            nsxApiClient.deleteSegment(cmd.getZoneId(), cmd.getDomainId(), cmd.getAccountId(), cmd.getVpcId(), cmd.getNetworkId(), segmentName);
        } catch (Exception e) {
            logger.error(String.format("Failed to delete NSX segment %s: %s", segmentName, e.getMessage()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(CreateNsxStaticNatCommand cmd) {
        String staticNatRuleName = NsxControllerUtils.getStaticNatRuleName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getNetworkResourceId(), cmd.isResourceVpc());
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getNetworkResourceId(), cmd.isResourceVpc());
        try {
            nsxApiClient.createStaticNatRule(cmd.getNetworkResourceName(), tier1GatewayName, staticNatRuleName, cmd.getPublicIp(), cmd.getVmIp());
        } catch (Exception e) {
            logger.error(String.format("Failed to add NSX static NAT rule %s for network: %s", staticNatRuleName, cmd.getNetworkResourceName()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(CreateNsxPortForwardRuleCommand cmd) {
        String ruleName = NsxControllerUtils.getPortForwardRuleName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getNetworkResourceId(), cmd.getRuleId(), cmd.isResourceVpc());
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getNetworkResourceId(), cmd.isResourceVpc());
        try {
            String privatePort = cmd.getPrivatePort();
            logger.debug("Checking if the rule {} exists on Tier 1 Gateway: {}", ruleName, tier1GatewayName);
            if (nsxApiClient.doesPfRuleExist(ruleName, tier1GatewayName)) {
                String msg = String.format("Port forward rule for port: %s (%s) exits on NSX, not adding it again", ruleName, privatePort);
                logger.debug(msg);
                NsxAnswer answer = new NsxAnswer(cmd, true, msg);
                answer.setObjectExists(true);
                return answer;
            }
            String service = privatePort.contains("-") ? nsxApiClient.getServicePath(ruleName, privatePort, cmd.getProtocol(), null, null) :
                    nsxApiClient.getNsxInfraServices(ruleName, privatePort, cmd.getProtocol(), null, null);
            nsxApiClient.createPortForwardingRule(ruleName, tier1GatewayName, cmd.getNetworkResourceName(), cmd.getPublicIp(),
                    cmd.getVmIp(), cmd.getPublicPort(), service);
        } catch (Exception e) {
            String msg = String.format("Failed to add NSX port forward rule %s for network: %s", ruleName, cmd.getNetworkResourceName());
            logger.error(msg, e);
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(DeleteNsxNatRuleCommand cmd) {
        String ruleName = null;
        if (cmd.getService() == Network.Service.StaticNat) {
            ruleName = NsxControllerUtils.getStaticNatRuleName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                    cmd.getNetworkResourceId(), cmd.isResourceVpc());
        } else if (cmd.getService() == Network.Service.PortForwarding) {
            ruleName = NsxControllerUtils.getPortForwardRuleName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                    cmd.getNetworkResourceId(), cmd.getRuleId(), cmd.isResourceVpc());
        }
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getNetworkResourceId(), cmd.isResourceVpc());
        try {
            nsxApiClient.deleteNatRule(cmd.getService(), cmd.getPrivatePort(), cmd.getProtocol(),
                    cmd.getNetworkResourceName(), tier1GatewayName, ruleName);
        } catch (Exception e) {
            String msg = String.format("Failed to delete NSX rule %s for network %s: due to %s", ruleName, cmd.getNetworkResourceName(), e.getMessage());
            logger.error(msg, e);
            return new NsxAnswer(cmd, new CloudRuntimeException(msg));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(CreateNsxLoadBalancerRuleCommand cmd) {
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getNetworkResourceId(), cmd.isResourceVpc());
        String ruleName = NsxControllerUtils.getLoadBalancerRuleName(tier1GatewayName, cmd.getLbId());
        try {
            nsxApiClient.createAndAddNsxLbVirtualServer(tier1GatewayName, cmd.getLbId(), cmd.getPublicIp(), cmd.getPublicPort(),
                    cmd.getMemberList(), cmd.getAlgorithm(), cmd.getProtocol(), cmd.getPrivatePort());
        } catch (Exception e) {
            logger.error(String.format("Failed to add NSX load balancer rule %s for network: %s", ruleName, cmd.getNetworkResourceName()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(DeleteNsxLoadBalancerRuleCommand cmd) {
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(),
                cmd.getZoneId(), cmd.getNetworkResourceId(), cmd.isResourceVpc());
        String ruleName = NsxControllerUtils.getLoadBalancerRuleName(tier1GatewayName, cmd.getLbId());
        try {
            nsxApiClient.deleteNsxLbResources(tier1GatewayName, cmd.getLbId());
        } catch (Exception e) {
            logger.error(String.format("Failed to add NSX load balancer rule %s for network: %s", ruleName, cmd.getNetworkResourceName()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(CreateNsxDistributedFirewallRulesCommand cmd) {
        String segmentName = NsxControllerUtils.getNsxSegmentId(cmd.getDomainId(), cmd.getAccountId(),
                cmd.getZoneId(), cmd.getVpcId(), cmd.getNetworkId());
        List<NsxNetworkRule> rules = cmd.getRules();
        try {
            nsxApiClient.createSegmentDistributedFirewall(segmentName, rules);
        } catch (Exception e) {
            logger.error(String.format("Failed to create NSX distributed firewall %s: %s", segmentName, e.getMessage()), e);
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(DeleteNsxDistributedFirewallRulesCommand cmd) {
        String segmentName = NsxControllerUtils.getNsxSegmentId(cmd.getDomainId(), cmd.getAccountId(),
                cmd.getZoneId(), cmd.getVpcId(), cmd.getNetworkId());
        List<NsxNetworkRule> rules = cmd.getRules();
        try {
            nsxApiClient.deleteDistributedFirewallRules(segmentName, rules);
        } catch (Exception e) {
            logger.error(String.format("Failed to delete NSX distributed firewall %s: %s", segmentName, e.getMessage()), e);
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
