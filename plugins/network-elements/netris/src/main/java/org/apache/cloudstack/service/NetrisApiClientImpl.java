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

import com.cloud.domain.Domain;
import com.cloud.network.netris.NetrisLbBackend;
import com.cloud.network.netris.NetrisNetworkRule;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.netris.ApiClient;
import io.netris.ApiException;
import io.netris.ApiResponse;
import io.netris.api.v1.AclApi;
import io.netris.api.v1.AuthenticationApi;
import io.netris.api.v1.RoutesApi;
import io.netris.api.v1.SitesApi;
import io.netris.api.v1.TenantsApi;
import io.netris.api.v2.IpamApi;
import io.netris.api.v2.L4LoadBalancerApi;
import io.netris.api.v2.NatApi;
import io.netris.api.v2.VNetApi;
import io.netris.api.v2.VpcApi;
import io.netris.model.AclAddItem;
import io.netris.model.AclBodyVpc;
import io.netris.model.AclDeleteItem;
import io.netris.model.AclEditItem;
import io.netris.model.AclGetBody;
import io.netris.model.AclResponseGetOk;
import io.netris.model.AllocationBody;
import io.netris.model.AllocationBodyVpc;
import io.netris.model.FilterBySites;
import io.netris.model.FilterByVpc;
import io.netris.model.GetSiteBody;

import io.netris.model.InlineResponse20015;
import io.netris.model.InlineResponse20016;
import io.netris.model.InlineResponse2003;
import io.netris.model.InlineResponse2004;
import io.netris.model.InlineResponse2004Data;
import io.netris.model.IpTree;
import io.netris.model.IpTreeAllocation;
import io.netris.model.IpTreeAllocationTenant;
import io.netris.model.IpTreeSubnet;
import io.netris.model.IpTreeSubnetSites;
import io.netris.model.L4LBSite;
import io.netris.model.L4LbAddItem;
import io.netris.model.L4LbEditItem;
import io.netris.model.L4LbTenant;
import io.netris.model.L4LbVpc;
import io.netris.model.L4LoadBalancerBackendItem;
import io.netris.model.L4LoadBalancerItem;
import io.netris.model.L4lbAddOrUpdateItem;
import io.netris.model.L4lbresBody;
import io.netris.model.NatBodySiteSite;
import io.netris.model.NatBodyVpcVpc;
import io.netris.model.NatGetBody;
import io.netris.model.NatPostBody;
import io.netris.model.NatPutBody;
import io.netris.model.NatResponseGetOk;
import io.netris.model.ResAddEditBody;
import io.netris.model.RoutesBody;
import io.netris.model.RoutesBodyId;
import io.netris.model.RoutesBodyVpcVpc;
import io.netris.model.RoutesGetBody;
import io.netris.model.RoutesPostBody;
import io.netris.model.RoutesPutBody;
import io.netris.model.RoutesResponseGetOk;
import io.netris.model.SitesResponseOK;
import io.netris.model.SubnetBody;
import io.netris.model.SubnetResBody;
import io.netris.model.VPCAdminTenant;
import io.netris.model.VPCCreate;
import io.netris.model.VPCListing;
import io.netris.model.VPCResource;
import io.netris.model.VPCResourceIpam;
import io.netris.model.VPCResponseOK;
import io.netris.model.VPCResponseObjectOK;
import io.netris.model.VPCResponseResourceOK;
import io.netris.model.VnetAddBody;
import io.netris.model.VnetAddBodyDhcp;
import io.netris.model.VnetAddBodyDhcpOptionSet;
import io.netris.model.VnetAddBodyGateways;
import io.netris.model.VnetAddBodyVpc;
import io.netris.model.VnetEditBody;
import io.netris.model.VnetEditBodyDhcp;
import io.netris.model.VnetEditBodyGateways;
import io.netris.model.VnetResAddBody;
import io.netris.model.VnetResDeleteBody;
import io.netris.model.VnetResListBody;
import io.netris.model.VnetsBody;
import io.netris.model.VpcEditResponseOK;
import io.netris.model.VpcVpcIdBody;
import io.netris.model.response.AuthResponse;
import io.netris.model.response.L4LbEditResponse;
import io.netris.model.response.TenantResponse;
import io.netris.model.response.TenantsResponse;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisACLCommand;
import org.apache.cloudstack.agent.api.AddOrUpdateNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisNatCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisACLCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVnetCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.agent.api.ListNetrisStaticRoutesCommand;
import org.apache.cloudstack.agent.api.NetrisCommand;
import org.apache.cloudstack.agent.api.ReleaseNatIpCommand;
import org.apache.cloudstack.agent.api.SetupNetrisPublicRangeCommand;
import org.apache.cloudstack.agent.api.UpdateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.UpdateNetrisVpcCommand;
import org.apache.cloudstack.resource.NetrisResourceObjectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NetrisApiClientImpl implements NetrisApiClient {

    private final Logger logger = LogManager.getLogger(getClass());
    private static final String ANY_IP = "0.0.0.0/0";
    private static final String[] PROTOCOL_LIST = new String[]{"TCP", "UDP", "ICMP", "ALL"};

    protected ApiClient apiClient;

    protected final int siteId;
    private final String siteName;
    protected final int tenantId;
    private final String tenantName;

    public NetrisApiClientImpl(String endpointBaseUrl, String username, String password, String siteName, String adminTenantName) {
        try {
            apiClient = new ApiClient(endpointBaseUrl, username, password, 1L);
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating the Netris API Client for %s", endpointBaseUrl), e);
        }
        Pair<Integer, String> sitePair = getNetrisSitePair(siteName);
        Pair<Integer, String> tenantPair = getNetrisTenantPair(adminTenantName);
        this.siteId = sitePair.first();
        this.siteName = sitePair.second();
        this.tenantId = tenantPair.first();
        this.tenantName = tenantPair.second();
    }

    private Pair<Integer, String> getNetrisSitePair(String siteName) {
        List<GetSiteBody> sites = listSites();
        if (CollectionUtils.isEmpty(sites)) {
            throw new CloudRuntimeException("There are no Netris sites, please check the Netris endpoint");
        }
        List<GetSiteBody> filteredSites = sites.stream().filter(x -> x.getName().equals(siteName)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filteredSites)) {
            throw new CloudRuntimeException(String.format("Cannot find a site matching name %s on Netris, please check the Netris endpoint", siteName));
        }
        return new Pair<>(filteredSites.get(0).getId(), siteName);
    }

    private Pair<Integer, String> getNetrisTenantPair(String adminTenantName) {
        List<TenantResponse> tenants = listTenants();
        if (CollectionUtils.isEmpty(tenants)) {
            throw new CloudRuntimeException("There are no Netris tenants, please check the Netris endpoint");
        }
        List<TenantResponse> filteredTenants = tenants.stream().filter(x -> x.getName().equals(adminTenantName)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filteredTenants)) {
            throw new CloudRuntimeException(String.format("Cannot find a site matching name %s on Netris, please check the Netris endpoint", adminTenantName));
        }
        return new Pair<>(filteredTenants.get(0).getId().intValue(), adminTenantName);
    }

    protected void logAndThrowException(String prefix, ApiException e) throws CloudRuntimeException {
        String msg = String.format("%s: (%s, %s, %s)", prefix, e.getCode(), e.getMessage(), e.getResponseBody());
        logger.error(msg, e);
        throw new CloudRuntimeException(msg);
    }

    @Override
    public boolean isSessionAlive() {
        ApiResponse<AuthResponse> response = null;
        try {
            AuthenticationApi api = apiClient.getApiStubForMethod(AuthenticationApi.class);
            response = api.apiAuthGet();
        } catch (ApiException e) {
            logAndThrowException("Error checking the Netris API session is alive", e);
        }
        return response != null && response.getStatusCode() == 200;
    }

    @Override
    public List<GetSiteBody> listSites() {
        SitesResponseOK response = null;
        try {
            SitesApi api = apiClient.getApiStubForMethod(SitesApi.class);
            response = api.apiSitesGet();
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris Sites", e);
        }
        return response != null ? response.getData() : null;
    }

    @Override
    public List<VPCListing> listVPCs() {
        VPCResponseOK response = null;
        try {
            VpcApi api = apiClient.getApiStubForMethod(VpcApi.class);
            response = api.apiV2VpcGet();
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris VPCs", e);
        }
        return response != null ? response.getData() : null;
    }

    @Override
    public List<TenantResponse> listTenants() {
        ApiResponse<TenantsResponse> response = null;
        try {
            TenantsApi api = apiClient.getApiStubForMethod(TenantsApi.class);
            response = api.apiTenantsGet();
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris Tenants", e);
        }
        return (response != null && response.getData() != null) ? response.getData().getData() : null;
    }

    private VPCResponseObjectOK createVpcInternal(String vpcName, int adminTenantId, String adminTenantName) {
        VPCResponseObjectOK response;
        logger.debug(String.format("Creating Netris VPC %s", vpcName));
        try {
            VpcApi vpcApi = apiClient.getApiStubForMethod(VpcApi.class);
            VPCCreate body = new VPCCreate();
            body.setName(vpcName);
            VPCAdminTenant vpcAdminTenant = new VPCAdminTenant();
            vpcAdminTenant.setId(adminTenantId);
            vpcAdminTenant.name(adminTenantName);
            body.setAdminTenant(vpcAdminTenant);
            response = vpcApi.apiV2VpcPost(body);
        } catch (ApiException e) {
            logAndThrowException("Error creating Netris VPC", e);
            return null;
        }
        return response;
    }

    private VpcEditResponseOK updateVpcInternal(String vpcName, String prevVpcName, int adminTenantId, String adminTenantName) {
        VpcEditResponseOK response;
        logger.debug(String.format("Updating Netris VPC name from %s to %s", prevVpcName, vpcName));
        try {
            VPCListing vpcResource = getVpcByNameAndTenant(prevVpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", prevVpcName, tenantId);
                return null;
            }
            VpcApi vpcApi = apiClient.getApiStubForMethod(VpcApi.class);
            VpcVpcIdBody body = new VpcVpcIdBody();
            body.setName(vpcName);
            VPCAdminTenant vpcAdminTenant = new VPCAdminTenant();
            vpcAdminTenant.setId(adminTenantId);
            vpcAdminTenant.name(adminTenantName);
            body.setAdminTenant(vpcAdminTenant);
            response = vpcApi.apiV2VpcVpcIdPut(body, vpcResource.getId());
        } catch (ApiException e) {
            logAndThrowException("Error updating Netris VPC", e);
            return null;
        }
        return response;
    }

    private InlineResponse2004Data createIpamAllocationInternal(String ipamName, String ipamPrefix, VPCListing vpc) {
        logger.debug(String.format("Creating Netris IPAM Allocation %s for VPC %s", ipamPrefix, vpc.getName()));
        try {
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            AllocationBody body = new AllocationBody();
            AllocationBodyVpc allocationBodyVpc = new AllocationBodyVpc();
            allocationBodyVpc.setId(vpc.getId());
            allocationBodyVpc.setName(vpc.getName());
            body.setVpc(allocationBodyVpc);
            body.setName(ipamName);
            body.setPrefix(ipamPrefix);
            IpTreeAllocationTenant allocationTenant = new IpTreeAllocationTenant();
            allocationTenant.setId(new BigDecimal(tenantId));
            allocationTenant.setName(tenantName);
            body.setTenant(allocationTenant);
            InlineResponse2004 ipamResponse = ipamApi.apiV2IpamAllocationPost(body);
            if (ipamResponse == null || !ipamResponse.isIsSuccess()) {
                String reason = ipamResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris Allocation {} for VPC {} creation failed: {}", ipamPrefix, vpc.getName(), reason);
                return null;
            }
            logger.debug(String.format("Successfully created VPC %s and its IPAM Allocation %s on Netris", vpc.getName(), ipamPrefix));
            return ipamResponse.getData();
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris IPAM Allocation %s for VPC %s", ipamPrefix, vpc.getName()), e);
            return null;
        }
    }

    @Override
    public boolean createVpc(CreateNetrisVpcCommand cmd) {
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC);
        VPCResponseObjectOK createdVpc = createVpcInternal(netrisVpcName, tenantId, tenantName);
        if (createdVpc == null || !createdVpc.isIsSuccess()) {
            String reason = createdVpc == null ? "Empty response" : "Operation failed on Netris";
            logger.debug("The Netris VPC {} creation failed: {}", cmd.getName(), reason);
            return false;
        }

        String netrisIpamAllocationName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_ALLOCATION, cmd.getCidr());
        String vpcCidr = cmd.getCidr();
        InlineResponse2004Data createdIpamAllocation = createIpamAllocationInternal(netrisIpamAllocationName, vpcCidr, createdVpc.getData());
        return createdIpamAllocation != null;
    }

    @Override
    public boolean updateVpc(UpdateNetrisVpcCommand cmd) {
        Long domainId = cmd.getDomainId();
        Long zoneId = cmd.getZoneId();
        Long accountId = cmd.getAccountId();
        Long vpcId = cmd.getId();
        String prevVpcName = cmd.getPreviousVpcName();
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC);
        String netrisPrevVpcName = String.format("D%s-A%s-Z%s-V%s-%s", domainId, accountId, zoneId, vpcId, prevVpcName);
        VpcEditResponseOK updatedVpc = updateVpcInternal(netrisVpcName, netrisPrevVpcName, tenantId, tenantName);
        if (updatedVpc == null || !updatedVpc.isIsSuccess()) {
            String reason = updatedVpc == null ? "Empty response" : "Operation failed on Netris";
            logger.debug("The update of Netris VPC {} failed: {}", cmd.getPreviousVpcName(), reason);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteNatRule(DeleteNetrisNatRuleCommand cmd) {
        try {
            String suffix = getNetrisVpcNameSuffix(cmd.getVpcId(), cmd.getVpcName(), cmd.getId(), cmd.getName(), cmd.isVpc());
            String vpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffix);
            VPCListing vpcResource = getVpcByNameAndTenant(vpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", vpcName, tenantId);
                return false;
            }
            String natRuleName = cmd.getNatRuleName();
            NatGetBody existingNatRule = netrisNatRuleExists(natRuleName);
            boolean ruleExists = Objects.nonNull(existingNatRule);
            if (ruleExists) {
                deleteNatRule(natRuleName, existingNatRule.getId(), vpcResource.getName());
                if (cmd.getNatRuleType().equals("STATICNAT")) {
                    String natIp = cmd.getNatIp();
                    String netrisSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, String.valueOf(cmd.getVpcId()), natIp);
                    deleteNatSubnet(netrisSubnetName, vpcResource.getId(), natIp);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Error deleting Netris NAT Rule", e);
        }
        return true;
    }

    @Override
    public boolean addOrUpdateAclRule(CreateOrUpdateNetrisACLCommand cmd, boolean forLb) {
        String aclName = cmd.getNetrisAclName();
        try {
            AclApi aclApi = apiClient.getApiStubForMethod(AclApi.class);
            VPCListing vpcResource;
            String netrisVpcName;
            if (forLb) {
                vpcResource = getSystemVpc();
                netrisVpcName = vpcResource.getName();

            } else {
                netrisVpcName = getNetrisVpcName(cmd, cmd.getVpcId(), cmd.getVpcName());
                vpcResource = getNetrisVpcResource(netrisVpcName);
                if (Objects.isNull(vpcResource)) {
                    return false;
                }
            }
            AclBodyVpc vpc = new AclBodyVpc().id(vpcResource.getId());
            List<String> aclNames = List.of(aclName);
            Pair<Boolean, List<BigDecimal>> resultAndMatchingAclIds = getMatchingAclIds(aclNames, netrisVpcName);
            List<BigDecimal> aclIdList = resultAndMatchingAclIds.second();
            if (!aclIdList.isEmpty()) {
                logger.debug("Netris ACL rule: {} already exists, updating it...", aclName);
                AclEditItem aclEditItem = getAclEditItem(cmd, aclName, aclIdList.get(0));
                aclEditItem.setVpc(vpc);
                try {
                    aclApi.apiAclPut(aclEditItem);
                } catch (ApiException e) {
                    if (e.getResponseBody().contains("This kind of acl already exists")) {
                        logger.info("Netris ACL rule: {} already exists and doesn't need to be updated", aclName);
                        return true;
                    }
                    throw new CloudRuntimeException("Error updating Netris ACL rule", e);
                }
                return true;
            }
            AclAddItem aclAddItem = getAclAddItem(cmd, aclName);
            aclAddItem.setVpc(vpc);
            aclApi.apiAclPost(aclAddItem);
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to create Netris ACL: %s", cmd.getNetrisAclName()), e);
        }
        return true;
    }

    AclAddItem getAclAddItem(CreateOrUpdateNetrisACLCommand cmd, String aclName) throws ApiException {
        AclAddItem aclAddItem = new AclAddItem();
        aclAddItem.setAction(cmd.getAction());
        aclAddItem.setComment(String.format("ACL rule: %s. %s", cmd.getNetrisAclName(), cmd.getReason()));
        aclAddItem.setName(aclName);
        String protocol = cmd.getProtocol();
        if ("TCP".equals(protocol)) {
            aclAddItem.setEstablished(new BigDecimal(1));
        } else {
            aclAddItem.setReverse("yes");
        }
        if (!Arrays.asList(PROTOCOL_LIST).contains(protocol)) {
            aclAddItem.setProto("ip");
            aclAddItem.setSrcPortTo(cmd.getIcmpType());
            // TODO: set proto number: where should the protocol number be set - API sets the protocol number to Src-from & to and Dest-from & to fields
        } else if ("ICMP".equals(protocol)) {
            aclAddItem.setProto("icmp");
            if (cmd.getIcmpType() != -1) {
                aclAddItem.setIcmpType(cmd.getIcmpType());
            }
        } else {
            aclAddItem.setProto(protocol.toLowerCase(Locale.ROOT));
        }

        aclAddItem.setDstPortFrom(cmd.getDestPortStart());
        aclAddItem.setDstPortTo(cmd.getDestPortEnd());
        aclAddItem.setDstPrefix(cmd.getDestPrefix());
        aclAddItem.setSrcPrefix(cmd.getSourcePrefix());
        aclAddItem.setSrcPortFrom(1);
        aclAddItem.setSrcPortTo(65535);
        if (NatPutBody.ProtocolEnum.ICMP.name().equalsIgnoreCase(protocol)) {
            aclAddItem.setIcmpType(cmd.getIcmpType());
        }

        return aclAddItem;
    }

    AclEditItem getAclEditItem(CreateOrUpdateNetrisACLCommand cmd, String aclName, BigDecimal aclId) throws ApiException {
        AclEditItem aclEditItem = new AclEditItem();
        aclEditItem.setId(aclId);
        aclEditItem.setAction(cmd.getAction());
        aclEditItem.setComment(String.format("ACL rule: %s. %s", cmd.getNetrisAclName(), cmd.getReason()));
        aclEditItem.setName(aclName);
        String protocol = cmd.getProtocol();
        if ("TCP".equals(protocol)) {
            aclEditItem.setEstablished(new BigDecimal(1));
        } else {
            aclEditItem.setReverse("yes");
        }
        if (!Arrays.asList(PROTOCOL_LIST).contains(protocol)) {
            aclEditItem.setProto("ip");
            aclEditItem.setSrcPortTo(cmd.getIcmpType());
            // TODO: set proto number: where should the protocol number be set - API sets the protocol number to Src-from & to and Dest-from & to fields
        } else if ("ICMP".equals(protocol)) {
            aclEditItem.setProto("icmp");
            if (cmd.getIcmpType() != -1) {
                aclEditItem.setIcmpType(cmd.getIcmpType());
            }
        } else {
            aclEditItem.setProto(protocol.toLowerCase(Locale.ROOT));
        }

        aclEditItem.setDstPortFrom(cmd.getDestPortStart());
        aclEditItem.setDstPortTo(cmd.getDestPortEnd());
        aclEditItem.setDstPrefix(cmd.getDestPrefix());
        aclEditItem.setSrcPrefix(cmd.getSourcePrefix());
        aclEditItem.setSrcPortFrom(1);
        aclEditItem.setSrcPortTo(65535);
        if (NatPutBody.ProtocolEnum.ICMP.name().equalsIgnoreCase(protocol)) {
            aclEditItem.setIcmpType(cmd.getIcmpType());
        }

        return aclEditItem;
    }

    @Override
    public boolean deleteAclRule(DeleteNetrisACLCommand cmd, boolean forLb) {
        List<String> aclNames = cmd.getAclRuleNames();
        try {
            AclApi aclApi = apiClient.getApiStubForMethod(AclApi.class);

            String vpcName;
            if (!forLb) {
                String suffix = getNetrisVpcNameSuffix(cmd.getVpcId(), cmd.getVpcName(), cmd.getId(), cmd.getName(), cmd.isVpc());
                vpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffix);
            } else {
                VPCListing vpcResource = getSystemVpc();
                vpcName = vpcResource.getName();
            }
            Pair<Boolean, List<BigDecimal>> resultAndMatchingAclIds = getMatchingAclIds(aclNames, vpcName);
            Boolean result = resultAndMatchingAclIds.first();
            List<BigDecimal> matchingAclIds = resultAndMatchingAclIds.second();
            if (!result) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", vpcName, tenantId);
                return false;
            }
            if (matchingAclIds.isEmpty()) {
                logger.warn("There doesn't seem to be any ACLs on Netris matching {}", aclNames.size() > 1 ? String.join(",", aclNames) : aclNames);
                return true;
            }
            AclDeleteItem aclDeleteItem = new AclDeleteItem();
            aclDeleteItem.setId(matchingAclIds);
            aclDeleteItem.setTenantsID(String.valueOf(tenantId));
            aclApi.apiAclDelete(aclDeleteItem);
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to delete Netris ACLs: %s", String.join(",", cmd.getAclRuleNames())), e);
        }
        return true;
    }

    Pair<Boolean, List<BigDecimal>> getMatchingAclIds(List<String> aclNames, String vpcName) {
        try {
            AclApi aclApi = apiClient.getApiStubForMethod(AclApi.class);
            VPCListing vpcResource = getVpcByNameAndTenant(vpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", vpcName, tenantId);
                return new Pair<>(false, Collections.emptyList());
            }
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(vpcResource.getId());
            FilterBySites siteFilter = new FilterBySites();
            siteFilter.add(siteId);
            AclResponseGetOk aclGetResponse = aclApi.apiAclGet(siteFilter, vpcFilter);
            if (aclGetResponse == null || !aclGetResponse.isIsSuccess()) {
                logger.warn("No ACLs were found to be present for the specific Netris VPC resource {}." +
                        " Netris ACLs may have been deleted out of band.", vpcName);
                return new Pair<>(true, Collections.emptyList());
            }
            List<AclGetBody> aclList = aclGetResponse.getData();
            return new Pair<>(true, aclList.stream()
                    .filter(acl -> aclNames.contains(acl.getName()))
                    .map(acl -> BigDecimal.valueOf(acl.getId()))
                    .collect(Collectors.toList()));
        } catch (ApiException e) {
            logAndThrowException("Failed to retrieve Netris ACLs", e);
        }
        return new Pair<>(true, Collections.emptyList());
    }

    public boolean addOrUpdateStaticRoute(AddOrUpdateNetrisStaticRouteCommand cmd) {
        String prefix = cmd.getPrefix();
        String nextHop = cmd.getNextHop();
        Long vpcId = cmd.getId();
        String vpcName = cmd.getName();
        boolean updateRoute = cmd.isUpdateRoute();
        try {
            String vpcSuffix = getNetrisVpcNameSuffix(vpcId, vpcName, null, null, true);
            String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, vpcSuffix);
            VPCListing vpcResource = getVpcByNameAndTenant(netrisVpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
                return false;
            }

            String[] suffixes = new String[2];
            suffixes[0] = vpcId.toString();
            suffixes[1] = cmd.getRouteId().toString();
            String staticRouteId = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffixes);

            Pair<Boolean, RoutesGetBody> existingStaticRoute = staticRouteExists(vpcResource.getId(), prefix, null, staticRouteId);
            if (updateRoute) {
                if (!existingStaticRoute.first()) {
                    logger.error("The Netris static route {} does not exist for VPC {}, adding it", prefix, netrisVpcName);
                    return addStaticRouteInternal(vpcResource, netrisVpcName, prefix, nextHop, staticRouteId);
                }
                return updateStaticRouteInternal(existingStaticRoute.second().getId(), netrisVpcName, prefix, nextHop, staticRouteId);
            } else {
                if (existingStaticRoute.first()) {
                    String existingNextHop = existingStaticRoute.second().getNextHop();
                    if (existingNextHop != null && existingNextHop.equals(nextHop)) {
                        logger.debug("The Netris static route {} already exists for VPC {}", prefix, netrisVpcName);
                        return true;
                    } else {
                        logger.debug("The Netris static route {} already exists but has different next hop {} for VPC {}", prefix, nextHop, netrisVpcName);
                        return false;
                    }
                }
                return addStaticRouteInternal(vpcResource, netrisVpcName, prefix, nextHop, staticRouteId);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Error adding Netris static route", e);
        }
    }

    private boolean addStaticRouteInternal(VPCListing vpcResource, String netrisVpcName, String prefix, String nextHop, String staticRouteId) {
        try {
            RoutesApi routesApi = apiClient.getApiStubForMethod(RoutesApi.class);
            RoutesPostBody routesPostBody = new RoutesPostBody();
            routesPostBody.setPrefix(prefix);
            routesPostBody.setNextHop(nextHop);
            routesPostBody.setSiteId(new BigDecimal(siteId));
            routesPostBody.setStateStatus(RoutesBody.StateStatusEnum.ACTIVE);

            RoutesBodyVpcVpc vpcBody = new RoutesBodyVpcVpc();
            vpcBody.setId(vpcResource.getId());
            vpcBody.setName(vpcResource.getName());
            vpcBody.setIsDefault(vpcResource.isIsDefault());
            vpcBody.setIsSystem(vpcResource.isIsSystem());
            routesPostBody.setVpc(vpcBody);

            routesPostBody.setDescription(staticRouteId);
            routesPostBody.setStateStatus(RoutesBody.StateStatusEnum.ACTIVE);
            routesPostBody.setSwitches(Collections.emptyList());

            InlineResponse2004 routeResponse = routesApi.apiRoutesPost(routesPostBody);
            if (routeResponse == null || !routeResponse.isIsSuccess()) {
                String reason = routeResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris static route creation failed for netris VPC - {}: {}", netrisVpcName, reason);
                throw new CloudRuntimeException(reason);
            }
        } catch (ApiException e) {
            logAndThrowException("Error adding Netris static route", e);
            return false;
        }
        return true;
    }

    private boolean updateStaticRouteInternal(Integer id, String netrisVpcName, String prefix, String nextHop, String staticRouteId) {
        try {
            RoutesApi routesApi = apiClient.getApiStubForMethod(RoutesApi.class);
            RoutesPutBody routesPutBody = new RoutesPutBody();
            routesPutBody.setId(id);
            routesPutBody.setPrefix(prefix);
            routesPutBody.setNextHop(nextHop);
            routesPutBody.setSiteId(new BigDecimal(siteId));
            routesPutBody.setStateStatus(RoutesPutBody.StateStatusEnum.ACTIVE);

            routesPutBody.setDescription(staticRouteId);
            routesPutBody.setSwitches(Collections.emptyList());

            InlineResponse2003 routeResponse = routesApi.apiRoutesPut(routesPutBody);
            if (routeResponse == null || !routeResponse.isIsSuccess()) {
                String reason = routeResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("Failed to update Netris static route for netris VPC - {}: {}", netrisVpcName, reason);
                throw new CloudRuntimeException(reason);
            }
        } catch (ApiException e) {
            logAndThrowException("Error updating Netris static route", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteStaticRoute(DeleteNetrisStaticRouteCommand cmd) {
        Long vpcId = cmd.getId();
        String vpcName = cmd.getName();
        String prefix = cmd.getPrefix();
        String nextHop = cmd.getNextHop();
        try {
            String vpcSuffix = getNetrisVpcNameSuffix(vpcId, vpcName, null, null, true);
            String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, vpcSuffix);
            VPCListing vpcResource = getVpcByNameAndTenant(netrisVpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
                return false;
            }

            String[] suffixes = new String[2];
            suffixes[0] = vpcId.toString();
            suffixes[1] = cmd.getRouteId().toString();
            String staticRouteId = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffixes);
            Pair<Boolean, RoutesGetBody> existingStaticRoute = staticRouteExists(vpcResource.getId(), prefix, nextHop, staticRouteId);

            if (Boolean.FALSE.equals(existingStaticRoute.first())) {
                logger.debug("The Netris static route {} does not exist for VPC {}", prefix, netrisVpcName);
                return true;
            }
            RoutesGetBody existingRoute = existingStaticRoute.second();
            RoutesApi routesApi = apiClient.getApiStubForMethod(RoutesApi.class);
            RoutesBodyId id = new RoutesBodyId();
            id.setId(existingRoute.getId());
            InlineResponse2003 routeDeleteResponse = routesApi.apiRoutesDelete(id);
            if (routeDeleteResponse == null || !routeDeleteResponse.isIsSuccess()) {
                String reason = routeDeleteResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris static route deletion failed for netris VPC - {}: {}", netrisVpcName, reason);
                throw new CloudRuntimeException(reason);
            }
            return true;
        } catch (ApiException e) {
            logAndThrowException("Error deleting Netris static route", e);
        }
        return false;
    }

    @Override
    public List<StaticRoute> listStaticRoutes(ListNetrisStaticRoutesCommand cmd) {
        Long vpcId = cmd.getId();
        String vpcName = cmd.getName();
        String prefix = cmd.getPrefix();
        String nextHop = cmd.getNextHop();
        String vpcSuffix = getNetrisVpcNameSuffix(vpcId, vpcName, null, null, true);
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, vpcSuffix);
        VPCListing vpcResource = getVpcByNameAndTenant(netrisVpcName);
        if (vpcResource == null) {
            logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
            return new ArrayList<>();
        }

        List<RoutesGetBody> staticRoutes = listStaticRoutes(vpcResource.getId(), prefix, nextHop);
        if (CollectionUtils.isEmpty(staticRoutes)) {
            return new ArrayList<>();
        }
        List<StaticRoute> result = new ArrayList<>();
        for (RoutesGetBody staticRoute : staticRoutes) {
            // All static routes belong the SYSTEM account, which does not matter
            result.add(new StaticRouteVO(null, staticRoute.getName(), vpcId, Account.ACCOUNT_ID_SYSTEM, Domain.ROOT_DOMAIN, staticRoute.getNextHop()));
        }
        return result;
    }

    @Override
    public boolean releaseNatIp(ReleaseNatIpCommand cmd) {
        String natIp = cmd.getNatIp() + "/32";
        try {
            VPCListing systemVpc = getSystemVpc();
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(systemVpc.getId());
            SubnetResBody subnetResponse = ipamApi.apiV2IpamSubnetsGet(vpcFilter);
            if (subnetResponse == null || !subnetResponse.isIsSuccess()) {
                String reason = subnetResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("Failed to retrieve Netris Public NAT IPs due to {}", reason);
                throw new CloudRuntimeException(reason);
            }
            List<IpTreeSubnet> natIps = subnetResponse.getData().stream().filter(ip -> ip.getPrefix().equals(natIp)).collect(Collectors.toList());
            if (!natIps.isEmpty()) {
                ipamApi.apiV2IpamTypeIdDelete("subnet", natIps.get(0).getId().intValue());
            }

        } catch (ApiException e) {
            logAndThrowException("Failed to release Netris IP", e);
        }
        return true;
    }

    @Override
    public boolean createOrUpdateLbRule(CreateOrUpdateNetrisLoadBalancerRuleCommand cmd) {
        boolean isVpc = cmd.isVpc();
        Long networkResourceId = cmd.getId();
        String networkResourceName = cmd.getName();
        Long domainId = cmd.getDomainId();
        Long accountId = cmd.getAccountId();
        Long zoneId = cmd.getZoneId();
        Long lbId = cmd.getLbId();
        String publicIp = cmd.getPublicIp();
        List<NetrisLbBackend> lbBackends = cmd.getLbBackends();

        try {
            String resourcePrefix = isVpc ? "V" : "N";
            String netrisResourceName = String.format("D%s-A%s-Z%s-%s%s-%s", domainId, accountId, zoneId, resourcePrefix, networkResourceId, networkResourceName);
            VPCListing vpcResource = getNetrisVpcResource(netrisResourceName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisResourceName, tenantId);
                return false;
            }
            createLBSubnet(cmd, publicIp + "/32", vpcResource.getId());

            String suffix = String.format("LB%s", lbId);
            String lbName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.LB, suffix);
            Pair<Boolean, List<BigDecimal>> resultAndMatchingLbId = getMatchingLbRule(lbName, vpcResource.getName());
            Boolean result = resultAndMatchingLbId.first();
            List<BigDecimal> matchingLbId = resultAndMatchingLbId.second();
            if (Boolean.FALSE.equals(result)) {
                logger.warn("Could not find the Netris LB rule with name {}", lbName);
            }
            boolean updateRule = !matchingLbId.isEmpty();
            L4lbAddOrUpdateItem l4lbAddItem = getL4LbRule(cmd, vpcResource, lbName, publicIp, lbBackends, updateRule);
            L4LoadBalancerApi loadBalancerApi = apiClient.getApiStubForMethod(L4LoadBalancerApi.class);
            boolean success;
            L4LbEditResponse editResponse = null;
            ResAddEditBody createResponse = null;
            if (updateRule) {
                editResponse = loadBalancerApi.apiV2L4lbIdPut((L4LbEditItem) l4lbAddItem, matchingLbId.get(0).intValue());
                success = editResponse.isIsSuccess();
            } else {
                createResponse = loadBalancerApi.apiV2L4lbPost((L4LbAddItem) l4lbAddItem);
                success = createResponse.isIsSuccess();
            }
            if (ObjectUtils.allNull(editResponse, createResponse) || Boolean.FALSE.equals(success)) {
                throw new CloudRuntimeException(String.format("Failed to %s Netris LB rule", updateRule ? "update" : "create"));
            }
            if (Objects.nonNull(cmd.getCidrList()) && !cmd.getCidrList().isEmpty()) {
                applyAclRulesForLb(cmd, lbName);
            }
        } catch (ApiException e) {
            logAndThrowException("Failed to create Netris load balancer rule", e);
        }
        return true;
    }

    private void applyAclRulesForLb(CreateOrUpdateNetrisLoadBalancerRuleCommand cmd, String lbName) {
        // Add deny all rule first
        addOrUpdateAclRule(createNetrisACLRuleCommand(cmd, lbName, "ANY",
                NetrisNetworkRule.NetrisRuleAction.DENY.name().toLowerCase(Locale.ROOT), 0), true);
        AtomicInteger cidrIndex = new AtomicInteger(1);
        for (String cidr : cmd.getCidrList().split(" ")) {
            try {
                addOrUpdateAclRule(createNetrisACLRuleCommand(cmd, lbName, cidr,
                        NetrisNetworkRule.NetrisRuleAction.PERMIT.name().toLowerCase(Locale.ROOT),
                        cidrIndex.getAndIncrement()), true);
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("Failed to add Netris ACL rule for LB CIDR %s", cidr), e);
            }
        }
    }

    private CreateOrUpdateNetrisACLCommand createNetrisACLRuleCommand(CreateOrUpdateNetrisLoadBalancerRuleCommand cmd, String netrisLbName, String cidr, String action, int index) {
        Long zoneId = cmd.getZoneId();
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        String networkName = null;
        Long networkId = null;
        String vpcName = null;
        Long vpcId = null;
        boolean isVpc = cmd.isVpc();
        if (isVpc) {
            vpcId = cmd.getId();
            vpcName = cmd.getName();
        } else {
            networkName = cmd.getName();
            networkId = cmd.getId();
        }
        String destinationPrefix = cmd.getPublicIp() + "/32";
        String srcPort = cmd.getPublicPort();
        String dstPort = cmd.getPublicPort();
        CreateOrUpdateNetrisACLCommand aclCommand = new CreateOrUpdateNetrisACLCommand(zoneId, accountId, domainId, networkName, networkId,
                vpcName, vpcId, Objects.nonNull(vpcId), action, NetrisServiceImpl.getPrefix(cidr), NetrisServiceImpl.getPrefix(destinationPrefix),
                Integer.parseInt(srcPort), Integer.parseInt(dstPort), cmd.getProtocol());
        String aclName;
        if (isVpc) {
            aclName =  String.format("V%s-LBACL%s-%s", vpcId, index, cmd.getRuleName());
        } else {
            aclName = String.format("N%s-LBACL%s-%s", networkId, index, cmd.getRuleName());
        }
        String netrisAclName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.ACL, aclName);
        aclCommand.setNetrisAclName(netrisAclName);
        aclCommand.setReason(String.format("ACL Rule for CIDR %s of LB %s ", aclName, netrisLbName));
        return aclCommand;
    }

    private L4lbAddOrUpdateItem getL4LbRule(CreateOrUpdateNetrisLoadBalancerRuleCommand cmd, VPCListing vpcResource, String lbName,
                                            String publicIp, List<NetrisLbBackend> lbBackends, boolean updateRule) {
        L4lbAddOrUpdateItem l4lbAddItem = updateRule ? new L4LbEditItem() : new L4LbAddItem();
        try {
            l4lbAddItem.setName(lbName);

            String protocol = cmd.getProtocol().toUpperCase(Locale.ROOT);
            if (!Arrays.asList("TCP", "UDP").contains(protocol)) {
                throw new CloudRuntimeException("Invalid protocol " + protocol);
            }
            l4lbAddItem.setProtocol(cmd.getProtocol().toUpperCase(Locale.ROOT));
            L4LBSite site = new L4LBSite();
            site.setId(siteId);
            site.setName(siteName);
            l4lbAddItem.setSite(site);
            l4lbAddItem.setSiteID(new BigDecimal(siteId));

            L4LbTenant tenant = new L4LbTenant();
            tenant.setId(tenantId);
            tenant.setName(tenantName);
            l4lbAddItem.setTenant(tenant);

            L4LbVpc vpc = new L4LbVpc();
            vpc.setId(vpcResource.getId());
            l4lbAddItem.setVpc(vpc);

            l4lbAddItem.setAutomatic(false);
            l4lbAddItem.setIpFamily(NetUtils.isIpv4(publicIp) ? L4lbAddOrUpdateItem.IpFamilyEnum.IPv4 : L4lbAddOrUpdateItem.IpFamilyEnum.IPv6);
            l4lbAddItem.setIp(publicIp);
            l4lbAddItem.setStatus("enable");

            List<L4LoadBalancerBackendItem> backends = new ArrayList<>();
            for (NetrisLbBackend backend : lbBackends) {
                L4LoadBalancerBackendItem backendItem = new L4LoadBalancerBackendItem();
                backendItem.setIp(backend.getVmIp());
                backendItem.setPort(backend.getPort());
                backends.add(backendItem);
            }
            l4lbAddItem.setBackend(backends);
            l4lbAddItem.setPort(Integer.valueOf(cmd.getPublicPort()));
            l4lbAddItem.setHealthCheck(L4lbAddOrUpdateItem.HealthCheckEnum.NONE);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to create Netris load balancer rule", e);
        }
        return  l4lbAddItem;
    }

    @Override
    public boolean deleteLbRule(DeleteNetrisLoadBalancerRuleCommand cmd) {
        boolean isVpc = cmd.isVpc();
        String vpcName = null;
        String networkName = null;
        Long vpcId = null;
        Long networkId = null;
        if (isVpc) {
            vpcName = cmd.getName();
            vpcId = cmd.getId();
        } else {
            networkName = cmd.getName();
            networkId = cmd.getId();
        }
        Long lbId = cmd.getLbId();
        String cidrList = cmd.getCidrList();
        try {
            String suffix = getNetrisVpcNameSuffix(vpcId, vpcName, networkId, networkName, isVpc);
            String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffix);
            suffix = String.format("LB%s", lbId);
            String lbName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.LB, suffix);
            Pair<Boolean, List<BigDecimal>> resultAndMatchingLbId = getMatchingLbRule(lbName, netrisVpcName);
            Boolean result = resultAndMatchingLbId.first();
            List<BigDecimal> matchingLbId = resultAndMatchingLbId.second();
            if (!result) {
                logger.error("Could not find the Netris LB rule with name {}", lbName);
                return false;
            }
            if (matchingLbId.isEmpty()) {
                logger.warn("There doesn't seem to be any LB rule on Netris matching {}", lbName);
                return true;
            }

            L4LoadBalancerApi lbApi = apiClient.getApiStubForMethod(L4LoadBalancerApi.class);
            lbApi.apiV2L4lbIdDelete(matchingLbId.get(0).intValue());
            if (Objects.nonNull(cidrList)) {
                deleteAclRulesForLb(cmd);
            }
        } catch (ApiException e) {
            logAndThrowException("Failed to delete Netris load balancer rule", e);
        }
        return true;
    }

    private void deleteAclRulesForLb(DeleteNetrisLoadBalancerRuleCommand cmd) {
        // delete the deny rule
        deleteAclRule(deleteNetrisACLCommand(cmd, 0), true);
        AtomicInteger cidrIndex = new AtomicInteger(1);
        for (String cidr : cmd.getCidrList().split(" ")) {
            try {
                deleteAclRule(deleteNetrisACLCommand(cmd, cidrIndex.getAndIncrement()), true);
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("Failed to delete Netris ACL rule for LB CIDR %s", cidr), e);
            }
        }
    }

    private DeleteNetrisACLCommand deleteNetrisACLCommand(DeleteNetrisLoadBalancerRuleCommand cmd, int index) {
        Long zoneId = cmd.getZoneId();
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        String networkName = null;
        Long networkId = null;
        String vpcName = null;
        Long vpcId = null;
        boolean isVpc = cmd.isVpc();
        if (isVpc) {
            vpcId = cmd.getId();
            vpcName = cmd.getName();
        } else {
            networkName = cmd.getName();
            networkId = cmd.getId();
        }
        DeleteNetrisACLCommand deleteAclCommand = new DeleteNetrisACLCommand(zoneId, accountId, domainId, networkName, networkId, isVpc, vpcId, vpcName);
        String aclName;
        if (isVpc) {
            aclName =  String.format("V%s-LBACL%s-%s", vpcId, index, cmd.getRuleName());
        } else {
            aclName = String.format("N%s-LBACL%s-%s", networkId, index, cmd.getRuleName());
        }
        String netrisAclName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.ACL, aclName);
        deleteAclCommand.setAclRuleNames(Collections.singletonList(netrisAclName));
        return deleteAclCommand;
    }

    private Pair<Boolean, List<BigDecimal>> getMatchingLbRule(String lbName, String vpcName) {
        try {
            VPCListing vpcResource = getVpcByNameAndTenant(vpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", vpcName, tenantId);
                return new Pair<>(false, Collections.emptyList());
            }
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(vpcResource.getId());
            FilterBySites siteFilter = new FilterBySites();
            siteFilter.add(siteId);
            L4LoadBalancerApi lbApi = apiClient.getApiStubForMethod(L4LoadBalancerApi.class);
            L4lbresBody lbGetResponse = lbApi.apiV2L4lbGet(siteFilter, vpcFilter);
            if (lbGetResponse == null || !lbGetResponse.isIsSuccess()) {
                logger.warn("No LB rules were found to be present for the specific Netris VPC resource {}." +
                        " Netris LB rules may have been deleted out of band.", vpcName);
                return new Pair<>(true, Collections.emptyList());
            }
            List<L4LoadBalancerItem> lbList = lbGetResponse.getData();
            return new Pair<>(true, lbList.stream()
                    .filter(lb -> lbName.equals(lb.getName()))
                    .map(acl -> BigDecimal.valueOf(acl.getId()))
                    .collect(Collectors.toList()));
        } catch (ApiException e) {
            logAndThrowException("Failed to retrieve Netris LB rules", e);
        }
        return new Pair<>(true, Collections.emptyList());
    }

    private List<RoutesGetBody> listStaticRoutes(Integer netrisVpcId, String prefix, String nextHop) {
        try {
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(netrisVpcId);
            FilterBySites sitesFilter = new FilterBySites();
            sitesFilter.add(siteId);
            RoutesApi routesApi = apiClient.getApiStubForMethod(RoutesApi.class);
            RoutesResponseGetOk routesResponseGetOk = routesApi.apiRoutesGet(sitesFilter, vpcFilter);
            if (Objects.isNull(routesResponseGetOk) || Boolean.FALSE.equals(routesResponseGetOk.isIsSuccess())) {
                logger.warn("Failed to retrieve static routes");
                return null;
            }
            List<RoutesGetBody> routesList = routesResponseGetOk.getData();
            return routesList.stream()
                    .filter(x -> (Objects.isNull(prefix) || x.getName().equals(prefix)) &&
                            (Objects.isNull(nextHop) || x.getNextHop().equals(nextHop)))
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris static routes", e);
        }
        return null;
    }

    private Pair<Boolean, RoutesGetBody> staticRouteExists(Integer netrisVpcId, String prefix, String nextHop, String description) {
        List<RoutesGetBody> staticRoutes = listStaticRoutes(netrisVpcId, prefix, nextHop);
        if (staticRoutes == null) {
            return new Pair<>(false, null);
        }
        return new Pair<>(!staticRoutes.isEmpty(), staticRoutes.isEmpty() ? null : staticRoutes.get(0));
    }

    public void deleteNatRule(String natRuleName, Integer snatRuleId, String netrisVpcName) {
        logger.debug("Deleting NAT rule on Netris: {} for VPC {}", natRuleName, netrisVpcName);
        try {
            NatApi natApi = apiClient.getApiStubForMethod(NatApi.class);
            natApi.apiV2NatIdDelete(snatRuleId);
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to delete NAT rule: %s for VPC: %s", natRuleName, netrisVpcName), e);
        }
    }

    private void deleteVpcIpamAllocationInternal(VPCListing vpcResource, String allocationName) {
        logger.debug("Deleting Netris VPC IPAM Allocation {} for VPC {}", allocationName, vpcResource.getName());
        try {
            VpcApi vpcApi = apiClient.getApiStubForMethod(VpcApi.class);
            VPCResponseResourceOK vpcResourcesResponse = vpcApi.apiV2VpcVpcIdResourcesGet(vpcResource.getId());
            VPCResourceIpam vpcAllocationResource = getVpcAllocationResource(vpcResourcesResponse, allocationName);
            if (Objects.isNull(vpcAllocationResource)) {
                logger.info("No VPC IPAM Allocation found for VPC {}", allocationName);
                return;
            }
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            logger.debug("Removing the IPAM allocation {} with ID {}", vpcAllocationResource.getName(), vpcAllocationResource.getId());
            ipamApi.apiV2IpamTypeIdDelete("allocation", vpcAllocationResource.getId());
        } catch (ApiException e) {
            logAndThrowException(String.format("Error removing IPAM Allocation %s for VPC %s", allocationName, vpcResource.getName()), e);
        }
    }

    private VPCResourceIpam getVpcAllocationResource(VPCResponseResourceOK vpcResourcesResponse, String allocationName) {
        VPCResource resource = vpcResourcesResponse.getData().get(0);
        List<VPCResourceIpam> vpcAllocations = resource.getAllocation();
        if (CollectionUtils.isNotEmpty(vpcAllocations)) {
            vpcAllocations = vpcAllocations.stream().filter(x -> x.getName().equalsIgnoreCase(allocationName)).collect(Collectors.toList());
            return CollectionUtils.isNotEmpty(vpcAllocations) ? vpcAllocations.get(0) : null;
        }
        return null;
    }

    private VPCListing getVpcByNameAndTenant(String vpcName) {
        try {
            List<VPCListing> vpcListings = listVPCs();
            List<VPCListing> vpcs = vpcListings.stream()
                    .filter(x -> x.getName().equals(vpcName) && x.getAdminTenant().getId().equals(tenantId))
                    .collect(Collectors.toList());
            return vpcs.isEmpty() ? null : vpcs.get(0);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Error getting VPC %s information: %s", vpcName, e.getMessage()), e);
        }
    }

    private VPCResponseObjectOK deleteVpcInternal(VPCListing vpcResource) {
        try {
            VpcApi vpcApi = apiClient.getApiStubForMethod(VpcApi.class);
            logger.debug("Removing the VPC {} with ID {}", vpcResource.getName(), vpcResource.getId());
            return vpcApi.apiV2VpcVpcIdDelete(vpcResource.getId());
        } catch (ApiException e) {
            logAndThrowException(String.format("Error deleting VPC %s: %s", vpcResource.getName(), e.getResponseBody()), e);
            return null;
        }
    }

    @Override
    public boolean deleteVpc(DeleteNetrisVpcCommand cmd) {
        String suffix = String.valueOf(cmd.getId());
        String vpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC);
        String snatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.SNAT, suffix);
        NatGetBody existingNatRule = netrisNatRuleExists(snatRuleName);
        boolean ruleExists = Objects.nonNull(existingNatRule);
        if (ruleExists) {
            deleteNatRule(snatRuleName, existingNatRule.getId(), vpcName);
        }

        VPCListing vpcResource = getVpcByNameAndTenant(vpcName);
        if (vpcResource == null) {
            logger.warn("The Netris VPC resource with name {} and tenant ID {} does not exist, cannot be removed", vpcName, tenantId);
            return true;
        }

        String vpcAllocationName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_ALLOCATION, cmd.getCidr());
        deleteVpcIpamAllocationInternal(vpcResource, vpcAllocationName);
        VPCResponseObjectOK response = deleteVpcInternal(vpcResource);
        return response != null && response.isIsSuccess();
    }

    @Override
    public boolean createVnet(CreateNetrisVnetCommand cmd) {
        String vpcName = cmd.getVpcName();
        Long vpcId = cmd.getVpcId();
        String networkName = cmd.getName();
        Long networkId = cmd.getId();
        String vnetCidr = cmd.getCidr();
        Integer vxlanId = cmd.getVxlanId();
        String netrisTag = cmd.getNetrisTag();
        String netmask = vnetCidr.split("/")[1];
        String netrisGateway = cmd.getGateway() + "/" + netmask;
        String netrisV6Cidr = cmd.getIpv6Cidr();
        boolean isVpc = cmd.isVpc();
        Boolean isGlobalRouting = cmd.isGlobalRouting();

        try {
            String netrisVpcName = getNetrisVpcName(cmd, vpcId, vpcName);
            VPCListing associatedVpc = getNetrisVpcResource(netrisVpcName);
            if (associatedVpc == null) {
                logger.error("Failed to find Netris VPC with name: {}, to create the corresponding vNet for network {}", netrisVpcName, networkName);
                return false;
            }

            String vNetName;
            if (isVpc) {
                vNetName = String.format("V%s-N%s-%s", vpcId, networkId, networkName);
            } else {
                vNetName = String.format("N%s-%s", networkId, networkName);
            }
            String netrisVnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VNET, vNetName) ;
            String netrisSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, String.valueOf(cmd.getVpcId()), vnetCidr) ;

            createIpamSubnetInternal(netrisSubnetName, vnetCidr, SubnetBody.PurposeEnum.COMMON, associatedVpc, isGlobalRouting);
            if (Objects.nonNull(netrisV6Cidr)) {
                String netrisV6IpamAllocationName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_ALLOCATION, netrisV6Cidr);
                String netrisV6SubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET,  String.valueOf(cmd.getVpcId()), netrisV6Cidr) ;
                BigDecimal ipamAllocationId = getIpamAllocationIdByPrefixAndVpc(netrisV6Cidr, associatedVpc);
                if (ipamAllocationId == null) {
                    InlineResponse2004Data createdIpamAllocation = createIpamAllocationInternal(netrisV6IpamAllocationName, netrisV6Cidr, associatedVpc);
                    if (Objects.isNull(createdIpamAllocation)) {
                        throw new CloudRuntimeException(String.format("Failed to create Netris IPAM Allocation %s for VPC %s", netrisV6IpamAllocationName, netrisVpcName));
                    }
                }
                createIpamSubnetInternal(netrisV6SubnetName, netrisV6Cidr, SubnetBody.PurposeEnum.COMMON, associatedVpc, isGlobalRouting);
            }
            logger.debug("Successfully created IPAM Subnet {} for network {} on Netris", netrisSubnetName, networkName);

            VnetResAddBody vnetResponse = createVnetInternal(associatedVpc, netrisVnetName, netrisGateway, netrisV6Cidr, vxlanId, netrisTag);
            if (vnetResponse == null || !vnetResponse.isIsSuccess()) {
                String reason = vnetResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris vNet creation {} failed: {}", vNetName, reason);
                return false;
            }
        } catch (ApiException e) {
            throw new CloudRuntimeException(String.format("Failed to create Netris vNet %s", networkName), e);
        }
        return true;
    }

    @Override
    public boolean updateVnet(UpdateNetrisVnetCommand cmd) {
        String networkName = cmd.getName();
        Long networkId = cmd.getId();
        String prevNetworkName = cmd.getPrevNetworkName();
        String vpcName = cmd.getVpcName();
        Long vpcId = cmd.getVpcId();
        boolean isVpc = cmd.isVpc();

        String netrisVpcName = getNetrisVpcName(cmd, vpcId, vpcName);
        VPCListing associatedVpc = getNetrisVpcResource(netrisVpcName);
        if (associatedVpc == null) {
            logger.error("Failed to find Netris VPC with name: {}, to create the corresponding vNet for network {}", netrisVpcName, networkName);
            return false;
        }

        String vNetName;
        String prevVnetName;
        if (isVpc) {
            vNetName = String.format("V%s-N%s-%s", vpcId, networkId, networkName);
            prevVnetName = String.format("V%s-N%s-%s", vpcId, networkId, prevNetworkName);
        } else {
            vNetName = String.format("N%s-%s", networkId, networkName);
            prevVnetName = String.format("N%s-%s", networkId, prevNetworkName);
        }
        String netrisVnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VNET, vNetName) ;
        String prevNetrisVnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VNET, prevVnetName) ;

        VnetResAddBody response = updateVnetInternal(associatedVpc, netrisVnetName, prevNetrisVnetName);
        if (response == null || !response.isIsSuccess()) {
            String reason = response == null ? "Empty response" : "Operation failed on Netris";
            logger.debug("Netris vNet: {} update failed: {}", vNetName, reason);
            return false;
        }
        return true;
    }

    private VnetResAddBody updateVnetInternal(VPCListing associatedVpc, String netrisVnetName, String prevVnetName) {
        logger.debug("Updating Netris vNet name from {} to {} ", netrisVnetName, prevVnetName);
        try {
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(associatedVpc.getId());
            List<VnetsBody> vnetsList = getVnets(associatedVpc, prevVnetName, vpcFilter);
            if (CollectionUtils.isEmpty(vnetsList)) {
                String errorMsg = String.format("Could not find vNet with name: %s", prevVnetName);
                logger.error(errorMsg);
                throw new CloudRuntimeException(errorMsg);
            }
            VnetsBody vnetsBody = vnetsList.get(0);

            VnetEditBody vnetBody = new VnetEditBody();

            vnetBody.setCustomAnycastMac(vnetBody.getCustomAnycastMac());

            VnetEditBodyGateways gatewayV4 = new VnetEditBodyGateways();
            gatewayV4.prefix(vnetsBody.getGateways().get(0).getPrefix());
            gatewayV4.setDhcpEnabled(false);
            VnetEditBodyDhcp dhcp = new VnetEditBodyDhcp();
            dhcp.setEnd("");
            dhcp.setStart("");
            dhcp.setOptionSet(new VnetAddBodyDhcpOptionSet());
            gatewayV4.setDhcp(dhcp);
            List<VnetEditBodyGateways> gatewaysList = new ArrayList<>();
            gatewaysList.add(gatewayV4);

            if (vnetsBody.getGateways().size() > 1 && Objects.nonNull(vnetsBody.getGateways().get(1))) {
                String netrisV6Gateway = vnetsBody.getGateways().get(1).getPrefix();
                VnetEditBodyGateways gatewayV6 = new VnetEditBodyGateways();
                gatewayV6.prefix(netrisV6Gateway);
                gatewayV6.setDhcpEnabled(false);
                gatewayV6.setDhcp(dhcp);
                gatewaysList.add(gatewayV6);
            }

            vnetBody.setGateways(gatewaysList);
            vnetBody.setGuestTenants(new ArrayList<>());
            vnetBody.setL3vpn(false);
            vnetBody.setName(netrisVnetName);
            vnetBody.setNativeVlan(0);
            vnetBody.setVxlanID(vnetsBody.getVxlanID());
            vnetBody.setPorts(new ArrayList<>());

            IpTreeSubnetSites subnetSites = new IpTreeSubnetSites();
            subnetSites.setId(new BigDecimal(siteId));
            subnetSites.setName(siteName);
            List<IpTreeSubnetSites> subnetSitesList = new ArrayList<>();
            subnetSitesList.add(subnetSites);
            vnetBody.setSites(subnetSitesList);

            vnetBody.setState(VnetEditBody.StateEnum.ACTIVE);

            vnetBody.setTags(new ArrayList<>());

            IpTreeAllocationTenant allocationTenant = new IpTreeAllocationTenant();
            allocationTenant.setId(new BigDecimal(tenantId));
            allocationTenant.setName(tenantName);
            vnetBody.setTenant(allocationTenant);

            vnetBody.setVlan(0);
            vnetBody.setVlanAware(false);
            vnetBody.setVlans("");

            VnetAddBodyVpc vpc = new VnetAddBodyVpc();
            vpc.setName(associatedVpc.getName());
            vpc.setId(associatedVpc.getId());
            vnetBody.setVpc(vpc);

            vnetBody.setTags(vnetsBody.getTags());

            VNetApi vnetApi = apiClient.getApiStubForMethod(VNetApi.class);
            return vnetApi.apiV2VnetIdPut(vnetBody, vnetsBody.getId().intValue());
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris vNet %s for VPC %s", netrisVnetName, associatedVpc.getName()), e);
            return null;
        }
    }

    @Override
    public boolean deleteVnet(DeleteNetrisVnetCommand cmd) {
        String vpcName = cmd.getVpcName();
        Long vpcId = cmd.getVpcId();
        String networkName = cmd.getName();
        Long networkId = cmd.getId();
        boolean isVpc = cmd.isVpc();
        String vnetCidr = cmd.getVNetCidr();
        String vnetV6Cidr = cmd.getvNetV6Cidr();
        try {
            String netrisVpcName = getNetrisVpcName(cmd, vpcId, vpcName);
            VPCListing associatedVpc = getNetrisVpcResource(netrisVpcName);
            if (associatedVpc == null) {
                logger.error("Failed to find Netris VPC with name: {}, to create the corresponding vNet for network {}", netrisVpcName, networkName);
                return false;
            }

            String vNetName;
            if (isVpc) {
                vNetName = String.format("V%s-N%s-%s", vpcId, networkId, networkName);
            } else {
                vNetName = String.format("N%s-%s", networkId, networkName);
            }

            String netrisVnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VNET, vNetName) ;
            String netrisSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, String.valueOf(cmd.getVpcId()), vnetCidr);
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(associatedVpc.getId());
            deleteVnetInternal(associatedVpc, vpcFilter, netrisVnetName, vNetName);

            logger.debug("Successfully deleted vNet {}", vNetName);
            deleteSubnetInternal(vpcFilter, netrisVnetName, netrisSubnetName);
            if (Objects.nonNull(vnetV6Cidr)) {
                String netrisV6IpamAllocationName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_ALLOCATION, vnetV6Cidr);
                String netrisV6SubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, String.valueOf(cmd.getVpcId()), vnetV6Cidr);
                deleteSubnetInternal(vpcFilter, netrisVnetName, netrisV6SubnetName);
                deleteVpcIpamAllocationInternal(associatedVpc, netrisV6IpamAllocationName);
            }

        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to delete Netris vNet %s", networkName), e);
        }
        return true;
    }

    protected VPCListing getSystemVpc() throws ApiException {
        List<VPCListing> systemVpcList = listVPCs().stream().filter(VPCListing::isIsSystem).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(systemVpcList)) {
            String msg = "Cannot find any system VPC";
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return systemVpcList.get(0);
    }

    private BigDecimal getIpamAllocationIdByPrefixAndVpc(String superCidrPrefix, VPCListing vpc) throws ApiException {
        IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
        FilterBySites filterBySites = new FilterBySites();
        filterBySites.add(siteId);
        FilterByVpc filterByVpc = new FilterByVpc();
        filterByVpc.add(vpc.getId());
        IpTree ipamTree = ipamApi.apiV2IpamGet(filterBySites, filterByVpc);
        List<IpTreeAllocation> superCidrList = ipamTree.getData().stream()
                .filter(x -> x.getPrefix().equals(superCidrPrefix) || isAllocationPartOfBiggerAllocation(x.getPrefix(), superCidrPrefix))
                .collect(Collectors.toList());
        return CollectionUtils.isEmpty(superCidrList) ? null : superCidrList.get(0).getId();
    }

    private boolean isAllocationPartOfBiggerAllocation(String netrisAllocation, String providedAllocation) {
        IPAddress biggerAllocation = new IPAddressString(netrisAllocation).getAddress();
        IPAddress smallerAllocation = new IPAddressString(providedAllocation).getAddress();

        return biggerAllocation.contains(smallerAllocation);

    }

    private IpTreeSubnet getIpamSubnetByAllocationAndPrefixAndPurposeAndVpc(BigDecimal ipamAllocationId, String exactCidr, IpTreeSubnet.PurposeEnum purpose, VPCListing vpc) throws ApiException {
        IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
        FilterByVpc filterByVpc = new FilterByVpc();
        filterByVpc.add(vpc.getId());
        SubnetResBody subnetResBody = ipamApi.apiV2IpamSubnetsGet(filterByVpc);
        List<IpTreeSubnet> exactSubnetList = subnetResBody.getData().stream()
                .filter(x -> ipamAllocationId != null ?
                        x.getAllocationID().equals(ipamAllocationId) && x.getPrefix().equals(exactCidr) && x.getPurpose() == purpose :
                        x.getPrefix().equals(exactCidr) && x.getPurpose() == purpose)
                .collect(Collectors.toList());
        return CollectionUtils.isEmpty(exactSubnetList) ? null : exactSubnetList.get(0);
    }

    @Override
    public boolean setupZoneLevelPublicRange(SetupNetrisPublicRangeCommand cmd) {
        String superCidr = cmd.getSuperCidr();
        String exactCidr = cmd.getExactCidr();
        try {
            VPCListing systemVpc = getSystemVpc();
            logger.debug("Checking if the Netris Public Super CIDR {} exists", superCidr);
            BigDecimal ipamAllocationId = getIpamAllocationIdByPrefixAndVpc(superCidr, systemVpc);
            if (ipamAllocationId == null) {
                String ipamName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_ALLOCATION, superCidr);
                InlineResponse2004Data ipamAllocation = createIpamAllocationInternal(ipamName, superCidr, systemVpc);
                if (ipamAllocation == null) {
                    String msg = String.format("Could not create the zone level super CIDR %s for the system VPC", superCidr);
                    logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
                ipamAllocationId = new BigDecimal(ipamAllocation.getId());
            }
            IpTreeSubnet exactSubnet = getIpamSubnetByAllocationAndPrefixAndPurposeAndVpc(ipamAllocationId, exactCidr, IpTreeSubnet.PurposeEnum.COMMON, systemVpc);
            if (exactSubnet == null) {
                String ipamSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, exactCidr);
                createIpamSubnetInternal(ipamSubnetName, exactCidr, SubnetBody.PurposeEnum.COMMON, systemVpc, null);
            }
        } catch (ApiException e) {
            String msg = String.format("Error setting up the Netris Public Range %s on super CIDR %s", exactCidr, superCidr);
            logAndThrowException(msg, e);
            return false;
        }
        return true;
    }

    private boolean createOrUpdateNatRuleInternal(CreateOrUpdateNetrisNatCommand cmd) {
        String ruleName = cmd.getNatRuleName();
        long vpcId = cmd.getVpcId();
        Long networkId = cmd.getId();
        String networkName = cmd.getName();
        String vpcName = cmd.getVpcName();
        String vpcCidr = cmd.getVpcCidr();
        boolean isVpc = cmd.isVpc();
        NatPostBody.ActionEnum action = getNatActionFromRuleType(cmd.getNatRuleType());
        NatPostBody.ProtocolEnum protocol = getProtocolFromString(cmd.getProtocol());
        NatPostBody.StateEnum state = getStateFromString(cmd.getState());

        String vNetName = isVpc ?
                String.format("V%s-N%s-%s", vpcId, networkId, networkName) :
                String.format("N%s-%s", networkId, networkName);
        String netrisVpcName = getNetrisVpcName(cmd, vpcId, vpcName);
        VPCListing vpcResource = getNetrisVpcResource(netrisVpcName);
        if (vpcResource == null) {
            logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
            return false;
        }

        String targetIpSubnet = null;
        if (NatPostBody.ActionEnum.SNAT == action) {
            targetIpSubnet = cmd.getNatIp() + "/32";
        } else if (NatPostBody.ActionEnum.DNAT == action) {
            targetIpSubnet = cmd.getDestinationAddress() + "/32";
        }

        if (StringUtils.isNotBlank(targetIpSubnet) && existsDestinationSubnet(targetIpSubnet)) {
            logger.debug("Creating subnet with NAT purpose for {}}", targetIpSubnet);
            createNatSubnet(cmd, targetIpSubnet, vpcResource.getId());
        }

        NatGetBody existingNatRule = netrisNatRuleExists(ruleName);
        boolean ruleExists = Objects.nonNull(existingNatRule);
        if (!ruleExists) {
            String destinationAddress = action == NatPostBody.ActionEnum.SNAT ? ANY_IP : cmd.getDestinationAddress() + "/32";
            String destinationPort = cmd.getDestinationPort();
            String sourceAddress = action == NatPostBody.ActionEnum.SNAT ? vpcCidr : ANY_IP;
            String sourcePort = "1-65535";
            String snatToIp = action == NatPostBody.ActionEnum.SNAT ? targetIpSubnet : null;
            String dnatToIp = action == NatPostBody.ActionEnum.DNAT ? cmd.getSourceAddress() + "/32" : null;
            String dnatToPort = action == NatPostBody.ActionEnum.DNAT ? cmd.getSourcePort() : null;
            return createNatRuleInternal(ruleName, action, protocol, state, destinationAddress, destinationPort,
                    sourceAddress, sourcePort, snatToIp, dnatToIp, dnatToPort, netrisVpcName, networkName, vNetName);
        } else if (NatPostBody.ActionEnum.SNAT == action) {
            return updateSnatRuleInternal(ruleName, targetIpSubnet, netrisVpcName, networkName, vNetName, existingNatRule.getId(), vpcCidr);
        }
        return true;
    }

    private NatPostBody.StateEnum getStateFromString(String state) {
        return NatPostBody.StateEnum.fromValue(state);
    }

    private NatPostBody.ActionEnum getNatActionFromRuleType(String natRuleType) {
        return NatPostBody.ActionEnum.fromValue(natRuleType);
    }

    @Override
    public boolean createOrUpdateSNATRule(CreateOrUpdateNetrisNatCommand cmd) {
        return createOrUpdateNatRuleInternal(cmd);
    }

    private boolean existsDestinationSubnet(String destinationSubnet) {
        try {
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(getSystemVpc().getId());
            List<IpTreeSubnet> targetSubnetList = getSubnet(vpcFilter, destinationSubnet);
            return targetSubnetList != null;
        } catch (ApiException e) {
            logAndThrowException(String.format("Error checking if subnet %s exists: %s", destinationSubnet, e.getMessage()), e);
            return false;
        }
    }

    @Override
    public boolean createStaticNatRule(CreateOrUpdateNetrisNatCommand cmd) {
        String staticNatRuleName = cmd.getNatRuleName();
        String natIP = cmd.getNatIp() + "/32";
        String vmIp = cmd.getVmIp() + "/32";
        String vpcName = cmd.getVpcName();
        String vpcCidr = cmd.getVpcCidr();
        Long vpcId = cmd.getVpcId();
        Long networkId = cmd.getId();
        String networkName = cmd.getName();
        boolean isVpc = cmd.isVpc();

        try {
            String netrisVpcName = getNetrisVpcName(cmd, vpcId, vpcName);
            VPCListing vpcResource = getNetrisVpcResource(netrisVpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
                return false;
            }
            // Create a /32 subnet for the DNAT IP
            createNatSubnet(cmd, natIP, vpcResource.getId());
            NatApi natApi = apiClient.getApiStubForMethod(NatApi.class);
            NatPostBody natBody = new NatPostBody();
            natBody.setAction(NatPostBody.ActionEnum.DNAT);
            natBody.setDestinationAddress(natIP);
            natBody.setName(staticNatRuleName);
            natBody.setProtocol(NatPostBody.ProtocolEnum.ALL);
            natBody.setState(NatPostBody.StateEnum.ENABLED);
            natBody.setComment(String.format("Static NAT rule for %s", netrisVpcName));

            NatBodySiteSite site = new NatBodySiteSite();
            site.setId(siteId);
            site.setName(siteName);
            natBody.setSite(site);
            natBody.setSourceAddress(ANY_IP);
            natBody.setDnatToIP(vmIp);

            NatBodyVpcVpc vpc = new NatBodyVpcVpc();
            vpc.setId(vpcResource.getId());
            vpc.setName(vpcResource.getName());
            natBody.setVpc(vpc);

            InlineResponse20015 natResponse = natApi.apiV2NatPost(natBody);
            if (natResponse == null || !natResponse.isIsSuccess()) {
                String reason = natResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris static NAT (DNAT) rule creation failed for netris VPC - {}: {}", netrisVpcName, reason);
                throw new CloudRuntimeException(reason);
            }
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to create Static NAT (DNAT) rule for network : %s", Objects.nonNull(vpcName) ? vpcName : networkName), e);
        }
        return true;
    }

    private void createNatSubnet(NetrisCommand cmd, String natIp, Integer netrisVpcId) {
        try {
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(netrisVpcId);
            String netrisSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, String.valueOf(((CreateOrUpdateNetrisNatCommand)cmd).getVpcId()), natIp);
            List<IpTreeSubnet> matchedSubnets = getSubnet(vpcFilter, netrisSubnetName);
            if (matchedSubnets.isEmpty()) {
                VPCListing systemVpc = getSystemVpc();
                createIpamSubnetInternal(netrisSubnetName, natIp, SubnetBody.PurposeEnum.NAT, systemVpc, null);
            } else {
                IpTreeSubnet existingSubnet = matchedSubnets.stream().filter(x -> x.getPrefix().equals(natIp)).collect(Collectors.toList()).get(0);
                if (existingSubnet.getPurpose() != IpTreeSubnet.PurposeEnum.NAT) {
                    VPCListing systemVpc = getSystemVpc();
                    logger.debug("Subnet: {} already exists, but purpose is not NAT, updating its purpose to 'nat'", natIp);
                    updateIpamSubnetInternal(existingSubnet.getId().intValue(), netrisSubnetName, natIp, SubnetBody.PurposeEnum.NAT, systemVpc, null);
                }
            }
        } catch (ApiException e) {
            throw new CloudRuntimeException(String.format("Failed to create subnet for %s with NAT purpose", natIp));
        }
    }

    private void createLBSubnet(NetrisCommand cmd, String lbIp, Integer netrisVpcId) {
        try {
            FilterByVpc vpcFilter = new FilterByVpc();
            vpcFilter.add(netrisVpcId);
            String netrisSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd,
                    NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET,
                    String.valueOf(cmd.getId()), lbIp);
            List<IpTreeSubnet> matchedSubnets = getSubnet(vpcFilter, netrisSubnetName);
            VPCListing systemVpc = getSystemVpc();
            if (matchedSubnets.isEmpty()) {
                createIpamSubnetInternal(netrisSubnetName, lbIp, SubnetBody.PurposeEnum.LOAD_BALANCER, systemVpc, null);
            } else if (IpTreeSubnet.PurposeEnum.LOAD_BALANCER != matchedSubnets.get(0).getPurpose()){
                logger.debug("Updating existing NAT subnet {} to have load balancer purpose", netrisSubnetName);
                updateIpamSubnetInternal(matchedSubnets.get(0).getId().intValue(), netrisSubnetName, lbIp, SubnetBody.PurposeEnum.LOAD_BALANCER, systemVpc, null);
            }
            logger.debug("LB subnet: {} already exists", netrisSubnetName);
        } catch (ApiException e) {
            throw new CloudRuntimeException(String.format("Failed to create subnet for %s with LB purpose", lbIp));
        }
    }

    private NatPostBody.ProtocolEnum getProtocolFromString(String protocol) {
        return NatPostBody.ProtocolEnum.fromValue(protocol);
    }

    private NatPostBody createNatRulePostBody(String ruleName, NatPostBody.ActionEnum action, NatPostBody.ProtocolEnum protocol, NatPostBody.StateEnum state,
                                              String destinationAddress, String destinationPort,
                                              String sourceAddress, String sourcePort,
                                              String dnatToIp, String dnatToPort,
                                              String netrisVpcName, String snatIP, String comment) {
        NatPostBody natBody = new NatPostBody();
        natBody.setAction(action);
        natBody.setName(ruleName);
        natBody.setProtocol(protocol);
        natBody.setState(state);
        if (StringUtils.isNotBlank(comment)) {
            natBody.setComment(comment);
        }

        natBody.setDestinationAddress(destinationAddress);
        if (StringUtils.isNotBlank(destinationPort)) {
            natBody.setDestinationPort(destinationPort);
        }

        if (StringUtils.isNotBlank(sourceAddress)) {
            natBody.setSourceAddress(sourceAddress);
        }
        if (StringUtils.isNotBlank(sourcePort)) {
            natBody.setSourcePort(sourcePort);
        }

        NatBodySiteSite site = new NatBodySiteSite();
        site.setId(siteId);
        site.setName(siteName);
        natBody.setSite(site);

        if (StringUtils.isNotBlank(snatIP)) {
            natBody.setSnatToIP(snatIP);
        }

        if (StringUtils.isNotBlank(dnatToIp)) {
            natBody.setDnatToIP(dnatToIp);
        }
        if (StringUtils.isNotBlank(dnatToPort)) {
            natBody.setDnatToPort(Integer.valueOf(dnatToPort));
        }

        NatBodyVpcVpc vpc = new NatBodyVpcVpc();
        VPCListing vpcResource = getVpcByNameAndTenant(netrisVpcName);
        if (vpcResource == null) {
            logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
            return null;
        }
        vpc.setId(vpcResource.getId());
        vpc.setName(vpcResource.getName());
        natBody.setVpc(vpc);
        return natBody;
    }

    @Override
    public boolean createOrUpdateDNATRule(CreateOrUpdateNetrisNatCommand cmd) {
        return createOrUpdateNatRuleInternal(cmd);
    }

    private boolean createNatRuleInternal(String ruleName, NatPostBody.ActionEnum action, NatPostBody.ProtocolEnum protocol, NatPostBody.StateEnum state,
                                          String destinationAddress, String destinationPort, String sourceAddress, String sourcePort,
                                          String sNatToIp, String dNatToIp, String dNatToPort,
                                          String netrisVpcName, String networkName, String vNetName) {
        try {
            NatApi natApi = apiClient.getApiStubForMethod(NatApi.class);
            String comment = String.format("NAT rule for %s with action %s", netrisVpcName, action.name());
            NatPostBody natBody = createNatRulePostBody(ruleName, action, protocol, state,
                    destinationAddress, destinationPort, sourceAddress, sourcePort,
                    dNatToIp, dNatToPort, netrisVpcName, sNatToIp, comment);
            if (natBody == null) {
                return false;
            }
            InlineResponse20015 natResponse = natApi.apiV2NatPost(natBody);
            if (natResponse == null || !natResponse.isIsSuccess()) {
                String reason = natResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris NAT rule {} creation failed for network(vNet) - {}({}): {}", action.name(), networkName, vNetName, reason);
                throw new CloudRuntimeException(reason);
            }
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to create NAT rule %s for network(vNet): %s(%s)", action.name(), networkName, vNetName), e);
        }
        return true;
    }

    private boolean updateSnatRuleInternal(String snatRuleName, String snatIP, String netrisVpcName, String networkName,
                                           String vNetName, Integer netisSnatId, String vpcCidr) {
        try {
            NatApi natApi = apiClient.getApiStubForMethod(NatApi.class);
            NatPutBody natBody = new NatPutBody();
            natBody.setAction(NatPutBody.ActionEnum.SNAT);
            natBody.setDestinationAddress(ANY_IP);
            natBody.setName(snatRuleName);
            natBody.setProtocol(NatPutBody.ProtocolEnum.ALL);

            NatBodySiteSite site = new NatBodySiteSite();
            site.setId(siteId);
            site.setName(siteName);
            natBody.setSite(site);
            natBody.setSourceAddress(vpcCidr);
            natBody.setSnatToIP(snatIP);

            NatBodyVpcVpc vpc = new NatBodyVpcVpc();
            VPCListing vpcResource = getVpcByNameAndTenant(netrisVpcName);
            if (vpcResource == null) {
                logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
                return false;
            }
            vpc.setId(vpcResource.getId());
            vpc.setName(vpcResource.getName());
            natBody.setVpc(vpc);

            InlineResponse20016 natUpdateResponse = natApi.apiV2NatIdPut(natBody, netisSnatId);
            if (natUpdateResponse == null || !natUpdateResponse.isIsSuccess()) {
                String reason = natUpdateResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("Update of Netris SNAT rule failed for network(vNet) - {}({}): {}", networkName, vNetName, reason);
                throw new CloudRuntimeException(reason);
            }
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to create SNAT rule for network(vNet): %s(%s)", networkName, vNetName), e);
        }
        return true;
    }

    private List<VnetsBody> getVnets(VPCListing associatedVpc, String netrisVnetName, FilterByVpc vpcFilter) {
        // Filter by site not working as expected, so filtering by VPC only and then filtering by name in the code
        try {
            VNetApi vNetApi = apiClient.getApiStubForMethod(VNetApi.class);
            VnetResListBody vnetList = vNetApi.apiV2VnetGet(null, vpcFilter);
            if (vnetList == null || !vnetList.isIsSuccess()) {
                throw new CloudRuntimeException(String.format("Failed to list vNets for the given VPC: %s and site: %s", associatedVpc.getName(), siteName));
            }
            return vnetList.getData().stream().filter(vnet -> vnet.getName().equals(netrisVnetName)).collect(Collectors.toList());
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to get vNets: %s", netrisVnetName), e);
        }
        return Collections.emptyList();
    }

    private void deleteVnetInternal(VPCListing associatedVpc, FilterByVpc vpcFilter, String netrisVnetName, String vNetName) {
        try {
            VNetApi vNetApi = apiClient.getApiStubForMethod(VNetApi.class);
            List<VnetsBody> vnetsList = getVnets(associatedVpc, netrisVnetName, vpcFilter);
            if (CollectionUtils.isEmpty(vnetsList)) {
                logger.debug("vNet: {} for the given VPC: {} appears to already be deleted on Netris", vNetName, associatedVpc.getName());
                return;
            }
            VnetsBody vnetsBody = vnetsList.get(0);

            VnetResDeleteBody deleteVnetResponse = vNetApi.apiV2VnetIdDelete(vnetsBody.getId().intValue());
            if (deleteVnetResponse == null || !deleteVnetResponse.isIsSuccess()) {
                throw new CloudRuntimeException(String.format("Failed to delete vNet: %s", vNetName));
            }
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to delete vNet: %s", netrisVnetName), e);
        }
    }

    private List<IpTreeSubnet> getSubnet(FilterByVpc vpcFilter, String netrisSubnetName) {
        try {
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            SubnetResBody subnetsResponse = ipamApi.apiV2IpamSubnetsGet(vpcFilter);
            List<IpTreeSubnet> subnets = subnetsResponse.getData();
            return subnets.stream().filter(subnet -> subnet.getName().equals(netrisSubnetName)).collect(Collectors.toList());
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to get IPAM subnet: %s", netrisSubnetName), e);
        }
        return new ArrayList<>();
    }

    private void deleteSubnetInternal(FilterByVpc vpcFilter, String netrisVnetName, String netrisSubnetName) {
        try {
            String logString = "";
            if (Objects.nonNull(netrisVnetName)) {
                logString = String.format("for vNet: %s ", netrisVnetName);
            }
            logger.debug("Deleting Netris VPC IPAM Subnet {} {}", netrisSubnetName, logString);
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            List<IpTreeSubnet> matchedSubnets = getSubnet(vpcFilter, netrisSubnetName);
            if (CollectionUtils.isEmpty(matchedSubnets)) {
                logger.debug("IPAM subnet: {} {} appears to already be deleted on Netris", netrisSubnetName, logString);
                return;
            }

            ipamApi.apiV2IpamTypeIdDelete("subnet", matchedSubnets.get(0).getId().intValue());
        } catch (ApiException e) {
            logAndThrowException(String.format("Failed to delete subnet: %s", netrisSubnetName), e);
        }
    }

    private SubnetBody getIpamSubnetBody(VPCListing vpc, SubnetBody.PurposeEnum purpose, String subnetName, String subnetPrefix, Boolean isGlobalRouting) {
        SubnetBody subnetBody = new SubnetBody();
        subnetBody.setName(subnetName);

        AllocationBodyVpc vpcAllocationBody = new AllocationBodyVpc();
        vpcAllocationBody.setName(vpc.getName());
        vpcAllocationBody.setId(vpc.getId());
        subnetBody.setVpc(vpcAllocationBody);

        IpTreeAllocationTenant allocationTenant = new IpTreeAllocationTenant();
        allocationTenant.setId(new BigDecimal(tenantId));
        allocationTenant.setName(tenantName);
        subnetBody.setTenant(allocationTenant);

        IpTreeSubnetSites subnetSites = new IpTreeSubnetSites();
        subnetSites.setId(new BigDecimal(siteId));
        subnetSites.setName(siteName);
        subnetBody.setSites(List.of(subnetSites));

        subnetBody.setPurpose(purpose);
        subnetBody.setPrefix(subnetPrefix);
        if (isGlobalRouting != null) {
            subnetBody.setGlobalRouting(isGlobalRouting);
        }
        return subnetBody;
    }

    private InlineResponse2004Data createIpamSubnetInternal(String subnetName, String subnetPrefix, SubnetBody.PurposeEnum purpose, VPCListing vpc, Boolean isGlobalRouting) {
        logger.debug("Creating Netris IPAM Subnet {} for VPC {}", subnetPrefix, vpc.getName());
        try {

            SubnetBody subnetBody = getIpamSubnetBody(vpc, purpose, subnetName, subnetPrefix, isGlobalRouting);
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            InlineResponse2004 subnetResponse = ipamApi.apiV2IpamSubnetPost(subnetBody);
            if (subnetResponse == null || !subnetResponse.isIsSuccess()) {
                String reason = subnetResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris IPAM Subnet {} creation failed: {}", subnetName, reason);
                throw new CloudRuntimeException(reason);
            }
            return subnetResponse.getData();
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris IPAM Subnet %s for VPC %s", subnetPrefix, vpc.getName()), e);
            return null;
        }
    }

    private InlineResponse2004Data updateIpamSubnetInternal(Integer netrisSubnetId, String subnetName, String subnetPrefix,
                                                            SubnetBody.PurposeEnum purpose, VPCListing vpc, Boolean isGlobalRouting) {
        logger.debug("Updating Netris IPAM Subnet {} for VPC {}", subnetPrefix, vpc.getName());
        try {

            SubnetBody subnetBody = getIpamSubnetBody(vpc, purpose, subnetName, subnetPrefix, isGlobalRouting);
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            InlineResponse2004 subnetResponse = ipamApi.apiV2IpamSubnetIdPut(subnetBody, netrisSubnetId);
            if (subnetResponse == null || !subnetResponse.isIsSuccess()) {
                String reason = subnetResponse == null ? "Empty response" : "Operation failed on Netris";
                logger.debug("The Netris IPAM Subnet {} update failed: {}", subnetName, reason);
                throw new CloudRuntimeException(reason);
            }
            return subnetResponse.getData();
        } catch (ApiException e) {
            logAndThrowException(String.format("Error Updating Netris IPAM Subnet %s for VPC %s", subnetPrefix, vpc.getName()), e);
            return null;
        }
    }

    VnetResAddBody createVnetInternal(VPCListing associatedVpc, String netrisVnetName, String netrisGateway, String netrisV6Cidr, Integer vxlanId, String netrisTag) {
        logger.debug("Creating Netris VPC vNet {} for CIDR {}", netrisVnetName, netrisGateway);
        try {
            VnetAddBody vnetBody = new VnetAddBody();

            vnetBody.setCustomAnycastMac("");

            VnetAddBodyGateways gatewayV4 = new VnetAddBodyGateways();
            gatewayV4.prefix(netrisGateway);
            gatewayV4.setDhcpEnabled(false);
            VnetAddBodyDhcp dhcp = new VnetAddBodyDhcp();
            dhcp.setEnd("");
            dhcp.setStart("");
            dhcp.setOptionSet(new VnetAddBodyDhcpOptionSet());
            gatewayV4.setDhcp(dhcp);
            List<VnetAddBodyGateways> gatewaysList = new ArrayList<>();
            gatewaysList.add(gatewayV4);

            if (Objects.nonNull(netrisV6Cidr)) {
                VnetAddBodyGateways gatewayV6 = new VnetAddBodyGateways();
                gatewayV6.prefix(NetUtils.getIpv6Gateway(netrisV6Cidr));
                gatewayV6.setDhcpEnabled(false);
                gatewayV6.setDhcp(dhcp);
                gatewaysList.add(gatewayV6);
            }

            vnetBody.setGateways(gatewaysList);
            vnetBody.setGuestTenants(new ArrayList<>());
            vnetBody.setL3vpn(false);
            vnetBody.setName(netrisVnetName);
            vnetBody.setNativeVlan(0);
            vnetBody.setVxlanID(vxlanId);
            vnetBody.setPorts(new ArrayList<>());

            IpTreeSubnetSites subnetSites = new IpTreeSubnetSites();
            subnetSites.setId(new BigDecimal(siteId));
            subnetSites.setName(siteName);
            List<IpTreeSubnetSites> subnetSitesList = new ArrayList<>();
            subnetSitesList.add(subnetSites);
            vnetBody.setSites(subnetSitesList);

            vnetBody.setState(VnetAddBody.StateEnum.ACTIVE);

            vnetBody.setTags(new ArrayList<>());

            IpTreeAllocationTenant allocationTenant = new IpTreeAllocationTenant();
            allocationTenant.setId(new BigDecimal(tenantId));
            allocationTenant.setName(tenantName);
            vnetBody.setTenant(allocationTenant);

            vnetBody.setVlan(0);
            vnetBody.setVlanAware(false);
            vnetBody.setVlans("");

            VnetAddBodyVpc vpc = new VnetAddBodyVpc();
            vpc.setName(associatedVpc.getName());
            vpc.setId(associatedVpc.getId());
            vnetBody.setVpc(vpc);

            vnetBody.setTags(Collections.singletonList(netrisTag));

            VNetApi vnetApi = apiClient.getApiStubForMethod(VNetApi.class);
            return vnetApi.apiV2VnetPost(vnetBody);
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris vNet %s for VPC %s", netrisVnetName, associatedVpc.getName()), e);
            return null;
        }
    }

    private String getNetrisVpcNameSuffix(Long vpcId, String vpcName, Long networkId, String networkName, boolean isVpc) {
        String suffix = null;
        if (isVpc) {
            suffix = String.format("%s-%s", vpcId, vpcName);
        } else {
            suffix = String.format("%s-%s", networkId, networkName);
        }
        return suffix;
    }

    private NatGetBody netrisNatRuleExists(String netrisNatRule) {
        try {
            NatApi natApi = apiClient.getApiStubForMethod(NatApi.class);
            //NatResponseGetOk response = natApi.apiV2NatGet(null, Arrays.asList(new BigDecimal(vpcId)));
            NatResponseGetOk response = natApi.apiV2NatGet(null, null);
            if (Objects.isNull(response) || !response.isIsSuccess()) {
                throw new CloudRuntimeException("Failed to list Netris NAT rules");
            }
            List<NatGetBody> data = response.getData().stream().filter(natData -> natData.getName().equals(netrisNatRule)).collect(Collectors.toList());
            if (data.isEmpty()) {
                return null;
            }
            return data.get(0);

        } catch (ApiException e) {
            throw new CloudRuntimeException("Failed to list Netris NAT rules");
        }
    }

    private VPCListing getNetrisVpcResource(String netrisVpcName) {
        VPCListing vpcResource = getVpcByNameAndTenant(netrisVpcName);
        if (vpcResource == null) {
            logger.error("Could not find the Netris VPC resource with name {} and tenant ID {}", netrisVpcName, tenantId);
        }
        return vpcResource;
    }

    private String getNetrisVpcName(NetrisCommand cmd, Long vpcId, String vpcName) {
        String suffix = getNetrisVpcNameSuffix(vpcId, vpcName, cmd.getId(), cmd.getName(), cmd.isVpc());
        return NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffix);
    }

    private void deleteNatSubnet(String netrisSubnetName, Integer netrisVpcId, String natIp) {
        FilterByVpc vpcFilter = new FilterByVpc();
        vpcFilter.add(netrisVpcId);
        deleteSubnetInternal(vpcFilter, null, netrisSubnetName);
    }
}
