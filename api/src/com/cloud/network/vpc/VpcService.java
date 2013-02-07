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
package com.cloud.network.vpc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.command.user.vpc.ListPrivateGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpc.ListStaticRoutesCmd;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public interface VpcService {

    public VpcOffering getVpcOffering(long vpcOfferingId);

    public VpcOffering createVpcOffering(String name, String displayText, List<String> supportedServices);

    public Vpc getVpc(long vpcId);

    public Vpc getActiveVpc(long vpcId);

    public List<? extends Network> getVpcNetworks(long vpcId);

    Map<Service, Set<Provider>> getVpcOffSvcProvidersMap(long vpcOffId);

    List<? extends VpcOffering> listVpcOfferings(Long id, String name, String displayText, List<String> supportedServicesStr,
            Boolean isDefault, String keyword, String state, Long startIndex, Long pageSizeVal);

    /**
     * @param offId
     * @return
     */
    public boolean deleteVpcOffering(long offId);

    /**
     * @param vpcOffId
     * @param vpcOfferingName
     * @param displayText
     * @param state
     * @return
     */
    public VpcOffering updateVpcOffering(long vpcOffId, String vpcOfferingName, String displayText, String state);

    /**
     * @param zoneId
     * @param vpcOffId
     * @param vpcOwnerId
     * @param vpcName
     * @param displayText
     * @param cidr
     * @param networkDomain TODO
     * @return
     * @throws ResourceAllocationException TODO
     */
    public Vpc createVpc(long zoneId, long vpcOffId, long vpcOwnerId, String vpcName, String displayText, String cidr,
            String networkDomain) throws ResourceAllocationException;

    /**
     * @param vpcId
     * @return
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    public boolean deleteVpc(long vpcId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param vpcId
     * @param vpcName
     * @param displayText
     * @return
     */
    public Vpc updateVpc(long vpcId, String vpcName, String displayText);

    /**
     * @param id
     * @param vpcName
     * @param displayText
     * @param supportedServicesStr
     * @param cidr
     * @param state TODO
     * @param accountName
     * @param domainId
     * @param keyword
     * @param startIndex
     * @param pageSizeVal
     * @param zoneId TODO
     * @param isRecursive TODO
     * @param listAll TODO
     * @param restartRequired TODO
     * @param tags TODO
     * @param projectId TODO
     * @param vpc
     * @return
     */
    public List<? extends Vpc> listVpcs(Long id, String vpcName, String displayText,
            List<String> supportedServicesStr, String cidr, Long vpcOffId, String state, String accountName, Long domainId,
            String keyword, Long startIndex, Long pageSizeVal, Long zoneId, Boolean isRecursive, Boolean listAll,
            Boolean restartRequired, Map<String, String> tags, Long projectId);

    /**
     * @param vpcId
     * @param destroyOnFailure TODO
     * @return
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean startVpc(long vpcId, boolean destroyOnFailure) throws ConcurrentOperationException,
                                                        ResourceUnavailableException, InsufficientCapacityException;

    /**
     * @param vpcId
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean shutdownVpc(long vpcId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param id
     * @return
     * @throws InsufficientCapacityException
     */
    boolean restartVpc(long id) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    PrivateGateway getVpcPrivateGateway(long id);

    /**
     * @param vpcId TODO
     * @param physicalNetworkId
     * @param vlan
     * @param ipAddress
     * @param gateway
     * @param netmask
     * @param gatewayOwnerId
     * @return
     * @throws InsufficientCapacityException
     * @throws ConcurrentOperationException
     * @throws ResourceAllocationException
     */
    public PrivateGateway createVpcPrivateGateway(long vpcId, Long physicalNetworkId, String vlan, String ipAddress,
            String gateway, String netmask, long gatewayOwnerId) throws ResourceAllocationException,
            ConcurrentOperationException, InsufficientCapacityException;

    /**
     * @param gatewayId
     * @param destroyOnFailure TODO
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    public PrivateGateway applyVpcPrivateGateway(long gatewayId, boolean destroyOnFailure) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param id
     * @return
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    boolean deleteVpcPrivateGateway(long gatewayId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param listPrivateGatewaysCmd
     * @return
     */
    public Pair<List<PrivateGateway>, Integer> listPrivateGateway(ListPrivateGatewaysCmd listPrivateGatewaysCmd);

    /**
     * @param routeId
     * @return
     */
    StaticRoute getStaticRoute(long routeId);

    /**
     * @param vpcId
     * @return
     * @throws ResourceUnavailableException
     */
    public boolean applyStaticRoutes(long vpcId) throws ResourceUnavailableException;

    /**
     * @param routeId
     * @return TODO
     * @throws ResourceUnavailableException
     */
    public boolean revokeStaticRoute(long routeId) throws ResourceUnavailableException;

    /**
     * @param gatewayId
     * @param cidr
     * @return
     */
    public StaticRoute createStaticRoute(long gatewayId, String cidr) throws NetworkRuleConflictException;

    /**
     * @param listStaticRoutesCmd
     * @return
     */
    public Pair<List<? extends StaticRoute>, Integer> listStaticRoutes(ListStaticRoutesCmd cmd);

    /**
     * @param id
     * @return
     */
    VpcGateway getVpcGateway(long id);

    /**
     * @param ipId
     * @param vpcId
     * @return
     * @throws ResourceAllocationException
     * @throws ResourceUnavailableException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    IpAddress associateIPToVpc(long ipId, long vpcId) throws ResourceAllocationException, ResourceUnavailableException,
        InsufficientAddressCapacityException, ConcurrentOperationException;

    public Network updateVpcGuestNetwork(long networkId, String name, String displayText, Account callerAccount,
            User callerUser, String domainSuffix, Long ntwkOffId, Boolean changeCidr);
}
