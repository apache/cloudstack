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
package org.apache.cloudstack.api;

import org.apache.cloudstack.region.PortableIp;
import org.apache.commons.lang3.StringUtils;

import com.cloud.network.GuestVlan;
import com.cloud.network.Networks;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.vm.VirtualMachine;

public enum ApiCommandJobType {
    None,
    VirtualMachine,
    DomainRouter,
    Volume,
    ConsoleProxy,
    Snapshot,
    Backup,
    Template,
    Iso,
    SystemVm,
    Host,
    StoragePool,
    ImageStore,
    IpAddress,
    PortableIpAddress,
    SecurityGroup,
    PhysicalNetwork,
    TrafficType,
    PhysicalNetworkServiceProvider,
    FirewallRule,
    Account,
    User,
    PrivateGateway,
    StaticRoute,
    Counter,
    Condition,
    AutoScalePolicy,
    AutoScaleVmProfile,
    AutoScaleVmGroup,
    GlobalLoadBalancerRule,
    LoadBalancerRule,
    AffinityGroup,
    InternalLbVm,
    DedicatedGuestVlanRange,
    GuestOs,
    GuestOsMapping,
    Network,
    Management,
    KubernetesCluster;

    public static Class<?> getTypeClass(ApiCommandJobType type) {
        switch (type) {
            case VirtualMachine:
                return VirtualMachine.class;
            case DomainRouter:
            case InternalLbVm:
                return VirtualRouter.class;
            case Volume:
                return com.cloud.storage.Volume.class;
            case ConsoleProxy:
                return com.cloud.vm.ConsoleProxy.class;
            case Snapshot:
                return com.cloud.storage.Snapshot.class;
            case Backup:
                return org.apache.cloudstack.backup.Backup.class;
            case Template:
            case Iso:
                return VirtualMachineTemplate.class;
            case SystemVm:
                return com.cloud.vm.SystemVm.class;
            case Host:
                return com.cloud.host.Host.class;
            case StoragePool:
                return com.cloud.storage.StoragePool.class;
            case ImageStore:
                return com.cloud.storage.ImageStore.class;
            case IpAddress:
                return com.cloud.network.IpAddress.class;
            case PortableIpAddress:
                return PortableIp.class;
            case SecurityGroup:
                return com.cloud.network.security.SecurityGroup.class;
            case PhysicalNetwork:
                return com.cloud.network.PhysicalNetwork.class;
            case TrafficType:
                return Networks.TrafficType.class;
            case PhysicalNetworkServiceProvider:
                return com.cloud.network.PhysicalNetworkServiceProvider.class;
            case FirewallRule:
                return com.cloud.network.rules.FirewallRule.class;
            case Account:
                return com.cloud.user.Account.class;
            case User:
                return com.cloud.user.User.class;
            case PrivateGateway:
                return com.cloud.network.vpc.PrivateGateway.class;
            case StaticRoute:
                return com.cloud.network.vpc.StaticRoute.class;
            case Counter:
                return com.cloud.network.as.Counter.class;
            case Condition:
                return com.cloud.network.as.Condition.class;
            case AutoScalePolicy:
                return com.cloud.network.as.AutoScalePolicy.class;
            case AutoScaleVmProfile:
                return com.cloud.network.as.AutoScaleVmProfile.class;
            case AutoScaleVmGroup:
                return com.cloud.network.as.AutoScaleVmGroup.class;
            case GlobalLoadBalancerRule:
                return com.cloud.region.ha.GlobalLoadBalancerRule.class;
            case LoadBalancerRule:
                return LoadBalancingRule.class;
            case AffinityGroup:
                return org.apache.cloudstack.affinity.AffinityGroup.class;
            case DedicatedGuestVlanRange:
                return GuestVlan.class;
            case GuestOs:
                return GuestOS.class;
            case GuestOsMapping:
                return GuestOSHypervisor.class;
            case Network:
                return com.cloud.network.Network.class;
        }
        return null;
    }

    public static Class<?> getTypeClass(String type) {
        if (StringUtils.isEmpty(type)) {
            return null;
        }
        return getTypeClass(valueOf(type));
    }
}
