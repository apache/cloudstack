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
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.infra.DhcpRelayConfigs;
import com.vmware.nsx_policy.infra.Segments;
import com.vmware.nsx_policy.infra.Sites;
import com.vmware.nsx_policy.infra.Tier1s;
import com.vmware.nsx_policy.infra.sites.EnforcementPoints;
import com.vmware.nsx_policy.infra.tier_0s.LocaleServices;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.DhcpRelayConfig;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.LocaleServicesListResult;
import com.vmware.nsx_policy.model.Segment;
import com.vmware.nsx_policy.model.SegmentSubnet;
import com.vmware.nsx_policy.model.SiteListResult;
import com.vmware.nsx_policy.model.Tier1;
import com.vmware.vapi.bindings.Service;
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
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.isNull;

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

    private List<String> getRouterAdvertisementTypeList(boolean sourceNatEnabled) {
        List<String> types = new ArrayList<>();
        types.add(RouteAdvertisementType.TIER1_IPSEC_LOCAL_ENDPOINT.name());
        types.add(RouteAdvertisementType.TIER1_NAT.name());
        if (!sourceNatEnabled) {
            types.add(RouteAdvertisementType.TIER1_CONNECTED.name());
        }
        return types;
    }

    public void createTier1Gateway(String name, String tier0Gateway, String edgeCluster, boolean sourceNatEnabled) {
        String tier0GatewayPath = TIER_0_GATEWAY_PATH_PREFIX + tier0Gateway;
        Tier1 tier1 = getTier1Gateway(name);
        if (tier1 != null) {
            throw new InvalidParameterValueException(String.format("VPC network with name %s exists in NSX zone", name));
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

    public void createSegment(long zoneId, long domainId, long accountId, Long vpcId, String segmentName, String gatewayAddress, String tier0Gateway, String enforcementPointPath, List<TransportZone> transportZones) {
        try {
            Segments segmentService = (Segments) nsxService.apply(Segments.class);
            SegmentSubnet subnet = new SegmentSubnet.Builder()
                    .setGatewayAddress(gatewayAddress)
                    .build();
            Segment segment = new Segment.Builder()
                    .setResourceType(SEGMENT_RESOURCE_TYPE)
                    .setId(segmentName)
                    .setDisplayName(segmentName)
                    .setConnectivityPath(isNull(vpcId) ? TIER_0_GATEWAY_PATH_PREFIX + tier0Gateway
                            : TIER_1_GATEWAY_PATH_PREFIX + NsxControllerUtils.getTier1GatewayName(domainId, accountId, zoneId, vpcId))
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
}
