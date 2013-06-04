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

import org.apache.cloudstack.acl.ControlledEntity.ACLType;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.addr.PublicIp;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;


public interface VpcManager extends VpcService{

    /**
     * Returns all existing VPCs for a given account
     * @param accountId
     * @return
     */
    List<? extends Vpc> getVpcsForAccount(long accountId);

    
    /**
     * Destroys the VPC
     * 
     * @param vpc
     * @param caller TODO
     * @param callerUserId TODO
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean destroyVpc(Vpc vpc, Account caller, Long callerUserId) throws ConcurrentOperationException, ResourceUnavailableException;


    /**
     * Returns true if the IP is allocated to the VPC; false otherwise
     * 
     * @param ip
     * @return
     */
    boolean isIpAllocatedToVpc(IpAddress ip);


    /**
     * Disassociates the public IP address from VPC
     * 
     * @param ipId
     * @param networkId
     */
    void unassignIPFromVpcNetwork(long ipId, long networkId);


    /**
     * Creates guest network in the VPC
     * 
     *
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
     * @param displayNetworkEnabled
     * @return
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceAllocationException
     */
    Network createVpcGuestNetwork(long ntwkOffId, String name, String displayText, String gateway, String cidr,
                                  String vlanId, String networkDomain, Account owner, Long domainId, PhysicalNetwork pNtwk, long zoneId,
                                  ACLType aclType, Boolean subdomainAccess, long vpcId, Long aclId, Account caller, Boolean displayNetworkEnabled)

                    throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException;


    /**
     * Assigns source nat public IP address to VPC
     * 
     * @param owner
     * @param vpc
     * @return public IP address object
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    PublicIp assignSourceNatIpAddressToVpc(Account owner, Vpc vpc) throws InsufficientAddressCapacityException, ConcurrentOperationException;


    /**
     * Validates network offering to find if it can be used for network creation in VPC
     * 
     * @param guestNtwkOff
     * @param supportedSvcs TODO
     */
    void validateNtwkOffForVpc(NetworkOffering guestNtwkOff, List<Service> supportedSvcs);


    /**
     * @return list of hypervisors that are supported by VPC
     */
    List<HypervisorType> getSupportedVpcHypervisors();
    
    
    /**
     * Lists all the services and providers that the current VPC suppots
     * @param vpcOffId
     * @return map of Service to Provider(s) map 
     */
    Map<Service, Set<Provider>> getVpcOffSvcProvidersMap(long vpcOffId);
    
    
    /**
     * Returns VPC that is ready to be used
     * @param vpcId
     * @return VPC object
     */
    public Vpc getActiveVpc(long vpcId);


    /**
     * Performs network offering validation to determine if it can be used for network upgrade inside the VPC 
     * @param networkId
     * @param newNtwkOffId
     * @param newCidr
     * @param newNetworkDomain
     * @param vpc
     * @param gateway
     * @param networkOwner TODO
     */
    void validateNtwkOffForNtwkInVpc(Long networkId, long newNtwkOffId, String newCidr, String newNetworkDomain, Vpc vpc, String gateway, Account networkOwner, Long aclId);

    List<PrivateGateway> getVpcPrivateGateways(long vpcId);
}
