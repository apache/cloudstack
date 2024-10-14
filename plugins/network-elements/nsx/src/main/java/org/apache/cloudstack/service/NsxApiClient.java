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
package org.apache.cloudstack.service;

import com.cloud.network.Network;
import com.cloud.network.nsx.NsxService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.cluster.Status;
import com.vmware.nsx.model.ClusterStatus;
import com.vmware.nsx.model.ControllerClusterStatus;
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.infra.DhcpRelayConfigs;
import com.vmware.nsx_policy.infra.LbAppProfiles;
import com.vmware.nsx_policy.infra.LbMonitorProfiles;
import com.vmware.nsx_policy.infra.LbPools;
import com.vmware.nsx_policy.infra.LbServices;
import com.vmware.nsx_policy.infra.LbVirtualServers;
import com.vmware.nsx_policy.infra.Segments;
import com.vmware.nsx_policy.infra.Services;
import com.vmware.nsx_policy.infra.Sites;
import com.vmware.nsx_policy.infra.Tier1s;
import com.vmware.nsx_policy.infra.domains.Groups;
import com.vmware.nsx_policy.infra.domains.SecurityPolicies;
import com.vmware.nsx_policy.infra.domains.groups.members.SegmentPorts;
import com.vmware.nsx_policy.infra.domains.security_policies.Rules;
import com.vmware.nsx_policy.infra.sites.EnforcementPoints;
import com.vmware.nsx_policy.infra.tier_0s.LocaleServices;
import com.vmware.nsx_policy.infra.tier_1s.nat.NatRules;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.DhcpRelayConfig;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.Group;
import com.vmware.nsx_policy.model.GroupListResult;
import com.vmware.nsx_policy.model.ICMPTypeServiceEntry;
import com.vmware.nsx_policy.model.L4PortSetServiceEntry;
import com.vmware.nsx_policy.model.LBAppProfileListResult;
import com.vmware.nsx_policy.model.LBIcmpMonitorProfile;
import com.vmware.nsx_policy.model.LBMonitorProfileListResult;
import com.vmware.nsx_policy.model.LBPool;
import com.vmware.nsx_policy.model.LBPoolListResult;
import com.vmware.nsx_policy.model.LBPoolMember;
import com.vmware.nsx_policy.model.LBService;
import com.vmware.nsx_policy.model.LBTcpMonitorProfile;
import com.vmware.nsx_policy.model.LBVirtualServer;
import com.vmware.nsx_policy.model.LBVirtualServerListResult;
import com.vmware.nsx_policy.model.LocaleServicesListResult;
import com.vmware.nsx_policy.model.PathExpression;
import com.vmware.nsx_policy.model.PolicyGroupMembersListResult;
import com.vmware.nsx_policy.model.PolicyNatRule;
import com.vmware.nsx_policy.model.PolicyNatRuleListResult;
import com.vmware.nsx_policy.model.Rule;
import com.vmware.nsx_policy.model.SecurityPolicy;
import com.vmware.nsx_policy.model.Segment;
import com.vmware.nsx_policy.model.SegmentSubnet;
import com.vmware.nsx_policy.model.ServiceListResult;
import com.vmware.nsx_policy.model.SiteListResult;
import com.vmware.nsx_policy.model.Tier1;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.bindings.Structure;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.cis.authn.SecurityContextFactory;
import com.vmware.vapi.client.ApiClient;
import com.vmware.vapi.client.ApiClients;
import com.vmware.vapi.client.Configuration;
import com.vmware.vapi.core.ExecutionContext;
import com.vmware.vapi.internal.protocol.RestProtocol;
import com.vmware.vapi.internal.protocol.client.rest.authn.BasicAuthenticationAppender;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.std.errors.Error;
import org.apache.cloudstack.resource.NsxLoadBalancerMember;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.cloudstack.utils.NsxControllerUtils.getServerPoolMemberName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServerPoolName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServiceName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVirtualServerName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServiceEntryName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getLoadBalancerName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getLoadBalancerAlgorithm;
import static org.apache.cloudstack.utils.NsxControllerUtils.getActiveMonitorProfileName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getTier1GatewayName;

public class NsxApiClient {

    protected ApiClient apiClient;
    protected Function<Class<? extends Service>, Service> nsxService;

    public static final int RESPONSE_TIMEOUT_SECONDS = 60;
    protected Logger logger = LogManager.getLogger(getClass());

    // Constants
    private static final String CLUSTER_STATUS_STABLE = "STABLE";
    private static final String TIER_1_RESOURCE_TYPE = "Tier1";
    private static final String TIER_1_LOCALE_SERVICE_ID = "default";
    private static final String SEGMENT_RESOURCE_TYPE = "Segment";
    private static final String TIER_0_GATEWAY_PATH_PREFIX = "/infra/tier-0s/";
    private static final String TIER_1_GATEWAY_PATH_PREFIX = "/infra/tier-1s/";
    protected static final String SEGMENTS_PATH = "/infra/segments";
    protected static final String DEFAULT_DOMAIN = "default";
    protected static final String GROUPS_PATH_PREFIX = "/infra/domains/default/groups";
    // TODO: Pass as global / zone-level setting?
    protected static final String NSX_LB_PASSIVE_MONITOR = "/infra/lb-monitor-profiles/default-passive-lb-monitor";
    protected static final String TCP_MONITOR_PROFILE = "LBTcpMonitorProfile";
    protected static final String ICMP_MONITOR_PROFILE = "LBIcmpMonitorProfile";
    protected static final String NAT_ID = "USER";

    private enum PoolAllocation { ROUTING, LB_SMALL, LB_MEDIUM, LB_LARGE, LB_XLARGE }

    private enum HAMode { ACTIVE_STANDBY, ACTIVE_ACTIVE }

    private enum FailoverMode { PREEMPTIVE, NON_PREEMPTIVE }

    private enum AdminState { UP, DOWN }

    private enum TransportType { OVERLAY, VLAN }

    private enum NatId { USER, INTERNAL, DEFAULT }

    private enum NatAction {SNAT, DNAT, REFLEXIVE}

    private enum FirewallMatch {
        MATCH_INTERNAL_ADDRESS,
        MATCH_EXTERNAL_ADDRESS,
        BYPASS
    }

    public enum LBAlgorithm {
        ROUND_ROBIN,
        LEAST_CONNECTION,
        IP_HASH
    }

    private enum LBSize {
        SMALL,
        MEDIUM,
        LARGE,
        XLARGE
    }

    private enum FirewallActions {
        ALLOW,
        DROP,
        REJECT,
        JUMP_TO_APPLICATION
    }

    public enum  RouteAdvertisementType { TIER1_STATIC_ROUTES, TIER1_CONNECTED, TIER1_NAT,
        TIER1_LB_VIP, TIER1_LB_SNAT, TIER1_DNS_FORWARDER_IP, TIER1_IPSEC_LOCAL_ENDPOINT
    }

    protected NsxApiClient() {
    }

    public NsxApiClient(String hostname, String port, String username, char[] password) {
        String controllerUrl = String.format("https://%s:%s", hostname, port);
        HttpConfiguration.SslConfiguration.Builder sslConfigBuilder = new HttpConfiguration.SslConfiguration.Builder();
        sslConfigBuilder
                .disableCertificateValidation()
                .disableHostnameVerification();
        HttpConfiguration.SslConfiguration sslConfig = sslConfigBuilder.getConfig();

        HttpConfiguration httpConfig = new HttpConfiguration.Builder()
                .setSoTimeout(RESPONSE_TIMEOUT_SECONDS * 1000)
                .setSslConfiguration(sslConfig).getConfig();

        StubConfiguration stubConfig = new StubConfiguration();
        ExecutionContext.SecurityContext securityContext = SecurityContextFactory
                .createUserPassSecurityContext(username, password);
        stubConfig.setSecurityContext(securityContext);

        Configuration.Builder configBuilder = new Configuration.Builder()
                .register(Configuration.HTTP_CONFIG_CFG, httpConfig)
                .register(Configuration.STUB_CONFIG_CFG, stubConfig)
                .register(RestProtocol.REST_REQUEST_AUTHENTICATOR_CFG, new BasicAuthenticationAppender());
        Configuration config = configBuilder.build();
        apiClient = ApiClients.newRestClient(controllerUrl, config);
        nsxService = apiClient::createStub;
    }

    public boolean isNsxControllerActive() {
        try {
            Status statusService = (Status) nsxService.apply(Status.class);
            ClusterStatus clusterStatus = statusService.get();
            if (clusterStatus == null) {
                logger.error("Cannot get NSX Cluster Status");
                return false;
            }
            ControllerClusterStatus status = clusterStatus.getControlClusterStatus();
            if (status == null) {
                logger.error("Cannot get NSX Controller Cluster Status");
                return false;
            }
            return CLUSTER_STATUS_STABLE.equalsIgnoreCase(status.getStatus());
        } catch (Error error) {
            logger.error("Error checking NSX Controller Health: {}", error.getMessage());
            return false;
        }
    }

    public void createTier1NatRule(String tier1GatewayName, String natId, String natRuleId,
                                   String action, String translatedIp) {
        NatRules natRulesService = (NatRules) nsxService.apply(NatRules.class);
        PolicyNatRule natPolicy = new PolicyNatRule.Builder()
                .setAction(action)
                .setTranslatedNetwork(translatedIp)
                .build();
        natRulesService.patch(tier1GatewayName, natId, natRuleId, natPolicy);
    }

    public void createDhcpRelayConfig(String dhcpRelayConfigName, List<String> addresses) {
        try {
            DhcpRelayConfigs service = (DhcpRelayConfigs) nsxService.apply(DhcpRelayConfigs.class);
            DhcpRelayConfig config = new DhcpRelayConfig.Builder()
                    .setServerAddresses(addresses)
                    .setId(dhcpRelayConfigName)
                    .setDisplayName(dhcpRelayConfigName)
                    .build();
            service.patch(dhcpRelayConfigName, config);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error creating the DHCP relay config with name %s: %s", dhcpRelayConfigName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(ae.getErrorMessage());
        }
    }

    public Segment getSegmentById(String segmentName) {
        try {
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            return segmentService.get(segmentName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error obtaining the segment with name %s: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(ae.getErrorMessage());
        }
    }

    public void updateSegment(String segmentName, Segment segment) {
        try {
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            segmentService.patch(segmentName, segment);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error updating the segment with name %s: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(ae.getErrorMessage());
        }
    }

    private Tier1 getTier1Gateway(String tier1GatewayId) {
        try {
            Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
            return tier1service.get(tier1GatewayId);
        } catch (Exception e) {
            logger.debug(String.format("NSX Tier-1 gateway with name: %s not found", tier1GatewayId));
        }
        return null;
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

    /**
     * To instantiate Tier-1 in Edge Cluster
     */
    private void createTier1LocaleServices(String tier1Id, String edgeCluster, String tier0Gateway) {
        try {
            List<com.vmware.nsx_policy.model.LocaleServices> localeServices = getTier0LocalServices(tier0Gateway);
            com.vmware.nsx_policy.infra.tier_1s.LocaleServices tier1LocalService = (com.vmware.nsx_policy.infra.tier_1s.LocaleServices) nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.LocaleServices.class);
            com.vmware.nsx_policy.model.LocaleServices localeService = new com.vmware.nsx_policy.model.LocaleServices.Builder()
                    .setEdgeClusterPath(localeServices.get(0).getEdgeClusterPath()).build();
            tier1LocalService.patch(tier1Id, TIER_1_LOCALE_SERVICE_ID, localeService);
        } catch (Error error) {
            throw new CloudRuntimeException(String.format("Failed to instantiate tier-1 gateway %s in edge cluster %s", tier1Id, edgeCluster));
        }
    }

    private List<String> getRouterAdvertisementTypeList(boolean sourceNatEnabled) {
        List<String> types = new ArrayList<>();
        types.add(RouteAdvertisementType.TIER1_IPSEC_LOCAL_ENDPOINT.name());
        types.add(RouteAdvertisementType.TIER1_LB_VIP.name());
        types.add(RouteAdvertisementType.TIER1_NAT.name());
        if (!sourceNatEnabled) {
            types.add(RouteAdvertisementType.TIER1_CONNECTED.name());
        }
        return types;
    }

    public void createTier1Gateway(String name, String tier0Gateway, String edgeCluster, boolean sourceNatEnabled) throws CloudRuntimeException {
        String tier0GatewayPath = TIER_0_GATEWAY_PATH_PREFIX + tier0Gateway;
        Tier1 tier1 = getTier1Gateway(name);
        if (tier1 != null) {
            logger.info(String.format("VPC network with name %s exists in NSX zone", name));
            return;
        }

        List<String> routeAdvertisementTypes = getRouterAdvertisementTypeList(sourceNatEnabled);

        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        tier1 = new Tier1.Builder()
                .setTier0Path(tier0GatewayPath)
                .setResourceType(TIER_1_RESOURCE_TYPE)
                .setPoolAllocation(PoolAllocation.ROUTING.name())
                .setHaMode(HAMode.ACTIVE_STANDBY.name())
                .setFailoverMode(FailoverMode.PREEMPTIVE.name())
                .setRouteAdvertisementTypes(routeAdvertisementTypes)
                .setId(name)
                .setDisplayName(name)
                .build();
        try {
            tier1service.patch(name, tier1);
            createTier1LocaleServices(name, edgeCluster, tier0Gateway);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error creating tier 1 gateway %s: %s", name, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteTier1Gateway(String tier1Id) {
        com.vmware.nsx_policy.infra.tier_1s.LocaleServices localeService = (com.vmware.nsx_policy.infra.tier_1s.LocaleServices)
                nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.LocaleServices.class);
        if (getTier1Gateway(tier1Id) == null) {
            logger.warn(String.format("The Tier 1 Gateway %s does not exist, cannot be removed", tier1Id));
            return;
        }
        removeTier1GatewayNatRules(tier1Id);
        localeService.delete(tier1Id, TIER_1_LOCALE_SERVICE_ID);
        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        tier1service.delete(tier1Id);
    }

    private void removeTier1GatewayNatRules(String tier1Id) {
        NatRules natRulesService = (NatRules) nsxService.apply(NatRules.class);
        PolicyNatRuleListResult result = natRulesService.list(tier1Id, NAT_ID, null, false, null, null, null, null);
        List<PolicyNatRule> natRules = result.getResults();
        if (CollectionUtils.isEmpty(natRules)) {
            logger.debug(String.format("Didn't find any NAT rule to remove on the Tier 1 Gateway %s", tier1Id));
        } else {
            for (PolicyNatRule natRule : natRules) {
                logger.debug(String.format("Removing NAT rule %s from Tier 1 Gateway %s", natRule.getId(), tier1Id));
                natRulesService.delete(tier1Id, NAT_ID, natRule.getId());
            }
        }

    }

    public String getDefaultSiteId() {
        SiteListResult sites = getSites();
        if (CollectionUtils.isEmpty(sites.getResults())) {
            String errorMsg = "No sites are found in the linked NSX infrastructure";
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        return sites.getResults().get(0).getId();
    }

    protected SiteListResult getSites() {
        try {
            Sites sites = (Sites) nsxService.apply(Sites.class);
            return sites.list(null, false, null, null, null, null);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch sites list due to %s", e.getMessage()));
        }
    }

    public String getDefaultEnforcementPointPath(String siteId) {
        EnforcementPointListResult epList = getEnforcementPoints(siteId);
        if (CollectionUtils.isEmpty(epList.getResults())) {
            String errorMsg = String.format("No enforcement points are found in the linked NSX infrastructure for site ID %s", siteId);
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        return epList.getResults().get(0).getPath();
    }

    protected EnforcementPointListResult getEnforcementPoints(String siteId) {
        try {
            EnforcementPoints enforcementPoints = (EnforcementPoints) nsxService.apply(EnforcementPoints.class);
            return enforcementPoints.list(siteId, null, false, null, null, null, null);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch enforcement points due to %s", e.getMessage()));
        }
    }

    public TransportZoneListResult getTransportZones() {
        try {
            com.vmware.nsx.TransportZones transportZones = (com.vmware.nsx.TransportZones) nsxService.apply(com.vmware.nsx.TransportZones.class);
            return transportZones.list(null, null, true, null, null, null, null, null, TransportType.OVERLAY.name(), null);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch transport zones due to %s", e.getMessage()));
        }
    }

    public void createSegment(String segmentName, String tier1GatewayName, String gatewayAddress, String enforcementPointPath,
                              List<TransportZone> transportZones) {
        try {
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            SegmentSubnet subnet = new SegmentSubnet.Builder()
                    .setGatewayAddress(gatewayAddress)
                    .build();
            Segment segment = new Segment.Builder()
                    .setResourceType(SEGMENT_RESOURCE_TYPE)
                    .setId(segmentName)
                    .setDisplayName(segmentName)
                    .setConnectivityPath(TIER_1_GATEWAY_PATH_PREFIX + tier1GatewayName)
                    .setAdminState(AdminState.UP.name())
                    .setSubnets(List.of(subnet))
                    .setTransportZonePath(enforcementPointPath + "/transport-zones/" + transportZones.get(0).getId())
                    .build();
            segmentService.patch(segmentName, segment);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error creating segment %s: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteSegment(long zoneId, long domainId, long accountId, Long vpcId, long networkId, String segmentName) {
        try {
            removeSegmentDistributedFirewallRules(segmentName);
            if (Objects.isNull(vpcId)) {
                String t1GatewayName = getTier1GatewayName(domainId, accountId, zoneId, networkId, false);
                deleteLoadBalancer(getLoadBalancerName(t1GatewayName));
            }
            removeSegment(segmentName, zoneId);
            DhcpRelayConfigs dhcpRelayConfig = (DhcpRelayConfigs) nsxService.apply(DhcpRelayConfigs.class);
            String dhcpRelayConfigId = NsxControllerUtils.getNsxDhcpRelayConfigId(zoneId, domainId, accountId, vpcId, networkId);
            logger.debug(String.format("Removing the DHCP relay config with ID %s", dhcpRelayConfigId));
            dhcpRelayConfig.delete(dhcpRelayConfigId);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error deleting segment %s: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }


    protected void removeSegment(String segmentName, long zoneId) {
        logger.debug(String.format("Removing the segment with ID %s", segmentName));
        Segments segmentService = (Segments) nsxService.apply(Segments.class);
        String errMsg = String.format("The segment with ID %s is not found, skipping removal", segmentName);
        try {
            Segment segment = segmentService.get(segmentName);
            if (segment == null) {
                logger.warn(errMsg);
                return;
            }
        } catch (Exception e) {
            logger.warn(errMsg);
            return;
        }
        String siteId = getDefaultSiteId();
        String enforcementPointPath = getDefaultEnforcementPointPath(siteId);
        SegmentPorts segmentPortsService = (SegmentPorts) nsxService.apply(SegmentPorts.class);
        PolicyGroupMembersListResult segmentPortsList = getSegmentPortList(segmentPortsService, segmentName, enforcementPointPath);
        Long portCount = segmentPortsList.getResultCount();
        if (portCount > 0L) {
            portCount = retrySegmentDeletion(segmentPortsService, segmentName, enforcementPointPath, zoneId);
        }
        if (portCount == 0L) {
            logger.debug(String.format("Removing the segment with ID %s", segmentName));
            removeGroupForSegment(segmentName);
            segmentService.delete(segmentName);
        } else {
            String msg = String.format("Cannot remove the NSX segment %s because there are still %s port group(s) attached to it", segmentName, portCount);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private PolicyGroupMembersListResult getSegmentPortList(SegmentPorts segmentPortsService, String segmentName, String enforcementPointPath) {
        return segmentPortsService.list(DEFAULT_DOMAIN, segmentName, null, enforcementPointPath,
                false, null, 50L, false, null);
    }

    private Long retrySegmentDeletion(SegmentPorts segmentPortsService, String segmentName, String enforcementPointPath, long zoneId) {
        int retries = NsxService.NSX_API_FAILURE_RETRIES.valueIn(zoneId);
        int waitingSecs = NsxService.NSX_API_FAILURE_INTERVAL.valueIn(zoneId);
        int count = 1;
        Long portCount;
        do {
            try {
                logger.info("Waiting for all port groups to be unlinked from the segment {} - " +
                        "Attempt: {}. Waiting for {} secs", segmentName, count++, waitingSecs);
                Thread.sleep(waitingSecs * 1000L);
                portCount = getSegmentPortList(segmentPortsService, segmentName, enforcementPointPath).getResultCount();
                retries--;
            } catch (InterruptedException e) {
                throw new CloudRuntimeException(String.format("Unable to delete segment %s due to: %s", segmentName, e.getLocalizedMessage()));
            }
        } while (retries > 0 && portCount > 0);
        return portCount;
    }

    public void createStaticNatRule(String vpcName, String tier1GatewayName,
                                    String ruleName, String publicIp, String vmIp) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            PolicyNatRule rule = new PolicyNatRule.Builder()
                    .setId(ruleName)
                    .setDisplayName(ruleName)
                    .setAction(NatAction.DNAT.name())
                    .setFirewallMatch(FirewallMatch.MATCH_INTERNAL_ADDRESS.name())
                    .setDestinationNetwork(publicIp)
                    .setTranslatedNetwork(vmIp)
                    .setEnabled(true)
                    .build();

            logger.debug(String.format("Creating NSX static NAT rule %s for tier-1 gateway %s (VPC: %s)", ruleName, tier1GatewayName, vpcName));
            natService.patch(tier1GatewayName, NatId.USER.name(), ruleName, rule);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error creating NSX Static NAT rule %s for tier-1 gateway %s (VPC: %s), due to %s",
                    ruleName, tier1GatewayName, vpcName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    protected void deletePortForwardingNatRuleService(String ruleName, String privatePort, String protocol) {
        String svcName = getServiceName(ruleName, privatePort, protocol, null, null);
        try {
            Services services = (Services) nsxService.apply(Services.class);
            com.vmware.nsx_policy.model.Service servicePFRule = services.get(svcName);
            if (servicePFRule != null && !servicePFRule.getMarkedForDelete() && !BooleanUtils.toBoolean(servicePFRule.getIsDefault())) {
                services.delete(svcName);
            }
        } catch (Error error) {
            String msg = String.format("Cannot find service %s associated to rule %s, skipping its deletion: %s",
                    svcName, ruleName, error.getMessage());
            logger.debug(msg);
        }
    }

    public void deleteNatRule(Network.Service service, String privatePort, String protocol, String networkName, String tier1GatewayName, String ruleName) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            logger.debug("Deleting NSX NAT rule {} for tier-1 gateway {} (network: {})", ruleName, tier1GatewayName, networkName);
            PolicyNatRule natRule = natService.get(tier1GatewayName, NatId.USER.name(), ruleName);
            if (natRule != null && !natRule.getMarkedForDelete()) {
                logger.debug("Deleting rule {} from Tier 1 Gateway {}", ruleName, tier1GatewayName);
                natService.delete(tier1GatewayName, NatId.USER.name(), ruleName);
            }
        } catch (Error error) {
            String msg = String.format("Cannot find NAT rule with name %s: %s, skipping deletion", ruleName, error.getMessage());
            logger.debug(msg);
        }

        if (service == Network.Service.PortForwarding) {
            deletePortForwardingNatRuleService(ruleName, privatePort, protocol);
        }
    }

    public void createPortForwardingRule(String ruleName, String tier1GatewayName, String networkName, String publicIp,
                                         String vmIp, String publicPort, String service) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            logger.debug(String.format("Creating NSX Port-Forwarding NAT %s for network %s", ruleName, networkName));
            PolicyNatRule rule = new PolicyNatRule.Builder()
                    .setId(ruleName)
                    .setDisplayName(ruleName)
                    .setAction(NatAction.DNAT.name())
                    .setFirewallMatch(FirewallMatch.MATCH_INTERNAL_ADDRESS.name())
                    .setDestinationNetwork(publicIp)
                    .setTranslatedNetwork(vmIp)
                    .setTranslatedPorts(String.valueOf(publicPort))
                    .setService(service)
                    .setEnabled(true)
                    .build();
            natService.patch(tier1GatewayName, NatId.USER.name(), ruleName, rule);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to add NSX Port-forward rule %s for network: %s, due to %s",
                    ruleName, networkName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public boolean doesPfRuleExist(String ruleName, String tier1GatewayName) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            PolicyNatRule rule = natService.get(tier1GatewayName, NAT_ID, ruleName);
            logger.debug("Rule {} from Tier 1 GW {}: {}", ruleName, tier1GatewayName,
                    rule == null ? "null" : rule.getId() + " " + rule.getPath());
            return !Objects.isNull(rule);
        } catch (Error error) {
            String msg = String.format("Error checking if port forwarding rule %s exists on Tier 1 Gateway %s: %s",
                    ruleName, tier1GatewayName, error.getMessage());
            Throwable throwable = error.getCause();
            logger.error(msg, throwable);
            return false;
        }
    }

    List<LBPoolMember> getLbPoolMembers(List<NsxLoadBalancerMember> memberList, String tier1GatewayName) {
        List<LBPoolMember> members = new ArrayList<>();
        for (NsxLoadBalancerMember member : memberList) {
            try {
                String serverPoolMemberName = getServerPoolMemberName(tier1GatewayName, member.getVmId());
                LBPoolMember lbPoolMember = new LBPoolMember.Builder()
                        .setDisplayName(serverPoolMemberName)
                        .setIpAddress(member.getVmIp())
                        .setPort(String.valueOf(member.getPort()))
                        .build();
                members.add(lbPoolMember);
            } catch (Error error) {
                ApiError ae = error.getData()._convertTo(ApiError.class);
                String msg = String.format("Failed to create NSX LB pool members, due to: %s", ae.getErrorMessage());
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
        }
        return members;
    }
    public void createNsxLbServerPool(List<NsxLoadBalancerMember> memberList, String tier1GatewayName, String lbServerPoolName,
                                      String algorithm, String privatePort, String protocol) {
        try {
            String activeMonitorPath = getLbActiveMonitorPath(lbServerPoolName, privatePort, protocol);
            List<LBPoolMember> members = getLbPoolMembers(memberList, tier1GatewayName);
            LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
            LBPool lbPool = new LBPool.Builder()
                    .setId(lbServerPoolName)
                    .setDisplayName(lbServerPoolName)
                    .setAlgorithm(getLoadBalancerAlgorithm(algorithm))
                    .setMembers(members)
                    .setPassiveMonitorPath(NSX_LB_PASSIVE_MONITOR)
                    .setActiveMonitorPaths(List.of(activeMonitorPath))
                    .build();
            lbPools.patch(lbServerPoolName, lbPool);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX LB server pool, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private String getLbActiveMonitorPath(String lbServerPoolName, String port, String protocol) {
        LbMonitorProfiles lbActiveMonitor = (LbMonitorProfiles) nsxService.apply(LbMonitorProfiles.class);
        String lbMonitorProfileId = getActiveMonitorProfileName(lbServerPoolName, port, protocol);
        if ("TCP".equals(protocol.toUpperCase(Locale.ROOT))) {
            LBTcpMonitorProfile lbTcpMonitorProfile = new LBTcpMonitorProfile.Builder(TCP_MONITOR_PROFILE)
                    .setDisplayName(lbMonitorProfileId)
                    .setMonitorPort(Long.parseLong(port))
                    .build();
            lbActiveMonitor.patch(lbMonitorProfileId, lbTcpMonitorProfile);
        } else if ("UDP".equals(protocol.toUpperCase(Locale.ROOT))) {
            LBIcmpMonitorProfile icmpMonitorProfile = new LBIcmpMonitorProfile.Builder(ICMP_MONITOR_PROFILE)
                    .setDisplayName(lbMonitorProfileId)
                    .build();
            lbActiveMonitor.patch(lbMonitorProfileId, icmpMonitorProfile);
        }

        LBMonitorProfileListResult listResult = listLBActiveMonitors(lbActiveMonitor);
        Optional<Structure> monitorProfile = listResult.getResults().stream().filter(profile -> profile._getDataValue().getField("id").toString().equals(lbMonitorProfileId)).findFirst();
        return monitorProfile.map(structure -> structure._getDataValue().getField("path").toString()).orElse(null);
    }

    LBMonitorProfileListResult listLBActiveMonitors(LbMonitorProfiles lbActiveMonitor) {
        return lbActiveMonitor.list(null, false, null, null, null, null);
    }

    public void createNsxLoadBalancer(String tier1GatewayName) {
        try {
            String lbName = getLoadBalancerName(tier1GatewayName);
            LbServices lbServices = (LbServices) nsxService.apply(LbServices.class);
            LBService lbService = getLbService(lbName);
            if (Objects.nonNull(lbService)) {
                return;
            }
            lbService = new LBService.Builder()
                    .setId(lbName)
                    .setDisplayName(lbName)
                    .setEnabled(true)
                    .setSize(LBSize.SMALL.name())
                    .setConnectivityPath(TIER_1_GATEWAY_PATH_PREFIX + tier1GatewayName)
                    .build();
            lbServices.patch(lbName, lbService);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX load balancer, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void createAndAddNsxLbVirtualServer(String tier1GatewayName, long lbId, String publicIp, String publicPort,
                                               List<NsxLoadBalancerMember> memberList, String algorithm, String protocol, String privatePort) {
        try {
            String lbServerPoolName = getServerPoolName(tier1GatewayName, lbId);
            createNsxLbServerPool(memberList, tier1GatewayName, lbServerPoolName, algorithm, privatePort, protocol);
            createNsxLoadBalancer(tier1GatewayName);

            String lbVirtualServerName = getVirtualServerName(tier1GatewayName, lbId);
            String lbServiceName = getLoadBalancerName(tier1GatewayName);
            LbVirtualServers lbVirtualServers = (LbVirtualServers) nsxService.apply(LbVirtualServers.class);
            if (Objects.nonNull(getLbVirtualServerService(lbVirtualServers, lbServiceName))) {
                return;
            }
            LBVirtualServer lbVirtualServer = new LBVirtualServer.Builder()
                    .setId(lbVirtualServerName)
                    .setDisplayName(lbVirtualServerName)
                    .setApplicationProfilePath(getLbProfileForProtocol(protocol))
                    .setIpAddress(publicIp)
                    .setLbServicePath(getLbPath(lbServiceName))
                    .setPoolPath(getLbPoolPath(lbServerPoolName))
                    .setPorts(List.of(publicPort))
                    .build();
            lbVirtualServers.patch(lbVirtualServerName, lbVirtualServer);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create and add NSX virtual server to the Load Balancer, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private LBVirtualServer getLbVirtualServerService(LbVirtualServers lbVirtualServers, String lbVSName) {
        try {
            LBVirtualServer lbVirtualServer = lbVirtualServers.get(lbVSName);
            if (Objects.nonNull(lbVirtualServer)) {
                return lbVirtualServer;
            }
        } catch (Exception e) {
            logger.debug(String.format("Found an LB virtual server named: %s on NSX", lbVSName));
            return null;
        }
        return null;
    }

    public void deleteNsxLbResources(String tier1GatewayName, long lbId) {
        try {
            // Delete associated Virtual servers
            LbVirtualServers lbVirtualServers = (LbVirtualServers) nsxService.apply(LbVirtualServers.class);
            String lbVirtualServerName = getVirtualServerName(tier1GatewayName, lbId);
            lbVirtualServers.delete(lbVirtualServerName, false);

            // Delete LB pool
            LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
            String lbServerPoolName = getServerPoolName(tier1GatewayName, lbId);
            lbPools.delete(lbServerPoolName, false);

            // delete associated LB Active monitor profile
            LbMonitorProfiles lbActiveMonitor = (LbMonitorProfiles) nsxService.apply(LbMonitorProfiles.class);
            LBMonitorProfileListResult listResult = listLBActiveMonitors(lbActiveMonitor);
            List<String> profileIds = listResult.getResults().stream().filter(profile -> profile._getDataValue().getField("id").toString().contains(lbServerPoolName))
                    .map(profile -> profile._getDataValue().getField("id").toString()).collect(Collectors.toList());
            for(String profileId : profileIds) {
                lbActiveMonitor.delete(profileId, true);
            }
            // Delete load balancer
            LBVirtualServerListResult lbVsListResult = lbVirtualServers.list(null, null, null, null, null, null);
            LBPoolListResult lbPoolListResult = lbPools.list(null, null, null, null, null, null);
            if (CollectionUtils.isEmpty(lbVsListResult.getResults()) && CollectionUtils.isEmpty(lbPoolListResult.getResults())) {
                String lbName = getLoadBalancerName(tier1GatewayName);
                deleteLoadBalancer(lbName);
            }

        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete NSX Load Balancer resources, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteLoadBalancer(String lbName) {
        LbServices lbServices = (LbServices) nsxService.apply(LbServices.class);
        lbServices.delete(lbName, true);
    }

    private String getLbPoolPath(String lbPoolName) {
        try {
            LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
            LBPool lbPool = lbPools.get(lbPoolName);
            return Objects.nonNull(lbPool) ? lbPool.getPath() : null;
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to get NSX LB server pool, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }
    private LBService getLbService(String lbName) {
        try {
            LbServices lbServices = (LbServices) nsxService.apply(LbServices.class);
            LBService lbService = lbServices.get(lbName);
            if (Objects.nonNull(lbService)) {
                return lbService;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String getLbPath(String lbServiceName) {
        try {
            LbServices lbServices = (LbServices) nsxService.apply(LbServices.class);
            LBService lbService = lbServices.get(lbServiceName);
            return Objects.nonNull(lbService) ? lbService.getPath() : null;
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to get NSX LB server pool, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private String getLbProfileForProtocol(String protocol) {
        try {
            LbAppProfiles lbAppProfiles = (LbAppProfiles) nsxService.apply(LbAppProfiles.class);
            LBAppProfileListResult lbAppProfileListResults = lbAppProfiles.list(null, null,
                    null, null, null, null);
            Optional<Structure> appProfile = lbAppProfileListResults.getResults().stream().filter(profile -> profile._getDataValue().getField("path").toString().contains(protocol.toLowerCase(Locale.ROOT))).findFirst();
            return appProfile.map(structure -> structure._getDataValue().getField("path").toString()).orElse(null);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to list NSX LB App profiles, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public String getNsxInfraServices(String ruleName, String port, String protocol, Integer icmpType, Integer icmpCode) {
        try {
            Services service = (Services) nsxService.apply(Services.class);

            // Find default service if present
            ServiceListResult serviceList = service.list(null, true, false, null, null, null, null);

            List<com.vmware.nsx_policy.model.Service> services = serviceList.getResults();
            List<String> matchedDefaultSvc = services.parallelStream().filter(svc ->
                            (svc.getServiceEntries().get(0)._getDataValue().getField("resource_type").toString().equals("L4PortSetServiceEntry")) &&
                                    svc.getServiceEntries().get(0)._getDataValue().getField("destination_ports").toString().equals("["+port+"]")
                                    && svc.getServiceEntries().get(0)._getDataValue().getField("l4_protocol").toString().equals(protocol))
                    .map(svc -> svc.getServiceEntries().get(0)._getDataValue().getField("parent_path").toString())
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(matchedDefaultSvc)) {
                return matchedDefaultSvc.get(0);
            }

            // Else, find if there's a service matching the rule name
            String servicePath = getServiceById(ruleName);
            if (Objects.nonNull(servicePath)) {
                return servicePath;
            }

            // Else, create a service entry
            return getServicePath(ruleName, port, protocol, icmpType, icmpCode);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to list NSX infra service, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }


    private com.vmware.nsx_policy.model.Service getInfraService(String ruleName, String port, String protocol, Integer icmpType, Integer icmpCode) {
        Services service = (Services) nsxService.apply(Services.class);
        String serviceName = getServiceName(ruleName, port, protocol, icmpType, icmpCode);
        createNsxInfraService(service, serviceName, ruleName, port, protocol, icmpType, icmpCode);
        return service.get(serviceName);
    }

    public String getServicePath(String ruleName, String port, String protocol, Integer icmpType, Integer icmpCode)  {
        com.vmware.nsx_policy.model.Service svc = getInfraService(ruleName, port, protocol, icmpType, icmpCode);
        return svc.getServiceEntries().get(0)._getDataValue().getField("parent_path").toString();
    }

    public void createNsxInfraService(Services service, String serviceName, String ruleName, String port, String protocol,
                                      Integer icmpType, Integer icmpCode) {
        try {
            List<Structure> serviceEntries = new ArrayList<>();
            protocol = "ICMP".equalsIgnoreCase(protocol) ? "ICMPv4" : protocol;
            String serviceEntryName = getServiceEntryName(ruleName, port, protocol);
            if (protocol.equals("ICMPv4")) {
                serviceEntries.add(new ICMPTypeServiceEntry.Builder()
                                .setId(serviceEntryName)
                                .setDisplayName(serviceEntryName)
//                                .setIcmpCode(Long.valueOf(icmpCode))
                                .setIcmpType(Long.valueOf(icmpType))
                                .setProtocol(protocol)
                                .build()
                );
            } else {
                serviceEntries.add(new L4PortSetServiceEntry.Builder()
                        .setId(serviceEntryName)
                        .setDisplayName(serviceEntryName)
                        .setDestinationPorts(List.of(port))
                        .setL4Protocol(protocol)
                        .build());
            }
            com.vmware.nsx_policy.model.Service infraService = new com.vmware.nsx_policy.model.Service.Builder()
                    .setServiceEntries(serviceEntries)
                    .setId(serviceName)
                    .setDisplayName(serviceName)
                    .build();
            service.patch(serviceName, infraService);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX infra service, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private String getServiceById(String ruleName) {
        try {
            Services service = (Services) nsxService.apply(Services.class);
            com.vmware.nsx_policy.model.Service svc1 = service.get(ruleName);
            if (Objects.nonNull(svc1)) {
                return ((L4PortSetServiceEntry) svc1.getServiceEntries().get(0)).getParentPath();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Create a Group for the Segment on the Inventory, with the same name as the segment and being the segment the only member of the group
     */
    public void createGroupForSegment(String segmentName) {
        logger.info(String.format("Creating Group for Segment %s", segmentName));

        PathExpression pathExpression = new PathExpression();
        List<String> paths = List.of(String.format("%s/%s", SEGMENTS_PATH, segmentName));
        pathExpression.setPaths(paths);

        Groups service = (Groups) nsxService.apply(Groups.class);
        Group group = new Group.Builder()
                .setId(segmentName)
                .setDisplayName(segmentName)
                .setExpression(List.of(pathExpression))
                .build();
        service.patch(DEFAULT_DOMAIN, segmentName, group);
    }

    /**
     * Remove Segment Group from the Inventory
     */
    private void removeGroupForSegment(String segmentName) {
        logger.info(String.format("Removing Group for Segment %s", segmentName));
        Groups service = (Groups) nsxService.apply(Groups.class);
        service.delete(DEFAULT_DOMAIN, segmentName, true, false);
    }

    private void removeSegmentDistributedFirewallRules(String segmentName) {
        try {
            SecurityPolicies services = (SecurityPolicies) nsxService.apply(SecurityPolicies.class);
            services.delete(DEFAULT_DOMAIN, segmentName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to remove NSX distributed firewall policy for segment %s, due to: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void createSegmentDistributedFirewall(String segmentName, List<NsxNetworkRule> nsxRules) {
        try {
            String groupPath = getGroupPath(segmentName);
            if (Objects.isNull(groupPath)) {
                throw new CloudRuntimeException(String.format("Failed to find group for segment %s", segmentName));
            }
            SecurityPolicies services = (SecurityPolicies) nsxService.apply(SecurityPolicies.class);
            List<Rule> rules = getRulesForDistributedFirewall(segmentName, nsxRules);
            SecurityPolicy policy = new SecurityPolicy.Builder()
                    .setDisplayName(segmentName)
                    .setId(segmentName)
                    .setCategory("Application")
                    .setRules(rules)
                    .setScope(List.of(groupPath))
                    .build();
            services.patch(DEFAULT_DOMAIN, segmentName, policy);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX distributed firewall policy for segment %s, due to: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteDistributedFirewallRules(String segmentName, List<NsxNetworkRule> nsxRules) {
        for(NsxNetworkRule rule : nsxRules) {
            String ruleId = NsxControllerUtils.getNsxDistributedFirewallPolicyRuleId(segmentName, rule.getRuleId());
           String svcName = getServiceName(ruleId, rule.getPrivatePort(), rule.getProtocol(), rule.getIcmpType(), rule.getIcmpCode());
            // delete rules
            Rules rules = (Rules) nsxService.apply(Rules.class);
            rules.delete(DEFAULT_DOMAIN, segmentName, ruleId);
            // delete service - if any
            Services services = (Services) nsxService.apply(Services.class);
            services.delete(svcName);
        }
    }

    private List<Rule> getRulesForDistributedFirewall(String segmentName, List<NsxNetworkRule> nsxRules) {
        List<Rule> rules = new ArrayList<>();
        String groupPath = getGroupPath(segmentName);
        if (Objects.isNull(groupPath)) {
            throw new CloudRuntimeException(String.format("Failed to find group for segment %s", segmentName));
        }
        for (NsxNetworkRule rule : nsxRules) {
            String ruleId = NsxControllerUtils.getNsxDistributedFirewallPolicyRuleId(segmentName, rule.getRuleId());
            Rule ruleToAdd = new Rule.Builder()
                    .setAction(rule.getAclAction().toString())
                    .setId(ruleId)
                    .setDisplayName(ruleId)
                    .setResourceType("SecurityPolicy")
                    .setSourceGroups(getGroupsForTraffic(rule, segmentName, true))
                    .setDestinationGroups(getGroupsForTraffic(rule, segmentName, false))
                    .setServices(getServicesListForDistributedFirewallRule(rule, segmentName))
                    .setScope(List.of(groupPath))
                    .build();
            rules.add(ruleToAdd);
        }
        return rules;
    }

    private List<String> getServicesListForDistributedFirewallRule(NsxNetworkRule rule, String segmentName) {
        List<String> services = List.of("ANY");
        if (!rule.getProtocol().equalsIgnoreCase("all")) {
            String ruleName = String.format("%s-R%s", segmentName, rule.getRuleId());
            String serviceName = getNsxInfraServices(ruleName, rule.getPrivatePort(), rule.getProtocol(),
                    rule.getIcmpType(), rule.getIcmpCode());
            services = List.of(serviceName);
        }
        return services;
    }

    protected List<String> getGroupsForTraffic(NsxNetworkRule rule,
                                             String segmentName, boolean source) {
        List<String> segmentGroup = List.of(String.format("%s/%s", GROUPS_PATH_PREFIX, segmentName));
        List<String> sourceCidrList = rule.getSourceCidrList();
        List<String> destCidrList = rule.getDestinationCidrList();
        List<String> ingressSource = (rule.getService() == Network.Service.NetworkACL ? segmentGroup : destCidrList);
        List<String> egressSource = (rule.getService() == Network.Service.NetworkACL ? sourceCidrList : destCidrList);

        String trafficType = rule.getTrafficType();
        if (trafficType.equalsIgnoreCase("ingress")) {
            return source ? sourceCidrList : ingressSource;
        } else if (trafficType.equalsIgnoreCase("egress")) {
            return source ? segmentGroup : egressSource;
       }
        String err = String.format("Unsupported traffic type %s", trafficType);
        logger.error(err);
        throw new CloudRuntimeException(err);
    }


    private List<Group> listNsxGroups() {
        try {
           Groups groups = (Groups) nsxService.apply(Groups.class);
           GroupListResult result = groups.list(DEFAULT_DOMAIN, null, false, null, null, null, null, null);
           return result.getResults();
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to list NSX groups, due to: %s", ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private String getGroupPath(String segmentName) {
        List<Group> groups = listNsxGroups();
        Optional<Group> matchingGroup = groups.stream().filter(group -> group.getDisplayName().equals(segmentName)).findFirst();
        return matchingGroup.map(Group::getPath).orElse(null);

    }
}
