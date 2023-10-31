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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.infra.DhcpRelayConfigs;
import com.vmware.nsx_policy.infra.LbAppProfiles;
import com.vmware.nsx_policy.infra.LbPools;
import com.vmware.nsx_policy.infra.LbServices;
import com.vmware.nsx_policy.infra.LbVirtualServers;
import com.vmware.nsx_policy.infra.Segments;
import com.vmware.nsx_policy.infra.Services;
import com.vmware.nsx_policy.infra.Sites;
import com.vmware.nsx_policy.infra.Tier1s;
import com.vmware.nsx_policy.infra.sites.EnforcementPoints;
import com.vmware.nsx_policy.infra.tier_0s.LocaleServices;
import com.vmware.nsx_policy.infra.tier_1s.nat.NatRules;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.DhcpRelayConfig;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.L4PortSetServiceEntry;
import com.vmware.nsx_policy.model.LBAppProfileListResult;
import com.vmware.nsx_policy.model.LBPool;
import com.vmware.nsx_policy.model.LBPoolListResult;
import com.vmware.nsx_policy.model.LBPoolMember;
import com.vmware.nsx_policy.model.LBService;
import com.vmware.nsx_policy.model.LBVirtualServer;
import com.vmware.nsx_policy.model.LBVirtualServerListResult;
import com.vmware.nsx_policy.model.LocaleServicesListResult;
import com.vmware.nsx_policy.model.PolicyNatRule;
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
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

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

public class NsxApiClient {

    private final Function<Class<? extends Service>, Service> nsxService;

    public static final int RESPONSE_TIMEOUT_SECONDS = 60;
    private static final Logger LOGGER = Logger.getLogger(NsxApiClient.class);

    // Constants
    private static final String TIER_1_RESOURCE_TYPE = "Tier1";
    private static final String Tier_1_LOCALE_SERVICE_ID = "default";
    private static final String SEGMENT_RESOURCE_TYPE = "Segment";
    private static final String TIER_0_GATEWAY_PATH_PREFIX = "/infra/tier-0s/";
    private static final String TIER_1_GATEWAY_PATH_PREFIX = "/infra/tier-1s/";

    private enum PoolAllocation { ROUTING, LB_SMALL, LB_MEDIUM, LB_LARGE, LB_XLARGE }

    private enum TYPE { ROUTED, NATTED }

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

    public enum  RouteAdvertisementType { TIER1_STATIC_ROUTES, TIER1_CONNECTED, TIER1_NAT,
        TIER1_LB_VIP, TIER1_LB_SNAT, TIER1_DNS_FORWARDER_IP, TIER1_IPSEC_LOCAL_ENDPOINT
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
        ApiClient apiClient = ApiClients.newRestClient(controllerUrl, config);
        nsxService = apiClient::createStub;
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
            LOGGER.error(msg);
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
            LOGGER.error(msg);
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
            LOGGER.error(msg);
            throw new CloudRuntimeException(ae.getErrorMessage());
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
            tier1LocalService.patch(tier1Id, Tier_1_LOCALE_SERVICE_ID, localeService);
        } catch (Error error) {
            throw new CloudRuntimeException(String.format("Failed to instantiate tier-1 gateway %s in edge cluster %s", tier1Id, edgeCluster));
        }
    }

    public void createTier1Gateway(String name, String tier0Gateway, String edgeCluster) {
        String tier0GatewayPath = TIER_0_GATEWAY_PATH_PREFIX + tier0Gateway;
        Tier1 tier1 = getTier1Gateway(name);
        if (tier1 != null) {
            throw new InvalidParameterValueException(String.format("VPC network with name %s exists in NSX zone", name));
        }

        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        tier1 = new Tier1.Builder()
                .setTier0Path(tier0GatewayPath)
                .setResourceType(TIER_1_RESOURCE_TYPE)
                .setPoolAllocation(PoolAllocation.ROUTING.name())
                .setHaMode(HAMode.ACTIVE_STANDBY.name())
                .setFailoverMode(FailoverMode.PREEMPTIVE.name())
                .setRouteAdvertisementTypes(List.of(RouteAdvertisementType.TIER1_CONNECTED.name(), RouteAdvertisementType.TIER1_IPSEC_LOCAL_ENDPOINT.name()))
                .setId(name)
                .setDisplayName(name)
                .build();
        try {
            tier1service.patch(name, tier1);
            createTier1LocaleServices(name, edgeCluster, tier0Gateway);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error creating tier 1 gateway %s: %s", name, ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteTier1Gateway(String tier1Id) {
        com.vmware.nsx_policy.infra.tier_1s.LocaleServices localeService = (com.vmware.nsx_policy.infra.tier_1s.LocaleServices)
                nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.LocaleServices.class);
        localeService.delete(tier1Id, Tier_1_LOCALE_SERVICE_ID);
        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        tier1service.delete(tier1Id);
    }

    public SiteListResult getSites() {
        try {
            Sites sites = (Sites) nsxService.apply(Sites.class);
            return sites.list(null, false, null, null, null, null);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch service segment list due to %s", e.getMessage()));
        }
    }

    public EnforcementPointListResult getEnforcementPoints(String siteId) {
        try {
            EnforcementPoints enforcementPoints = (EnforcementPoints) nsxService.apply(EnforcementPoints.class);
            return enforcementPoints.list(siteId, null, false, null, null, null, null);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch service segment list due to %s", e.getMessage()));
        }
    }

    public TransportZoneListResult getTransportZones() {
        try {
            com.vmware.nsx.TransportZones transportZones = (com.vmware.nsx.TransportZones) nsxService.apply(com.vmware.nsx.TransportZones.class);
            return transportZones.list(null, null, true, null, true, null, null, null, TransportType.OVERLAY.name(), null);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch service segment list due to %s", e.getMessage()));
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
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteSegment(long zoneId, long domainId, long accountId, long vpcId, long networkId, String segmentName) {
        try {
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            LOGGER.debug(String.format("Removing the segment with ID %s", segmentName));
            segmentService.delete(segmentName);
            DhcpRelayConfigs dhcpRelayConfig = (DhcpRelayConfigs) nsxService.apply(DhcpRelayConfigs.class);
            String dhcpRelayConfigId = NsxControllerUtils.getNsxDhcpRelayConfigId(zoneId, domainId, accountId, vpcId, networkId);
            LOGGER.debug(String.format("Removing the DHCP relay config with ID %s", dhcpRelayConfigId));
            dhcpRelayConfig.delete(dhcpRelayConfigId);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error deleting segment %s: %s", segmentName, ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
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

            LOGGER.debug(String.format("Creating NSX static NAT rule %s for tier-1 gateway %s (VPC: %s)", ruleName, tier1GatewayName, vpcName));
            natService.patch(tier1GatewayName, NatId.USER.name(), ruleName, rule);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error creating NSX Static NAT rule %s for tier-1 gateway %s (VPC: %s), due to %s",
                    ruleName, tier1GatewayName, vpcName, ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteNatRule(Network.Service service, String privatePort, String protocol, String networkName, String tier1GatewayName, String ruleName) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            LOGGER.debug(String.format("Deleting NSX static NAT rule %s for tier-1 gateway %s (network: %s)", ruleName, tier1GatewayName, networkName));
            // delete NAT rule
            natService.delete(tier1GatewayName, NatId.USER.name(), ruleName);
            if (service == Network.Service.PortForwarding) {
                String svcName = getServiceName(ruleName, privatePort, protocol);
                // Delete service
                Services services = (Services) nsxService.apply(Services.class);
                services.delete(svcName);
            }
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete NSX Static NAT rule %s for tier-1 gateway %s (VPC: %s), due to %s",
                    ruleName, tier1GatewayName, networkName, ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void createPortForwardingRule(String ruleName, String tier1GatewayName, String networkName, String publicIp,
                                         String vmIp, String publicPort, String service) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            LOGGER.debug(String.format("Creating NSX Port-Forwarding NAT %s for network %s", ruleName, networkName));
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
            String msg = String.format("Failed to delete NSX Port-forward rule %s for network: %s, due to %s",
                    ruleName, networkName, ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void createNsxLbServerPool(List<NsxLoadBalancerMember> memberList, String tier1GatewayName, String lbServerPoolName, String algorithm) {
        for (NsxLoadBalancerMember member : memberList) {
            try {
                String serverPoolMemberName = getServerPoolMemberName(tier1GatewayName, member.getVmId());
                LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
                LBPoolMember lbPoolMember = new LBPoolMember.Builder()
                        .setDisplayName(serverPoolMemberName)
                        .setIpAddress(member.getVmIp())
                        .setPort(String.valueOf(member.getPort()))
                        .build();
                LBPool lbPool = new LBPool.Builder()
                        .setId(lbServerPoolName)
                        .setDisplayName(lbServerPoolName)
                        .setAlgorithm(getLoadBalancerAlgorithm(algorithm))
                        .setMembers(List.of(lbPoolMember))
                        .build();
                lbPools.patch(lbServerPoolName, lbPool);
            } catch (Error error) {
                ApiError ae = error.getData()._convertTo(ApiError.class);
                String msg = String.format("Failed to create NSX LB server pool, due to: %s", ae.getErrorMessage());
                LOGGER.error(msg);
                throw new CloudRuntimeException(msg);
            }
        }
    }

    public void createNsxLoadBalancer(String tier1GatewayName, long lbId) {
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
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void createAndAddNsxLbVirtualServer(String tier1GatewayName, long lbId, String publicIp, String publicPort,
                                               List<NsxLoadBalancerMember> memberList, String algorithm, String protocol) {
        try {
            String lbServerPoolName = getServerPoolName(tier1GatewayName, lbId);
            createNsxLbServerPool(memberList, tier1GatewayName, lbServerPoolName, algorithm);
            createNsxLoadBalancer(tier1GatewayName, lbId);

            String lbVirtualServerName = getVirtualServerName(tier1GatewayName, lbId);
            String lbServiceName = getLoadBalancerName(tier1GatewayName);
            LbVirtualServers lbVirtualServers = (LbVirtualServers) nsxService.apply(LbVirtualServers.class);
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
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteNsxLbResources(String tier1GatewayName, long lbId, long vmId) {
        try {
            // Delete associated Virtual servers
            LbVirtualServers lbVirtualServers = (LbVirtualServers) nsxService.apply(LbVirtualServers.class);
            String lbVirtualServerName = getVirtualServerName(tier1GatewayName, lbId);
            lbVirtualServers.delete(lbVirtualServerName, false);

            // Delete LB pool
            LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
            String lbServerPoolName = getServerPoolName(tier1GatewayName, lbId);
            lbPools.delete(lbServerPoolName, false);

            // Delete load balancer
            LBVirtualServerListResult lbVsListResult = lbVirtualServers.list(null, null, null, null, null, null);
            LBPoolListResult lbPoolListResult = lbPools.list(null, null, null, null, null, null);
            if (CollectionUtils.isEmpty(lbVsListResult.getResults()) && CollectionUtils.isEmpty(lbPoolListResult.getResults())) {
                String lbName = getLoadBalancerName(tier1GatewayName);
                LbServices lbServices = (LbServices) nsxService.apply(LbServices.class);
                lbServices.delete(lbName, true);
            }

        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete NSX Load Balancer resources, due to: %s", ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private String getLbPoolPath(String lbPoolName) {
        try {
            LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
            LBPool lbPool = lbPools.get(lbPoolName);
            return Objects.nonNull(lbPool) ? lbPool.getPath() : null;
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to get NSX LB server pool, due to: %s", ae.getErrorMessage());
            LOGGER.error(msg);
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
            LOGGER.error(msg);
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
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public String getNsxInfraServices(String ruleName, String port, String protocol) {
        try {
            Services service = (Services) nsxService.apply(Services.class);

            // Find default service if present
            ServiceListResult serviceList = service.list(null, true, false, null, null, null, null);

            List<com.vmware.nsx_policy.model.Service> services = serviceList.getResults();
            List<String> matchedDefaultSvc = services.parallelStream().filter(svc ->
                            (svc.getServiceEntries().get(0) instanceof L4PortSetServiceEntry) &&
                                    ((L4PortSetServiceEntry) svc.getServiceEntries().get(0)).getDestinationPorts().get(0).equals(port)
                    && (((L4PortSetServiceEntry) svc.getServiceEntries().get(0)).getL4Protocol().equals(protocol)))
                    .map(svc -> ((L4PortSetServiceEntry) svc.getServiceEntries().get(0)).getDestinationPorts().get(0))
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
            return createNsxInfraService(ruleName, port, protocol);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to list NSX infra service, due to: %s", ae.getErrorMessage());
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public String createNsxInfraService(String ruleName, String port, String protocol) {
        try {
            String serviceEntryName = getServiceEntryName(ruleName, port, protocol);
            String serviceName = getServiceName(ruleName, port, protocol);
            Services service = (Services) nsxService.apply(Services.class);
            com.vmware.nsx_policy.model.Service infraService = new com.vmware.nsx_policy.model.Service.Builder()
                    .setServiceEntries(List.of(
                            new L4PortSetServiceEntry.Builder()
                                    .setId(serviceEntryName)
                                    .setDisplayName(serviceEntryName)
                                    .setDestinationPorts(List.of(port))
                                    .setL4Protocol(protocol)
                                    .build()
                    ))
                    .setId(serviceName)
                    .setDisplayName(serviceName)
                    .build();
            service.patch(serviceName, infraService);

            com.vmware.nsx_policy.model.Service svc = service.get(serviceName);
            return svc.getServiceEntries().get(0)._getDataValue().getField("parent_path").toString();

        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX infra service, due to: %s", ae.getErrorMessage());
            LOGGER.error(msg);
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
}
