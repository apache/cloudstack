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

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import io.netris.ApiClient;
import io.netris.ApiException;
import io.netris.ApiResponse;
import io.netris.api.v1.AuthenticationApi;
import io.netris.api.v1.SitesApi;
import io.netris.api.v1.TenantsApi;
import io.netris.api.v2.IpamApi;
import io.netris.api.v2.VNetApi;
import io.netris.api.v2.VpcApi;
import io.netris.model.AllocationBody;
import io.netris.model.AllocationBodyVpc;
import io.netris.model.GetSiteBody;
import io.netris.model.InlineResponse2004;
import io.netris.model.IpTreeAllocationTenant;
import io.netris.model.IpTreeSubnetSites;
import io.netris.model.SitesResponseOK;
import io.netris.model.SubnetBody;
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
import io.netris.model.VnetResAddBody;
import io.netris.model.response.AuthResponse;
import io.netris.model.response.TenantResponse;
import io.netris.model.response.TenantsResponse;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.resource.NetrisResourceObjectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetrisApiClientImpl implements NetrisApiClient {

    private final Logger logger = LogManager.getLogger(getClass());

    private static ApiClient apiClient;

    private final int siteId;
    private final String siteName;
    private final int tenantId;
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

    private InlineResponse2004 createVpcAllocationInternal(VPCResponseObjectOK createdVpc, String cidr, int adminTenantId,
                                                           String adminTenantName, String netrisIpamAllocationName) {
        logger.debug(String.format("Creating Netris VPC Allocation %s for VPC %s", cidr, createdVpc.getData().getName()));
        try {
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            AllocationBody body = new AllocationBody();
            AllocationBodyVpc allocationBodyVpc = new AllocationBodyVpc();
            allocationBodyVpc.setId(createdVpc.getData().getId());
            allocationBodyVpc.setName(createdVpc.getData().getName());
            body.setVpc(allocationBodyVpc);
            body.setName(netrisIpamAllocationName);
            body.setPrefix(cidr);
            IpTreeAllocationTenant allocationTenant = new IpTreeAllocationTenant();
            allocationTenant.setId(new BigDecimal(adminTenantId));
            allocationTenant.setName(adminTenantName);
            body.setTenant(allocationTenant);
            return ipamApi.apiV2IpamAllocationPost(body);
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris Allocation %s for VPC %s", cidr, createdVpc.getData().getName()), e);
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
        InlineResponse2004 ipamResponse = createVpcAllocationInternal(createdVpc, vpcCidr, tenantId, tenantName, netrisIpamAllocationName);
        if (ipamResponse == null || !ipamResponse.isIsSuccess()) {
            String reason = ipamResponse == null ? "Empty response" : "Operation failed on Netris";
            logger.debug("The Netris Allocation {} for VPC {} creation failed: {}", vpcCidr, cmd.getName(), reason);
            return false;
        }
        logger.debug(String.format("Successfully created VPC %s and its IPAM Allocation %s on Netris", cmd.getName(), vpcCidr));
        return true;
    }

    private void deleteVpcIpamAllocationInternal(VPCListing vpcResource, String vpcCidr) {
        logger.debug(String.format("Deleting Netris VPC IPAM Allocation %s for VPC %s", vpcCidr, vpcResource.getName()));
        try {
            VpcApi vpcApi = apiClient.getApiStubForMethod(VpcApi.class);
            VPCResponseResourceOK vpcResourcesResponse = vpcApi.apiV2VpcVpcIdResourcesGet(vpcResource.getId());
            VPCResourceIpam vpcAllocationResource = getVpcAllocationResource(vpcResourcesResponse);
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            logger.debug("Removing the IPAM allocation {} with ID {}", vpcAllocationResource.getName(), vpcAllocationResource.getId());
            ipamApi.apiV2IpamTypeIdDelete("allocation", vpcAllocationResource.getId());
        } catch (ApiException e) {
            logAndThrowException(String.format("Error removing IPAM Allocation %s for VPC %s", vpcCidr, vpcResource.getName()), e);
        }
    }

    private VPCResourceIpam getVpcAllocationResource(VPCResponseResourceOK vpcResourcesResponse) {
        VPCResource resource = vpcResourcesResponse.getData().get(0);
        List<VPCResourceIpam> vpcAllocations = resource.getAllocation();
        if (CollectionUtils.isNotEmpty(vpcAllocations)) {
            if (vpcAllocations.size() > 1) {
                logger.warn("Unexpected VPC allocations size {}, one expected", vpcAllocations.size());
            }
            return vpcAllocations.get(0);
        }
        return null;
    }

    private VPCListing getVpcByNameAndTenant(String vpcName) {
        try {
            List<VPCListing> vpcListings = listVPCs();
            List<VPCListing> vpcs = vpcListings.stream()
                    .filter(x -> x.getName().equals(vpcName) && x.getAdminTenant().getId().equals(tenantId))
                    .collect(Collectors.toList());
            return vpcs.get(0);
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
        String vpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC);
        VPCListing vpcResource = getVpcByNameAndTenant(vpcName);
        if (vpcResource == null) {
            logger.error(String.format("Could not find the Netris VPC resource with name %s and tenant ID %s", vpcName, tenantId));
            return false;
        }
        String vpcCidr = cmd.getCidr();
        deleteVpcIpamAllocationInternal(vpcResource, vpcCidr);
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
        boolean isVpc = cmd.isVpc();

        String suffix = String.format("%s-%s", vpcId, vpcName);
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, suffix);
        VPCListing associatedVpc = getVpcByNameAndTenant(netrisVpcName);
        if (associatedVpc == null) {
            logger.error(String.format("Failed to find Netris VPC with name: %s, to create the corresponding vNet for network %s", vpcName, networkName));
            return false;
        }

        String vNetName;
        if (isVpc) {
            vNetName = String.format("V%s-N%s-%s", vpcId, networkId, networkName);
        } else {
            vNetName = String.format("N%s-%s", networkId, networkName);
        }
        String netrisVnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VNET, vNetName) ;
        String netrisSubnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, vnetCidr) ;

        InlineResponse2004 subnetResponse = createVpcSubnetInternal(associatedVpc, vNetName, vnetCidr, netrisSubnetName);
        if (subnetResponse == null || !subnetResponse.isIsSuccess()) {
            String reason = subnetResponse == null ? "Empty response" : "Operation failed on Netris";
            logger.debug("The Netris Subnet {} for VPC {} for network {} creation failed: {}", vnetCidr, vpcName, networkName, reason);
            return false;
        }
        logger.debug("Successfully created VPC {} and its IPAM Subnet {} on Netris", vpcName, vnetCidr);

        VnetResAddBody vnetResponse = createVnetInternal(associatedVpc, netrisVnetName, vnetCidr);
        if (vnetResponse == null || !vnetResponse.isIsSuccess()) {
            String reason = vnetResponse == null ? "Empty response" : "Operation failed on Netris";
            logger.debug("The Netris vNet creation {} for VPC {} failed: {}", vNetName, vpcName, reason);
            return false;
        }
        return true;
    }

    private InlineResponse2004 createVpcSubnetInternal(VPCListing associatedVpc, String vNetName, String vNetCidr, String netrisSubnetName) {
        logger.debug("Creating Netris VPC Subnet {} for VPC {} for vNet {}", vNetCidr, associatedVpc.getName(), vNetName);
        try {

            SubnetBody subnetBody = new SubnetBody();
            subnetBody.setName(netrisSubnetName);

            AllocationBodyVpc vpcAllocationBody = new AllocationBodyVpc();
            vpcAllocationBody.setName(associatedVpc.getName());
            vpcAllocationBody.setId(associatedVpc.getId());
            subnetBody.setVpc(vpcAllocationBody);

            IpTreeAllocationTenant allocationTenant = new IpTreeAllocationTenant();
            allocationTenant.setId(new BigDecimal(tenantId));
            allocationTenant.setName(tenantName);
            subnetBody.setTenant(allocationTenant);

            IpTreeSubnetSites subnetSites = new IpTreeSubnetSites();
            subnetSites.setId(new BigDecimal(siteId));
            subnetSites.setName(siteName);
            subnetBody.setSites(List.of(subnetSites));

            subnetBody.setPurpose(SubnetBody.PurposeEnum.COMMON);
            subnetBody.setPrefix(vNetCidr);
            IpamApi ipamApi = apiClient.getApiStubForMethod(IpamApi.class);
            return ipamApi.apiV2IpamSubnetPost(subnetBody);
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris Subnet %s for VPC %s", vNetCidr, associatedVpc.getName()), e);
            return null;
        }
    }

    VnetResAddBody createVnetInternal(VPCListing associatedVpc, String netrisVnetName, String vNetCidr) {
        logger.debug("Creating Netris VPC vNet {} for CIDR {}", netrisVnetName, vNetCidr);
        try {
            VnetAddBody vnetBody = new VnetAddBody();

            vnetBody.setCustomAnycastMac("");

            VnetAddBodyGateways gateways = new VnetAddBodyGateways();
            gateways.prefix(vNetCidr);
            gateways.setDhcpEnabled(false);
            VnetAddBodyDhcp dhcp = new VnetAddBodyDhcp();
            dhcp.setEnd("");
            dhcp.setStart("");
            dhcp.setOptionSet(new VnetAddBodyDhcpOptionSet());
            gateways.setDhcp(dhcp);
            List<VnetAddBodyGateways> gatewaysList = new ArrayList<>();
            gatewaysList.add(gateways);
            vnetBody.setGateways(gatewaysList);

            vnetBody.setGuestTenants(new ArrayList<>());
            vnetBody.setL3vpn(false);
            vnetBody.setName(netrisVnetName);
            vnetBody.setNativeVlan(0);
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

            VNetApi vnetApi = apiClient.getApiStubForMethod(VNetApi.class);
            return vnetApi.apiV2VnetPost(vnetBody);
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating Netris vNet %s for VPC %s", netrisVnetName, associatedVpc.getName()), e);
            return null;
        }
    }
}
