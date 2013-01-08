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

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.addr.PublicIp;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;


public interface VpcManager extends VpcService{

    /**
     * @param ntwkOffId
     * @param cidr
     * @param networkDomain
     * @param networkOwner
     * @param vpc TODO
     * @param networkId TODO
     * @param gateway TODO
     * @return
     */
    void validateNtkwOffForVpc(long ntwkOffId, String cidr, String networkDomain, Account networkOwner, 
            Vpc vpc, Long networkId, String gateway);

    
    List<? extends Vpc> getVpcsForAccount(long accountId);

    /**
     * @param vpc
     * @param caller TODO
     * @param callerUserId TODO
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean destroyVpc(Vpc vpc, Account caller, Long callerUserId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * @param vpcId
     * @return
     */
    List<DomainRouterVO> getVpcRouters(long vpcId);

    /**
     * @param zoneId
     * @return
     */
    boolean vpcProviderEnabledInZone(long zoneId);

    /**
     * @param vpcId
     * @return
     */
    VpcGateway getPrivateGatewayForVpc(long vpcId);


    /**
     * @param ip
     * @return
     */
    boolean ipUsedInVpc(IpAddress ip);


    /**
     * @param ipId
     * @param networkId
     */
    void unassignIPFromVpcNetwork(long ipId, long networkId);


    /**
     * @param ntwkOffId
     * @param name
     * @param displayText
     * @param gateway
     * @param cidr
     * @param vlanId
     * @param networkDomain
     * @param owner
     * @param domainId
     * @param pNtwk
     * @param zoneId
     * @param aclType
     * @param subdomainAccess
     * @param vpcId
     * @param caller
     * @return
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceAllocationException
     */
    Network createVpcGuestNetwork(long ntwkOffId, String name, String displayText, String gateway, String cidr, 
            String vlanId, String networkDomain, Account owner, Long domainId, PhysicalNetwork pNtwk, long zoneId,
            ACLType aclType, Boolean subdomainAccess, long vpcId, Account caller) 
                    throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException;


    /**
     * @param owner
     * @param vpc
     * @return
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    PublicIp assignSourceNatIpAddressToVpc(Account owner, Vpc vpc) throws InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * @param guestNtwkOff
     * @param supportedSvcs TODO
     */
    void validateNtwkOffForVpc(NetworkOffering guestNtwkOff, List<Service> supportedSvcs);


    /**
     * @return
     */
    List<HypervisorType> getSupportedVpcHypervisors();

}
