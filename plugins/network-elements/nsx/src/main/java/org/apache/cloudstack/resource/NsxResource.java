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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.EdgeClusters;
import com.vmware.nsx.model.EdgeCluster;
import com.vmware.nsx_policy.infra.Segments;
import com.vmware.nsx_policy.infra.Tier1s;
import com.vmware.nsx_policy.infra.segments.ServiceSegments;
import com.vmware.nsx_policy.infra.tier_0s.LocaleServices;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.ChildLocaleServices;
import com.vmware.nsx_policy.model.LocaleServicesListResult;
import com.vmware.nsx_policy.model.Segment;
import com.vmware.nsx_policy.model.SegmentSubnet;
import com.vmware.nsx_policy.model.ServiceSegmentListResult;
import com.vmware.nsx_policy.model.Tier1;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.std.errors.Error;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.StartupNsxCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.service.NsxApi;
import org.apache.cloudstack.utils.NsxApiClientUtils;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static org.apache.cloudstack.utils.NsxApiClientUtils.HAMode.ACTIVE_STANDBY;
import static org.apache.cloudstack.utils.NsxApiClientUtils.FailoverMode.PREEMPTIVE;
import static org.apache.cloudstack.utils.NsxApiClientUtils.PoolAllocation.ROUTING;
import static org.apache.cloudstack.utils.NsxApiClientUtils.createApiClient;

public class NsxResource implements ServerResource {
    private static final Logger LOGGER = Logger.getLogger(NsxResource.class);
    private static final String TIER_0_GATEWAY_PATH_PREFIX = "/infra/tier-0s/";
    private static final String TIER_1_GATEWAY_PATH_PREFIX = "/infra/tier-1s/";
    private static final String TIER_1_RESOURCE_TYPE = "Tier1";
    private static final String SEGMENT_RESOURCE_TYPE = "Segment";

    private String name;
    protected String hostname;
    protected String username;
    protected String password;
    protected String guid;
    protected String port;
    protected String tier0Gateway;
    protected String edgeCluster;
    protected String zoneId;

    protected NsxApi nsxApi;

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

        nsxApi = new NsxApi();
        nsxApi.setApiClient(createApiClient(hostname, port, username, password.toCharArray()));
        return true;
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Function<Class<? extends Service>, Service> nsxService = svcClass -> nsxApi.getApiClient().createStub(svcClass);
    private Answer executeRequest(CreateNsxTier1GatewayCommand cmd) {
        String name = getTier1GatewayName(cmd);
        Tier1 tier1 = getTier1Gateway(name);
        if (tier1 != null) {
            throw new InvalidParameterValueException(String.format("VPC network with name %s exists in NSX zone: %s and account %s", name, cmd.getZoneName(), cmd.getAccountName()));
        }

        List<com.vmware.nsx_policy.model.LocaleServices> localeServices = getTier0LocalServices(tier0Gateway);
        String tier0GatewayPath = TIER_0_GATEWAY_PATH_PREFIX + tier0Gateway;

        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        tier1 = new Tier1.Builder()
                .setTier0Path(tier0GatewayPath)
                .setResourceType(TIER_1_RESOURCE_TYPE)
                .setPoolAllocation(ROUTING.name())
                .setHaMode(ACTIVE_STANDBY.name())
                .setFailoverMode(PREEMPTIVE.name())
                .setId(name)
                .setDisplayName(name)
                .setChildren(
                        List.of(new ChildLocaleServices.Builder("ChildLocaleServices")
                                        .setLocaleServices(
                                                new com.vmware.nsx_policy.model.LocaleServices.Builder()
                                                        .setEdgeClusterPath(localeServices.get(0).getEdgeClusterPath())
                                                        .setParentPath(TIER_1_GATEWAY_PATH_PREFIX + getTier1GatewayName(cmd))
                                                        .setResourceType("LocaleServices")
                                                        .build()
                                        ).build())).build();
        try {
            tier1service.patch(name, tier1);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            return new NsxAnswer(cmd, new CloudRuntimeException(ae.getErrorMessage()));
        }
        return new NsxAnswer(cmd, true, "");
    }

    private Answer executeRequest(DeleteNsxTier1GatewayCommand cmd) {
        try {
            Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
            tier1service.delete(getTier1GatewayName(cmd));
        } catch (Exception e) {
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private Answer executeRequest(CreateNsxSegmentCommand cmd) {
        try {
            String segmentName = getSegmentName(cmd);
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            SegmentSubnet subnet = new SegmentSubnet.Builder()
                    .setGatewayAddress(cmd.getTierNetwork().getGateway() + "/" + cmd.getTierNetwork().getCidr().split("/")[1]).build();
            Segment segment = new Segment.Builder()
                    .setResourceType(SEGMENT_RESOURCE_TYPE)
                    .setId(segmentName)
                    .setDisplayName(segmentName)
                    .setConnectivityPath(isNull(cmd.getVpcName()) ? TIER_0_GATEWAY_PATH_PREFIX + tier0Gateway
                            : TIER_1_GATEWAY_PATH_PREFIX + getTier1GatewayName(cmd))
                    .setAdminState(NsxApiClientUtils.AdminState.UP.name())
                    .setSubnets(List.of(subnet))
                    .build();
            segmentService.patch(segmentName, segment);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to create network: %s", cmd.getTierNetwork().getName()));
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private NsxAnswer executeRequest(DeleteNsxSegmentCommand cmd) {
        try {
            String segmentName = getSegmentName(cmd);
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            segmentService.delete(segmentName);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to delete NSX segment: %s", getSegmentName(cmd)) );
            return new NsxAnswer(cmd, new CloudRuntimeException(e.getMessage()));
        }
        return new NsxAnswer(cmd, true, null);
    }

    private List<com.vmware.nsx_policy.model.LocaleServices> getTier0LocalServices(String tier0Gateway) {
        try {
            LocaleServices tier0LocaleServices = (LocaleServices) nsxService.apply(LocaleServices.class);
            LocaleServicesListResult result = tier0LocaleServices.list(tier0Gateway, null, false, null, null, null, null);
            return result.getResults();
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch locale services for tier gateway %s due to %s", tier0Gateway, e.getMessage()));
        }
    }

    private Tier1 getTier1Gateway(String tier1GatewayId) {
        try {
            Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
            return tier1service.get(tier1GatewayId);
        } catch (Exception e) {
            LOGGER.debug(String.format("NSX Tier-1 gateway with name: %s not found", tier1GatewayId));
        }
        return null;
    }

    private String getTier1GatewayName(CreateNsxTier1GatewayCommand cmd) {
        return cmd.getZoneName() + "-" + cmd.getAccountName() + "-" + cmd.getVpcName();
    }

    private String getSegmentName(CreateNsxSegmentCommand cmd) {
        return cmd.getAccountName() + "-" + cmd.getTierNetwork().getName();
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
