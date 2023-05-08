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

public enum ApiCommandResourceType {
    None(null),
    VirtualMachine(com.cloud.vm.VirtualMachine.class),
    DomainRouter(com.cloud.network.router.VirtualRouter.class),
    Volume(com.cloud.storage.Volume.class),
    ConsoleProxy(com.cloud.vm.VirtualMachine.class),
    Snapshot(com.cloud.storage.Snapshot.class),
    Backup(org.apache.cloudstack.backup.Backup.class),
    Template(com.cloud.template.VirtualMachineTemplate.class),
    Iso(com.cloud.template.VirtualMachineTemplate.class),
    SystemVm(com.cloud.vm.VirtualMachine.class),
    Host(com.cloud.host.Host.class),
    StoragePool(com.cloud.storage.StoragePool.class),
    ImageStore(com.cloud.storage.ImageStore.class),
    IpAddress(com.cloud.network.IpAddress.class),
    PortableIpAddress(PortableIp.class),
    SecurityGroup(com.cloud.network.security.SecurityGroup.class),
    PhysicalNetwork(com.cloud.network.PhysicalNetwork.class),
    TrafficType(com.cloud.network.PhysicalNetworkTrafficType.class),
    PhysicalNetworkServiceProvider(com.cloud.network.PhysicalNetworkServiceProvider.class),
    FirewallRule(com.cloud.network.rules.FirewallRule.class),
    Account(com.cloud.user.Account.class),
    User(com.cloud.user.User.class),
    PrivateGateway(com.cloud.network.vpc.PrivateGateway.class),
    StaticRoute(com.cloud.network.vpc.StaticRoute.class),
    Counter(com.cloud.network.as.Counter.class),
    Condition(com.cloud.network.as.Condition.class),
    AutoScalePolicy(com.cloud.network.as.AutoScalePolicy.class),
    AutoScaleVmProfile(com.cloud.network.as.AutoScaleVmProfile.class),
    AutoScaleVmGroup(com.cloud.network.as.AutoScaleVmGroup.class),
    GlobalLoadBalancerRule(com.cloud.region.ha.GlobalLoadBalancerRule.class),
    LoadBalancerRule(com.cloud.network.lb.LoadBalancingRule.class),
    AffinityGroup(org.apache.cloudstack.affinity.AffinityGroup.class),
    InternalLbVm(com.cloud.network.router.VirtualRouter.class),
    DedicatedGuestVlanRange(com.cloud.network.GuestVlan.class),
    GuestOs(com.cloud.storage.GuestOS.class),
    GuestOsMapping(com.cloud.storage.GuestOSHypervisor.class),
    Network(com.cloud.network.Network.class),
    NetworkAcl(com.cloud.network.vpc.NetworkACL.class),
    NetworkAclItem(com.cloud.network.vpc.NetworkACLItem.class),
    Project(com.cloud.projects.Project.class),
    Domain(com.cloud.domain.Domain.class),
    DiskOffering(com.cloud.offering.DiskOffering.class),
    ServiceOffering(com.cloud.offering.ServiceOffering.class),
    NetworkOffering(com.cloud.offering.NetworkOffering.class),
    VpcOffering(com.cloud.network.vpc.VpcOffering.class),
    BackupOffering(org.apache.cloudstack.backup.BackupOffering.class),
    Zone(com.cloud.dc.DataCenter.class),
    Vpc(com.cloud.network.vpc.Vpc.class),
    Cluster(com.cloud.org.Cluster.class),
    Pod(com.cloud.dc.Pod.class),
    VmSnapshot(com.cloud.vm.snapshot.VMSnapshot.class),
    Role(org.apache.cloudstack.acl.Role.class),
    VpnCustomerGateway(com.cloud.network.Site2SiteCustomerGateway.class),
    ManagementServer(org.apache.cloudstack.management.ManagementServerHost.class);

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
