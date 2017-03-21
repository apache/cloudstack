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
package org.apache.cloudstack.network.lb;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineProfile.Param;

public interface VpcInlineLoadBalancerVmManager extends PluggableService {
    static final ConfigKey<String> LB_SERVICE_OFFERING = new ConfigKey<String>("Advanced", String.class, "vpcinlinelbvm.service.offering", null,
            "Uuid of the service offering used by vpc inline lb vm; if NULL - default system vpc inline lb offering will be used", false, ConfigKey.Scope.Global);

    //RAM/CPU for the system offering used by VpcInline LB VMs
    public static final int DEFAULT_LB_VM_RAMSIZE = 128;            // 128 MB
    public static final int DEFAULT_LB_VM_CPU_MHZ = 256;            // 256 MHz

    boolean destroyVpcInlineLbVm(long vmId, Account caller, Long callerUserId)
            throws ResourceUnavailableException, ConcurrentOperationException;

    List<DomainRouterVO> deployVpcInlineLbVm(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;

    boolean applyLoadBalancingRules(Network network, List<LoadBalancingRule> rules, DomainRouterVO vpcInlineLbVm, Map<Ip, String> publicIpGuestIpMapping)
            throws ResourceUnavailableException;

    List<DomainRouterVO> deployVpcInlineLbVm(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params, Collection<IPAddressVO> lbIps)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;

    String createSecondaryIpMapping(Network guestNetwork, DeployDestination dest, Account owner, Ip requestedPublicIp, VirtualRouter vpcInlineLbVm)
            throws ResourceUnavailableException, InsufficientAddressCapacityException;

    boolean removeSecondaryIpMapping(Network guestNetwork, Account owner, Ip requestedPublicIp, VirtualRouter vpcInlineLbVm) throws ResourceUnavailableException;

    List<? extends VirtualRouter> findVpcInlineLbVms(Network guestNetworkId, Ip requestedGuestIp);

    boolean cleanupUnusedVpcInlineLbVms(long guestNetworkId, Account caller, Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException;

    void fillPublicIps(ListResponse<DomainRouterResponse> vpcInlineLbVms);

    VirtualRouter startVpcInlineLbVm(long vpcInlineLbVmId, Account caller, long callerUserId)
            throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;

    VirtualRouter stopVpcInlineLbVm(long vmId, boolean forced, Account caller, long callerUserId)
            throws ConcurrentOperationException, ResourceUnavailableException;
}
