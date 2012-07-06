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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.vpc.VpcOffering.State;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;


public interface VpcManager extends VpcService{

    /**
     * @param name
     * @param displayText
     * @param svcProviderMap
     * @param isDefault
     * @param state TODO
     * @return
     */
    VpcOffering createVpcOffering(String name, String displayText, Map<Service, Set<Provider>> svcProviderMap, 
            boolean isDefault, State state);

    /**
     * @param vpcOffId
     * @param services
     * @return
     */
    boolean areServicesSupportedByVpcOffering(long vpcOffId, Service[] services);

    /**
     * @param zoneId
     * @param vpcOffId
     * @param vpcOwner
     * @param vpcName
     * @param displayText
     * @param cidr
     * @param networkDomain TODO
     * @return
     */
    Vpc createVpc(long zoneId, long vpcOffId, Account vpcOwner, String vpcName, String displayText, String cidr, String networkDomain);
    
    List<Service> getSupportedServices();

    /**
     * @param guestNtwkOff
     * @param cidr
     * @param networkDomain
     * @param networkOwner
     * @param vpc TODO
     * @param networkId TODO
     * @param gateway TODO
     * @return
     */
    void validateGuestNtkwForVpc(NetworkOffering guestNtwkOff, String cidr, String networkDomain, Account networkOwner, 
            Vpc vpc, Long networkId, String gateway);

    /**
     * @return
     */
    VpcProvider getVpcElement();
    
    List<? extends Vpc> getVpcsForAccount(long accountId);

    /**
     * @param vpc
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean destroyVpc(Vpc vpc) throws ConcurrentOperationException, ResourceUnavailableException;

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
}
