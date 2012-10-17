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
package com.cloud.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.alert.AlertManagerImpl;
import com.cloud.alert.dao.AlertDaoImpl;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.configuration.dao.ConfigurationDaoImpl;
import com.cloud.configuration.dao.ResourceCountDaoImpl;
import com.cloud.configuration.dao.ResourceLimitDaoImpl;
import com.cloud.dao.EntityManager;
import com.cloud.dao.EntityManagerImpl;
import com.cloud.dc.dao.AccountVlanMapDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.VlanDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.network.MockNetworkManagerImpl;
import com.cloud.network.dao.FirewallRulesCidrsDaoImpl;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.LoadBalancerDaoImpl;
import com.cloud.network.dao.NetworkDaoImpl;
import com.cloud.network.dao.NetworkDomainDaoImpl;
import com.cloud.network.dao.NetworkRuleConfigDaoImpl;
import com.cloud.network.dao.RemoteAccessVpnDaoImpl;
import com.cloud.network.dao.Site2SiteCustomerGatewayDaoImpl;
import com.cloud.network.dao.Site2SiteVpnGatewayDaoImpl;
import com.cloud.network.dao.VpnUserDaoImpl;
import com.cloud.network.security.MockSecurityGroupManagerImpl;
import com.cloud.network.security.dao.SecurityGroupDaoImpl;
import com.cloud.network.vpc.dao.VpcDaoImpl;
import com.cloud.network.vpn.MockRemoteAccessVpnManagerImpl;
import com.cloud.offerings.dao.NetworkOfferingDaoImpl;
import com.cloud.projects.MockProjectManagerImpl;
import com.cloud.projects.dao.ProjectAccountDaoImpl;
import com.cloud.projects.dao.ProjectDaoImpl;
import com.cloud.region.RegionManagerImpl;
import com.cloud.region.dao.RegionDaoImpl;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.storage.MockStorageManagerImpl;
import com.cloud.storage.dao.DiskOfferingDaoImpl;
import com.cloud.storage.dao.GuestOSCategoryDaoImpl;
import com.cloud.storage.dao.GuestOSDaoImpl;
import com.cloud.storage.dao.LaunchPermissionDaoImpl;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.dao.StoragePoolDaoImpl;
import com.cloud.storage.dao.UploadDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateHostDaoImpl;
import com.cloud.storage.dao.VMTemplateSwiftDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.storage.dao.VolumeHostDaoImpl;
import com.cloud.storage.snapshot.MockSnapshotManagerImpl;
import com.cloud.template.MockTemplateManagerImpl;
import com.cloud.user.AccountDetailsDaoImpl;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.DomainManagerImpl;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.SSHKeyPairDaoImpl;
import com.cloud.user.dao.UserAccountDaoImpl;
import com.cloud.user.dao.UserDaoImpl;
import com.cloud.user.dao.UserStatisticsDaoImpl;
import com.cloud.utils.component.Adapter;
import com.cloud.utils.component.ComponentLibrary;
import com.cloud.utils.component.ComponentLibraryBase;
import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.GenericDao;
import com.cloud.uuididentity.dao.IdentityDaoImpl;
import com.cloud.vm.MockUserVmManagerImpl;
import com.cloud.vm.MockVirtualMachineManagerImpl;
import com.cloud.vm.dao.ConsoleProxyDaoImpl;
import com.cloud.vm.dao.DomainRouterDaoImpl;
import com.cloud.vm.dao.InstanceGroupDaoImpl;
import com.cloud.vm.dao.UserVmDaoImpl;
import com.cloud.vm.dao.UserVmDetailsDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import com.cloud.vpc.MockConfigurationManagerImpl;
import com.cloud.vpc.MockResourceLimitManagerImpl;
import com.cloud.vpc.MockSite2SiteVpnManagerImpl;
import com.cloud.vpc.MockVpcManagerImpl;


public class RegionsComponentLibrary extends ComponentLibraryBase implements ComponentLibrary {
    protected void populateDaos() {
        addDao("DomainDao", DomainDaoImpl.class);
        addDao("AccountDao", AccountDaoImpl.class);
        addDao("UserDao", UserDaoImpl.class);
        addDao("UserAccountDao", UserAccountDaoImpl.class);
        addDao("NetworkOfferingDao", NetworkOfferingDaoImpl.class);
        addDao("RegionDao", RegionDaoImpl.class);
        addDao("IdentityDao", IdentityDaoImpl.class);
        addDao("AccountVlanMapDao", AccountVlanMapDaoImpl.class);
        addDao("CapacityDao", CapacityDaoImpl.class);
        addDao("ClusterDao", ClusterDaoImpl.class);
        addDao("ServiceOfferingDao", ServiceOfferingDaoImpl.class);
        addDao("DiskOfferingDao", DiskOfferingDaoImpl.class);
        addDao("DomainRouterDao", DomainRouterDaoImpl.class);
        addDao("GuestOSDao", GuestOSDaoImpl.class);
        addDao("GuestOSCategoryDao", GuestOSCategoryDaoImpl.class);
        addDao("HostDao", HostDaoImpl.class);
        addDao("IPAddressDao", IPAddressDaoImpl.class);
        addDao("LoadBalancerDao", LoadBalancerDaoImpl.class);
        addDao("NetworkRuleConfigDao", NetworkRuleConfigDaoImpl.class);
        addDao("HostPodDao", HostPodDaoImpl.class);
        addDao("SnapshotDao", SnapshotDaoImpl.class);
        addDao("StoragePoolDao", StoragePoolDaoImpl.class);
        addDao("ConfigurationDao", ConfigurationDaoImpl.class);
        addDao("DataCenterDao", DataCenterDaoImpl.class);
        addDao("VMTemplateZoneDao", VMTemplateZoneDaoImpl.class);
        addDao("VMTemplateDetailsDao", VMTemplateDetailsDaoImpl.class);
        addDao("VMTemplateDao", VMTemplateDaoImpl.class);
        addDao("VMTemplateHostDao", VMTemplateHostDaoImpl.class);
        addDao("VMTemplateSwiftDao", VMTemplateSwiftDaoImpl.class);
        addDao("UploadDao", UploadDaoImpl.class);
        addDao("UserDao", UserDaoImpl.class);
        addDao("UserStatisticsDao", UserStatisticsDaoImpl.class);
        addDao("UserVmDao", UserVmDaoImpl.class);
        addDao("VlanDao", VlanDaoImpl.class);
        addDao("VolumeDao", VolumeDaoImpl.class);
        addDao("Site2SiteVpnGatewayDao", Site2SiteVpnGatewayDaoImpl.class);
        addDao("Site2SiteCustomerGatewayDao", Site2SiteCustomerGatewayDaoImpl.class);
        addDao("VolumeHostDao", VolumeHostDaoImpl.class);
        addDao("SecurityGroupDao", SecurityGroupDaoImpl.class);
        addDao("NetworkConfigurationDao", NetworkDaoImpl.class);
        addDao("ConsoleProxyDao", ConsoleProxyDaoImpl.class);
        addDao("FirewallRulesCidrsDao", FirewallRulesCidrsDaoImpl.class);
        addDao("VMInstanceDao", VMInstanceDaoImpl.class);
        addDao("AccountDetailsDao", AccountDetailsDaoImpl.class);
        addDao("NetworkDomainDao", NetworkDomainDaoImpl.class);
        addDao("SSHKeyPairDao", SSHKeyPairDaoImpl.class);
        addDao("UserVmDetailsDao", UserVmDetailsDaoImpl.class);
        addDao("ResourceCountDao", ResourceCountDaoImpl.class);
        addDao("InstanceGroupDao", InstanceGroupDaoImpl.class);
        addDao("RemoteAccessVpnDao", RemoteAccessVpnDaoImpl.class);
        addDao("VpnUserDao", VpnUserDaoImpl.class);
        addDao("ProjectDao", ProjectDaoImpl.class);
        addDao("ProjectAccountDao", ProjectAccountDaoImpl.class);
        addDao("LaunchPermissionDao", LaunchPermissionDaoImpl.class);
    }

    @Override
    public synchronized Map<String, ComponentInfo<GenericDao<?, ?>>> getDaos() {
        if (_daos.size() == 0) {
            populateDaos();
        }
        return _daos;
    }

    protected void populateManagers() {
        addManager("configuration manager", MockConfigurationManagerImpl.class);
        addManager("account manager", AccountManagerImpl.class);
        addManager("domain manager", DomainManagerImpl.class);
        addManager("Region Manager", RegionManagerImpl.class);
        addManager("ResourceLimit Manager", MockResourceLimitManagerImpl.class);
        addManager("Network Manager", MockNetworkManagerImpl.class);
        addManager("UserVm Manager", MockUserVmManagerImpl.class);
        addManager("Vm Manager", MockVirtualMachineManagerImpl.class);
        addManager("Project Manager", MockProjectManagerImpl.class);
        addManager("Vpc Manager", MockVpcManagerImpl.class);
        addManager("Site2SiteVpn Manager", MockSite2SiteVpnManagerImpl.class);
        addManager("SecurityGroup Manager", MockSecurityGroupManagerImpl.class);
        addManager("Snapshot Manager", MockSnapshotManagerImpl.class);
        addManager("Template Manager", MockTemplateManagerImpl.class);
        addManager("Storage Manager", MockStorageManagerImpl.class);
        addManager("RemoteAccessVpn Manager", MockRemoteAccessVpnManagerImpl.class);
        addManager("Entity Manager", EntityManagerImpl.class);
    }

    @Override
    public synchronized Map<String, ComponentInfo<Manager>> getManagers() {
        if (_managers.size() == 0) {
            populateManagers();
        }
        return _managers;
    }

    protected void populateAdapters() {
    }

    @Override
    public synchronized Map<String, List<ComponentInfo<Adapter>>> getAdapters() {
        if (_adapters.size() == 0) {
            populateAdapters();
        }
        return _adapters;
    }

    @Override
    public synchronized Map<Class<?>, Class<?>> getFactories() {
        HashMap<Class<?>, Class<?>> factories = new HashMap<Class<?>, Class<?>>();
        factories.put(EntityManager.class, EntityManagerImpl.class);
        return factories;
    }

    protected void populateServices() {
    }

    @Override
    public synchronized Map<String, ComponentInfo<PluggableService>> getPluggableServices() {
        if (_pluggableServices.size() == 0) {
            populateServices();
        }
        return _pluggableServices;
    }
}
