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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.region.PortableIp;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenter;
import com.cloud.network.GuestVlan;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

public enum ApiCommandResourceType {
    None(null),
    Account(com.cloud.user.Account.class),
    AffinityGroup(org.apache.cloudstack.affinity.AffinityGroup.class),
    AutoScalePolicy(com.cloud.network.as.AutoScalePolicy.class),
    AutoScaleVmGroup(com.cloud.network.as.AutoScaleVmGroup.class),
    AutoScaleVmProfile(com.cloud.network.as.AutoScaleVmProfile.class),
    Backup(org.apache.cloudstack.backup.Backup.class),
    BackupOffering(org.apache.cloudstack.backup.BackupOffering.class),
    Cluster(com.cloud.org.Cluster.class),
    Condition(com.cloud.network.as.Condition.class),
    ConsoleProxy(VirtualMachine.class),
    Counter(com.cloud.network.as.Counter.class),
    DedicatedGuestVlanRange(GuestVlan.class),
    DiskOffering(com.cloud.offering.DiskOffering.class),
    Domain(com.cloud.domain.Domain.class),
    DomainRouter(VirtualRouter.class),
    FirewallRule(com.cloud.network.rules.FirewallRule.class),
    GlobalLoadBalancerRule(com.cloud.region.ha.GlobalLoadBalancerRule.class),
    GuestOs(GuestOS.class),
    GuestOsMapping(GuestOSHypervisor.class),
    Host(com.cloud.host.Host.class),
    ImageStore(com.cloud.storage.ImageStore.class),
    InternalLbVm(VirtualRouter.class),
    IpAddress(com.cloud.network.IpAddress.class),
    Iso(VirtualMachineTemplate.class),
    LoadBalancerRule(LoadBalancingRule.class),
    Network(com.cloud.network.Network.class),
    NetworkAcl(NetworkACL.class),
    NetworkAclItem(NetworkACLItem.class),
    NetworkOffering(com.cloud.offering.NetworkOffering.class),
    PhysicalNetwork(com.cloud.network.PhysicalNetwork.class),
    PhysicalNetworkServiceProvider(com.cloud.network.PhysicalNetworkServiceProvider.class),
    Pod(com.cloud.dc.Pod.class),
    PortableIpAddress(PortableIp.class),
    PrivateGateway(com.cloud.network.vpc.PrivateGateway.class),
    Project(com.cloud.projects.Project.class),
    SecurityGroup(com.cloud.network.security.SecurityGroup.class),
    ServiceOffering(com.cloud.offering.ServiceOffering.class),
    Snapshot(com.cloud.storage.Snapshot.class),
    StaticRoute(com.cloud.network.vpc.StaticRoute.class),
    StoragePool(com.cloud.storage.StoragePool.class),
    SystemVm(VirtualMachine.class),
    Template(VirtualMachineTemplate.class),
    TrafficType(com.cloud.network.PhysicalNetworkTrafficType.class),
    User(com.cloud.user.User.class),
    VirtualMachine(VirtualMachine.class),
    VmSnapshot(VMSnapshot.class),
    Volume(com.cloud.storage.Volume.class),
    Vpc(com.cloud.network.vpc.Vpc.class),
    VpcOffering(com.cloud.network.vpc.VpcOffering.class),
    Zone(DataCenter.class);

    private final Class<?> clazz;

    private ApiCommandResourceType(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getAssociatedClass() {
        return this.clazz;
    }

    public static List<ApiCommandResourceType> valuesFromAssociatedClass(Class<?> clazz) {
        List<ApiCommandResourceType> types = new ArrayList<>();
        for (ApiCommandResourceType type : ApiCommandResourceType.values()) {
            if (type.getAssociatedClass() == clazz) {
                types.add(type);
            }
        }
        return types;
    }

    public static ApiCommandResourceType valueFromAssociatedClass(Class<?> clazz) {
        List<ApiCommandResourceType> types = valuesFromAssociatedClass(clazz);
        return CollectionUtils.isEmpty(types) ? null : types.get(0);
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static ApiCommandResourceType fromString(String value) {
        if (StringUtils.isNotEmpty(value) && EnumUtils.isValidEnum(ApiCommandResourceType.class, value)) {
            return valueOf(value);
        }
        return null;
    }
}
