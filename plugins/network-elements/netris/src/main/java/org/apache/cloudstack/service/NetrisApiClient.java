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

import com.cloud.network.vpc.StaticRoute;
import io.netris.ApiException;
import io.netris.model.GetSiteBody;
import io.netris.model.VPCListing;
import io.netris.model.response.TenantResponse;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisACLCommand;
import org.apache.cloudstack.agent.api.AddOrUpdateNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisNatCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisACLCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVnetCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.agent.api.ListNetrisStaticRoutesCommand;
import org.apache.cloudstack.agent.api.ReleaseNatIpCommand;
import org.apache.cloudstack.agent.api.SetupNetrisPublicRangeCommand;
import org.apache.cloudstack.agent.api.UpdateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.UpdateNetrisVpcCommand;

import java.util.List;

public interface NetrisApiClient {
    boolean isSessionAlive();
    List<GetSiteBody> listSites();
    List<VPCListing> listVPCs();
    List<TenantResponse> listTenants() throws ApiException;

    /**
     * Create a VPC on CloudStack creates the following Netris resources:
     * - Create a Netris VPC with the VPC name
     * - Create an IPAM Allocation for the created Netris VPC using the Prefix = VPC CIDR
     */
    boolean createVpc(CreateNetrisVpcCommand cmd);

    boolean updateVpc(UpdateNetrisVpcCommand cmd);

    /**
     * Delete a VPC on CloudStack removes the following Netris resources:
     * - Delete the IPAM Allocation for the VPC using the Prefix = VPC CIDR
     * - Delete a Netris VPC with the VPC name
     */
    boolean deleteVpc(DeleteNetrisVpcCommand cmd);

    /**
     * Creation of a VPC network tier creates the following Netris resources:
     * - Creates a Netris IPAM Subnet for the specified network tier's CIDR
     * - Creates a Netris vNet
     */
    boolean createVnet(CreateNetrisVnetCommand cmd);

    boolean updateVnet(UpdateNetrisVnetCommand cmd);

    /**
     * Deletion of a VPC network tier deletes the following Netris resources:
     * - Deletes the Netris IPAM Subnet for the specified network tier's CIDR
     * - Deletes the Netris vNet
     */
    boolean deleteVnet(DeleteNetrisVnetCommand cmd);

    /**
     * Check and create zone level Netris Public range in the following manner:
     * - Check the IPAM allocation for the zone super CIDR. In case it doesn't exist, create it
     * - Check the IPAM subnet for NAT purpose for the range start-end. In case it doesn't exist, create it
     */
    boolean setupZoneLevelPublicRange(SetupNetrisPublicRangeCommand cmd);
    boolean createOrUpdateSNATRule(CreateOrUpdateNetrisNatCommand cmd);
    boolean createOrUpdateDNATRule(CreateOrUpdateNetrisNatCommand cmd);
    boolean createStaticNatRule(CreateOrUpdateNetrisNatCommand cmd);
    boolean deleteNatRule(DeleteNetrisNatRuleCommand cmd);
    boolean addOrUpdateAclRule(CreateOrUpdateNetrisACLCommand cmd, boolean forLb);
    boolean deleteAclRule(DeleteNetrisACLCommand cmd, boolean forLb);
    boolean addOrUpdateStaticRoute(AddOrUpdateNetrisStaticRouteCommand cmd);
    boolean deleteStaticRoute(DeleteNetrisStaticRouteCommand cmd);
    List<StaticRoute> listStaticRoutes(ListNetrisStaticRoutesCommand cmd);
    boolean releaseNatIp(ReleaseNatIpCommand cmd);
    boolean createOrUpdateLbRule(CreateOrUpdateNetrisLoadBalancerRuleCommand cmd);
    boolean deleteLbRule(DeleteNetrisLoadBalancerRuleCommand cmd);
}
