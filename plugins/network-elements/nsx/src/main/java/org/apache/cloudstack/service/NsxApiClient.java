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
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.network.nsx.NsxService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.cluster.Status;
import com.vmware.nsx.model.ClusterStatus;
import com.vmware.nsx.model.ControllerClusterStatus;
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.infra.DhcpRelayConfigs;
import com.vmware.nsx_policy.infra.IpsecVpnDpdProfiles;
import com.vmware.nsx_policy.infra.IpsecVpnIkeProfiles;
import com.vmware.nsx_policy.infra.IpsecVpnTunnelProfiles;
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
import com.vmware.nsx_policy.infra.tier_1s.IpsecVpnServices;
import com.vmware.nsx_policy.infra.tier_1s.ipsec_vpn_services.LocalEndpoints;
import com.vmware.nsx_policy.infra.tier_1s.ipsec_vpn_services.Sessions;
import com.vmware.nsx_policy.infra.tier_1s.ipsec_vpn_services.sessions.DetailedStatus;
import com.vmware.nsx_policy.infra.tier_1s.nat.NatRules;
import com.vmware.nsx_policy.model.AggregateIPSecVpnSessionStatus;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.DhcpRelayConfig;
import com.vmware.nsx_policy.model.EnforcementPoint;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.Group;
import com.vmware.nsx_policy.model.GroupListResult;
import com.vmware.nsx_policy.model.ICMPTypeServiceEntry;
import com.vmware.nsx_policy.model.IPSecVpnDpdProfile;
import com.vmware.nsx_policy.model.IPSecVpnIkeProfile;
import com.vmware.nsx_policy.model.IPSecVpnLocalEndpoint;
import com.vmware.nsx_policy.model.IPSecVpnLocalEndpointListResult;
import com.vmware.nsx_policy.model.IPSecVpnService;
import com.vmware.nsx_policy.model.IPSecVpnSession;
import com.vmware.nsx_policy.model.IPSecVpnServiceListResult;
import com.vmware.nsx_policy.model.IPSecVpnSessionListResult;
import com.vmware.nsx_policy.model.IPSecVpnSessionStatusNsxt;
import com.vmware.nsx_policy.model.IPSecVpnTunnelInterface;
import com.vmware.nsx_policy.model.IPSecVpnTunnelProfile;
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
import com.vmware.nsx_policy.model.PolicyGroupMemberDetails;
import com.vmware.nsx_policy.model.RouteBasedIPSecVpnSession;
import com.vmware.nsx_policy.model.RouterNexthop;
import com.vmware.nsx_policy.model.Rule;
import com.vmware.nsx_policy.model.SecurityPolicy;
import com.vmware.nsx_policy.model.Segment;
import com.vmware.nsx_policy.model.SegmentSubnet;
import com.vmware.nsx_policy.model.ServiceListResult;
import com.vmware.nsx_policy.model.Site;
import com.vmware.nsx_policy.model.StaticRoutesListResult;
import com.vmware.nsx_policy.model.Tag;
import com.vmware.nsx_policy.model.Tier1;
import com.vmware.nsx_policy.model.TunnelInterfaceIPSubnet;
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
import com.vmware.vapi.std.errors.NotFound;
import org.apache.cloudstack.resource.NsxLoadBalancerMember;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.cloudstack.utils.NsxVpnCryptoUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServerPoolMemberName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServerPoolName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServiceName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVirtualServerName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getServiceEntryName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getLoadBalancerName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getLoadBalancerAlgorithm;
import static org.apache.cloudstack.utils.NsxControllerUtils.getActiveMonitorProfileName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getTier1GatewayName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnDpdProfileName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnEspProfileName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnIkeProfileName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnLocalEndpointName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnLocalEndpointNoSnatRuleName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnNoSnatRuleName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnNoSnatRuleNamePrefix;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnServiceName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnSessionName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnStaticRouteName;
import static org.apache.cloudstack.utils.NsxControllerUtils.getVpnStaticRouteNamePrefix;

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
    protected static final String IPSEC_VPN_IKE_PROFILES_PATH_PREFIX = "/infra/ipsec-vpn-ike-profiles/";
    protected static final String IPSEC_VPN_TUNNEL_PROFILES_PATH_PREFIX = "/infra/ipsec-vpn-tunnel-profiles/";
    protected static final String IPSEC_VPN_DPD_PROFILES_PATH_PREFIX = "/infra/ipsec-vpn-dpd-profiles/";
    // NSX resolves NAT rules scoped to a VTI only for a tunnel interface with this exact name (KB 435087)
    protected static final String VPN_DEFAULT_TUNNEL_INTERFACE_NAME = "default-tunnel-interface";
    // NSX purges objects marked for deletion in a cycle that it documents as taking up to 5 minutes,
    // and rejects recreating an object under the same path until then
    protected static final int VPN_MARKED_FOR_DELETION_RETRIES = 24;
    protected static final int VPN_MARKED_FOR_DELETION_RETRY_INTERVAL_SECS = 15;
    // In on demand mode the probe interval is the idle time before a probe is sent, which NSX limits
    // to 1-10 seconds (the 3-360 second range only applies to periodic probing)
    protected static final long VPN_DPD_PROBE_INTERVAL_SECS = 10L;
    protected static final long VPN_DPD_RETRY_COUNT = 10L;
    // NSX evaluates NAT rules by ascending sequence number, so the traffic the VPN exempts from source
    // NAT has to be matched before the catch all source NAT rule of the VPC
    protected static final long VPN_NO_SNAT_SEQUENCE_NUMBER = 100L;
    protected static final long CATCH_ALL_NAT_SEQUENCE_NUMBER = 1000L;
    protected static final String VPN_ORIGINAL_SNAT_SEQUENCE_TAG_SCOPE = "cloudstack-vpn-original-snat-sequence";
    protected static final int NSX_MAX_TAGS = 30;
    protected static final String VPN_SESSION_STATUS_UNKNOWN = "UNKNOWN";
    protected static final String VPN_SESSION_STATUS_NOT_FOUND = "NOT_FOUND";

    public enum VpnSessionProvisioningResult {
        CREATED,
        PREEXISTING
    }

    private enum PoolAllocation { ROUTING, LB_SMALL, LB_MEDIUM, LB_LARGE, LB_XLARGE }

    private enum HAMode { ACTIVE_STANDBY, ACTIVE_ACTIVE }

    private enum FailoverMode { PREEMPTIVE, NON_PREEMPTIVE }

    private enum AdminState { UP, DOWN }

    private enum TransportType { OVERLAY, VLAN }

    private enum NatId { USER, INTERNAL, DEFAULT }

    private enum NatAction {SNAT, DNAT, REFLEXIVE, NO_SNAT}

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

    /**
     * The IKE traffic the gateway originates from the local endpoint must keep that address as its
     * source: the catch all source NAT rule would otherwise rewrite it to the VPC source NAT IP and the
     * peer, which only knows the local endpoint address, would ignore the packets. Applied whenever a
     * VPN service or connection is created so that gateways predating this also get the exemption.
     */
    public void ensureVpnNatExemptions(String tier1GatewayName, String localEndpointIp) {
        demoteCatchAllSourceNatRule(tier1GatewayName);
        String localEndpointNoSnatRuleName = getVpnLocalEndpointNoSnatRuleName(getVpnServiceName(tier1GatewayName));
        NatRules natService = (NatRules) nsxService.apply(NatRules.class);
        PolicyNatRule localEndpointNoSnatRule = new PolicyNatRule.Builder()
                .setId(localEndpointNoSnatRuleName)
                .setDisplayName(localEndpointNoSnatRuleName)
                .setAction(NatAction.NO_SNAT.name())
                .setSourceNetwork(localEndpointIp)
                .setSequenceNumber(VPN_NO_SNAT_SEQUENCE_NUMBER)
                .setEnabled(true)
                .build();
        natService.patch(tier1GatewayName, NatId.USER.name(), localEndpointNoSnatRuleName, localEndpointNoSnatRule);
    }

    /** Existing CloudStack source NAT must be evaluated after the VPN no-SNAT rules. */
    private void demoteCatchAllSourceNatRule(String tier1GatewayName) {
        NatRules natRulesService = (NatRules) nsxService.apply(NatRules.class);
        try {
            String ruleId = getCloudStackSourceNatRuleId(tier1GatewayName);
            PolicyNatRule natRule = natRulesService.get(tier1GatewayName, NatId.USER.name(), ruleId);
            if (!isCatchAllSourceNatRule(natRule)) {
                return;
            }
            List<Tag> tags = copyNatRuleTags(natRule);
            Tag originalSequenceTag = findOriginalSnatSequenceTag(tags);
            if (originalSequenceTag == null) {
                if (tags.size() >= NSX_MAX_TAGS) {
                    throw new CloudRuntimeException(String.format(
                            "Cannot preserve the source NAT rule sequence of tier-1 gateway %s because the rule already has the maximum number of tags",
                            tier1GatewayName));
                }
                long originalSequence = natRule.getSequenceNumber() == null ? 0L : natRule.getSequenceNumber();
                tags.add(new Tag.Builder()
                        .setScope(VPN_ORIGINAL_SNAT_SEQUENCE_TAG_SCOPE)
                        .setTag(String.valueOf(originalSequence))
                        .build());
            }
            logger.debug("Moving CloudStack source NAT rule {} on tier-1 gateway {} behind VPN no-SNAT rules",
                    ruleId, tier1GatewayName);
            natRulesService.patch(tier1GatewayName, NatId.USER.name(), ruleId,
                    copyNatRule(natRule, CATCH_ALL_NAT_SEQUENCE_NUMBER, tags));
        } catch (NotFound e) {
            logger.debug("CloudStack source NAT rule is absent on tier-1 gateway {}; no VPN NAT ordering change is required",
                    tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format(
                    "Failed to order the source NAT rules of tier-1 gateway %s before creating the NSX VPN exemptions: %s",
                    tier1GatewayName, ae.getErrorMessage()), error);
        }
    }

    void restoreSourceNatRuleSequence(String tier1GatewayName) {
        NatRules natRulesService = (NatRules) nsxService.apply(NatRules.class);
        try {
            String ruleId = getCloudStackSourceNatRuleId(tier1GatewayName);
            PolicyNatRule natRule = natRulesService.get(tier1GatewayName, NatId.USER.name(), ruleId);
            List<Tag> tags = copyNatRuleTags(natRule);
            Tag originalSequenceTag = findOriginalSnatSequenceTag(tags);
            if (originalSequenceTag == null) {
                return;
            }
            long originalSequence;
            try {
                originalSequence = Long.parseLong(originalSequenceTag.getTag());
            } catch (NumberFormatException e) {
                throw new CloudRuntimeException(String.format(
                        "Invalid saved source NAT sequence '%s' on tier-1 gateway %s",
                        originalSequenceTag.getTag(), tier1GatewayName), e);
            }
            tags.remove(originalSequenceTag);
            natRulesService.patch(tier1GatewayName, NatId.USER.name(), ruleId,
                    copyNatRule(natRule, originalSequence, tags));
        } catch (NotFound e) {
            logger.debug("CloudStack source NAT rule is absent on tier-1 gateway {}; no sequence restoration is required",
                    tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format(
                    "Failed to restore the source NAT rule order of tier-1 gateway %s after deleting the NSX VPN gateway: %s",
                    tier1GatewayName, ae.getErrorMessage()), error);
        }
    }

    private String getCloudStackSourceNatRuleId(String tier1GatewayName) {
        return tier1GatewayName + "-NAT";
    }

    private List<Tag> copyNatRuleTags(PolicyNatRule natRule) {
        return natRule.getTags() == null ? new ArrayList<>() : new ArrayList<>(natRule.getTags());
    }

    private Tag findOriginalSnatSequenceTag(List<Tag> tags) {
        return tags.stream()
                .filter(tag -> VPN_ORIGINAL_SNAT_SEQUENCE_TAG_SCOPE.equals(tag.getScope()))
                .findFirst()
                .orElse(null);
    }

    private PolicyNatRule copyNatRule(PolicyNatRule natRule, long sequenceNumber, List<Tag> tags) {
        return new PolicyNatRule.Builder()
                .setId(natRule.getId())
                .setDisplayName(natRule.getDisplayName())
                .setDescription(natRule.getDescription())
                .setAction(natRule.getAction())
                .setTranslatedNetwork(natRule.getTranslatedNetwork())
                .setTranslatedPorts(natRule.getTranslatedPorts())
                .setSourceNetwork(natRule.getSourceNetwork())
                .setDestinationNetwork(natRule.getDestinationNetwork())
                .setService(natRule.getService())
                .setScope(natRule.getScope())
                .setFirewallMatch(natRule.getFirewallMatch())
                .setPolicyBasedVpnMode(natRule.getPolicyBasedVpnMode())
                .setLogging(natRule.getLogging())
                .setEnabled(natRule.getEnabled())
                .setTags(tags)
                .setSequenceNumber(sequenceNumber)
                .build();
    }

    private boolean isCatchAllSourceNatRule(PolicyNatRule natRule) {
        return NatAction.SNAT.name().equals(natRule.getAction())
                && isAnyNatMatch(natRule.getSourceNetwork())
                && isAnyNatMatch(natRule.getDestinationNetwork());
    }

    private boolean isAnyNatMatch(String network) {
        return network == null || network.isBlank() || "ANY".equalsIgnoreCase(network)
                || "0.0.0.0/0".equals(network) || "::/0".equals(network);
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
            logger.debug("NSX Tier-1 gateway with name: {} not found", tier1GatewayId);
        }
        return null;
    }

    private Optional<com.vmware.nsx_policy.model.LocaleServices> findTier0LocalServices(String tier0Gateway) {
        try {
            LocaleServices tier0LocaleServices = (LocaleServices) nsxService.apply(LocaleServices.class);
            LocaleServicesListResult result = tier0LocaleServices.list(tier0Gateway, null, false, null, 1L, null, null);
            return Optional.ofNullable(result.getResults())
                    .filter(Predicate.not(List::isEmpty))
                    .map(l -> l.get(0));
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch locale services for tier gateway %s due to %s", tier0Gateway, e.getMessage()));
        }
    }

    /**
     * To instantiate Tier-1 in Edge Cluster
     */
    private void createTier1LocaleServices(String tier1Id, String edgeCluster, String tier0Gateway) {
        try {
            Optional<com.vmware.nsx_policy.model.LocaleServices> localeServices = findTier0LocalServices(tier0Gateway);
            if (localeServices.isEmpty()) {
                throw new CloudRuntimeException(String.format("Failed to find locale services for tier-0 gateway %s", tier0Gateway));
            }
            com.vmware.nsx_policy.infra.tier_1s.LocaleServices tier1LocalService = (com.vmware.nsx_policy.infra.tier_1s.LocaleServices) nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.LocaleServices.class);
            com.vmware.nsx_policy.model.LocaleServices localeService = new com.vmware.nsx_policy.model.LocaleServices.Builder()
                    .setEdgeClusterPath(localeServices.get().getEdgeClusterPath()).build();
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
            logger.info("VPC network with name {} exists in NSX zone", name);
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
            logger.warn("The Tier 1 Gateway {} does not exist, cannot be removed", tier1Id);
            return;
        }
        removeTier1VpnResources(tier1Id);
        removeTier1GatewayNatRules(tier1Id);
        localeService.delete(tier1Id, TIER_1_LOCALE_SERVICE_ID);
        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        tier1service.delete(tier1Id);
    }

    private void removeTier1GatewayNatRules(String tier1Id) {
        NatRules natRulesService = (NatRules) nsxService.apply(NatRules.class);
        List<PolicyNatRule> natRules = PagedFetcher.<PolicyNatRuleListResult, PolicyNatRule>withPageFetcher(
                cursor -> natRulesService.list(tier1Id, NAT_ID, cursor, false, null, null, null, null)
                ).cursorExtractor(PolicyNatRuleListResult::getCursor)
                .itemsExtractor(PolicyNatRuleListResult::getResults)
                .itemsSetter((page, allItems) -> {
                    page.setResults(allItems);
                    page.setResultCount((long) allItems.size());
                })
                .fetchAll()
                .getResults();
        if (CollectionUtils.isEmpty(natRules)) {
            logger.debug("Didn't find any NAT rule to remove on the Tier 1 Gateway {}", tier1Id);
        } else {
            for (PolicyNatRule natRule : natRules) {
                logger.debug("Removing NAT rule {} from Tier 1 Gateway {}", natRule.getId(), tier1Id);
                natRulesService.delete(tier1Id, NAT_ID, natRule.getId());
            }
        }

    }

    public String getDefaultSiteId() {
        Optional<Site> site = findFirstSite();
        if (site.isEmpty()) {
            String errorMsg = "No sites are found in the linked NSX infrastructure";
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        return site.get().getId();
    }

    protected Optional<Site> findFirstSite() {
        try {
            Sites sites = (Sites) nsxService.apply(Sites.class);
            List<Site> siteList = sites.list(null, false, null, 1L, null, null)
                    .getResults();
            return Optional.ofNullable(siteList)
                    .filter(Predicate.not(List::isEmpty))
                    .map(l -> l.get(0));
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch sites list due to %s", e.getMessage()));
        }
    }

    public String getDefaultEnforcementPointPath(String siteId) {
        Optional<EnforcementPoint> ep = findFirstEnforcementPoint(siteId);
        if (ep.isEmpty()) {
            String errorMsg = String.format("No enforcement points are found in the linked NSX infrastructure for site ID %s", siteId);
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        return ep.get().getPath();
    }

    protected Optional<EnforcementPoint> findFirstEnforcementPoint(String siteId) {
        try {
            EnforcementPoints enforcementPoints = (EnforcementPoints) nsxService.apply(EnforcementPoints.class);
            EnforcementPointListResult result = enforcementPoints.list(siteId, null, false, null, 1L, null, null);
            return Optional.ofNullable(result.getResults())
                    .filter(Predicate.not(List::isEmpty))
                    .map(l -> l.get(0));
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch enforcement points due to %s", e.getMessage()));
        }
    }

    public TransportZoneListResult getTransportZones() {
        try {
            com.vmware.nsx.TransportZones transportZones = (com.vmware.nsx.TransportZones) nsxService.apply(com.vmware.nsx.TransportZones.class);
            return PagedFetcher.<TransportZoneListResult, TransportZone>withPageFetcher(
                    cursor -> transportZones.list(cursor, null, true, null, null, null, null, null, TransportType.OVERLAY.name(), null)
                    ).cursorExtractor(TransportZoneListResult::getCursor)
                    .itemsExtractor(TransportZoneListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll();
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
            logger.debug("Removing the DHCP relay config with ID {}", dhcpRelayConfigId);
            dhcpRelayConfig.delete(dhcpRelayConfigId);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Error deleting segment %s: %s", segmentName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    protected void removeSegment(String segmentName, long zoneId) {
        logger.debug("Removing the segment with ID {}", segmentName);
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
            logger.debug("Removing the segment with ID {}", segmentName);
            removeGroupForSegment(segmentName);
            segmentService.delete(segmentName);
        } else {
            String msg = String.format("Cannot remove the NSX segment %s because there are still %s port group(s) attached to it", segmentName, portCount);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private PolicyGroupMembersListResult getSegmentPortList(SegmentPorts segmentPortsService, String segmentName, String enforcementPointPath) {
        return PagedFetcher.
                <PolicyGroupMembersListResult, PolicyGroupMemberDetails>withPageFetcher(
                cursor -> segmentPortsService.list(DEFAULT_DOMAIN, segmentName, cursor, enforcementPointPath,
                        false, null, 50L, false, null)
                )
                .cursorExtractor(PolicyGroupMembersListResult::getCursor)
                .itemsExtractor(PolicyGroupMembersListResult::getResults)
                .itemsSetter((page, allItems) -> {
                    page.setResults(allItems);
                    page.setResultCount((long) allItems.size());
                })
                .fetchAll();
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

            logger.debug("Creating NSX static NAT rule {} for tier-1 gateway {} (VPC: {})", ruleName, tier1GatewayName, vpcName);
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
            logger.debug("Cannot find NAT rule with name {}: {}, skipping deletion", ruleName, error.getMessage());
        }

        if (service == Network.Service.PortForwarding) {
            deletePortForwardingNatRuleService(ruleName, privatePort, protocol);
        }
    }

    public void createPortForwardingRule(String ruleName, String tier1GatewayName, String networkName, String publicIp,
                                         String vmIp, String publicPort, String service) {
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            logger.debug("Creating NSX Port-Forwarding NAT {} for network {}", ruleName, networkName);
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
            List<LBPoolMember> members = getLbPoolMembers(memberList, tier1GatewayName);
            LbPools lbPools = (LbPools) nsxService.apply(LbPools.class);
            Optional<LBPool> nsxLbServerPool = getNsxLbServerPool(lbPools, lbServerPoolName);
            // Skip if pool exists and members unchanged
            if (nsxLbServerPool.isPresent()) {
                List<LBPoolMember> existingMembers = nsxLbServerPool
                        .map(LBPool::getMembers)
                        .orElseGet(List::of);
                if (hasSamePoolMembers(existingMembers, members)) {
                    logger.debug("Skipping patch for LB pool {} on Tier-1 {}: members unchanged", lbServerPoolName, tier1GatewayName);
                    return;
                }
            }
            String activeMonitorPath = getLbActiveMonitorPath(lbServerPoolName, privatePort, protocol);
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

    private Optional<LBPool> getNsxLbServerPool(LbPools lbPools, String lbServerPoolName) {
        try {
            return Optional.ofNullable(lbPools.get(lbServerPoolName));
        } catch (NotFound e) {
            logger.warn("Server Pool not found: {}", lbServerPoolName);
            return Optional.empty();
        }
    }

    private boolean hasSamePoolMembers(List<LBPoolMember> existingMembers, List<LBPoolMember> membersUpdate) {
        Set<String> existingMembersSet = existingMembers.stream()
                .map(this::buildPoolMemberKey)
                .collect(toSet());
        Set<String> updateMembersSet = membersUpdate.stream()
                .map(this::buildPoolMemberKey)
                .collect(toSet());

        return existingMembersSet.size() == updateMembersSet.size()
               && existingMembersSet.containsAll(updateMembersSet);
    }

    private String buildPoolMemberKey(LBPoolMember member) {
        return member.getIpAddress() + ':' + member.getPort() + ':' + member.getDisplayName();
    }

    private String getLbActiveMonitorPath(String lbServerPoolName, String port, String protocol) {
        LbMonitorProfiles lbActiveMonitor = (LbMonitorProfiles) nsxService.apply(LbMonitorProfiles.class);
        String lbMonitorProfileId = getActiveMonitorProfileName(lbServerPoolName, port, protocol);
        Optional<Structure> monitorProfile = getMonitorProfile(lbActiveMonitor, lbMonitorProfileId);
        if (monitorProfile.isEmpty()) {
            patchMonitoringProfile(port, protocol, lbMonitorProfileId, lbActiveMonitor);
            monitorProfile = getMonitorProfile(lbActiveMonitor, lbMonitorProfileId);
        }
        return monitorProfile.map(structure -> structure._getDataValue().getField("path").toString()).orElse(null);
    }

    private Optional<Structure> getMonitorProfile(LbMonitorProfiles lbActiveMonitor, String lbMonitorProfileId) {
        try {
            return Optional.ofNullable(lbActiveMonitor.get(lbMonitorProfileId));
        } catch (NotFound e) {
            logger.warn("LB Monitor Profile not found: {}", lbMonitorProfileId);
            return Optional.empty();
        }
    }

    private void patchMonitoringProfile(String port, String protocol, String lbMonitorProfileId, LbMonitorProfiles lbActiveMonitor) {
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
    }

    LBMonitorProfileListResult listLBActiveMonitors(LbMonitorProfiles lbActiveMonitor) {
        return PagedFetcher.<LBMonitorProfileListResult, Structure>withPageFetcher(
                cursor -> lbActiveMonitor.list(cursor, false, null, null, null, null)
                ).cursorExtractor(LBMonitorProfileListResult::getCursor)
                .itemsExtractor(LBMonitorProfileListResult::getResults)
                .itemsSetter((page, allItems) -> {
                    page.setResults(allItems);
                    page.setResultCount((long) allItems.size());
                })
                .fetchAll();
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
            if (Objects.nonNull(getLbVirtualServerService(lbVirtualServers, lbVirtualServerName))) {
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
            logger.debug("Found an LB virtual server named: {} on NSX", lbVSName);
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
            LBAppProfileListResult lbAppProfileListResults = PagedFetcher.<LBAppProfileListResult, Structure>withPageFetcher(
                            cursor -> lbAppProfiles.list(cursor, null, null, null, null, null)
                    ).cursorExtractor(LBAppProfileListResult::getCursor)
                    .itemsExtractor(LBAppProfileListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll();
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
            ServiceListResult serviceList = PagedFetcher.<ServiceListResult, com.vmware.nsx_policy.model.Service>withPageFetcher(
                            cursor -> service.list(cursor, true, false, null, null, null, null)
                    ).cursorExtractor(ServiceListResult::getCursor)
                    .itemsExtractor(ServiceListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll();

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
        for(NsxNetworkRule nsxRule : nsxRules) {
            SDNProviderNetworkRule rule = nsxRule.getBaseRule();
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
        for (NsxNetworkRule nsxRule : nsxRules) {
            SDNProviderNetworkRule rule = nsxRule.getBaseRule();
            String ruleId = NsxControllerUtils.getNsxDistributedFirewallPolicyRuleId(segmentName, rule.getRuleId());
            Rule ruleToAdd = new Rule.Builder()
                    .setAction(nsxRule.getAclAction().toString())
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

    private List<String> getServicesListForDistributedFirewallRule(SDNProviderNetworkRule rule, String segmentName) {
        List<String> services = List.of("ANY");
        if (!rule.getProtocol().equalsIgnoreCase("all")) {
            String ruleName = String.format("%s-R%s", segmentName, rule.getRuleId());
            String serviceName = getNsxInfraServices(ruleName, rule.getPrivatePort(), rule.getProtocol(),
                    rule.getIcmpType(), rule.getIcmpCode());
            services = List.of(serviceName);
        }
        return services;
    }

    protected List<String> getGroupsForTraffic(SDNProviderNetworkRule rule,
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
            GroupListResult result = PagedFetcher.<GroupListResult, Group>withPageFetcher(
                            cursor -> groups.list(DEFAULT_DOMAIN, cursor, false, null, null, null, null, null)
                    ).cursorExtractor(GroupListResult::getCursor)
                    .itemsExtractor(GroupListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll();
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

    public void createVpnService(String tier1GatewayName, String localEndpointIp) {
        String vpnServiceName = getVpnServiceName(tier1GatewayName);
        String localEndpointName = getVpnLocalEndpointName(vpnServiceName);
        try {
            ensureTier1IpsecLocalEndpointAdvertisement(tier1GatewayName);
            IpsecVpnServices vpnServices = (IpsecVpnServices) nsxService.apply(IpsecVpnServices.class);
            IPSecVpnService vpnService = new IPSecVpnService.Builder()
                    .setId(vpnServiceName)
                    .setDisplayName(vpnServiceName)
                    .setEnabled(true)
                    .build();
            vpnServices.patch(tier1GatewayName, vpnServiceName, vpnService);

            LocalEndpoints localEndpoints = (LocalEndpoints) nsxService.apply(LocalEndpoints.class);
            IPSecVpnLocalEndpoint localEndpoint = new IPSecVpnLocalEndpoint.Builder()
                    .setId(localEndpointName)
                    .setDisplayName(localEndpointName)
                    .setLocalAddress(localEndpointIp)
                    .setLocalId(localEndpointIp)
                    .build();
            localEndpoints.patch(tier1GatewayName, vpnServiceName, localEndpointName, localEndpoint);

            ensureVpnNatExemptions(tier1GatewayName, localEndpointIp);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX IPSec VPN service %s on tier-1 gateway %s, due to: %s",
                    vpnServiceName, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    /**
     * Returns whether the CloudStack-owned VPN service already exists. The resource uses this
     * preflight result to avoid deleting a valid service when an idempotent create request fails
     * after an ambiguous timeout.
     */
    public boolean isVpnServicePresent(String tier1GatewayName) {
        try {
            IpsecVpnServices vpnServices = (IpsecVpnServices) nsxService.apply(IpsecVpnServices.class);
            return vpnServices.get(tier1GatewayName, getVpnServiceName(tier1GatewayName)) != null;
        } catch (NotFound e) {
            return false;
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format("Failed to check NSX IPSec VPN service on tier-1 gateway %s, due to: %s",
                    tier1GatewayName, ae.getErrorMessage()), error);
        }
    }

    /**
     * The local endpoint IP realizes as a Tier-1 loopback and is routable only when the Tier-1
     * advertises TIER1_IPSEC_LOCAL_ENDPOINT; gateways created by this plugin always do, but
     * gateways created before that behavior are patched here
     */
    private void ensureTier1IpsecLocalEndpointAdvertisement(String tier1GatewayName) {
        Tier1 tier1 = getTier1Gateway(tier1GatewayName);
        if (tier1 == null) {
            throw new CloudRuntimeException(String.format("The Tier 1 Gateway %s does not exist", tier1GatewayName));
        }
        List<String> advertisementTypes = tier1.getRouteAdvertisementTypes();
        if (CollectionUtils.isNotEmpty(advertisementTypes)
                && advertisementTypes.contains(RouteAdvertisementType.TIER1_IPSEC_LOCAL_ENDPOINT.name())) {
            return;
        }
        List<String> updatedTypes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(advertisementTypes)) {
            updatedTypes.addAll(advertisementTypes);
        }
        updatedTypes.add(RouteAdvertisementType.TIER1_IPSEC_LOCAL_ENDPOINT.name());
        Tier1s tier1service = (Tier1s) nsxService.apply(Tier1s.class);
        Tier1 tier1Update = new Tier1.Builder()
                .setRouteAdvertisementTypes(updatedTypes)
                .build();
        tier1service.patch(tier1GatewayName, tier1Update);
    }

    public void deleteVpnService(String tier1GatewayName) {
        if (getTier1Gateway(tier1GatewayName) == null) {
            // On VPC teardown the tier-1 gateway is removed (along with its VPN objects) before the
            // VPN gateway cleanup runs
            logger.debug("The Tier 1 Gateway {} does not exist, skipping the removal of its VPN service", tier1GatewayName);
            return;
        }
        removeTier1VpnResources(tier1GatewayName);
        restoreSourceNatRuleSequence(tier1GatewayName);
    }

    public VpnSessionProvisioningResult createRouteBasedVpnSession(String tier1GatewayName, String connectionUuid,
                                                                   String peerAddress, String psk, String ikePolicy,
                                                                   String espPolicy, Long ikeLifetime, Long espLifetime,
                                                                   boolean dpdEnabled, String ikeVersion, boolean passive,
                                                                   String vtiLocalIp, int vtiPrefixLength) {
        return retryWhileMarkedForDeletion(connectionUuid, () -> doCreateRouteBasedVpnSession(tier1GatewayName,
                connectionUuid, peerAddress, psk, ikePolicy, espPolicy, ikeLifetime, espLifetime, dpdEnabled,
                ikeVersion, passive, vtiLocalIp, vtiPrefixLength));
    }

    /**
     * NSX reaps deleted policy objects asynchronously and rejects a create under a path that is still
     * marked for deletion. All VPN objects use deterministic IDs and PATCH/upsert semantics, so a
     * retry of the complete idempotent operation is safe and preserves already-existing objects.
     */
    private <T> T retryWhileMarkedForDeletion(String connectionUuid, Supplier<T> operation) {
        CloudRuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= VPN_MARKED_FOR_DELETION_RETRIES; attempt++) {
            try {
                return operation.get();
            } catch (CloudRuntimeException e) {
                lastFailure = e;
                if (attempt == VPN_MARKED_FOR_DELETION_RETRIES || !isMarkedForDeletionError(e)) {
                    throw e;
                }
                logger.info("A VPN object for connection {} is still being purged by NSX, retrying in {}s (attempt {}/{})",
                        connectionUuid, VPN_MARKED_FOR_DELETION_RETRY_INTERVAL_SECS, attempt, VPN_MARKED_FOR_DELETION_RETRIES);
                try {
                    Thread.sleep(VPN_MARKED_FOR_DELETION_RETRY_INTERVAL_SECS * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastFailure;
    }

    private boolean isMarkedForDeletionError(CloudRuntimeException e) {
        return e.getMessage() != null && e.getMessage().contains("marked for deletion");
    }

    private VpnSessionProvisioningResult doCreateRouteBasedVpnSession(String tier1GatewayName, String connectionUuid,
                                                                      String peerAddress, String psk, String ikePolicy,
                                                                      String espPolicy, Long ikeLifetime, Long espLifetime,
                                                                      boolean dpdEnabled, String ikeVersion, boolean passive,
                                                                      String vtiLocalIp, int vtiPrefixLength) {
        String vpnServiceName = getVpnServiceName(tier1GatewayName);
        String localEndpointName = getVpnLocalEndpointName(vpnServiceName);
        String sessionName = getVpnSessionName(connectionUuid);
        String ikeProfileName = getVpnIkeProfileName(connectionUuid);
        String espProfileName = getVpnEspProfileName(connectionUuid);
        String dpdProfileName = getVpnDpdProfileName(connectionUuid);
        try {
            IpsecVpnIkeProfiles ikeProfiles = (IpsecVpnIkeProfiles) nsxService.apply(IpsecVpnIkeProfiles.class);
            IpsecVpnTunnelProfiles espProfiles = (IpsecVpnTunnelProfiles) nsxService.apply(IpsecVpnTunnelProfiles.class);
            IpsecVpnDpdProfiles dpdProfiles = (IpsecVpnDpdProfiles) nsxService.apply(IpsecVpnDpdProfiles.class);
            Sessions sessions = (Sessions) nsxService.apply(Sessions.class);
            boolean sessionExisted = isVpnSessionPresent(sessions, tier1GatewayName, vpnServiceName, sessionName);
            if (sessionExisted) {
                // Disable the existing session before replacing any referenced profiles so a failed
                // reconciliation cannot leave an active tunnel using a partially updated policy.
                updateVpnConnectionState(tier1GatewayName, connectionUuid, false);
            }
            boolean ikeProfileExisted = isVpnIkeProfilePresent(ikeProfiles, ikeProfileName);
            boolean espProfileExisted = isVpnTunnelProfilePresent(espProfiles, espProfileName);
            boolean dpdProfileExisted = isVpnDpdProfilePresent(dpdProfiles, dpdProfileName);

            try {
                IPSecVpnIkeProfile ikeProfile = new IPSecVpnIkeProfile.Builder()
                    .setId(ikeProfileName)
                    .setDisplayName(ikeProfileName)
                    .setEncryptionAlgorithms(NsxVpnCryptoUtils.getEncryptionAlgorithms(ikePolicy))
                    .setDigestAlgorithms(NsxVpnCryptoUtils.getDigestAlgorithms(ikePolicy))
                    .setDhGroups(NsxVpnCryptoUtils.getDhGroups(ikePolicy))
                    .setIkeVersion(NsxVpnCryptoUtils.getIkeVersion(ikeVersion))
                    .setSaLifeTime(ikeLifetime)
                    .build();
            ikeProfiles.patch(ikeProfileName, ikeProfile);

            List<String> espDhGroups = NsxVpnCryptoUtils.getDhGroups(espPolicy);
            IPSecVpnTunnelProfile.Builder espProfileBuilder = new IPSecVpnTunnelProfile.Builder()
                    .setId(espProfileName)
                    .setDisplayName(espProfileName)
                    .setEncryptionAlgorithms(NsxVpnCryptoUtils.getEncryptionAlgorithms(espPolicy))
                    .setDigestAlgorithms(NsxVpnCryptoUtils.getDigestAlgorithms(espPolicy))
                    .setEnablePerfectForwardSecrecy(!espDhGroups.isEmpty())
                    .setSaLifeTime(espLifetime);
            if (!espDhGroups.isEmpty()) {
                espProfileBuilder.setDhGroups(espDhGroups);
            }
            espProfiles.patch(espProfileName, espProfileBuilder.build());

            // On demand probing only checks the peer when there is traffic to send and nothing has been
            // heard back, so an idle tunnel is not torn down for want of a probe response the way the
            // periodic default does; CloudStack only exposes DPD as a flag, hence the fixed timers
            IPSecVpnDpdProfile dpdProfile = new IPSecVpnDpdProfile.Builder()
                    .setId(dpdProfileName)
                    .setDisplayName(dpdProfileName)
                    .setEnabled(dpdEnabled)
                    .setDpdProbeMode(IPSecVpnDpdProfile.DPD_PROBE_MODE_ON_DEMAND)
                    .setDpdProbeInterval(VPN_DPD_PROBE_INTERVAL_SECS)
                    .setRetryCount(VPN_DPD_RETRY_COUNT)
                    .build();
            dpdProfiles.patch(dpdProfileName, dpdProfile);

            IPSecVpnTunnelInterface tunnelInterface = new IPSecVpnTunnelInterface.Builder()
                    .setId(VPN_DEFAULT_TUNNEL_INTERFACE_NAME)
                    .setDisplayName(VPN_DEFAULT_TUNNEL_INTERFACE_NAME)
                    .setIpSubnets(List.of(new TunnelInterfaceIPSubnet.Builder()
                            .setIpAddresses(List.of(vtiLocalIp))
                            .setPrefixLength((long) vtiPrefixLength)
                            .build()))
                    .build();
            RouteBasedIPSecVpnSession session = new RouteBasedIPSecVpnSession.Builder()
                    .setId(sessionName)
                    .setDisplayName(sessionName)
                    .setEnabled(false)
                    .setAuthenticationMode(IPSecVpnSession.AUTHENTICATION_MODE_PSK)
                    .setPsk(psk)
                    .setPeerAddress(peerAddress)
                    .setPeerId(peerAddress)
                    .setConnectionInitiationMode(passive ? IPSecVpnSession.CONNECTION_INITIATION_MODE_RESPOND_ONLY
                            : IPSecVpnSession.CONNECTION_INITIATION_MODE_INITIATOR)
                    .setIkeProfilePath(IPSEC_VPN_IKE_PROFILES_PATH_PREFIX + ikeProfileName)
                    .setTunnelProfilePath(IPSEC_VPN_TUNNEL_PROFILES_PATH_PREFIX + espProfileName)
                    .setDpdProfilePath(IPSEC_VPN_DPD_PROFILES_PATH_PREFIX + dpdProfileName)
                    .setLocalEndpointPath(getVpnLocalEndpointPath(tier1GatewayName, vpnServiceName, localEndpointName))
                    .setTunnelInterfaces(List.of(tunnelInterface))
                    .build();
                sessions.patch(tier1GatewayName, vpnServiceName, sessionName, session);
                return sessionExisted ? VpnSessionProvisioningResult.PREEXISTING
                        : VpnSessionProvisioningResult.CREATED;
            } catch (RuntimeException e) {
                if (!sessionExisted) {
                    boolean sessionRemoved = deleteVpnSessionAfterCreateFailure(sessions, tier1GatewayName,
                            vpnServiceName, sessionName);
                    if (sessionRemoved) {
                        deleteNewVpnProfilesAfterCreateFailure(ikeProfiles, espProfiles, dpdProfiles,
                                connectionUuid, ikeProfileExisted, espProfileExisted, dpdProfileExisted);
                    }
                }
                throw e;
            }
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to create NSX IPSec VPN session %s on tier-1 gateway %s, due to: %s",
                    sessionName, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private boolean isVpnSessionPresent(Sessions sessions, String tier1GatewayName, String vpnServiceName,
                                        String sessionName) {
        try {
            return sessions.get(tier1GatewayName, vpnServiceName, sessionName) != null;
        } catch (NotFound e) {
            return false;
        }
    }

    private boolean isVpnIkeProfilePresent(IpsecVpnIkeProfiles profiles, String profileName) {
        try {
            return profiles.get(profileName) != null;
        } catch (NotFound e) {
            return false;
        }
    }

    private boolean isVpnTunnelProfilePresent(IpsecVpnTunnelProfiles profiles, String profileName) {
        try {
            return profiles.get(profileName) != null;
        } catch (NotFound e) {
            return false;
        }
    }

    private boolean isVpnDpdProfilePresent(IpsecVpnDpdProfiles profiles, String profileName) {
        try {
            return profiles.get(profileName) != null;
        } catch (NotFound e) {
            return false;
        }
    }

    private boolean deleteVpnSessionAfterCreateFailure(Sessions sessions, String tier1GatewayName,
                                                        String vpnServiceName, String sessionName) {
        try {
            sessions.delete(tier1GatewayName, vpnServiceName, sessionName);
            return true;
        } catch (NotFound e) {
            logger.debug("The partially created VPN session {} on tier-1 gateway {} was not present during cleanup",
                    sessionName, tier1GatewayName);
            return true;
        } catch (Error e) {
            logger.warn("Failed to remove the partially created VPN session {} on tier-1 gateway {} after creation failed: {}",
                    sessionName, tier1GatewayName, e.getMessage());
            return false;
        } catch (RuntimeException e) {
            logger.warn("Failed to remove the partially created VPN session {} on tier-1 gateway {} after creation failed: {}",
                    sessionName, tier1GatewayName, e.getMessage());
            return false;
        }
    }

    private void deleteNewVpnProfilesAfterCreateFailure(IpsecVpnIkeProfiles ikeProfiles,
                                                         IpsecVpnTunnelProfiles espProfiles,
                                                         IpsecVpnDpdProfiles dpdProfiles,
                                                         String connectionUuid,
                                                         boolean ikeProfileExisted,
                                                         boolean espProfileExisted,
                                                         boolean dpdProfileExisted) {
        if (!ikeProfileExisted) {
            deleteVpnProfileAfterCreateFailure(() -> ikeProfiles.delete(getVpnIkeProfileName(connectionUuid)),
                    "IKE", connectionUuid);
        }
        if (!espProfileExisted) {
            deleteVpnProfileAfterCreateFailure(() -> espProfiles.delete(getVpnEspProfileName(connectionUuid)),
                    "tunnel", connectionUuid);
        }
        if (!dpdProfileExisted) {
            deleteVpnProfileAfterCreateFailure(() -> dpdProfiles.delete(getVpnDpdProfileName(connectionUuid)),
                    "DPD", connectionUuid);
        }
    }

    private void deleteVpnProfileAfterCreateFailure(Runnable deleteAction, String profileType,
                                                     String connectionUuid) {
        try {
            deleteAction.run();
        } catch (NotFound e) {
            logger.debug("The partially created {} profile of VPN connection {} was absent during cleanup",
                    profileType, connectionUuid);
        } catch (RuntimeException e) {
            logger.warn("Failed to remove the partially created {} profile of VPN connection {}: {}",
                    profileType, connectionUuid, e.getMessage());
        }
    }

    public void addVpnConnectionRoutes(String tier1GatewayName, String connectionUuid, List<String> peerCidrs,
                                       String vtiPeerIp, String vpcCidr) {
        retryWhileMarkedForDeletion(connectionUuid, () -> {
            doAddVpnConnectionRoutes(tier1GatewayName, connectionUuid, peerCidrs, vtiPeerIp, vpcCidr);
            return null;
        });
    }

    private void doAddVpnConnectionRoutes(String tier1GatewayName, String connectionUuid, List<String> peerCidrs,
                                          String vtiPeerIp, String vpcCidr) {
        try {
            com.vmware.nsx_policy.infra.tier_1s.StaticRoutes staticRoutesService =
                    (com.vmware.nsx_policy.infra.tier_1s.StaticRoutes) nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.StaticRoutes.class);
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            Set<String> desiredRouteIds = new HashSet<>();
            Set<String> desiredNoSnatRuleIds = new HashSet<>();
            for (int i = 0; i < peerCidrs.size(); i++) {
                String peerCidr = peerCidrs.get(i);
                String routeName = getVpnStaticRouteName(connectionUuid, i);
                desiredRouteIds.add(routeName);
                com.vmware.nsx_policy.model.StaticRoutes staticRoute = new com.vmware.nsx_policy.model.StaticRoutes.Builder()
                        .setId(routeName)
                        .setDisplayName(routeName)
                        .setNetwork(peerCidr)
                        .setNextHops(List.of(new RouterNexthop.Builder().setIpAddress(vtiPeerIp).build()))
                        .build();
                staticRoutesService.patch(tier1GatewayName, routeName, staticRoute);

                // Route-based VPN does not bypass NAT: without a NO_SNAT rule the tier-1 match-any
                // SNAT would rewrite VPC-to-remote traffic before it enters the tunnel
                String noSnatRuleName = getVpnNoSnatRuleName(connectionUuid, i);
                desiredNoSnatRuleIds.add(noSnatRuleName);
                PolicyNatRule noSnatRule = new PolicyNatRule.Builder()
                        .setId(noSnatRuleName)
                        .setDisplayName(noSnatRuleName)
                        .setAction(NatAction.NO_SNAT.name())
                        .setSourceNetwork(vpcCidr)
                        .setDestinationNetwork(peerCidr)
                        .setSequenceNumber(VPN_NO_SNAT_SEQUENCE_NUMBER)
                        .setEnabled(true)
                        .build();
                natService.patch(tier1GatewayName, NatId.USER.name(), noSnatRuleName, noSnatRule);
            }
            // Keep existing routes and exemptions in place until every desired object has been
            // accepted. This makes an idempotent retry non-destructive if an NSX PATCH fails.
            deleteVpnStaticRoutesByPrefix(tier1GatewayName, getVpnStaticRouteNamePrefix(connectionUuid),
                    desiredRouteIds);
            deleteVpnNoSnatRulesByPrefix(tier1GatewayName, getVpnNoSnatRuleNamePrefix(connectionUuid),
                    desiredNoSnatRuleIds);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to add the routes for NSX IPSec VPN connection %s on tier-1 gateway %s, due to: %s",
                    connectionUuid, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void deleteVpnConnection(String tier1GatewayName, String connectionUuid) {
        RuntimeException failure = null;
        // Delete by prefix instead of recomputing names from the current peer CIDR list: the
        // customer gateway's CIDRs may have changed since the routes and NO_SNAT rules were created
        failure = runVpnCleanupStep(failure, "static routes", connectionUuid,
                () -> deleteVpnStaticRoutesByPrefix(tier1GatewayName, getVpnStaticRouteNamePrefix(connectionUuid)));
        failure = runVpnCleanupStep(failure, "NO_SNAT rules", connectionUuid,
                () -> deleteVpnNoSnatRulesByPrefix(tier1GatewayName, getVpnNoSnatRuleNamePrefix(connectionUuid)));
        failure = runVpnCleanupStep(failure, "session", connectionUuid,
                () -> deleteVpnSession(tier1GatewayName, connectionUuid));
        failure = runVpnCleanupStep(failure, "profiles", connectionUuid,
                () -> deleteVpnSessionProfiles(connectionUuid));
        throwVpnCleanupFailure(failure, connectionUuid);
    }

    private void deleteVpnSession(String tier1GatewayName, String connectionUuid) {
        String vpnServiceName = getVpnServiceName(tier1GatewayName);
        String sessionName = getVpnSessionName(connectionUuid);
        try {
            Sessions sessions = (Sessions) nsxService.apply(Sessions.class);
            sessions.delete(tier1GatewayName, vpnServiceName, sessionName);
        } catch (NotFound e) {
            logger.debug("The VPN session {} on tier-1 gateway {} no longer exists, skipping deletion",
                    sessionName, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete the NSX IPSec VPN session %s on tier-1 gateway %s, due to: %s",
                    sessionName, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public void updateVpnConnectionState(String tier1GatewayName, String connectionUuid, boolean enabled) {
        String vpnServiceName = getVpnServiceName(tier1GatewayName);
        String sessionName = getVpnSessionName(connectionUuid);
        try {
            Sessions sessions = (Sessions) nsxService.apply(Sessions.class);
            Structure current = sessions.showsensitivedata(tier1GatewayName, vpnServiceName, sessionName);
            if (current == null) {
                throw new CloudRuntimeException(String.format(
                        "NSX returned no data for IPSec VPN session %s on tier-1 gateway %s",
                        sessionName, tier1GatewayName));
            }
            if (!current._hasTypeNameOf(RouteBasedIPSecVpnSession.class)) {
                throw new CloudRuntimeException(String.format(
                        "IPSec VPN session %s on tier-1 gateway %s is not route-based",
                        sessionName, tier1GatewayName));
            }
            RouteBasedIPSecVpnSession update = current._convertTo(RouteBasedIPSecVpnSession.class);
            if (update.getRevision() == null) {
                throw new CloudRuntimeException(String.format(
                        "NSX returned no revision for IPSec VPN session %s on tier-1 gateway %s",
                        sessionName, tier1GatewayName));
            }
            if (IPSecVpnSession.AUTHENTICATION_MODE_PSK.equals(update.getAuthenticationMode())
                    && update.getPsk() == null) {
                throw new CloudRuntimeException(String.format(
                        "NSX did not return sensitive authentication data for IPSec VPN session %s on tier-1 gateway %s",
                        sessionName, tier1GatewayName));
            }
            update.setEnabled(enabled);
            sessions.update(tier1GatewayName, vpnServiceName, sessionName, update);
        } catch (NotFound e) {
            if (enabled) {
                throw new CloudRuntimeException(String.format(
                        "Cannot enable NSX IPSec VPN session %s on tier-1 gateway %s because it does not exist",
                        sessionName, tier1GatewayName), e);
            }
            logger.debug("The VPN session {} no longer exists on tier-1 gateway {}, skipping state update",
                    sessionName, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format(
                    "Failed to update the state of NSX IPSec VPN session %s on tier-1 gateway %s, due to: %s",
                    sessionName, tier1GatewayName, ae.getErrorMessage()), error);
        }
        if (!enabled) {
            deleteVpnStaticRoutesByPrefix(tier1GatewayName, getVpnStaticRouteNamePrefix(connectionUuid));
            deleteVpnNoSnatRulesByPrefix(tier1GatewayName, getVpnNoSnatRuleNamePrefix(connectionUuid));
        }
    }

    /**
     * Removes every object created for a connection when route or NAT programming fails after the
     * session itself was created. This is also used by the permanent connection-delete path.
     */
    public void rollbackVpnConnection(String tier1GatewayName, String connectionUuid) {
        deleteVpnConnection(tier1GatewayName, connectionUuid);
    }

    private void deleteVpnStaticRoutesByPrefix(String tier1GatewayName, String routeNamePrefix) {
        deleteVpnStaticRoutesByPrefix(tier1GatewayName, routeNamePrefix, Set.of());
    }

    private void deleteVpnStaticRoutesByPrefix(String tier1GatewayName, String routeNamePrefix,
                                               Set<String> retainedRouteIds) {
        com.vmware.nsx_policy.infra.tier_1s.StaticRoutes staticRoutesService =
                (com.vmware.nsx_policy.infra.tier_1s.StaticRoutes) nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.StaticRoutes.class);
        try {
            List<com.vmware.nsx_policy.model.StaticRoutes> staticRoutes =
                    PagedFetcher.<StaticRoutesListResult, com.vmware.nsx_policy.model.StaticRoutes>withPageFetcher(
                            cursor -> staticRoutesService.list(tier1GatewayName, cursor, false, null, null, null, null)
                    ).cursorExtractor(StaticRoutesListResult::getCursor)
                    .itemsExtractor(StaticRoutesListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll()
                    .getResults();
            if (CollectionUtils.isEmpty(staticRoutes)) {
                return;
            }
            RuntimeException failure = null;
            for (com.vmware.nsx_policy.model.StaticRoutes staticRoute : staticRoutes) {
                if (staticRoute.getId() != null && staticRoute.getId().startsWith(routeNamePrefix)
                        && !retainedRouteIds.contains(staticRoute.getId())) {
                    logger.debug("Removing the VPN static route {} from tier-1 gateway {}", staticRoute.getId(), tier1GatewayName);
                    String routeId = staticRoute.getId();
                    failure = runVpnCleanupStep(failure, String.format("static route %s", routeId), routeNamePrefix,
                            () -> deleteVpnStaticRoute(staticRoutesService, tier1GatewayName, routeId));
                }
            }
            throwVpnPrefixCleanupFailure(failure, "static routes", routeNamePrefix, tier1GatewayName);
        } catch (NotFound e) {
            logger.debug("No static routes matching the prefix {} are left on tier-1 gateway {}, skipping deletion",
                    routeNamePrefix, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete the VPN static routes matching the prefix %s on tier-1 gateway %s, due to: %s",
                    routeNamePrefix, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private void deleteVpnStaticRoute(com.vmware.nsx_policy.infra.tier_1s.StaticRoutes staticRoutesService,
                                      String tier1GatewayName, String routeId) {
        try {
            staticRoutesService.delete(tier1GatewayName, routeId);
        } catch (NotFound e) {
            logger.debug("The VPN static route {} on tier-1 gateway {} no longer exists, skipping deletion",
                    routeId, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format(
                    "Failed to delete the VPN static route %s on tier-1 gateway %s, due to: %s",
                    routeId, tier1GatewayName, ae.getErrorMessage()), error);
        }
    }

    private void deleteVpnNoSnatRulesByPrefix(String tier1GatewayName, String ruleNamePrefix) {
        deleteVpnNoSnatRulesByPrefix(tier1GatewayName, ruleNamePrefix, Set.of());
    }

    private void deleteVpnNoSnatRulesByPrefix(String tier1GatewayName, String ruleNamePrefix,
                                              Set<String> retainedRuleIds) {
        NatRules natService = (NatRules) nsxService.apply(NatRules.class);
        try {
            List<PolicyNatRule> natRules = PagedFetcher.<PolicyNatRuleListResult, PolicyNatRule>withPageFetcher(
                    cursor -> natService.list(tier1GatewayName, NatId.USER.name(), cursor, false, null, null, null, null)
                    ).cursorExtractor(PolicyNatRuleListResult::getCursor)
                    .itemsExtractor(PolicyNatRuleListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll()
                    .getResults();
            if (CollectionUtils.isEmpty(natRules)) {
                return;
            }
            RuntimeException failure = null;
            for (PolicyNatRule natRule : natRules) {
                if (natRule.getId() != null && natRule.getId().startsWith(ruleNamePrefix)
                        && !retainedRuleIds.contains(natRule.getId())) {
                    logger.debug("Removing the VPN NO_SNAT rule {} from tier-1 gateway {}", natRule.getId(), tier1GatewayName);
                    String ruleId = natRule.getId();
                    failure = runVpnCleanupStep(failure, String.format("NO_SNAT rule %s", ruleId), ruleNamePrefix,
                            () -> deleteVpnNoSnatRule(natService, tier1GatewayName, ruleId));
                }
            }
            throwVpnPrefixCleanupFailure(failure, "NO_SNAT rules", ruleNamePrefix, tier1GatewayName);
        } catch (NotFound e) {
            logger.debug("No NO_SNAT rules matching the prefix {} are left on tier-1 gateway {}, skipping deletion",
                    ruleNamePrefix, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete the VPN NO_SNAT rules matching the prefix %s on tier-1 gateway %s, due to: %s",
                    ruleNamePrefix, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private void deleteVpnNoSnatRule(NatRules natService, String tier1GatewayName, String ruleId) {
        try {
            natService.delete(tier1GatewayName, NatId.USER.name(), ruleId);
        } catch (NotFound e) {
            logger.debug("The VPN NO_SNAT rule {} on tier-1 gateway {} no longer exists, skipping deletion",
                    ruleId, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format(
                    "Failed to delete the VPN NO_SNAT rule %s on tier-1 gateway %s, due to: %s",
                    ruleId, tier1GatewayName, ae.getErrorMessage()), error);
        }
    }

    private void throwVpnPrefixCleanupFailure(RuntimeException failure, String resource, String prefix,
                                              String tier1GatewayName) {
        if (failure != null) {
            throw new CloudRuntimeException(String.format(
                    "Failed to remove all NSX VPN %s matching prefix %s on tier-1 gateway %s",
                    resource, prefix, tier1GatewayName), failure);
        }
    }

    private void deleteVpnSessionProfiles(String connectionUuid) {
        RuntimeException failure = null;
        failure = runVpnCleanupStep(failure, "IKE profile", connectionUuid,
                () -> deleteVpnIkeProfile(connectionUuid));
        failure = runVpnCleanupStep(failure, "tunnel profile", connectionUuid,
                () -> deleteVpnTunnelProfile(connectionUuid));
        failure = runVpnCleanupStep(failure, "DPD profile", connectionUuid,
                () -> deleteVpnDpdProfile(connectionUuid));
        throwVpnCleanupFailure(failure, connectionUuid);
    }

    private void deleteVpnIkeProfile(String connectionUuid) {
        try {
            IpsecVpnIkeProfiles ikeProfiles = (IpsecVpnIkeProfiles) nsxService.apply(IpsecVpnIkeProfiles.class);
            ikeProfiles.delete(getVpnIkeProfileName(connectionUuid));
        } catch (NotFound e) {
            logger.debug("The IKE profile of VPN connection {} no longer exists, skipping deletion", connectionUuid);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete the IKE profile of VPN connection %s, due to: %s",
                    connectionUuid, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private void deleteVpnTunnelProfile(String connectionUuid) {
        try {
            IpsecVpnTunnelProfiles espProfiles = (IpsecVpnTunnelProfiles) nsxService.apply(IpsecVpnTunnelProfiles.class);
            espProfiles.delete(getVpnEspProfileName(connectionUuid));
        } catch (NotFound e) {
            logger.debug("The tunnel profile of VPN connection {} no longer exists, skipping deletion", connectionUuid);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete the tunnel profile of VPN connection %s, due to: %s",
                    connectionUuid, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private void deleteVpnDpdProfile(String connectionUuid) {
        try {
            IpsecVpnDpdProfiles dpdProfiles = (IpsecVpnDpdProfiles) nsxService.apply(IpsecVpnDpdProfiles.class);
            dpdProfiles.delete(getVpnDpdProfileName(connectionUuid));
        } catch (NotFound e) {
            logger.debug("The DPD profile of VPN connection {} no longer exists, skipping deletion", connectionUuid);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to delete the DPD profile of VPN connection %s, due to: %s",
                    connectionUuid, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private RuntimeException runVpnCleanupStep(RuntimeException failure, String resource, String connectionUuid,
                                               Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException e) {
            if (failure == null) {
                return e;
            }
            failure.addSuppressed(e);
            logger.warn("Failed to remove NSX VPN {} for connection {} after an earlier cleanup failure: {}",
                    resource, connectionUuid, e.getMessage());
        }
        return failure;
    }

    private void throwVpnCleanupFailure(RuntimeException failure, String connectionUuid) {
        if (failure == null) {
            return;
        }
        if (failure instanceof CloudRuntimeException) {
            throw (CloudRuntimeException) failure;
        }
        throw new CloudRuntimeException(String.format(
                "Failed to remove all NSX VPN resources for connection %s: %s", connectionUuid, failure.getMessage()), failure);
    }

    public String getVpnSessionStatus(String tier1GatewayName, String connectionUuid) {
        String vpnServiceName = getVpnServiceName(tier1GatewayName);
        String sessionName = getVpnSessionName(connectionUuid);
        try {
            DetailedStatus detailedStatusService = (DetailedStatus) nsxService.apply(DetailedStatus.class);
            AggregateIPSecVpnSessionStatus aggregateStatus = detailedStatusService.get(tier1GatewayName, vpnServiceName, sessionName, null, null);
            List<Structure> results = aggregateStatus == null ? null : aggregateStatus.getResults();
            if (CollectionUtils.isEmpty(results)) {
                return VPN_SESSION_STATUS_UNKNOWN;
            }
            List<String> statuses = results.stream()
                    .map(result -> result._convertTo(IPSecVpnSessionStatusNsxt.class).getRuntimeStatus())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (statuses.isEmpty()) {
                return VPN_SESSION_STATUS_UNKNOWN;
            }
            if (statuses.contains(IPSecVpnSessionStatusNsxt.RUNTIME_STATUS_DOWN)) {
                return IPSecVpnSessionStatusNsxt.RUNTIME_STATUS_DOWN;
            }
            if (statuses.stream().allMatch(IPSecVpnSessionStatusNsxt.RUNTIME_STATUS_UP::equals)) {
                return IPSecVpnSessionStatusNsxt.RUNTIME_STATUS_UP;
            }
            return statuses.get(0);
        } catch (NotFound e) {
            logger.debug("The VPN session {} no longer exists on tier-1 gateway {}", sessionName, tier1GatewayName);
            return VPN_SESSION_STATUS_NOT_FOUND;
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to get the status of NSX IPSec VPN session %s on tier-1 gateway %s, due to: %s",
                    sessionName, tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    /**
     * VPN sessions, local endpoints, services and static routes must be removed before the tier-1
     * locale-services deletion during gateway teardown
     */
    private void removeTier1VpnResources(String tier1Id) {
        deleteVpnStaticRoutesByPrefix(tier1Id, getVpnSessionName(""));
        deleteVpnNoSnatRulesByPrefix(tier1Id, getVpnSessionName(""));
        deleteVpnLocalEndpointNoSnatRule(tier1Id);
        try {
            IpsecVpnServices vpnServices = (IpsecVpnServices) nsxService.apply(IpsecVpnServices.class);
            List<IPSecVpnService> services = new ArrayList<>(PagedFetcher.<IPSecVpnServiceListResult, IPSecVpnService>withPageFetcher(
                            cursor -> vpnServices.list(tier1Id, cursor, false, null, null, false, null))
                    .cursorExtractor(IPSecVpnServiceListResult::getCursor)
                    .itemsExtractor(IPSecVpnServiceListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll().getResults());
            // A Tier-1 may also carry VPN services owned by an operator or another integration.
            // CloudStack owns exactly the deterministic service created for this gateway.
            String cloudStackVpnServiceName = getVpnServiceName(tier1Id);
            services.removeIf(service -> !cloudStackVpnServiceName.equals(service.getId()));
            if (CollectionUtils.isEmpty(services)) {
                return;
            }
            Sessions sessions = (Sessions) nsxService.apply(Sessions.class);
            LocalEndpoints localEndpoints = (LocalEndpoints) nsxService.apply(LocalEndpoints.class);
            for (IPSecVpnService service : services) {
                List<Structure> sessionResults = PagedFetcher.<IPSecVpnSessionListResult, Structure>withPageFetcher(
                                cursor -> sessions.list(tier1Id, service.getId(), cursor, false, null, null, false, null))
                        .cursorExtractor(IPSecVpnSessionListResult::getCursor)
                        .itemsExtractor(IPSecVpnSessionListResult::getResults)
                        .itemsSetter((page, allItems) -> {
                            page.setResults(allItems);
                            page.setResultCount((long) allItems.size());
                        })
                        .fetchAll().getResults();
                if (CollectionUtils.isNotEmpty(sessionResults)) {
                    String sessionNamePrefix = getVpnSessionName("");
                    for (Structure result : sessionResults) {
                        IPSecVpnSession session = result._convertTo(IPSecVpnSession.class);
                        logger.debug("Removing VPN session {} from the VPN service {} of Tier 1 Gateway {}", session.getId(), service.getId(), tier1Id);
                        sessions.delete(tier1Id, service.getId(), session.getId());
                        if (session.getId().startsWith(sessionNamePrefix)) {
                            deleteVpnSessionProfiles(session.getId().substring(sessionNamePrefix.length()));
                        }
                    }
                }
                List<IPSecVpnLocalEndpoint> localEndpointResults = PagedFetcher.<IPSecVpnLocalEndpointListResult, IPSecVpnLocalEndpoint>withPageFetcher(
                                cursor -> localEndpoints.list(tier1Id, service.getId(), cursor, false, null, null, false, null))
                        .cursorExtractor(IPSecVpnLocalEndpointListResult::getCursor)
                        .itemsExtractor(IPSecVpnLocalEndpointListResult::getResults)
                        .itemsSetter((page, allItems) -> {
                            page.setResults(allItems);
                            page.setResultCount((long) allItems.size());
                        })
                        .fetchAll().getResults();
                if (CollectionUtils.isNotEmpty(localEndpointResults)) {
                    for (IPSecVpnLocalEndpoint localEndpoint : localEndpointResults) {
                        logger.debug("Removing VPN local endpoint {} from the VPN service {} of Tier 1 Gateway {}", localEndpoint.getId(), service.getId(), tier1Id);
                        localEndpoints.delete(tier1Id, service.getId(), localEndpoint.getId());
                    }
                }
                logger.debug("Removing VPN service {} from Tier 1 Gateway {}", service.getId(), tier1Id);
                vpnServices.delete(tier1Id, service.getId());
            }
        } catch (NotFound e) {
            logger.debug("The VPN resources of the Tier 1 Gateway {} no longer exist, skipping deletion", tier1Id);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to remove the VPN resources of the Tier 1 Gateway %s, due to: %s",
                    tier1Id, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    private void deleteVpnLocalEndpointNoSnatRule(String tier1GatewayName) {
        String ruleName = getVpnLocalEndpointNoSnatRuleName(getVpnServiceName(tier1GatewayName));
        try {
            NatRules natService = (NatRules) nsxService.apply(NatRules.class);
            natService.delete(tier1GatewayName, NatId.USER.name(), ruleName);
        } catch (NotFound e) {
            logger.debug("The VPN local-endpoint no-SNAT rule {} no longer exists on tier-1 gateway {}",
                    ruleName, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            throw new CloudRuntimeException(String.format(
                    "Failed to delete the VPN local-endpoint no-SNAT rule %s on tier-1 gateway %s, due to: %s",
                    ruleName, tier1GatewayName, ae.getErrorMessage()), error);
        }
    }

    /**
     * Lists the local VTI addresses of the route-based VPN sessions on a tier-1 gateway, excluding
     * the session of the given connection; used to fail closed on deterministic VTI collisions.
     */
    public Set<String> getRouteBasedVpnSessionLocalVtiIps(String tier1GatewayName, String excludedConnectionUuid) {
        String vpnServiceName = getVpnServiceName(tier1GatewayName);
        String excludedSessionName = getVpnSessionName(excludedConnectionUuid);
        Set<String> vtiIps = new HashSet<>();
        try {
            Sessions sessions = (Sessions) nsxService.apply(Sessions.class);
            List<Structure> sessionResults = PagedFetcher.<IPSecVpnSessionListResult, Structure>withPageFetcher(
                            cursor -> sessions.list(tier1GatewayName, vpnServiceName, cursor, false, null, null, false, null))
                    .cursorExtractor(IPSecVpnSessionListResult::getCursor)
                    .itemsExtractor(IPSecVpnSessionListResult::getResults)
                    .itemsSetter((page, allItems) -> {
                        page.setResults(allItems);
                        page.setResultCount((long) allItems.size());
                    })
                    .fetchAll().getResults();
            for (Structure result : sessionResults) {
                IPSecVpnSession session = result._convertTo(IPSecVpnSession.class);
                if (excludedSessionName.equals(session.getId())
                        || !RouteBasedIPSecVpnSession.class.getSimpleName().equals(session.getResourceType())) {
                    continue;
                }
                RouteBasedIPSecVpnSession routeBasedSession = result._convertTo(RouteBasedIPSecVpnSession.class);
                if (CollectionUtils.isEmpty(routeBasedSession.getTunnelInterfaces())) {
                    continue;
                }
                for (IPSecVpnTunnelInterface tunnelInterface : routeBasedSession.getTunnelInterfaces()) {
                    if (CollectionUtils.isEmpty(tunnelInterface.getIpSubnets())) {
                        continue;
                    }
                    for (TunnelInterfaceIPSubnet ipSubnet : tunnelInterface.getIpSubnets()) {
                        if (CollectionUtils.isNotEmpty(ipSubnet.getIpAddresses())) {
                            vtiIps.addAll(ipSubnet.getIpAddresses());
                        }
                    }
                }
            }
        } catch (NotFound e) {
            logger.debug("The VPN service {} does not exist yet on tier-1 gateway {}, no VTI addresses are in use",
                    vpnServiceName, tier1GatewayName);
        } catch (Error error) {
            ApiError ae = error.getData()._convertTo(ApiError.class);
            String msg = String.format("Failed to list the VPN sessions on tier-1 gateway %s, due to: %s",
                    tier1GatewayName, ae.getErrorMessage());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return vtiIps;
    }

    private String getVpnLocalEndpointPath(String tier1GatewayName, String vpnServiceName, String localEndpointName) {
        return TIER_1_GATEWAY_PATH_PREFIX + tier1GatewayName + "/ipsec-vpn-services/" + vpnServiceName
                + "/local-endpoints/" + localEndpointName;
    }
}
