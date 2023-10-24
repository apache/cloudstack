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
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;

import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.Segment;
import com.vmware.nsx_policy.model.SiteListResult;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.StartupNsxCommand;
import org.apache.cloudstack.agent.api.CreateNsxDhcpRelayConfigCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.service.NsxApiClient;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NsxResource implements ServerResource {
    private static final Logger LOGGER = Logger.getLogger(NsxResource.class);
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

    private Answer executeRequest(CreateNsxDhcpRelayConfigCommand cmd) {
        long zoneId = cmd.getZoneId();
        long domainId = cmd.getDomainId();
        long accountId = cmd.getAccountId();
        long vpcId = cmd.getVpcId();
        long networkId = cmd.getNetworkId();
        String vpcName = cmd.getVpcName();
        String networkName = cmd.getNetworkName();
        List<String> addresses = cmd.getAddresses();

        String dhcpRelayConfigName = NsxControllerUtils.getNsxDhcpRelayConfigId(zoneId, domainId, accountId, vpcId, networkId);

        String msg = String.format("Creating DHCP relay config with name %s on network %s of VPC %s",
                dhcpRelayConfigName, networkName, vpcName);
        LOGGER.debug(msg);

        try {
            nsxApiClient.createDhcpRelayConfig(dhcpRelayConfigName, addresses);
        } catch (CloudRuntimeException e) {
            msg = String.format("Error creating the DHCP relay config with name %s: %s", dhcpRelayConfigName, e.getMessage());
            LOGGER.error(msg, e);
            return new NsxAnswer(cmd, e);
        }

        String segmentName = NsxControllerUtils.getNsxSegmentId(domainId, accountId, zoneId, vpcId, networkId);
        String dhcpConfigPath = String.format("%s/%s", DHCP_RELAY_CONFIGS_PATH_PREFIX, dhcpRelayConfigName);
        try {
            Segment segment = nsxApiClient.getSegmentById(segmentName);
            segment.setDhcpConfigPath(dhcpConfigPath);
            nsxApiClient.updateSegment(segmentName, segment);
        } catch (CloudRuntimeException e) {
            msg = String.format("Error adding the DHCP relay config with name %s to the segment %s: %s", dhcpRelayConfigName, segmentName, e.getMessage());
            LOGGER.error(msg);
            return new NsxAnswer(cmd, e);
        }

        return new NsxAnswer(cmd, true, "");
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(CreateNsxTier1GatewayCommand cmd) {
        String name = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(), cmd.getVpcId());
        try {
            nsxApiClient.createTier1Gateway(name, tier0Gateway, edgeCluster);
            return new NsxAnswer(cmd, true, "");
        } catch (CloudRuntimeException e) {
            LOGGER.error(String.format("Cannot create tier 1 gateway %s (VPC: %s): %s", name, cmd.getVpcName(), e.getMessage()));
            return new NsxAnswer(cmd, e);
        }
    }

    private Answer executeRequest(DeleteNsxTier1GatewayCommand cmd) {
        String tier1Id = NsxControllerUtils.getTier1GatewayName(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(), cmd.getVpcId());
        try {
            nsxApiClient.deleteTier1Gateway(tier1Id);
        } catch (Exception e) {
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private Answer executeRequest(CreateNsxSegmentCommand cmd) {
        try {
            SiteListResult sites = nsxApiClient.getSites();
            String errorMsg;
            String networkName = cmd.getNetworkName();
            if (CollectionUtils.isEmpty(sites.getResults())) {
                errorMsg = String.format("Failed to create network: %s as no sites are found in the linked NSX infrastructure", networkName);
                LOGGER.error(errorMsg);
                return new NsxAnswer(cmd, new CloudRuntimeException(errorMsg));
            }
            String siteId = sites.getResults().get(0).getId();

            EnforcementPointListResult epList = nsxApiClient.getEnforcementPoints(siteId);
            if (CollectionUtils.isEmpty(epList.getResults())) {
                errorMsg = String.format("Failed to create network: %s as no enforcement points are found in the linked NSX infrastructure", networkName);
                LOGGER.error(errorMsg);
                return new NsxAnswer(cmd, new CloudRuntimeException(errorMsg));
            }
            String enforcementPointPath = epList.getResults().get(0).getPath();

            TransportZoneListResult transportZoneListResult = nsxApiClient.getTransportZones();
            if (CollectionUtils.isEmpty(transportZoneListResult.getResults())) {
                errorMsg = String.format("Failed to create network: %s as no transport zones were found in the linked NSX infrastructure", networkName);
                LOGGER.error(errorMsg);
                return new NsxAnswer(cmd, new CloudRuntimeException(errorMsg));
            }
            List<TransportZone> transportZones = transportZoneListResult.getResults().stream().filter(tz -> tz.getDisplayName().equals(transportZone)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(transportZones)) {
                errorMsg = String.format("Failed to create network: %s as no transport zone of name %s was found in the linked NSX infrastructure", networkName, transportZone);
                LOGGER.error(errorMsg);
                return new NsxAnswer(cmd, new CloudRuntimeException(errorMsg));
            }

            String segmentName = NsxControllerUtils.getNsxSegmentId(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(), cmd.getVpcId(), cmd.getNetworkId());
            String gatewayAddress = cmd.getNetworkGateway() + "/" + cmd.getNetworkCidr().split("/")[1];

            nsxApiClient.createSegment(cmd.getZoneId(), cmd.getDomainId(), cmd.getAccountId(), cmd.getVpcId(),
                    segmentName, gatewayAddress, tier0Gateway, enforcementPointPath, transportZones);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to create network: %s", cmd.getNetworkName()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(DeleteNsxSegmentCommand cmd) {
        String segmentName = NsxControllerUtils.getNsxSegmentId(cmd.getDomainId(), cmd.getAccountId(), cmd.getZoneId(),
                cmd.getVpcId(), cmd.getNetworkId());
        try {
            Thread.sleep(30*1000);
            nsxApiClient.deleteSegment(cmd.getZoneId(), cmd.getDomainId(), cmd.getAccountId(), cmd.getVpcId(), cmd.getNetworkId(), segmentName);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to delete NSX segment: %s", segmentName));
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
